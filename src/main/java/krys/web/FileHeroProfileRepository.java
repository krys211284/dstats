package krys.web;

import krys.hero.HeroClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Proste plikowe repozytorium bohaterów użytkownika. */
public final class FileHeroProfileRepository implements HeroProfileRepository {
    private static final String HERO_PREFIX = "HERO";
    private static final String ACTIVE_PREFIX = "ACTIVE";

    private final Path heroesFilePath;
    private final Path activeHeroFilePath;

    public FileHeroProfileRepository(Path dataDirectory) {
        this.heroesFilePath = dataDirectory.resolve("heroes.db");
        this.activeHeroFilePath = dataDirectory.resolve("active-hero.db");
    }

    @Override
    public synchronized HeroProfile save(HeroProfile heroProfile) {
        List<HeroProfile> heroes = loadHeroes();
        heroes.removeIf(existingHero -> existingHero.getHeroId() == heroProfile.getHeroId());
        heroes.add(heroProfile);
        heroes.sort(Comparator.comparingLong(HeroProfile::getHeroId));
        writeHeroes(heroes);
        return heroProfile;
    }

    @Override
    public synchronized List<HeroProfile> findAll() {
        return List.copyOf(loadHeroes());
    }

    @Override
    public synchronized Optional<HeroProfile> findById(long heroId) {
        return loadHeroes().stream()
                .filter(hero -> hero.getHeroId() == heroId)
                .findFirst();
    }

    @Override
    public synchronized void delete(long heroId) {
        List<HeroProfile> heroes = loadHeroes();
        heroes.removeIf(hero -> hero.getHeroId() == heroId);
        writeHeroes(heroes);
    }

    @Override
    public synchronized Optional<Long> loadActiveHeroId() {
        if (!Files.exists(activeHeroFilePath)) {
            return Optional.empty();
        }
        try {
            List<String> lines = Files.readAllLines(activeHeroFilePath, StandardCharsets.UTF_8);
            if (lines.isEmpty()) {
                return Optional.empty();
            }
            String[] tokens = lines.getFirst().split("\\|", -1);
            if (tokens.length != 2 || !ACTIVE_PREFIX.equals(tokens[0])) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(tokens[1]));
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się odczytać aktywnego bohatera.", exception);
        }
    }

    @Override
    public synchronized void saveActiveHeroId(Long heroId) {
        ensureDirectoryExists();
        try {
            if (heroId == null) {
                Files.deleteIfExists(activeHeroFilePath);
                return;
            }
            Files.writeString(activeHeroFilePath, ACTIVE_PREFIX + "|" + heroId, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się zapisać aktywnego bohatera.", exception);
        }
    }

    private List<HeroProfile> loadHeroes() {
        if (!Files.exists(heroesFilePath)) {
            return new ArrayList<>();
        }
        try {
            List<HeroProfile> heroes = new ArrayList<>();
            for (String line : Files.readAllLines(heroesFilePath, StandardCharsets.UTF_8)) {
                String trimmedLine = line.trim();
                if (trimmedLine.isBlank()) {
                    continue;
                }
                heroes.add(parseHero(trimmedLine));
            }
            heroes.sort(Comparator.comparingLong(HeroProfile::getHeroId));
            return heroes;
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się odczytać listy bohaterów.", exception);
        }
    }

    private HeroProfile parseHero(String line) {
        String[] tokens = line.split("\\|", -1);
        if ((tokens.length != 6 && tokens.length != 7) || !HERO_PREFIX.equals(tokens[0])) {
            throw new IllegalStateException("Plik bohaterów ma niepoprawny format.");
        }
        String encodedSkillLoadout = tokens.length == 7 ? tokens[6] : "";
        HeroSkillLoadout skillLoadout = encodedSkillLoadout.isBlank()
                ? HeroSkillLoadout.fromCurrentBuildFormData(CurrentBuildFormQuerySupport.fromSerializedQuery(decode(tokens[4])))
                : decodeSkillLoadout(encodedSkillLoadout);
        return new HeroProfile(
                Long.parseLong(tokens[1]),
                decode(tokens[2]),
                HeroClass.valueOf(tokens[3]),
                decode(tokens[4]),
                decodeSelection(tokens[5]),
                skillLoadout
        );
    }

    private void writeHeroes(List<HeroProfile> heroes) {
        ensureDirectoryExists();
        List<String> lines = new ArrayList<>();
        for (HeroProfile hero : heroes) {
            lines.add(String.join("|",
                    HERO_PREFIX,
                    Long.toString(hero.getHeroId()),
                    encode(hero.getName()),
                    hero.getHeroClass().name(),
                    encode(hero.getCurrentBuildQuery()),
                    encodeSelection(hero.getItemSelection()),
                    encodeSkillLoadout(hero.getSkillLoadout())
            ));
        }
        try {
            Files.write(heroesFilePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się zapisać listy bohaterów.", exception);
        }
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(heroesFilePath.getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się przygotować katalogu danych bohaterów.", exception);
        }
    }

    private static String encodeSelection(HeroItemSelection selection) {
        StringBuilder serialized = new StringBuilder();
        boolean first = true;
        for (java.util.Map.Entry<krys.item.HeroEquipmentSlot, Long> entry : selection.getSelectedItemIdsBySlot().entrySet()) {
            if (!first) {
                serialized.append(",");
            }
            serialized.append(entry.getKey().name()).append(":").append(entry.getValue());
            first = false;
        }
        return encode(serialized.toString());
    }

    private static HeroItemSelection decodeSelection(String serializedSelection) {
        String decoded = decode(serializedSelection);
        if (decoded.isBlank()) {
            return HeroItemSelection.empty();
        }
        java.util.EnumMap<krys.item.HeroEquipmentSlot, Long> selection = new java.util.EnumMap<>(krys.item.HeroEquipmentSlot.class);
        for (String token : decoded.split(",")) {
            if (token.isBlank()) {
                continue;
            }
            String[] parts = token.split(":", -1);
            if (parts.length != 2) {
                continue;
            }
            selection.put(krys.item.HeroEquipmentSlot.valueOf(parts[0]), Long.parseLong(parts[1]));
        }
        return new HeroItemSelection(selection);
    }

    private static String encodeSkillLoadout(HeroSkillLoadout skillLoadout) {
        List<String> skillEntries = new ArrayList<>();
        for (HeroAssignedSkill assignedSkill : skillLoadout.getAssignedSkills().values()) {
            skillEntries.add(String.join(":",
                    assignedSkill.getSkillId().name(),
                    Integer.toString(assignedSkill.getRank()),
                    Boolean.toString(assignedSkill.isBaseUpgrade()),
                    assignedSkill.getChoiceUpgrade().name()
            ));
        }
        String actionBar = skillLoadout.getActionBarSkills().stream()
                .map(Enum::name)
                .reduce((left, right) -> left + "," + right)
                .orElse("");
        return encode(String.join(";", String.join(",", skillEntries), actionBar));
    }

    private static HeroSkillLoadout decodeSkillLoadout(String serializedSkillLoadout) {
        String decoded = decode(serializedSkillLoadout);
        if (decoded.isBlank()) {
            return HeroSkillLoadout.foundationDefault();
        }
        String[] parts = decoded.split(";", -1);
        EnumMap<krys.skill.SkillId, HeroAssignedSkill> assignedSkills = new EnumMap<>(krys.skill.SkillId.class);
        if (parts.length >= 1 && !parts[0].isBlank()) {
            for (String token : parts[0].split(",")) {
                if (token.isBlank()) {
                    continue;
                }
                String[] skillParts = token.split(":", -1);
                if (skillParts.length != 4) {
                    continue;
                }
                krys.skill.SkillId skillId = krys.skill.SkillId.valueOf(skillParts[0]);
                assignedSkills.put(skillId, new HeroAssignedSkill(
                        skillId,
                        Integer.parseInt(skillParts[1]),
                        Boolean.parseBoolean(skillParts[2]),
                        krys.skill.SkillUpgradeChoice.valueOf(skillParts[3])
                ));
            }
        }
        List<krys.skill.SkillId> actionBarSkills = new ArrayList<>();
        if (parts.length >= 2 && !parts[1].isBlank()) {
            for (String rawSkillId : parts[1].split(",")) {
                if (rawSkillId.isBlank()) {
                    continue;
                }
                actionBarSkills.add(krys.skill.SkillId.valueOf(rawSkillId));
            }
        }
        return new HeroSkillLoadout(assignedSkills, actionBarSkills);
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}

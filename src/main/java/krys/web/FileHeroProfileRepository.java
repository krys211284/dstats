package krys.web;

import krys.hero.HeroClass;
import krys.runtime.RuntimeSkillLoadout;
import krys.runtime.legacy.LegacyPaladinRuntimeSkillLoadoutAdapter;
import krys.skill.SkillState;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
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
        try {
            HeroClass heroClass = HeroClass.valueOf(tokens[3]);
            String query = decode(tokens[4]);
            String payload = tokens.length == 7 ? decode(tokens[6]) : "";
            RuntimeSkillLoadout skillLoadout;
            if (payload.startsWith("runtime-")) {
                skillLoadout = RuntimeSkillLoadoutCodec.decode(payload);
            } else {
                if (heroClass != HeroClass.PALADIN) {
                    throw new IllegalArgumentException("Historyczny format bohatera obsługuje wyłącznie Paladyna.");
                }
                skillLoadout = payload.isEmpty()
                        ? LegacyPaladinHeroSkillLoadoutAdapter.toRuntimeLoadout(HeroSkillLoadout.fromCurrentBuildFormData(
                                CurrentBuildFormQuerySupport.fromSerializedQuery(query)))
                        : decodeLegacySkillLoadout(payload);
            }
            return new HeroProfile(Long.parseLong(tokens[1]), decode(tokens[2]), heroClass, query,
                    decodeSelection(tokens[5]), skillLoadout);
        } catch (IllegalArgumentException | UnsupportedOperationException exception) {
            throw new IllegalStateException("Niepoprawne dane bohatera: " + exception.getMessage(), exception);
        }
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
                    encode(RuntimeSkillLoadoutCodec.encode(hero.getRuntimeSkillLoadout()))
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
        if (decoded.isEmpty()) {
            return HeroItemSelection.empty();
        }
        java.util.EnumMap<krys.item.HeroEquipmentSlot, Long> selection = new java.util.EnumMap<>(krys.item.HeroEquipmentSlot.class);
        for (String token : decoded.split(",", -1)) {
            String[] parts = token.split(":", -1);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Niepoprawny wpis selekcji itemów bohatera.");
            }
            if (selection.putIfAbsent(krys.item.HeroEquipmentSlot.valueOf(parts[0]), Long.parseLong(parts[1])) != null) {
                throw new IllegalArgumentException("Powtórzony slot itemów bohatera.");
            }
        }
        return new HeroItemSelection(selection);
    }

    private static RuntimeSkillLoadout decodeLegacySkillLoadout(String decoded) {
        String[] parts = decoded.split("~", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Niepoprawny historyczny skill payload.");
        }
        EnumMap<krys.skill.SkillId, SkillState> assignedSkills = new EnumMap<>(krys.skill.SkillId.class);
        if (!parts[0].isEmpty()) {
            for (String token : parts[0].split(";", -1)) {
                String[] skillParts = token.split(":", -1);
                if (skillParts.length != 4 && skillParts.length != 5 && skillParts.length != 8) {
                    throw new IllegalArgumentException("Niepoprawny wpis historycznej konfiguracji skilla.");
                }
                if (!"true".equals(skillParts[2]) && !"false".equals(skillParts[2])) {
                    throw new IllegalArgumentException("Niepoprawna wartość baseUpgrade w historycznej konfiguracji.");
                }
                krys.skill.SkillId skillId = krys.skill.SkillId.valueOf(skillParts[0]);
                krys.skill.SkillRuntimeModifierChoice runtimeModifierChoice = skillParts.length == 5
                        ? krys.skill.SkillRuntimeModifierChoice.valueOf(skillParts[4])
                        : krys.skill.SkillRuntimeModifierChoice.NONE;
                String choiceGroup1 = krys.skill.SkillState.NO_TREE_CHOICE;
                String choiceGroup2 = krys.skill.SkillState.NO_TREE_CHOICE;
                String choiceGroup3 = krys.skill.SkillState.NO_TREE_CHOICE;
                if (skillParts.length == 8) {
                    runtimeModifierChoice = krys.skill.SkillRuntimeModifierChoice.valueOf(skillParts[4]);
                    choiceGroup1 = skillParts[5];
                    choiceGroup2 = skillParts[6];
                    choiceGroup3 = skillParts[7];
                } else if (skillId == krys.skill.SkillId.CLASH) {
                    if (runtimeModifierChoice == krys.skill.SkillRuntimeModifierChoice.ANIMUS) {
                        choiceGroup1 = krys.skill.SkillState.CLASH_ANIMUS_CHOICE;
                    }
                    if (krys.skill.SkillUpgradeChoice.LEFT.name().equals(skillParts[3])) {
                        choiceGroup3 = krys.skill.SkillState.CLASH_PUNISHMENT_CHOICE;
                    }
                }
                if (assignedSkills.putIfAbsent(skillId, new SkillState(
                        skillId,
                        Integer.parseInt(skillParts[1]),
                        Boolean.parseBoolean(skillParts[2]),
                        krys.skill.SkillUpgradeChoice.valueOf(skillParts[3]),
                        runtimeModifierChoice,
                        choiceGroup1,
                        choiceGroup2,
                        choiceGroup3
                )) != null) {
                    throw new IllegalArgumentException("Powtórzony skill w historycznej konfiguracji.");
                }
            }
        }
        List<krys.skill.SkillId> actionBarSkills = new ArrayList<>();
        if (!parts[1].isEmpty()) {
            for (String rawSkillId : parts[1].split(",", -1)) {
                actionBarSkills.add(krys.skill.SkillId.valueOf(rawSkillId));
            }
        }
        return LegacyPaladinRuntimeSkillLoadoutAdapter.toRuntimeLoadout(assignedSkills, actionBarSkills);
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

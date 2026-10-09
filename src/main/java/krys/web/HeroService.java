package krys.web;

import krys.hero.HeroClass;
import krys.item.HeroEquipmentSlot;
import krys.runtime.RuntimeSkillLoadout;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Warstwa aplikacyjna zarządzająca wieloma bohaterami i aktywnym bohaterem. */
public final class HeroService {
    private final HeroProfileRepository repository;

    public HeroService(HeroProfileRepository repository) {
        this.repository = repository;
    }

    public List<HeroProfile> getHeroes() {
        return repository.findAll().stream()
                .sorted(Comparator.comparingLong(HeroProfile::getHeroId))
                .toList();
    }

    public Optional<HeroProfile> getActiveHero() {
        Optional<Long> activeHeroId = repository.loadActiveHeroId();
        if (activeHeroId.isEmpty()) {
            return Optional.empty();
        }
        return repository.findById(activeHeroId.get());
    }

    public HeroProfile createHero(String heroName, HeroClass heroClass, int level) {
        validateDetails(heroName, level);
        if (heroClass == null) {
            throw new IllegalArgumentException("Klasa bohatera jest wymagana.");
        }
        long nextHeroId = repository.findAll().stream()
                .mapToLong(HeroProfile::getHeroId)
                .max()
                .orElse(0L) + 1L;
        CurrentBuildFormData buildFormData = CurrentBuildFormQuerySupport.withHeroLevel(CurrentBuildFormData.defaultValues(), level);
        RuntimeSkillLoadout loadout = heroClass == HeroClass.PALADIN
                ? LegacyPaladinHeroSkillLoadoutAdapter.toRuntimeLoadout(HeroSkillLoadout.fromCurrentBuildFormData(buildFormData))
                : new RuntimeSkillLoadout(heroClass.getTechnicalId(), Map.of(), List.of());
        HeroProfile hero = repository.save(new HeroProfile(nextHeroId, heroName.trim(), heroClass,
                CurrentBuildFormQuerySupport.toQuery(buildFormData), HeroItemSelection.empty(), loadout));
        if (repository.loadActiveHeroId().isEmpty()) {
            repository.saveActiveHeroId(hero.getHeroId());
        }
        return hero;
    }

    private static void validateDetails(String heroName, int level) {
        if (heroName == null || heroName.isBlank()) {
            throw new IllegalArgumentException("Nazwa bohatera jest wymagana.");
        }
        validateLevel(level);
    }

    private static void validateLevel(int level) {
        if (level < HeroSkillPointBudget.MIN_HERO_LEVEL || level > HeroSkillPointBudget.MAX_HERO_LEVEL) {
            throw new IllegalArgumentException("Poziom bohatera musi być w zakresie 1..70.");
        }
    }

    public HeroProfile updateHeroDetails(long heroId, String name, int level) {
        validateDetails(name, level);
        HeroProfile hero = repository.findById(heroId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono bohatera o podanym id."));
        // Zmieniamy wyłącznie poziom, zachowując również pola query spoza aktualnego formularza.
        var fields = UrlEncodedFormSupport.parseQuery(hero.getCurrentBuildQuery());
        fields.put("level", Integer.toString(level));
        String query = fields.entrySet().stream().map(entry ->
                URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "="
                        + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
        return repository.save(hero.withDetails(name.trim(), query));
    }

    public void setActiveHero(long heroId) {
        repository.findById(heroId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono bohatera o podanym id."));
        repository.saveActiveHeroId(heroId);
    }

    public void deleteHero(long heroId) {
        repository.findById(heroId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono bohatera o podanym id."));
        repository.delete(heroId);
        Optional<Long> activeHeroId = repository.loadActiveHeroId();
        if (activeHeroId.isPresent() && activeHeroId.get() == heroId) {
            List<HeroProfile> remainingHeroes = getHeroes();
            repository.saveActiveHeroId(remainingHeroes.isEmpty() ? null : remainingHeroes.getFirst().getHeroId());
        }
    }

    public HeroProfile requireActiveHero() {
        return getActiveHero().orElseThrow(() -> new IllegalArgumentException("Brak aktywnego bohatera. Utwórz albo wybierz bohatera."));
    }

    public void updateActiveHeroBuildFormData(CurrentBuildFormData formData) {
        HeroProfile activeHero = requireActiveHero();
        CurrentBuildRuntimeSupport.requireLegacyPaladin(activeHero);
        HeroSkillLoadout updatedSkillLoadout = activeHero.getSkillLoadout().withAppliedFormData(formData);
        CurrentBuildFormData normalizedFormData = updatedSkillLoadout.applyToFormData(formData);
        repository.save(activeHero.withCurrentBuildState(normalizedFormData, updatedSkillLoadout));
    }

    public void saveActiveHeroState(CurrentBuildFormData formData, HeroSkillLoadout skillLoadout) {
        HeroProfile activeHero = requireActiveHero();
        CurrentBuildRuntimeSupport.requireLegacyPaladin(activeHero);
        repository.save(activeHero.withCurrentBuildState(formData, skillLoadout));
    }

    public void updateActiveHeroLevel(int level) {
        validateLevel(level);
        HeroProfile activeHero = requireActiveHero();
        updateHeroDetails(activeHero.getHeroId(), activeHero.getName(), level);
    }

    public void saveActiveHeroRuntimeState(String query, RuntimeSkillLoadout loadout) {
        repository.save(requireActiveHero().withCurrentBuildRuntimeState(query, loadout));
    }

    /** Jeden zapis po walidacji całego formularza, również jego wyborów ekwipunku. */
    public void saveActiveHeroRuntimeState(String query, RuntimeSkillLoadout loadout, HeroItemSelection selection) {
        repository.save(requireActiveHero().withCurrentBuildRuntimeState(query, loadout).withItemSelection(selection));
    }

    public void replaceActiveHeroItemSelection(HeroItemSelection selection) {
        HeroProfile activeHero = requireActiveHero();
        repository.save(activeHero.withItemSelection(selection));
    }

    public void setActiveHeroItem(HeroEquipmentSlot heroSlot, long itemId) {
        HeroProfile activeHero = requireActiveHero();
        repository.save(activeHero.withItemSelection(activeHero.getItemSelection().withSelectedItem(heroSlot, itemId)));
    }

    public void clearActiveHeroItem(HeroEquipmentSlot heroSlot) {
        HeroProfile activeHero = requireActiveHero();
        repository.save(activeHero.withItemSelection(activeHero.getItemSelection().withoutSlot(heroSlot)));
    }

    public void clearItemFromAllHeroes(long itemId) {
        List<HeroProfile> heroes = new ArrayList<>(repository.findAll());
        for (HeroProfile hero : heroes) {
            HeroItemSelection updatedSelection = hero.getItemSelection().withoutItemId(itemId);
            if (!updatedSelection.getSelectedItemIdsBySlot().equals(hero.getItemSelection().getSelectedItemIdsBySlot())) {
                repository.save(hero.withItemSelection(updatedSelection));
            }
        }
    }

    public void addSkillToActiveHero(krys.skill.SkillId skillId) {
        HeroProfile activeHero = requireActiveHero();
        CurrentBuildRuntimeSupport.requireLegacyPaladin(activeHero);
        HeroSkillLoadout updatedSkillLoadout = activeHero.getSkillLoadout().withAssignedSkill(skillId);
        CurrentBuildFormData normalizedFormData = updatedSkillLoadout.applyToFormData(activeHero.getCurrentBuildFormData());
        repository.save(activeHero.withCurrentBuildState(normalizedFormData, updatedSkillLoadout));
    }

    public void removeSkillFromActiveHero(krys.skill.SkillId skillId) {
        HeroProfile activeHero = requireActiveHero();
        CurrentBuildRuntimeSupport.requireLegacyPaladin(activeHero);
        HeroSkillLoadout updatedSkillLoadout = activeHero.getSkillLoadout().withoutAssignedSkill(skillId);
        CurrentBuildFormData normalizedFormData = updatedSkillLoadout.applyToFormData(activeHero.getCurrentBuildFormData());
        repository.save(activeHero.withCurrentBuildState(normalizedFormData, updatedSkillLoadout));
    }
}

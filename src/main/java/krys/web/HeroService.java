package krys.web;

import krys.hero.HeroClass;
import krys.item.HeroEquipmentSlot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
        if (heroName == null || heroName.isBlank()) {
            throw new IllegalArgumentException("Nazwa bohatera jest wymagana.");
        }
        if (level <= 0) {
            throw new IllegalArgumentException("Poziom bohatera musi być dodatni.");
        }
        long nextHeroId = repository.findAll().stream()
                .mapToLong(HeroProfile::getHeroId)
                .max()
                .orElse(0L) + 1L;
        CurrentBuildFormData buildFormData = CurrentBuildFormQuerySupport.withHeroLevel(CurrentBuildFormData.defaultValues(), level);
        HeroProfile hero = repository.save(new HeroProfile(
                nextHeroId,
                heroName.trim(),
                heroClass,
                CurrentBuildFormQuerySupport.toQuery(buildFormData),
                HeroItemSelection.empty(),
                HeroSkillLoadout.fromCurrentBuildFormData(buildFormData)
        ));
        if (repository.loadActiveHeroId().isEmpty()) {
            repository.saveActiveHeroId(hero.getHeroId());
        }
        return hero;
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
        HeroSkillLoadout updatedSkillLoadout = activeHero.getSkillLoadout().withAppliedFormData(formData);
        CurrentBuildFormData normalizedFormData = updatedSkillLoadout.applyToFormData(formData);
        repository.save(activeHero.withCurrentBuildState(normalizedFormData, updatedSkillLoadout));
    }

    public void saveActiveHeroState(CurrentBuildFormData formData, HeroSkillLoadout skillLoadout) {
        HeroProfile activeHero = requireActiveHero();
        repository.save(activeHero.withCurrentBuildState(formData, skillLoadout));
    }

    public void updateActiveHeroLevel(int level) {
        if (level <= 0) {
            throw new IllegalArgumentException("Poziom bohatera musi być dodatni.");
        }
        HeroProfile activeHero = requireActiveHero();
        CurrentBuildFormData updatedFormData = CurrentBuildFormQuerySupport.withHeroLevel(activeHero.getCurrentBuildFormData(), level);
        repository.save(activeHero.withCurrentBuildState(updatedFormData, activeHero.getSkillLoadout()));
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
        HeroSkillLoadout updatedSkillLoadout = activeHero.getSkillLoadout().withAssignedSkill(skillId);
        CurrentBuildFormData normalizedFormData = updatedSkillLoadout.applyToFormData(activeHero.getCurrentBuildFormData());
        repository.save(activeHero.withCurrentBuildState(normalizedFormData, updatedSkillLoadout));
    }

    public void removeSkillFromActiveHero(krys.skill.SkillId skillId) {
        HeroProfile activeHero = requireActiveHero();
        HeroSkillLoadout updatedSkillLoadout = activeHero.getSkillLoadout().withoutAssignedSkill(skillId);
        CurrentBuildFormData normalizedFormData = updatedSkillLoadout.applyToFormData(activeHero.getCurrentBuildFormData());
        repository.save(activeHero.withCurrentBuildState(normalizedFormData, updatedSkillLoadout));
    }
}

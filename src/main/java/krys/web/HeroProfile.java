package krys.web;

import krys.hero.HeroClass;

/** Trwale zapisywany bohater aplikacji wraz z własnym kontekstem buildu i selekcją slotów. */
public final class HeroProfile {
    private final long heroId;
    private final String name;
    private final HeroClass heroClass;
    private final String currentBuildQuery;
    private final HeroItemSelection itemSelection;
    private final HeroSkillLoadout skillLoadout;

    public HeroProfile(long heroId,
                       String name,
                       HeroClass heroClass,
                       String currentBuildQuery,
                       HeroItemSelection itemSelection) {
        this(heroId, name, heroClass, currentBuildQuery, itemSelection, HeroSkillLoadout.foundationDefault());
    }

    public HeroProfile(long heroId,
                       String name,
                       HeroClass heroClass,
                       String currentBuildQuery,
                       HeroItemSelection itemSelection,
                       HeroSkillLoadout skillLoadout) {
        if (heroId <= 0L) {
            throw new IllegalArgumentException("Id bohatera musi być dodatnie.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Nazwa bohatera jest wymagana.");
        }
        if (heroClass == null) {
            throw new IllegalArgumentException("Klasa bohatera jest wymagana.");
        }
        if (currentBuildQuery == null || currentBuildQuery.isBlank()) {
            throw new IllegalArgumentException("Kontekst buildu bohatera jest wymagany.");
        }
        this.heroId = heroId;
        this.name = name;
        this.heroClass = heroClass;
        this.currentBuildQuery = currentBuildQuery;
        this.itemSelection = itemSelection == null ? HeroItemSelection.empty() : itemSelection;
        this.skillLoadout = skillLoadout == null ? HeroSkillLoadout.foundationDefault() : skillLoadout;
    }

    public long getHeroId() {
        return heroId;
    }

    public String getName() {
        return name;
    }

    public HeroClass getHeroClass() {
        return heroClass;
    }

    public String getCurrentBuildQuery() {
        return currentBuildQuery;
    }

    public HeroItemSelection getItemSelection() {
        return itemSelection;
    }

    public HeroSkillLoadout getSkillLoadout() {
        return skillLoadout;
    }

    public CurrentBuildFormData getCurrentBuildFormData() {
        CurrentBuildFormData baseFormData = CurrentBuildFormQuerySupport.fromSerializedQuery(currentBuildQuery);
        return skillLoadout.applyToFormData(baseFormData);
    }

    public HeroProfile withCurrentBuildQuery(String updatedQuery) {
        return new HeroProfile(heroId, name, heroClass, updatedQuery, itemSelection, skillLoadout);
    }

    public HeroProfile withItemSelection(HeroItemSelection updatedSelection) {
        return new HeroProfile(heroId, name, heroClass, currentBuildQuery, updatedSelection, skillLoadout);
    }

    public HeroProfile withSkillLoadout(HeroSkillLoadout updatedSkillLoadout) {
        return new HeroProfile(heroId, name, heroClass, currentBuildQuery, itemSelection, updatedSkillLoadout);
    }

    public HeroProfile withCurrentBuildState(CurrentBuildFormData formData, HeroSkillLoadout updatedSkillLoadout) {
        return new HeroProfile(
                heroId,
                name,
                heroClass,
                CurrentBuildFormQuerySupport.toQuery(formData),
                itemSelection,
                updatedSkillLoadout
        );
    }
}

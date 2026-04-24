package krys.web;

import krys.hero.HeroClass;

/** Trwale zapisywany bohater aplikacji wraz z własnym kontekstem buildu i selekcją slotów. */
public final class HeroProfile {
    private final long heroId;
    private final String name;
    private final HeroClass heroClass;
    private final String currentBuildQuery;
    private final HeroItemSelection itemSelection;

    public HeroProfile(long heroId,
                       String name,
                       HeroClass heroClass,
                       String currentBuildQuery,
                       HeroItemSelection itemSelection) {
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

    public CurrentBuildFormData getCurrentBuildFormData() {
        return CurrentBuildFormQuerySupport.fromSerializedQuery(currentBuildQuery);
    }

    public HeroProfile withCurrentBuildQuery(String updatedQuery) {
        return new HeroProfile(heroId, name, heroClass, updatedQuery, itemSelection);
    }

    public HeroProfile withItemSelection(HeroItemSelection updatedSelection) {
        return new HeroProfile(heroId, name, heroClass, currentBuildQuery, updatedSelection);
    }
}

package krys.web;

import krys.hero.HeroClass;
import krys.runtime.RuntimeSkillLoadout;

import java.util.Objects;

/** Trwale zapisywany bohater aplikacji wraz z własnym kontekstem buildu i selekcją slotów. */
public final class HeroProfile {
    private final long heroId;
    private final String name;
    private final HeroClass heroClass;
    private final String currentBuildQuery;
    private final HeroItemSelection itemSelection;
    private final RuntimeSkillLoadout runtimeSkillLoadout;

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
        this(heroId, name, heroClass, currentBuildQuery, itemSelection, legacyLoadout(heroClass, skillLoadout));
    }

    public HeroProfile(long heroId,
                       String name,
                       HeroClass heroClass,
                       String currentBuildQuery,
                       HeroItemSelection itemSelection,
                       RuntimeSkillLoadout runtimeSkillLoadout) {
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
        this.runtimeSkillLoadout = Objects.requireNonNull(runtimeSkillLoadout, "runtimeSkillLoadout");
        if (!heroClass.getTechnicalId().equals(runtimeSkillLoadout.classId())) {
            throw new IllegalArgumentException("Klasa bohatera musi odpowiadać RuntimeSkillLoadout.classId.");
        }
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
        return LegacyPaladinHeroSkillLoadoutAdapter.toLegacyLoadout(runtimeSkillLoadout);
    }

    public RuntimeSkillLoadout getRuntimeSkillLoadout() {
        return runtimeSkillLoadout;
    }

    public CurrentBuildFormData getCurrentBuildFormData() {
        CurrentBuildFormData baseFormData = CurrentBuildFormQuerySupport.fromSerializedQuery(currentBuildQuery);
        return heroClass == HeroClass.PALADIN ? getSkillLoadout().applyToFormData(baseFormData) : baseFormData;
    }

    public HeroProfile withCurrentBuildQuery(String updatedQuery) {
        return new HeroProfile(heroId, name, heroClass, updatedQuery, itemSelection, runtimeSkillLoadout);
    }

    public HeroProfile withItemSelection(HeroItemSelection updatedSelection) {
        return new HeroProfile(heroId, name, heroClass, currentBuildQuery, updatedSelection, runtimeSkillLoadout);
    }

    public HeroProfile withSkillLoadout(HeroSkillLoadout updatedSkillLoadout) {
        return new HeroProfile(heroId, name, heroClass, currentBuildQuery, itemSelection, updatedSkillLoadout);
    }

    public HeroProfile withRuntimeSkillLoadout(RuntimeSkillLoadout loadout) {
        return new HeroProfile(heroId, name, heroClass, currentBuildQuery, itemSelection, loadout);
    }

    public HeroProfile withCurrentBuildRuntimeState(String query, RuntimeSkillLoadout loadout) {
        return new HeroProfile(heroId, name, heroClass, query, itemSelection, loadout);
    }

    public HeroProfile withDetails(String updatedName, String updatedQuery) {
        return new HeroProfile(heroId, updatedName, heroClass, updatedQuery, itemSelection, runtimeSkillLoadout);
    }

    private static RuntimeSkillLoadout legacyLoadout(HeroClass heroClass, HeroSkillLoadout loadout) {
        if (heroClass == null) {
            throw new IllegalArgumentException("Klasa bohatera jest wymagana.");
        }
        if (heroClass != HeroClass.PALADIN) {
            throw new UnsupportedOperationException("Legacy Paladin view jest dostępny wyłącznie dla Paladyna.");
        }
        return LegacyPaladinHeroSkillLoadoutAdapter.toRuntimeLoadout(loadout == null ? HeroSkillLoadout.foundationDefault() : loadout);
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

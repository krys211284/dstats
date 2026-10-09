package krys.web;

import krys.app.CurrentBuildCalculation;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.CurrentHeroActiveItemStats;
import krys.itemlibrary.EffectiveCurrentBuildResolution;
import krys.itemlibrary.HeroSlotItemAssignment;
import krys.itemlibrary.SavedImportedItem;
import krys.necromancer.app.NecromancerCurrentBuildCalculation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import krys.skill.SkillId;

/** Model widoku pojedynczej strony M8 dla flow „Policz aktualny build”. */
public final class CurrentBuildPageModel {
    private final CurrentBuildFormData formData;
    private final List<SelectOption> skillOptions;
    private final List<SelectOption> rankOptions;
    private final List<SelectOption> choiceOptions;
    private final List<String> messages;
    private final List<String> validationErrors;
    private final CurrentBuildCalculation calculation;
    private final EffectiveCurrentBuildResolution effectiveCurrentBuildResolution;
    private final HeroProfile activeHero;
    private final List<HeroProfile> heroes;
    private final List<SavedImportedItem> savedLibraryItems;
    private final String itemLibraryUrl;
    private final String itemImportUrl;
    private final String heroesUrl;
    private final String choiceHelpText;
    private final HeroSkillLoadout renderedSkillLoadout;
    private final boolean currentBuildRuntimeSupported;
    private final CurrentBuildRuntimeMode runtimeMode;
    private final NecromancerCurrentBuildFormData necromancerFormData;
    private final NecromancerCurrentBuildCalculation necromancerCalculation;

    public CurrentBuildPageModel(CurrentBuildFormData formData,
                                 List<SelectOption> skillOptions,
                                 List<SelectOption> rankOptions,
                                 List<SelectOption> choiceOptions,
                                 List<String> messages,
                                 List<String> validationErrors,
                                 CurrentBuildCalculation calculation,
                                 EffectiveCurrentBuildResolution effectiveCurrentBuildResolution,
                                 HeroProfile activeHero,
                                 List<HeroProfile> heroes,
                                 List<SavedImportedItem> savedLibraryItems,
                                 String itemLibraryUrl,
                                 String itemImportUrl,
                                 String heroesUrl,
                                 String choiceHelpText) {
        this(formData, skillOptions, rankOptions, choiceOptions, messages, validationErrors, calculation,
                effectiveCurrentBuildResolution, activeHero, heroes, savedLibraryItems, itemLibraryUrl, itemImportUrl,
                heroesUrl, choiceHelpText, null, null);
    }

    private CurrentBuildPageModel(CurrentBuildFormData formData, List<SelectOption> skillOptions,
                                  List<SelectOption> rankOptions, List<SelectOption> choiceOptions,
                                  List<String> messages, List<String> validationErrors, CurrentBuildCalculation calculation,
                                  EffectiveCurrentBuildResolution effectiveCurrentBuildResolution,
                                  HeroProfile activeHero, List<HeroProfile> heroes, List<SavedImportedItem> savedLibraryItems,
                                  String itemLibraryUrl, String itemImportUrl, String heroesUrl, String choiceHelpText,
                                  NecromancerCurrentBuildFormData necromancerFormData,
                                  NecromancerCurrentBuildCalculation necromancerCalculation) {
        this.formData = formData;
        this.runtimeMode = CurrentBuildRuntimeMode.forHero(activeHero);
        this.necromancerFormData = necromancerFormData == null && runtimeMode == CurrentBuildRuntimeMode.NECROMANCER_DIRECT_HIT
                ? NecromancerCurrentBuildFormQuerySupport.fromSerializedQuery(activeHero.getCurrentBuildQuery(), activeHero.getRuntimeSkillLoadout())
                : necromancerFormData;
        this.necromancerCalculation = necromancerCalculation;
        this.currentBuildRuntimeSupported = activeHero != null && CurrentBuildRuntimeSupport.isSupported(activeHero.getHeroClass());
        this.skillOptions = Collections.unmodifiableList(new ArrayList<>(skillOptions));
        this.rankOptions = Collections.unmodifiableList(new ArrayList<>(rankOptions));
        this.choiceOptions = Collections.unmodifiableList(new ArrayList<>(choiceOptions));
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
        this.validationErrors = Collections.unmodifiableList(new ArrayList<>(validationErrors));
        this.calculation = calculation;
        this.effectiveCurrentBuildResolution = effectiveCurrentBuildResolution;
        this.activeHero = activeHero;
        this.heroes = Collections.unmodifiableList(new ArrayList<>(heroes));
        this.savedLibraryItems = Collections.unmodifiableList(new ArrayList<>(savedLibraryItems));
        this.itemLibraryUrl = itemLibraryUrl;
        this.itemImportUrl = itemImportUrl;
        this.heroesUrl = heroesUrl;
        this.choiceHelpText = choiceHelpText;
        this.renderedSkillLoadout = !currentBuildRuntimeSupported
                ? null
                : activeHero.getSkillLoadout().withAppliedFormData(formData);
    }

    public static CurrentBuildPageModel forNecromancer(NecromancerCurrentBuildFormData form, List<String> messages,
                                                       List<String> errors, NecromancerCurrentBuildCalculation calculation,
                                                       EffectiveCurrentBuildResolution resolution, HeroProfile hero,
                                                       List<HeroProfile> heroes, List<SavedImportedItem> items) {
        return new CurrentBuildPageModel(null, List.of(), List.of(), List.of(), messages, errors, null,
                resolution, hero, heroes, items, "/biblioteka-itemow", "/importuj-item-ze-screena", "/bohaterowie", "",
                form, calculation);
    }

    public CurrentBuildRuntimeMode getRuntimeMode() { return runtimeMode; }
    public NecromancerCurrentBuildFormData getNecromancerFormData() { return necromancerFormData; }
    public NecromancerCurrentBuildCalculation getNecromancerCalculation() { return necromancerCalculation; }

    public String getHeroLevel() {
        return runtimeMode == CurrentBuildRuntimeMode.NECROMANCER_DIRECT_HIT
                ? UrlEncodedFormSupport.parseQuery(activeHero.getCurrentBuildQuery()).getOrDefault("level", "70")
                : formData.getLevel();
    }

    public String getItemImportQuery() {
        return runtimeMode == CurrentBuildRuntimeMode.NECROMANCER_DIRECT_HIT
                ? NecromancerCurrentBuildFormQuerySupport.withFormData(activeHero.getCurrentBuildQuery(), necromancerFormData)
                : CurrentBuildFormQuerySupport.toQuery(formData);
    }

    public CurrentBuildFormData getFormData() {
        return formData;
    }

    public List<SelectOption> getSkillOptions() {
        return skillOptions;
    }

    public List<SelectOption> getRankOptions() {
        return rankOptions;
    }

    public List<SelectOption> getChoiceOptions() {
        return choiceOptions;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public List<String> getMessages() {
        return messages;
    }

    public CurrentBuildCalculation getCalculation() {
        return calculation;
    }

    public EffectiveCurrentBuildResolution getEffectiveCurrentBuildResolution() {
        return effectiveCurrentBuildResolution;
    }

    public HeroProfile getActiveHero() {
        return activeHero;
    }

    public List<SavedImportedItem> getSavedLibraryItems() {
        return savedLibraryItems;
    }

    public List<HeroProfile> getHeroes() {
        return heroes;
    }

    public String getItemLibraryUrl() {
        return itemLibraryUrl;
    }

    public String getItemImportUrl() {
        return itemImportUrl;
    }

    public String getHeroesUrl() {
        return heroesUrl;
    }

    public String getChoiceHelpText() {
        return choiceHelpText;
    }

    public boolean hasActiveHero() {
        return activeHero != null;
    }

    public boolean isCurrentBuildRuntimeSupported() {
        // Compatibility guard legacy formularza Paladyna; Necro korzysta z jawnego runtimeMode.
        return currentBuildRuntimeSupported;
    }

    public List<SkillId> getAssignedSkillIds() {
        if (renderedSkillLoadout == null) {
            return List.of();
        }
        return renderedSkillLoadout.getAssignedSkillIds();
    }

    public List<SkillId> getActionBarEligibleSkillIds() {
        if (renderedSkillLoadout == null) {
            return List.of();
        }
        return renderedSkillLoadout.getActionBarEligibleSkills();
    }

    public boolean hasResult() {
        return calculation != null || necromancerCalculation != null;
    }

    public boolean hasActiveLibraryItems() {
        return effectiveCurrentBuildResolution != null && !effectiveCurrentBuildResolution.getActiveItems().isEmpty();
    }

    public boolean hasLibraryItems() {
        return !savedLibraryItems.isEmpty();
    }

    public List<HeroSlotItemAssignment> getActiveLibraryItems() {
        if (effectiveCurrentBuildResolution == null) {
            return List.of();
        }
        return effectiveCurrentBuildResolution.getActiveItems();
    }

    public CurrentBuildImportableStats getActiveLibraryContribution() {
        if (effectiveCurrentBuildResolution == null) {
            return new CurrentBuildImportableStats(0L, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
        }
        return effectiveCurrentBuildResolution.getActiveItemsContribution();
    }

    public CurrentHeroActiveItemStats getActiveHeroItemStats() {
        if (effectiveCurrentBuildResolution == null) {
            return CurrentHeroActiveItemStats.empty();
        }
        return effectiveCurrentBuildResolution.getActiveHeroItemStats();
    }

    public CurrentBuildImportableStats getEffectiveStats() {
        if (effectiveCurrentBuildResolution == null) {
            return null;
        }
        return effectiveCurrentBuildResolution.getEffectiveStats();
    }

    public HeroSkillPointBudget getSkillPointBudget() {
        if (renderedSkillLoadout == null) {
            return null;
        }
        return HeroSkillPointBudget.from(formData, renderedSkillLoadout);
    }

    /** Najprostsza reprezentacja opcji selecta renderowanej po stronie serwera. */
    public static final class SelectOption {
        private final String value;
        private final String label;
        private final boolean selected;

        public SelectOption(String value, String label, boolean selected) {
            this.value = value;
            this.label = label;
            this.selected = selected;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }

        public boolean isSelected() {
            return selected;
        }
    }
}

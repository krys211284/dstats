package krys.web;

import krys.app.CurrentBuildCalculation;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.EffectiveCurrentBuildResolution;
import krys.itemlibrary.HeroSlotItemAssignment;
import krys.itemlibrary.SavedImportedItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private final List<SavedImportedItem> savedLibraryItems;
    private final String itemLibraryUrl;
    private final String itemImportUrl;
    private final String heroesUrl;
    private final String choiceHelpText;

    public CurrentBuildPageModel(CurrentBuildFormData formData,
                                 List<SelectOption> skillOptions,
                                 List<SelectOption> rankOptions,
                                 List<SelectOption> choiceOptions,
                                 List<String> messages,
                                 List<String> validationErrors,
                                 CurrentBuildCalculation calculation,
                                 EffectiveCurrentBuildResolution effectiveCurrentBuildResolution,
                                 HeroProfile activeHero,
                                 List<SavedImportedItem> savedLibraryItems,
                                 String itemLibraryUrl,
                                 String itemImportUrl,
                                 String heroesUrl,
                                 String choiceHelpText) {
        this.formData = formData;
        this.skillOptions = Collections.unmodifiableList(new ArrayList<>(skillOptions));
        this.rankOptions = Collections.unmodifiableList(new ArrayList<>(rankOptions));
        this.choiceOptions = Collections.unmodifiableList(new ArrayList<>(choiceOptions));
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
        this.validationErrors = Collections.unmodifiableList(new ArrayList<>(validationErrors));
        this.calculation = calculation;
        this.effectiveCurrentBuildResolution = effectiveCurrentBuildResolution;
        this.activeHero = activeHero;
        this.savedLibraryItems = Collections.unmodifiableList(new ArrayList<>(savedLibraryItems));
        this.itemLibraryUrl = itemLibraryUrl;
        this.itemImportUrl = itemImportUrl;
        this.heroesUrl = heroesUrl;
        this.choiceHelpText = choiceHelpText;
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

    public boolean hasResult() {
        return calculation != null;
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

    public CurrentBuildImportableStats getEffectiveStats() {
        if (effectiveCurrentBuildResolution == null) {
            return null;
        }
        return effectiveCurrentBuildResolution.getEffectiveStats();
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

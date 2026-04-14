package krys.web;

import krys.app.CurrentBuildCalculation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Model widoku pojedynczej strony M4 dla flow „Policz aktualny build”. */
public final class CurrentBuildPageModel {
    private final CurrentBuildFormData formData;
    private final List<SelectOption> skillOptions;
    private final List<SelectOption> rankOptions;
    private final List<SelectOption> choiceOptions;
    private final List<String> validationErrors;
    private final CurrentBuildCalculation calculation;
    private final String choiceHelpText;

    public CurrentBuildPageModel(CurrentBuildFormData formData,
                                 List<SelectOption> skillOptions,
                                 List<SelectOption> rankOptions,
                                 List<SelectOption> choiceOptions,
                                 List<String> validationErrors,
                                 CurrentBuildCalculation calculation,
                                 String choiceHelpText) {
        this.formData = formData;
        this.skillOptions = Collections.unmodifiableList(new ArrayList<>(skillOptions));
        this.rankOptions = Collections.unmodifiableList(new ArrayList<>(rankOptions));
        this.choiceOptions = Collections.unmodifiableList(new ArrayList<>(choiceOptions));
        this.validationErrors = Collections.unmodifiableList(new ArrayList<>(validationErrors));
        this.calculation = calculation;
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

    public CurrentBuildCalculation getCalculation() {
        return calculation;
    }

    public String getChoiceHelpText() {
        return choiceHelpText;
    }

    public boolean hasResult() {
        return calculation != null;
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

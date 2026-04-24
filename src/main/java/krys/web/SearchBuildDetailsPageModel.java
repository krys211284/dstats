package krys.web;

import krys.app.CurrentBuildCalculation;
import krys.search.BuildSearchCandidate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Model widoku szczegółów reprezentanta znormalizowanego wyniku searcha. */
public final class SearchBuildDetailsPageModel {
    private final int selectedRank;
    private final List<String> validationErrors;
    private final BuildSearchCandidate candidate;
    private final CurrentBuildCalculation calculation;
    private final HeroProfile activeHero;
    private final String helpText;

    public SearchBuildDetailsPageModel(int selectedRank,
                                       List<String> validationErrors,
                                       BuildSearchCandidate candidate,
                                       CurrentBuildCalculation calculation,
                                       HeroProfile activeHero,
                                       String helpText) {
        this.selectedRank = selectedRank;
        this.validationErrors = Collections.unmodifiableList(new ArrayList<>(validationErrors));
        this.candidate = candidate;
        this.calculation = calculation;
        this.activeHero = activeHero;
        this.helpText = helpText;
    }

    public int getSelectedRank() {
        return selectedRank;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public BuildSearchCandidate getCandidate() {
        return candidate;
    }

    public CurrentBuildCalculation getCalculation() {
        return calculation;
    }

    public HeroProfile getActiveHero() {
        return activeHero;
    }

    public String getHelpText() {
        return helpText;
    }

    public boolean hasCalculation() {
        return calculation != null;
    }
}

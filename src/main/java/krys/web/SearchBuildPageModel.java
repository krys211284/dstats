package krys.web;

import krys.search.BuildSearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Model widoku pojedynczej strony M12 dla flow „Znajdź najlepszy build”. */
public final class SearchBuildPageModel {
    private final SearchBuildFormData formData;
    private final List<String> validationErrors;
    private final BuildSearchResult result;
    private final String helpText;

    public SearchBuildPageModel(SearchBuildFormData formData,
                                List<String> validationErrors,
                                BuildSearchResult result,
                                String helpText) {
        this.formData = formData;
        this.validationErrors = Collections.unmodifiableList(new ArrayList<>(validationErrors));
        this.result = result;
        this.helpText = helpText;
    }

    public SearchBuildFormData getFormData() {
        return formData;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public BuildSearchResult getResult() {
        return result;
    }

    public String getHelpText() {
        return helpText;
    }

    public boolean hasResult() {
        return result != null;
    }
}

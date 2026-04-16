package krys.web;

import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ItemImageImportCandidateParseResult;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ValidatedImportedItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Model widoku SSR dla foundation importu pojedynczego itemu ze screena. */
public final class ItemImportPageModel {
    private final ItemImportEditableForm editableForm;
    private final ItemImageImportCandidateParseResult parseResult;
    private final List<String> validationErrors;
    private final ConfirmedImportView confirmedImportView;
    private final String helpText;

    public ItemImportPageModel(ItemImportEditableForm editableForm,
                               ItemImageImportCandidateParseResult parseResult,
                               List<String> validationErrors,
                               ConfirmedImportView confirmedImportView,
                               String helpText) {
        this.editableForm = editableForm;
        this.parseResult = parseResult;
        this.validationErrors = Collections.unmodifiableList(new ArrayList<>(validationErrors));
        this.confirmedImportView = confirmedImportView;
        this.helpText = helpText;
    }

    public ItemImportEditableForm getEditableForm() {
        return editableForm;
    }

    public ItemImageImportCandidateParseResult getParseResult() {
        return parseResult;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public ConfirmedImportView getConfirmedImportView() {
        return confirmedImportView;
    }

    public String getHelpText() {
        return helpText;
    }

    public boolean hasParseResult() {
        return parseResult != null && editableForm != null;
    }

    public boolean hasEditableForm() {
        return editableForm != null;
    }

    public boolean hasConfirmedImport() {
        return confirmedImportView != null;
    }

    /** Widok zatwierdzonego itemu i jego mapowania do aktualnego modelu buildu. */
    public static final class ConfirmedImportView {
        private final ValidatedImportedItem importedItem;
        private final krys.item.Item mappedItem;
        private final ImportedItemCurrentBuildContribution contribution;
        private final String currentBuildPrefillUrl;

        public ConfirmedImportView(ValidatedImportedItem importedItem,
                                   krys.item.Item mappedItem,
                                   ImportedItemCurrentBuildContribution contribution,
                                   String currentBuildPrefillUrl) {
            this.importedItem = importedItem;
            this.mappedItem = mappedItem;
            this.contribution = contribution;
            this.currentBuildPrefillUrl = currentBuildPrefillUrl;
        }

        public ValidatedImportedItem getImportedItem() {
            return importedItem;
        }

        public krys.item.Item getMappedItem() {
            return mappedItem;
        }

        public ImportedItemCurrentBuildContribution getContribution() {
            return contribution;
        }

        public String getCurrentBuildPrefillUrl() {
            return currentBuildPrefillUrl;
        }
    }
}

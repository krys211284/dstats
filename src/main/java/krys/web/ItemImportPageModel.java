package krys.web;

import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ItemImageImportCandidateParseResult;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ValidatedImportedItem;
import krys.itemlibrary.SavedImportedItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Model widoku SSR dla foundation importu pojedynczego itemu ze screena. */
public final class ItemImportPageModel {
    private final ItemImportEditableForm editableForm;
    private final ItemImageImportCandidateParseResult parseResult;
    private final List<String> validationErrors;
    private final ConfirmedImportView confirmedImportView;
    private final HeroProfile activeHero;
    private final String helpText;
    private final String currentBuildQuery;

    public ItemImportPageModel(ItemImportEditableForm editableForm,
                               ItemImageImportCandidateParseResult parseResult,
                               List<String> validationErrors,
                               ConfirmedImportView confirmedImportView,
                               HeroProfile activeHero,
                               String helpText,
                               String currentBuildQuery) {
        this.editableForm = editableForm;
        this.parseResult = parseResult;
        this.validationErrors = Collections.unmodifiableList(new ArrayList<>(validationErrors));
        this.confirmedImportView = confirmedImportView;
        this.activeHero = activeHero;
        this.helpText = helpText;
        this.currentBuildQuery = currentBuildQuery;
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

    public HeroProfile getActiveHero() {
        return activeHero;
    }

    public String getHelpText() {
        return helpText;
    }

    public String getCurrentBuildQuery() {
        return currentBuildQuery;
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

    public boolean hasActiveHero() {
        return activeHero != null;
    }

    /** Widok zatwierdzonego itemu i jego mapowania do aktualnego modelu buildu. */
    public static final class ConfirmedImportView {
        private final ValidatedImportedItem importedItem;
        private final SavedImportedItem savedItem;
        private final krys.item.Item mappedItem;
        private final ImportedItemCurrentBuildContribution contribution;

        public ConfirmedImportView(ValidatedImportedItem importedItem,
                                   SavedImportedItem savedItem,
                                   krys.item.Item mappedItem,
                                   ImportedItemCurrentBuildContribution contribution) {
            this.importedItem = importedItem;
            this.savedItem = savedItem;
            this.mappedItem = mappedItem;
            this.contribution = contribution;
        }

        public ValidatedImportedItem getImportedItem() {
            return importedItem;
        }

        public SavedImportedItem getSavedItem() {
            return savedItem;
        }

        public krys.item.Item getMappedItem() {
            return mappedItem;
        }

        public ImportedItemCurrentBuildContribution getContribution() {
            return contribution;
        }
    }
}

package krys.web;

import krys.itemlibrary.ActiveItemSelection;
import krys.itemlibrary.SavedImportedItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Model widoku SSR dla minimalnej biblioteki zapisanych itemów. */
public final class ItemLibraryPageModel {
    private final List<SavedImportedItem> savedItems;
    private final ActiveItemSelection activeSelection;
    private final List<String> errors;
    private final List<String> messages;
    private final String currentBuildQuery;

    public ItemLibraryPageModel(List<SavedImportedItem> savedItems,
                                ActiveItemSelection activeSelection,
                                List<String> errors,
                                List<String> messages,
                                String currentBuildQuery) {
        this.savedItems = Collections.unmodifiableList(new ArrayList<>(savedItems));
        this.activeSelection = activeSelection;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
        this.currentBuildQuery = currentBuildQuery;
    }

    public List<SavedImportedItem> getSavedItems() {
        return savedItems;
    }

    public ActiveItemSelection getActiveSelection() {
        return activeSelection;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getMessages() {
        return messages;
    }

    public String getCurrentBuildQuery() {
        return currentBuildQuery;
    }
}

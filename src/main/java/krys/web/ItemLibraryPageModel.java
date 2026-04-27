package krys.web;

import krys.itemlibrary.SavedImportedItem;
import krys.itemlibrary.ItemLibraryFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Model widoku SSR dla minimalnej biblioteki zapisanych itemów. */
public final class ItemLibraryPageModel {
    private final List<SavedImportedItem> savedItems;
    private final HeroProfile activeHero;
    private final HeroItemSelection activeSelection;
    private final List<String> errors;
    private final List<String> messages;
    private final String currentBuildQuery;
    private final SavedImportedItem savedItemFeedback;
    private final ItemLibraryFilter filter;
    private final List<String> availableItemTypes;
    private final int totalSavedItemCount;

    public ItemLibraryPageModel(List<SavedImportedItem> savedItems,
                                HeroProfile activeHero,
                                HeroItemSelection activeSelection,
                                List<String> errors,
                                List<String> messages,
                                String currentBuildQuery,
                                SavedImportedItem savedItemFeedback) {
        this.savedItems = Collections.unmodifiableList(new ArrayList<>(savedItems));
        this.activeHero = activeHero;
        this.activeSelection = activeSelection;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
        this.currentBuildQuery = currentBuildQuery;
        this.savedItemFeedback = savedItemFeedback;
        this.filter = ItemLibraryFilter.empty();
        this.availableItemTypes = List.of();
        this.totalSavedItemCount = this.savedItems.size();
    }

    public ItemLibraryPageModel(List<SavedImportedItem> savedItems,
                                HeroProfile activeHero,
                                HeroItemSelection activeSelection,
                                List<String> errors,
                                List<String> messages,
                                String currentBuildQuery,
                                SavedImportedItem savedItemFeedback,
                                ItemLibraryFilter filter,
                                List<String> availableItemTypes,
                                int totalSavedItemCount) {
        this.savedItems = Collections.unmodifiableList(new ArrayList<>(savedItems));
        this.activeHero = activeHero;
        this.activeSelection = activeSelection;
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
        this.currentBuildQuery = currentBuildQuery;
        this.savedItemFeedback = savedItemFeedback;
        this.filter = filter == null ? ItemLibraryFilter.empty() : filter;
        this.availableItemTypes = Collections.unmodifiableList(new ArrayList<>(availableItemTypes == null ? List.of() : availableItemTypes));
        this.totalSavedItemCount = Math.max(totalSavedItemCount, this.savedItems.size());
    }

    public List<SavedImportedItem> getSavedItems() {
        return savedItems;
    }

    public HeroProfile getActiveHero() {
        return activeHero;
    }

    public HeroItemSelection getActiveSelection() {
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

    public SavedImportedItem getSavedItemFeedback() {
        return savedItemFeedback;
    }

    public boolean hasSavedItemFeedback() {
        return savedItemFeedback != null;
    }

    public boolean hasActiveHero() {
        return activeHero != null;
    }

    public ItemLibraryFilter getFilter() {
        return filter;
    }

    public List<String> getAvailableItemTypes() {
        return availableItemTypes;
    }

    public int getTotalSavedItemCount() {
        return totalSavedItemCount;
    }
}

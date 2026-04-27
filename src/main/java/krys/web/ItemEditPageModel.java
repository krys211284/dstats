package krys.web;

import krys.itemimport.ItemImportEditableForm;
import krys.itemlibrary.ItemLibraryFilter;
import krys.itemlibrary.SavedImportedItem;

import java.util.List;

/** Model SSR formularza edycji zapisanego itemu z biblioteki. */
final class ItemEditPageModel {
    private final SavedImportedItem item;
    private final ItemImportEditableForm form;
    private final List<String> errors;
    private final List<String> messages;
    private final ItemLibraryFilter filter;

    ItemEditPageModel(SavedImportedItem item,
                      ItemImportEditableForm form,
                      List<String> errors,
                      List<String> messages,
                      ItemLibraryFilter filter) {
        this.item = item;
        this.form = form;
        this.errors = List.copyOf(errors == null ? List.of() : errors);
        this.messages = List.copyOf(messages == null ? List.of() : messages);
        this.filter = filter == null ? ItemLibraryFilter.empty() : filter;
    }

    SavedImportedItem getItem() {
        return item;
    }

    ItemImportEditableForm getForm() {
        return form;
    }

    List<String> getErrors() {
        return errors;
    }

    List<String> getMessages() {
        return messages;
    }

    ItemLibraryFilter getFilter() {
        return filter;
    }

    boolean hasItem() {
        return item != null && form != null;
    }
}

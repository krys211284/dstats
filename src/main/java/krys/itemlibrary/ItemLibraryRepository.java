package krys.itemlibrary;

import java.util.List;
import java.util.Optional;

/** Minimalne trwałe repozytorium biblioteki zapisanych itemów użytkownika. */
public interface ItemLibraryRepository {
    SavedImportedItem save(SavedImportedItem item);

    List<SavedImportedItem> findAll();

    Optional<SavedImportedItem> findById(long itemId);

    void delete(long itemId);

    ActiveItemSelection loadSelection();

    void saveSelection(ActiveItemSelection selection);
}

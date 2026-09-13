package krys.itemimport.image;

import krys.itemimport.ItemImageImportRequest;

import java.util.List;

/** Sklada jeden lub wiele obrazow itemu w neutralny finalny obraz dla przyszlego pipeline'u OCR. */
public interface ItemImageAssembler {
    AssembledItemImage assemble(List<ItemImageImportRequest> requests);
}

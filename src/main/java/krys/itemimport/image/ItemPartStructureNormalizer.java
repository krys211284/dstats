package krys.itemimport.image;

import java.util.List;

/** Neutralny Etap 3A: przypisuje uporzadkowane surowe czesci do stabilnych slotow strukturalnych. */
public interface ItemPartStructureNormalizer {
    StructuredItemParts normalize(List<ItemImagePart> rawParts);
}

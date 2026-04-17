package krys.itemimport;

import java.util.List;

/** Czyta tekst OCR z przygotowanych wariantów obrazu itemu. */
interface ItemImageOcrTextReader {
    List<ItemImageOcrTextVariant> readTextVariants(List<ItemImageOcrVariant> variants);
}

package krys.itemimport.image;

/** Odczytuje neutralny tekst OCR z pojedynczego PropertyBlock bez mapowania domenowego. */
public interface PropertyBlockOcrReader {
    PropertyBlockReadResult read(PropertyBlock block);
}

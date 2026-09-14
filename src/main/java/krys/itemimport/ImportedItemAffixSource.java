package krys.itemimport;

/** Źródło affixu w zapisanym modelu itemu. */
public enum ImportedItemAffixSource {
    /** Zachowane pochodzenie dla starszych/importowanych danych; nie uruchamia OCR runtime. */
    OCR,
    MANUAL,
    CORRECTED
}

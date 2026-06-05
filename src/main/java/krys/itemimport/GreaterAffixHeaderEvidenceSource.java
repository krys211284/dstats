package krys.itemimport;

/** Źródło dowodu liczby gwiazdek Greater Affix wykrytej w nagłówku itemu. */
public enum GreaterAffixHeaderEvidenceSource {
    OCR_HEADER_LITERAL_STARS,
    OCR_HEADER_ZERO_LIKE_RUN_HEURISTIC,
    OCR_HEADER_MIXED,
    NOT_DETECTED
}

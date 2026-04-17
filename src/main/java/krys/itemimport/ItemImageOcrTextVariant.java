package krys.itemimport;

/** Tekst OCR zwrócony dla konkretnego wariantu obrazu. */
final class ItemImageOcrTextVariant {
    private final String variantId;
    private final String text;

    ItemImageOcrTextVariant(String variantId, String text) {
        this.variantId = variantId;
        this.text = text;
    }

    String getVariantId() {
        return variantId;
    }

    String getText() {
        return text;
    }
}

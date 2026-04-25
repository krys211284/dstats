package krys.itemimport;

/** Jedna bezpiecznie zachowana linia pełnego odczytu OCR itemu. */
public final class FullItemReadLine {
    private final FullItemReadLineType type;
    private final String text;

    public FullItemReadLine(FullItemReadLineType type, String text) {
        this.type = type == null ? FullItemReadLineType.OTHER : type;
        this.text = text == null ? "" : text.trim();
    }

    public FullItemReadLineType getType() {
        return type;
    }

    public String getText() {
        return text;
    }
}

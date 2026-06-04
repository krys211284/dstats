package krys.itemimport;

/** Jedna bezpiecznie zachowana linia pełnego odczytu OCR itemu. */
public final class FullItemReadLine {
    private final FullItemReadLineType type;
    private final String text;
    private final FullItemReadLineSource source;

    public FullItemReadLine(FullItemReadLineType type, String text) {
        this(type, text, FullItemReadLineSource.unknown(text));
    }

    public FullItemReadLine(FullItemReadLineType type, String text, FullItemReadLineSource source) {
        this.type = type == null ? FullItemReadLineType.OTHER : type;
        this.text = text == null ? "" : text.trim();
        this.source = source == null ? FullItemReadLineSource.unknown(this.text) : source;
    }

    public FullItemReadLineType getType() {
        return type;
    }

    public String getText() {
        return text;
    }

    public FullItemReadLineSource getSource() {
        return source;
    }

    public FullItemReadLine withSource(FullItemReadLineSource newSource) {
        return new FullItemReadLine(type, text, newSource);
    }
}

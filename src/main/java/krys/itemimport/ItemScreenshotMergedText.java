package krys.itemimport;

import java.util.List;

/** Wynik merge OCR z metadanymi linii, używany tam, gdzie sama treść nie wystarcza do klasyfikacji. */
final class ItemScreenshotMergedText {
    private final List<MergedOcrLine> lines;

    ItemScreenshotMergedText(List<MergedOcrLine> lines) {
        this.lines = lines == null ? List.of() : List.copyOf(lines);
    }

    List<MergedOcrLine> getLines() {
        return lines;
    }

    List<String> textLines() {
        return lines.stream()
                .map(MergedOcrLine::getText)
                .toList();
    }

    String asPlainText() {
        return String.join(System.lineSeparator(), textLines());
    }

    boolean isBlank() {
        return lines.isEmpty() || textLines().stream().allMatch(String::isBlank);
    }
}

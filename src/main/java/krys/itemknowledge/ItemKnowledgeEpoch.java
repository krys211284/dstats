package krys.itemknowledge;

/** Aktywna epoka wiedzy oddzielająca obserwacje między sezonami albo większymi patchami. */
public record ItemKnowledgeEpoch(int sequence, String label) {
    public ItemKnowledgeEpoch {
        if (sequence <= 0) {
            throw new IllegalArgumentException("Numer epoki wiedzy musi być dodatni.");
        }
        if (label == null || label.isBlank()) {
            label = "Epoka wiedzy " + sequence;
        }
    }

    public ItemKnowledgeEpoch next(String requestedLabel) {
        int nextSequence = sequence + 1;
        String nextLabel = requestedLabel == null || requestedLabel.isBlank()
                ? "Epoka wiedzy " + nextSequence
                : requestedLabel.trim();
        return new ItemKnowledgeEpoch(nextSequence, nextLabel);
    }
}

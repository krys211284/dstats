package krys.itemimport.image;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Wynik Etapu 3A: surowe czesci oraz ich deterministyczne mapowanie na sloty strukturalne. */
public final class StructuredItemParts {
    private final ItemStructureStatus status;
    private final List<ItemImagePart> rawParts;
    private final List<StructuralItemPart> structuralParts;
    private final String diagnosticReason;

    public StructuredItemParts(ItemStructureStatus status,
                               List<ItemImagePart> rawParts,
                               List<StructuralItemPart> structuralParts,
                               String diagnosticReason) {
        this.status = Objects.requireNonNull(status, "status");
        this.rawParts = List.copyOf(rawParts == null ? List.of() : rawParts);
        this.structuralParts = List.copyOf(structuralParts == null ? List.of() : structuralParts);
        this.diagnosticReason = diagnosticReason == null ? "" : diagnosticReason;
    }

    public ItemStructureStatus getStatus() {
        return status;
    }

    public List<ItemImagePart> getRawParts() {
        return rawParts;
    }

    public List<StructuralItemPart> getStructuralParts() {
        return structuralParts;
    }

    public String getDiagnosticReason() {
        return diagnosticReason;
    }

    public Optional<StructuralItemPart> find(StructuralItemPartSlot slot) {
        Objects.requireNonNull(slot, "slot");
        return structuralParts.stream()
                .filter(part -> part.getSlot() == slot)
                .findFirst();
    }
}

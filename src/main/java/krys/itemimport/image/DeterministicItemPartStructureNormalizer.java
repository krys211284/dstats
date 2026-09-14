package krys.itemimport.image;

import java.util.List;

/** Deterministyczna implementacja kontraktu 3/4 czesci bez OCR, OpenCV i heurystyk tresci. */
public final class DeterministicItemPartStructureNormalizer implements ItemPartStructureNormalizer {
    @Override
    public StructuredItemParts normalize(List<ItemImagePart> rawParts) {
        List<ItemImagePart> safeRawParts = List.copyOf(rawParts == null ? List.of() : rawParts);
        int rawPartCount = safeRawParts.size();
        return switch (rawPartCount) {
            case 3 -> normalized(safeRawParts, List.of(
                    new StructuralItemPart(StructuralItemPartSlot.PART_1, safeRawParts.get(0)),
                    new StructuralItemPart(StructuralItemPartSlot.PART_3, safeRawParts.get(1)),
                    new StructuralItemPart(StructuralItemPartSlot.PART_4, safeRawParts.get(2))
            ));
            case 4 -> normalized(safeRawParts, List.of(
                    new StructuralItemPart(StructuralItemPartSlot.PART_1, safeRawParts.get(0)),
                    new StructuralItemPart(StructuralItemPartSlot.PART_2, safeRawParts.get(1)),
                    new StructuralItemPart(StructuralItemPartSlot.PART_3, safeRawParts.get(2)),
                    new StructuralItemPart(StructuralItemPartSlot.PART_4, safeRawParts.get(3))
            ));
            case 0, 1, 2 -> new StructuredItemParts(
                    ItemStructureStatus.INCOMPLETE,
                    safeRawParts,
                    List.of(),
                    "Brak jednoznacznej normalizacji strukturalnej dla " + rawPartCount + " surowych czesci."
            );
            default -> new StructuredItemParts(
                    ItemStructureStatus.INVALID_STRUCTURE,
                    safeRawParts,
                    List.of(),
                    "Nieoczekiwana liczba surowych czesci: " + rawPartCount + "."
            );
        };
    }

    private static StructuredItemParts normalized(List<ItemImagePart> rawParts, List<StructuralItemPart> structuralParts) {
        return new StructuredItemParts(
                ItemStructureStatus.STRUCTURALLY_NORMALIZED,
                rawParts,
                structuralParts,
                ""
        );
    }
}

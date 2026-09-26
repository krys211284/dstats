package krys.itemimport.image;

import java.util.List;
import java.util.Objects;

/** Wynik neutralnej segmentacji PART_2/PART_3 na logiczne bloki właściwości. */
public final class PropertyBlockSegmentationResult {
    private final StructuralItemPart sourcePart;
    private final List<PropertyBlock> blocks;
    private final String diagnostics;

    public PropertyBlockSegmentationResult(StructuralItemPart sourcePart,
                                           List<PropertyBlock> blocks,
                                           String diagnostics) {
        this.sourcePart = Objects.requireNonNull(sourcePart, "sourcePart");
        this.blocks = List.copyOf(blocks == null ? List.of() : blocks);
        this.diagnostics = diagnostics == null ? "" : diagnostics;
    }

    public StructuralItemPart getSourcePart() {
        return sourcePart;
    }

    public List<PropertyBlock> getBlocks() {
        return blocks;
    }

    public String getDiagnostics() {
        return diagnostics;
    }
}

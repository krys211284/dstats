package krys.itemimport.image;

import java.util.Objects;

/** Powiazanie stabilnego slotu strukturalnego z oryginalna surowa czescia obrazu. */
public final class StructuralItemPart {
    private final StructuralItemPartSlot slot;
    private final ItemImagePart sourcePart;

    public StructuralItemPart(StructuralItemPartSlot slot, ItemImagePart sourcePart) {
        this.slot = Objects.requireNonNull(slot, "slot");
        this.sourcePart = Objects.requireNonNull(sourcePart, "sourcePart");
    }

    public StructuralItemPartSlot getSlot() {
        return slot;
    }

    public ItemImagePart getSourcePart() {
        return sourcePart;
    }
}

package krys.transfiguration;

import java.util.List;

/** Definicja affixu z puli Przeistoczenia. */
public final class TransfigurationAffixDefinition {
    private final String id;
    private final String displayName;
    private final String sourceName;
    private final double min;
    private final double max;
    private final TransfigurationAffixValueKind valueKind;
    private final boolean doublesOnTwoHandedWeapon;
    private final String notes;
    private final List<String> elementOptions;

    public TransfigurationAffixDefinition(String id,
                                          String displayName,
                                          String sourceName,
                                          double min,
                                          double max,
                                          TransfigurationAffixValueKind valueKind,
                                          boolean doublesOnTwoHandedWeapon,
                                          String notes,
                                          List<String> elementOptions) {
        this.id = id;
        this.displayName = displayName;
        this.sourceName = sourceName;
        this.min = min;
        this.max = max;
        this.valueKind = valueKind;
        this.doublesOnTwoHandedWeapon = doublesOnTwoHandedWeapon;
        this.notes = notes == null ? "" : notes;
        this.elementOptions = elementOptions == null ? List.of() : List.copyOf(elementOptions);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSourceName() {
        return sourceName;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public TransfigurationAffixValueKind getValueKind() {
        return valueKind;
    }

    public boolean isDoublesOnTwoHandedWeapon() {
        return doublesOnTwoHandedWeapon;
    }

    public String getNotes() {
        return notes;
    }

    public List<String> getElementOptions() {
        return elementOptions;
    }

    public boolean accepts(double value) {
        return value >= min - 0.0000001d && value <= max + 0.0000001d;
    }
}

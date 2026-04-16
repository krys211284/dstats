package krys.search;

/** Jawna kategoria skali search space dla audytu M12. */
public enum BuildSearchSpaceScale {
    SMALL("mała"),
    MEDIUM("średnia"),
    LARGE("duża");

    private final String displayName;

    BuildSearchSpaceScale(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

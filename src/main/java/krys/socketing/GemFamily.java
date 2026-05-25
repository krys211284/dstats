package krys.socketing;

/** Rodzina gema z polską nazwą używaną w UI. */
public enum GemFamily {
    RUBY("Rubin", "ruby"),
    SAPPHIRE("Szafir", "sapphire"),
    EMERALD("Szmaragd", "emerald"),
    TOPAZ("Topaz", "topaz"),
    AMETHYST("Ametyst", "amethyst"),
    SKULL("Czaszka", "skull"),
    DIAMOND("Diament", "diamond");

    private final String displayName;
    private final String idPrefix;

    GemFamily(String displayName, String idPrefix) {
        this.displayName = displayName;
        this.idPrefix = idPrefix;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIdPrefix() {
        return idPrefix;
    }
}

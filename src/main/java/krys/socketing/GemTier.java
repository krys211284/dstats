package krys.socketing;

/** Tier gema z trwałym sufiksem id. */
public enum GemTier {
    CHIPPED("chipped"),
    CRUDE("crude"),
    STANDARD("standard"),
    FLAWLESS("flawless"),
    ROYAL("royal"),
    GRAND("grand"),
    HORADRIC("horadric"),
    FLAWLESS_HORADRIC("flawless_horadric");

    private final String idSuffix;

    GemTier(String idSuffix) {
        this.idSuffix = idSuffix;
    }

    public String getIdSuffix() {
        return idSuffix;
    }
}

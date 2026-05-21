package krys.masterworking;

/** Dane itemu dla mechaniki Doskonalenia. Na tym etapie nie wpływa na runtime. */
public final class ItemMasterworking {
    public static final int DEFAULT_QUALITY_CURRENT = 0;
    public static final int DEFAULT_QUALITY_MAX = 25;

    private final boolean enabled;
    private final int qualityCurrent;
    private final int qualityMax;

    public ItemMasterworking(boolean enabled, int qualityCurrent, int qualityMax) {
        this.enabled = enabled;
        this.qualityCurrent = qualityCurrent;
        this.qualityMax = qualityMax;
    }

    public static ItemMasterworking defaultState() {
        return new ItemMasterworking(false, DEFAULT_QUALITY_CURRENT, DEFAULT_QUALITY_MAX);
    }

    public static ItemMasterworking enabled(int qualityCurrent) {
        return new ItemMasterworking(true, qualityCurrent, DEFAULT_QUALITY_MAX);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getQualityCurrent() {
        return qualityCurrent;
    }

    public int getQualityMax() {
        return qualityMax;
    }

    public String qualityLabel() {
        return qualityCurrent + "/" + qualityMax;
    }
}

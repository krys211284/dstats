package krys.masterworking;

/** Dane itemu dla mechaniki Doskonalenia. Na tym etapie nie wpływa na runtime. */
public final class ItemMasterworking {
    public static final int DEFAULT_QUALITY_CURRENT = 0;
    public static final int DEFAULT_QUALITY_MAX = 25;

    private final int qualityCurrent;
    private final int qualityMax;
    private final MasterworkedAffixSelection perfectedAffix;

    public ItemMasterworking(int qualityCurrent, int qualityMax) {
        this(qualityCurrent, qualityMax, null);
    }

    public ItemMasterworking(int qualityCurrent, int qualityMax, MasterworkedAffixSelection perfectedAffix) {
        this.qualityCurrent = qualityCurrent;
        this.qualityMax = qualityMax;
        this.perfectedAffix = perfectedAffix;
    }

    public static ItemMasterworking defaultState() {
        return new ItemMasterworking(DEFAULT_QUALITY_CURRENT, DEFAULT_QUALITY_MAX);
    }

    public static ItemMasterworking quality(int qualityCurrent) {
        return new ItemMasterworking(qualityCurrent, DEFAULT_QUALITY_MAX);
    }

    public static ItemMasterworking fromLegacy(boolean ignoredEnabled, int qualityCurrent, int qualityMax) {
        return new ItemMasterworking(qualityCurrent, qualityMax);
    }

    public int getQualityCurrent() {
        return qualityCurrent;
    }

    public int getQualityMax() {
        return qualityMax;
    }

    public MasterworkedAffixSelection getPerfectedAffix() {
        return perfectedAffix;
    }

    public boolean hasVisibleProgress() {
        return qualityCurrent > DEFAULT_QUALITY_CURRENT;
    }

    public String qualityLabel() {
        return qualityCurrent + "/" + qualityMax;
    }
}

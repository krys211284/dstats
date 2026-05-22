package krys.masterworking;

import java.util.List;

/** Dane itemu dla mechaniki Doskonalenia. Na tym etapie nie wpływa na runtime. */
public final class ItemMasterworking {
    public static final int DEFAULT_QUALITY_CURRENT = 0;
    public static final int DEFAULT_QUALITY_MAX = 25;
    public static final List<Integer> ALLOWED_QUALITY_STEPS = List.of(0, 3, 6, 9, 12, 15, 17, 20, 21, 25);

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
        return fromPersisted(qualityCurrent, qualityMax, null);
    }

    public static ItemMasterworking fromPersisted(int qualityCurrent, int qualityMax, MasterworkedAffixSelection perfectedAffix) {
        int safeCurrent = isAllowedQualityStep(qualityCurrent) ? qualityCurrent : DEFAULT_QUALITY_CURRENT;
        int safeMax = qualityMax == DEFAULT_QUALITY_MAX ? qualityMax : DEFAULT_QUALITY_MAX;
        return new ItemMasterworking(safeCurrent, safeMax, perfectedAffix);
    }

    public static boolean isAllowedQualityStep(int qualityCurrent) {
        return ALLOWED_QUALITY_STEPS.contains(qualityCurrent);
    }

    public static String allowedQualityStepsLabel() {
        return ALLOWED_QUALITY_STEPS.stream()
                .map(String::valueOf)
                .reduce("", (left, right) -> left.isBlank() ? right : left + ", " + right);
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

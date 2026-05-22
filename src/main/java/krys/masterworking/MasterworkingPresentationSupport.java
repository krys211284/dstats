package krys.masterworking;

/** Wspólne etykiety prezentacyjne Doskonalenia. */
public final class MasterworkingPresentationSupport {
    private MasterworkingPresentationSupport() {
    }

    public static String qualityLabel(ItemMasterworking masterworking) {
        ItemMasterworking safe = masterworking == null ? ItemMasterworking.defaultState() : masterworking;
        return safe.getQualityCurrent() + "/" + safe.getQualityMax();
    }

    public static String runtimeStatusLabel() {
        return "Dane itemu / runtime nieaktywny";
    }

    public static String compactRuntimeStatusLabel() {
        return "Runtime nieaktywny";
    }

    public static String compactSummary(ItemMasterworking masterworking, String perfectedAffixLabel) {
        ItemMasterworking safe = masterworking == null ? ItemMasterworking.defaultState() : masterworking;
        String summary = "Doskonalenie · Jakość " + qualityLabel(safe);
        if (safe.getQualityCurrent() == ItemMasterworking.DEFAULT_QUALITY_MAX
                && perfectedAffixLabel != null
                && !perfectedAffixLabel.isBlank()) {
            summary += " · Doskonalony afiks: " + perfectedAffixLabel;
        }
        return summary + " · " + compactRuntimeStatusLabel();
    }
}

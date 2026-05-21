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
}

package krys.itemimport.image;

import java.util.List;

/** Neutralne evidence wielu profili OCR dla jednego tokenu liczbowego. */
public final class PropertyBlockNumericEvidence {
    private final int tokenOrder;
    private final String selectedValue;
    private final List<String> supportingProfiles;
    private final List<String> alternativeValues;
    private final int supportCount;
    private final boolean ambiguous;

    public PropertyBlockNumericEvidence(int tokenOrder,
                                        String selectedValue,
                                        List<String> supportingProfiles,
                                        List<String> alternativeValues,
                                        int supportCount,
                                        boolean ambiguous) {
        this.tokenOrder = Math.max(1, tokenOrder);
        this.selectedValue = selectedValue == null ? "" : selectedValue;
        this.supportingProfiles = List.copyOf(
                supportingProfiles == null ? List.of() : supportingProfiles);
        this.alternativeValues = List.copyOf(
                alternativeValues == null ? List.of() : alternativeValues);
        this.supportCount = Math.max(0, supportCount);
        this.ambiguous = ambiguous;
    }

    public int getTokenOrder() {
        return tokenOrder;
    }

    public String getSelectedValue() {
        return selectedValue;
    }

    public List<String> getSupportingProfiles() {
        return supportingProfiles;
    }

    public List<String> getAlternativeValues() {
        return alternativeValues;
    }

    public int getSupportCount() {
        return supportCount;
    }

    public boolean isAmbiguous() {
        return ambiguous;
    }
}

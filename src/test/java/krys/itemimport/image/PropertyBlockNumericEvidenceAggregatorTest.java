package krys.itemimport.image;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regresje neutralnego consensus wielu profili OCR. */
class PropertyBlockNumericEvidenceAggregatorTest {
    @Test
    void shouldPrefer2004OverSingle20604Outlier() {
        List<PropertyBlockOcrProfileResult> profiles = new ArrayList<>();
        for (int index = 1; index <= 5; index++) {
            profiles.add(profile("p" + index, "2 004 pkt. pancerza"));
        }
        profiles.add(profile("outlier", "20604 pkt. pancerza"));
        profiles.add(profile("empty", ""));

        PropertyBlockNumericEvidence evidence = aggregate(profiles).evidence().get(0);

        assertEquals("2004", evidence.getSelectedValue());
        assertEquals(5, evidence.getSupportCount());
        assertEquals(List.of("20604"), evidence.getAlternativeValues());
        assertFalse(evidence.isAmbiguous());
    }

    @Test
    void shouldPrefer123Over125Outlier() {
        PropertyBlockNumericEvidence evidence = aggregate(List.of(
                profile("p1", "12,3%"),
                profile("p2", "12,3%"),
                profile("p3", "12,3%"),
                profile("p4", "12,3%"),
                profile("p5", "12,3%"),
                profile("outlier", "12,5%")
        )).evidence().get(0);

        assertEquals("12,3%", evidence.getSelectedValue());
        assertEquals(5, evidence.getSupportCount());
        assertEquals(List.of("12,5%"), evidence.getAlternativeValues());
        assertFalse(evidence.isAmbiguous());
    }

    @Test
    void shouldPreferRangeSupportedBySeveralProfiles() {
        PropertyBlockNumericEvidence evidence = aggregate(List.of(
                profile("p1", "+115 [75 - 100]"),
                profile("p2", "+115 [75 - 100]"),
                profile("p3", "+115 [75 - 100]"),
                profile("p4", "+115 [75 - 100]"),
                profile("p5", "+115 [75 - 100]"),
                profile("outlier-1", "+115 [75 - 1001]"),
                profile("outlier-2", "1115 [75 - 106]")
        )).evidence().get(1);

        assertEquals("75-100", evidence.getSelectedValue());
        assertEquals(5, evidence.getSupportCount());
        assertEquals(List.of("75-1001", "75-106"), evidence.getAlternativeValues());
        assertFalse(evidence.isAmbiguous());
    }

    @Test
    void shouldMarkTieAsAmbiguousWithoutGuessing() {
        PropertyBlockNumericEvidenceAggregator.Aggregation aggregation = aggregate(List.of(
                profile("p1", "70-100%"),
                profile("p2", "70-100%"),
                profile("p3", "70-160%"),
                profile("p4", "70-160%"),
                profile("empty", "")
        ));
        PropertyBlockNumericEvidence evidence = aggregation.evidence().get(0);

        assertEquals("", evidence.getSelectedValue());
        assertEquals(2, evidence.getSupportCount());
        assertEquals(List.of("70-100%", "70-160%"), evidence.getAlternativeValues());
        assertTrue(evidence.isAmbiguous());
        assertTrue(aggregation.diagnostics().get(0).startsWith("NUMERIC_OCR_AMBIGUOUS"));
    }

    @Test
    void shouldNotSelectConsensusFromFewerThanFourCorrelatedProfiles() {
        PropertyBlockNumericEvidenceAggregator.Aggregation aggregation = aggregate(List.of(
                profile("p1", "+15,0% [12,01]%"),
                profile("p2", "+15,0% [12,01]%"),
                profile("p3", "+15,07% [12,01]%"),
                profile("p4", "+15,076 [12,0]%"),
                profile("empty", "")
        ));

        assertTrue(aggregation.evidence().get(0).isAmbiguous());
        assertEquals("", aggregation.evidence().get(0).getSelectedValue());
        assertTrue(aggregation.evidence().get(1).isAmbiguous());
        assertEquals("", aggregation.evidence().get(1).getSelectedValue());
        assertEquals(2, aggregation.diagnostics().size());
    }

    private static PropertyBlockNumericEvidenceAggregator.Aggregation aggregate(
            List<PropertyBlockOcrProfileResult> profiles) {
        return PropertyBlockNumericEvidenceAggregator.aggregate(profiles);
    }

    private static PropertyBlockOcrProfileResult profile(String name, String rawText) {
        return new PropertyBlockOcrProfileResult(
                name,
                rawText,
                PropertyBlockNumericEvidenceAggregator.extractNumericTokens(rawText),
                0.0d,
                ""
        );
    }
}

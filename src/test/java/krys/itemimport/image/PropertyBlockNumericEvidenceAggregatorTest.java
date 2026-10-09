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

    @Test
    void shouldAlignDziedzicFullSecondValueAcrossInsertedNoise() {
        PropertyBlockNumericEvidenceAggregator.Aggregation aggregation = aggregate(List.of(
                profile("B3", "+15,07% [12,01]%"),
                profile("BP", "+15,07% [12,01]%"),
                profile("B4", "+15,0% [12,01]%"),
                profile("N3", "+15,0% [12,01]%"),
                profile("N4", "+15,0% 6 [12,0]%"),
                profileTokens("N5", "+15", "75", "12.0", "7"),
                profile("A", "+15,0% [12,0]%")
        ));

        PropertyBlockNumericEvidence second = aggregation.evidence().get(1);

        assertTrue(second.isAmbiguous());
        assertEquals("", second.getSelectedValue());
        assertEquals(4, second.getSupportCount());
        assertTrue(second.getAlternativeValues().contains("12,01%"), second.getAlternativeValues().toString());
        assertTrue(second.getAlternativeValues().contains("12,0%"), second.getAlternativeValues().toString());
        assertTrue(second.getAlternativeValues().contains("12.0"), describe(aggregation.evidence()));
        assertTrue(aggregation.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.startsWith("NUMERIC_OCR_AMBIGUOUS tokenOrder=2")));
    }

    @Test
    void shouldKeepMixedRangeAndSingleTokenColumnAmbiguous() {
        PropertyBlockNumericEvidenceAggregator.Aggregation aggregation = aggregate(List.of(
                profile("p1", "6,5-8,5%"),
                profile("p2", "6,5-8,5%"),
                profile("p3", "6,5-8,5%"),
                profile("p4", "6,5-8,5%"),
                profile("p5", "6,5-8,5%"),
                profile("single", "7%"),
                profile("range-outlier", "5,5-5,5%")
        ));

        PropertyBlockNumericEvidence evidence = aggregation.evidence().getFirst();

        assertTrue(evidence.isAmbiguous());
        assertEquals("", evidence.getSelectedValue());
        assertEquals(5, evidence.getSupportCount());
        assertTrue(evidence.getAlternativeValues().contains("6,5-8,5%"));
        assertTrue(evidence.getAlternativeValues().contains("7%"));
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

    private static PropertyBlockOcrProfileResult profileTokens(String name, String... tokens) {
        return new PropertyBlockOcrProfileResult(
                name,
                String.join(" ", tokens),
                List.of(tokens),
                0.0d,
                ""
        );
    }

    private static String describe(List<PropertyBlockNumericEvidence> evidence) {
        return evidence.stream()
                .map(value -> value.getTokenOrder() + "=" + value.getSelectedValue()
                        + " support=" + value.getSupportCount()
                        + " alternatives=" + value.getAlternativeValues()
                        + " profiles=" + value.getSupportingProfiles()
                        + " ambiguous=" + value.isAmbiguous())
                .toList()
                .toString();
    }
}

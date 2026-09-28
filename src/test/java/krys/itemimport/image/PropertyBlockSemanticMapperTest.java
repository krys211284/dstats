package krys.itemimport.image;

import krys.transfiguration.HoradricTransfigurationOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regresje kolejności neutralnej klasyfikacji semantycznej PropertyBlock. */
class PropertyBlockSemanticMapperTest {
    private final PropertyBlockSemanticMapper mapper = new PropertyBlockSemanticMapper();

    @Test
    void shouldClassifySpecialAnchorsBeforeOrdinaryAffixMatching() {
        PropertyBlockSemanticResult socket = map(
                PropertyBlockAnchorType.FILLED_SOCKET, "+150 siły", evidence("150"));
        PropertyBlockSemanticResult transfiguration = map(
                PropertyBlockAnchorType.TRANSFIGURATION_MARK,
                "+115 pkt. do wszystkich współczynników +[75 - 100]",
                evidence("+115"), evidence("75-100"));
        PropertyBlockSemanticResult aspect = map(
                PropertyBlockAnchorType.UNIQUE_POWER_STAR,
                "+25% szybkości ruchu [20]%",
                evidence("+25%"), evidence("20%"));

        assertSemantic(socket, PropertyBlockSemanticKind.SOCKET, "FILLED_SOCKET");
        assertSemantic(transfiguration, PropertyBlockSemanticKind.TRANSFIGURATION, "ALL_STATS");
        assertEquals(HoradricTransfigurationOutcome.UNKNOWN, transfiguration.getTransfigurationOutcome());
        assertEquals(PropertyBlockSemanticKind.ASPECT, aspect.getKind());
        assertFalse(aspect.hasDefinitionId());
    }

    @Test
    void shouldRecognizeBaseStatsAndImplicitsFromTextInEitherSourcePart() {
        PropertyBlockSemanticResult armorFromPart3 = map(
                StructuralItemPartSlot.PART_3,
                PropertyBlockAnchorType.NONE,
                "2 004 pkt. pancerza",
                evidence("2004"));
        PropertyBlockSemanticResult blockChanceFromPart3 = map(
                StructuralItemPartSlot.PART_3,
                PropertyBlockAnchorType.DIAMOND,
                "20,0% szansy na blok [20,0]%",
                evidence("20,0%"), evidence("20,0%"));
        PropertyBlockSemanticResult weaponImplicitFromPart2 = map(
                StructuralItemPartSlot.PART_2,
                PropertyBlockAnchorType.DIAMOND,
                "+134 obrażeń od broni [94 - 157]",
                evidence("+134"), evidence("94-157"));

        assertEquals(PropertyBlockSemanticKind.BASE_STAT, armorFromPart3.getKind());
        assertEquals(PropertyBlockSemanticKind.IMPLICIT, blockChanceFromPart3.getKind());
        assertEquals(PropertyBlockSemanticKind.IMPLICIT, weaponImplicitFromPart2.getKind());
    }

    @Test
    void shouldUseExistingRegistriesAndKeepGreaterStarAsModifier() {
        PropertyBlockSemanticResult affix = map(
                PropertyBlockAnchorType.GREATER_STAR,
                "12,3% redukcji czasu odnowienia [10 - 12]%",
                evidence("12,3%"), evidence("10-12%"));
        PropertyBlockSemanticResult tempering = map(
                PropertyBlockAnchorType.GREATER_STAR,
                "+12 do maksymalnej liczby kumulacji Animuszu",
                evidence("+12"));
        PropertyBlockSemanticResult quality = map(
                PropertyBlockAnchorType.TRANSFIGURATION_MARK,
                "+4 do jakości przedmiotu [1 - 15]",
                evidence("+4"), evidence("1-15"));

        assertSemantic(affix, PropertyBlockSemanticKind.AFFIX, "cooldown_reduction");
        assertTrue(affix.isGreaterAffix());
        assertSemantic(tempering, PropertyBlockSemanticKind.TEMPERING, "defense_max_animus");
        assertTrue(tempering.isGreaterAffix());
        assertSemantic(quality, PropertyBlockSemanticKind.TRANSFIGURATION, "BONUS_ITEM_QUALITY");
        assertEquals(HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY, quality.getTransfigurationOutcome());
    }

    @Test
    void shouldKeepDefinitionButRejectCertainValueForAmbiguousNumericEvidence() {
        PropertyBlockNumericEvidence ambiguous = new PropertyBlockNumericEvidence(
                1,
                "",
                List.of("profile-1", "profile-2"),
                List.of("12,3%", "12,5%"),
                2,
                true);

        PropertyBlockSemanticResult result = map(
                PropertyBlockAnchorType.GREATER_STAR,
                "12,3% redukcji czasu odnowienia [10 - 12]%",
                ambiguous,
                evidence("10-12%"));

        assertSemantic(result, PropertyBlockSemanticKind.AFFIX, "cooldown_reduction");
        assertFalse(result.isNumericValueCertain());
        assertTrue(result.getDiagnostics().stream().anyMatch(value -> value.contains("NUMERIC_OCR_AMBIGUOUS")));
    }

    @Test
    void shouldExposeCompetingDefinitionsWithoutGuessing() {
        PropertyBlockSemanticResult result = map(
                PropertyBlockAnchorType.DIAMOND,
                "+100 siły oraz +100 inteligencji",
                evidence("+100"), evidence("+100"));

        assertEquals(PropertyBlockSemanticKind.AMBIGUOUS, result.getKind());
        assertEquals("", result.getDefinitionId());
        assertEquals(List.of("strength", "intelligence"), result.getCandidateDefinitionIds());
        assertTrue(result.getDiagnostics().stream().anyMatch(value -> value.startsWith("SEMANTIC_MATCH_AMBIGUOUS")));
    }

    private PropertyBlockSemanticResult map(PropertyBlockAnchorType anchorType,
                                            String rawText,
                                            PropertyBlockNumericEvidence... evidence) {
        return map(StructuralItemPartSlot.PART_3, anchorType, rawText, evidence);
    }

    private PropertyBlockSemanticResult map(StructuralItemPartSlot sourcePart,
                                            PropertyBlockAnchorType anchorType,
                                            String rawText,
                                            PropertyBlockNumericEvidence... evidence) {
        PropertyBlock block = new PropertyBlock(
                sourcePart, 1, 0, 0, 10, 10, anchorType, null, "PNG", new byte[]{1}, "");
        PropertyBlockReadResult readResult = new PropertyBlockReadResult(
                sourcePart, 1, anchorType, rawText, 1L, "test", List.of(), List.of(evidence), List.of());
        return mapper.map(block, readResult);
    }

    private static PropertyBlockNumericEvidence evidence(String selectedValue) {
        return new PropertyBlockNumericEvidence(
                1,
                selectedValue,
                List.of("profile-1", "profile-2", "profile-3", "profile-4"),
                List.of(),
                4,
                false);
    }

    private static void assertSemantic(PropertyBlockSemanticResult result,
                                       PropertyBlockSemanticKind kind,
                                       String definitionId) {
        assertEquals(kind, result.getKind());
        assertEquals(definitionId, result.getDefinitionId());
    }
}

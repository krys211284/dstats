package krys.itemimport.image;

import krys.transfiguration.HoradricTransfigurationOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regresje neutralnego odczytu wartosci po PropertyBlockReadResult i PropertyBlockSemanticResult. */
class PropertyBlockValueReaderTest {
    private final PropertyBlockValueReader reader = new PropertyBlockValueReader();

    @Test
    void shouldReadTarczaBaseImplicitAffixAndGreaterAffixValues() {
        PropertyBlockValueReadResult armor = read(
                PropertyBlockSemanticKind.BASE_STAT,
                "",
                false,
                true,
                "1502 pkt. pancerza",
                evidence(1, "1502"));
        PropertyBlockValueReadResult blockChance = read(
                PropertyBlockSemanticKind.IMPLICIT,
                "",
                false,
                true,
                "20,0% szansy na blok [20,0]%",
                evidence(1, "20,0%"),
                evidence(2, "20,0%"));
        PropertyBlockValueReadResult strength = read(
                PropertyBlockSemanticKind.AFFIX,
                "strength",
                false,
                true,
                "+217 siły [150 - 180]",
                evidence(1, "+217"),
                evidence(2, "150-180"));
        PropertyBlockValueReadResult cooldown = read(
                PropertyBlockSemanticKind.AFFIX,
                "cooldown_reduction",
                true,
                true,
                "12,3% redukcji czasu odnowienia",
                evidence(1, "12,3%"));

        assertEquals(1502L, armor.getItemArmor());
        assertEquals("itemArmor", armor.getValueRole());
        assertTrue(armor.isCertain());

        assertEquals(20.0d, blockChance.getValue());
        assertEquals("%", blockChance.getUnit());
        assertEquals(20.0d, blockChance.getReferenceValue());
        assertFalse(blockChance.hasRollRange());
        assertEquals("referenceValue", blockChance.getParameters().get(1).getRole());

        assertEquals(217.0d, strength.getValue());
        assertEquals(150.0d, strength.getRollRangeMin());
        assertEquals(180.0d, strength.getRollRangeMax());
        assertEquals("rollRange", strength.getParameters().get(1).getRole());

        assertEquals(12.3d, cooldown.getValue());
        assertEquals("%", cooldown.getUnit());
        assertEquals("affixValue", cooldown.getValueRole());
        assertTrue(cooldown.isCertain());
    }

    @Test
    void shouldKeepDziedzicAmbiguityAndFilledSocketsAsSocketParameters() {
        PropertyBlockValueReadResult armor = read(
                PropertyBlockSemanticKind.BASE_STAT,
                "",
                false,
                true,
                "2 004 pkt. pancerza",
                evidence(1, "2004", false, "20604"));
        PropertyBlockValueReadResult ambiguousTransfiguration = read(
                PropertyBlockSemanticKind.TRANSFIGURATION,
                "ALL_STATS",
                false,
                false,
                "+115 do wszystkich współczynników [75 - 100]",
                evidence(1, "", true, "+115", "1115", "115"),
                evidence(2, "75-100", false, "75-1001", "75-106"));
        PropertyBlockValueReadResult firstSocket = readSocket("+150 siły", evidence(1, "+150"));
        PropertyBlockValueReadResult secondSocket = readSocket("+120 siły", evidence(1, "+120"));

        assertEquals(2004L, armor.getItemArmor());
        assertTrue(armor.isCertain());

        assertTrue(ambiguousTransfiguration.isAmbiguous());
        assertFalse(ambiguousTransfiguration.isCertain());
        assertEquals(115.0d, ambiguousTransfiguration.getValue());
        assertEquals(75.0d, ambiguousTransfiguration.getRollRangeMin());
        assertEquals(100.0d, ambiguousTransfiguration.getRollRangeMax());
        assertEquals("transfigurationValue", ambiguousTransfiguration.getValueRole());
        assertTrue(ambiguousTransfiguration.getParameters().get(0).isAmbiguous());
        assertEquals(List.of("+115", "1115", "115"), ambiguousTransfiguration.getParameters().get(0).getAlternativeValues());

        assertEquals("socketParameters", firstSocket.getValueRole());
        assertEquals("socketParameter", firstSocket.getParameters().getFirst().getRole());
        assertEquals(150.0d, firstSocket.getParameters().getFirst().getValue());
        assertEquals("socketParameters", secondSocket.getValueRole());
        assertEquals(120.0d, secondSocket.getParameters().getFirst().getValue());
    }

    @Test
    void shouldReadVerathielWeaponBaseStatsStrengthAndAspectParameterOrder() {
        PropertyBlockValueReadResult baseStats = read(
                PropertyBlockSemanticKind.BASE_STAT,
                "",
                false,
                true,
                "1 874 obrażeń na sek.\n[1 390 - 2 018] obrażeń za trafienie\n1,10 ataku na sekundę",
                evidence(1, "1874"),
                evidence(2, "1390-2018"),
                evidence(3, "1,10"));
        PropertyBlockValueReadResult strength = read(
                PropertyBlockSemanticKind.AFFIX,
                "strength",
                false,
                true,
                "+172 siły [150 - 180]",
                evidence(1, "+172"),
                evidence(2, "150-180"));
        PropertyBlockValueReadResult aspect = read(
                PropertyBlockSemanticKind.ASPECT,
                "verathiel_shard",
                false,
                true,
                "Umiejętności Podstawowe zadają 100% obrażeń [70 - 100]% i kosztują 25 podstawowego zasobu",
                evidence(1, "100%"),
                evidence(2, "70-100%"),
                evidence(3, "25"));

        assertEquals(1874L, baseStats.getWeaponDps());
        assertEquals(1390L, baseStats.getWeaponDamageMin());
        assertEquals(2018L, baseStats.getWeaponDamageMax());
        assertEquals(1.10d, baseStats.getAttacksPerSecond());
        assertEquals(List.of("weaponDps", "weaponDamageRange", "attacksPerSecond"),
                baseStats.getParameters().stream().map(PropertyBlockValueReadResult.NumericParameter::getRole).toList());

        assertEquals(172.0d, strength.getValue());
        assertEquals(150.0d, strength.getRollRangeMin());
        assertEquals(180.0d, strength.getRollRangeMax());

        assertEquals("aspectParameters", aspect.getValueRole());
        assertNull(aspect.getValue());
        assertEquals(List.of("100%", "70-100%", "25"),
                aspect.getParameters().stream().map(PropertyBlockValueReadResult.NumericParameter::getRawToken).toList());
        assertEquals(100.0d, aspect.getParameters().get(0).getValue());
        assertEquals(70.0d, aspect.getParameters().get(1).getRangeMin());
        assertEquals(100.0d, aspect.getParameters().get(1).getRangeMax());
        assertEquals(25.0d, aspect.getParameters().get(2).getValue());
    }

    @Test
    void shouldReadTemperingAndTransfigurationOnlyWhenNumericOcrIsCertain() {
        PropertyBlockValueReadResult transfiguration = read(
                PropertyBlockSemanticKind.TRANSFIGURATION,
                "BONUS_ITEM_QUALITY",
                false,
                true,
                "+4 do jakości przedmiotu [1 - 15]",
                evidence(1, "+4"),
                evidence(2, "1-15"));
        PropertyBlockValueReadResult tempering = read(
                PropertyBlockSemanticKind.TEMPERING,
                "defense_max_animus",
                true,
                true,
                "+12 do maksymalnej liczby kumulacji Animuszu",
                evidence(1, "+12"));

        assertEquals(4.0d, transfiguration.getValue());
        assertEquals(1.0d, transfiguration.getRollRangeMin());
        assertEquals(15.0d, transfiguration.getRollRangeMax());
        assertTrue(transfiguration.isCertain());

        assertEquals(12.0d, tempering.getValue());
        assertEquals("temperingValue", tempering.getValueRole());
        assertTrue(tempering.isCertain());
    }

    private PropertyBlockValueReadResult readSocket(String rawText, PropertyBlockNumericEvidence... evidence) {
        return read(PropertyBlockSemanticKind.SOCKET, "FILLED_SOCKET", false, true, rawText, evidence);
    }

    private PropertyBlockValueReadResult read(PropertyBlockSemanticKind kind,
                                              String definitionId,
                                              boolean greaterAffix,
                                              boolean numericValueCertain,
                                              String rawText,
                                              PropertyBlockNumericEvidence... evidence) {
        PropertyBlockReadResult readResult = new PropertyBlockReadResult(
                StructuralItemPartSlot.PART_3,
                1,
                PropertyBlockAnchorType.DIAMOND,
                rawText,
                1L,
                "test",
                List.of(),
                List.of(evidence),
                List.of());
        PropertyBlockSemanticResult semanticResult = new PropertyBlockSemanticResult(
                StructuralItemPartSlot.PART_3,
                1,
                PropertyBlockAnchorType.DIAMOND,
                kind,
                definitionId,
                definitionId.isBlank() ? List.of() : List.of(definitionId),
                greaterAffix,
                numericValueCertain,
                rawText,
                List.of(evidence),
                HoradricTransfigurationOutcome.NONE,
                numericValueCertain ? List.of() : List.of("NUMERIC_OCR_AMBIGUOUS tokenOrder=1"));
        return reader.read(readResult, semanticResult);
    }

    private static PropertyBlockNumericEvidence evidence(int order, String selectedValue, String... alternatives) {
        return evidence(order, selectedValue, false, alternatives);
    }

    private static PropertyBlockNumericEvidence evidence(int order,
                                                         String selectedValue,
                                                         boolean ambiguous,
                                                         String... alternatives) {
        return new PropertyBlockNumericEvidence(
                order,
                selectedValue,
                ambiguous ? List.of("profile-1", "profile-2") : List.of("profile-1", "profile-2", "profile-3", "profile-4"),
                List.of(alternatives),
                ambiguous ? 2 : 4,
                ambiguous);
    }
}

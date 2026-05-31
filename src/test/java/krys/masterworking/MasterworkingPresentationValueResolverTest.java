package krys.masterworking;

import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingRuntimeStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class MasterworkingPresentationValueResolverTest {
    private final MasterworkingPresentationValueResolver resolver = new MasterworkingPresentationValueResolver();

    @Test
    void shouldResolveArmorGoldenValues() {
        Map<Integer, Long> expected = Map.of(
                0, 1202L,
                3, 1238L,
                6, 1274L,
                9, 1310L,
                12, 1346L,
                15, 1382L,
                17, 1406L,
                20, 1442L,
                21, 1454L,
                25, 1502L
        );

        expected.forEach((quality, value) -> assertEquals(value, resolver.displayArmor(1202L, quality)));
    }

    @Test
    void shouldResolveStrengthGreaterAffixGoldenValues() {
        assertIntegerGoldenValues(ImportedItemAffixType.STRENGTH, 225.0d, Map.of(
                0, "225",
                3, "230",
                6, "236",
                9, "241",
                12, "247",
                15, "252",
                17, "256",
                20, "261",
                21, "263",
                25, "270"
        ));
    }

    @Test
    void shouldResolveAllResistanceGreaterAffixGoldenValues() {
        assertIntegerGoldenValues(ImportedItemAffixType.ALL_RESISTANCE, 490.0d, Map.of(
                0, "490",
                3, "501",
                6, "513",
                9, "525",
                12, "537",
                15, "548",
                17, "556",
                20, "568",
                21, "572",
                25, "588"
        ));
    }

    @Test
    void shouldResolveFireResistanceGreaterAffixGoldenValues() {
        assertIntegerGoldenValues(ImportedItemAffixType.FIRE_RESISTANCE, 787.0d, Map.of(
                0, "787",
                3, "806",
                6, "825",
                9, "844",
                12, "863",
                15, "882",
                17, "894",
                20, "913",
                21, "919",
                25, "945"
        ));
    }

    @Test
    void shouldResolveDamageReductionGoldenValues() {
        ImportedItemAffix affix = new ImportedItemAffix(ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, "%", false,
                0, "11,4% redukcji obrażeń", ImportedItemAffixSource.OCR);
        Map<Integer, String> expected = Map.of(
                0, "11,4%",
                3, "11,8%",
                6, "12,1%",
                9, "12,5%",
                12, "12,8%",
                15, "13,2%",
                17, "13,4%",
                20, "13,7%",
                21, "13,8%",
                25, "14,3%"
        );

        expected.forEach((quality, value) -> assertEquals(value,
                resolver.resolveAffix(affix, ItemMasterworking.quality(quality)).getDisplayValueLabel()));
    }

    @Test
    void shouldResolveGenericNumericAffixesWithoutUnsupportedNote() {
        ItemMasterworking masterworking = new ItemMasterworking(25, 25);

        assertPresentation(ImportedItemAffixType.STRENGTH, 173.6d, false, "217");
        assertPresentation(ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 8.8d, false, "11,0%");
        assertPresentation(ImportedItemAffixType.DAMAGE_REDUCTION, 14.08d, false, "17,6%");
        assertPresentation(ImportedItemAffixType.COOLDOWN_REDUCTION, 10.25d, true, "12,3%");
    }

    @Test
    void shouldPresentMythicDisplayedValueWithoutReferenceWithoutRemastering() {
        MasterworkingPresentationValue value = resolver.resolveAffix(
                new ImportedItemAffix(ImportedItemAffixType.LUCKY_HIT_CHANCE, 25.0d, "%", false, 0,
                        "+25,0% szansy na szczęśliwy traf", ImportedItemAffixSource.OCR),
                new ItemMasterworking(25, 25),
                true
        );

        assertEquals("25,0%", value.getDisplayValueLabel());
        assertEquals("25,0%", value.getBaseValueLabel());
        assertEquals(false, value.hasChangedValue());
    }

    @Test
    void shouldPresentMythicDisplayedValueWithReferenceWithoutRemastering() {
        MasterworkingPresentationValue value = resolver.resolveAffix(
                new ImportedItemAffix(ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 15.0d, "%", false, 0,
                        "+15,0% szansy na trafienie krytyczne [12,0]%", ImportedItemAffixSource.OCR,
                        "critical_strike_chance", null, null, 12.0d, ""),
                new ItemMasterworking(25, 25),
                true
        );

        assertEquals("15,0%", value.getDisplayValueLabel());
        assertEquals("15,0%", value.getBaseValueLabel());
        assertEquals(false, value.hasChangedValue());
    }

    @Test
    void shouldResolveMaxAnimusTemperingGoldenValues() {
        ItemTemperingAffix affix = maxAnimusTempering();
        for (int quality : new int[]{0, 3, 6, 9, 12, 15, 17, 20, 21}) {
            assertEquals("+5", resolver.resolveTempering(affix, ItemMasterworking.quality(quality)).getDisplayValueLabel());
        }
        assertEquals("+7", resolver.resolveTempering(affix, ItemMasterworking.quality(25)).getDisplayValueLabel());
        assertEquals("+12", resolver.resolveTempering(
                affix,
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.temperingAffix("defense_max_animus"))
        ).getDisplayValueLabel());
    }

    @Test
    void shouldResolvePerfectedOrdinaryAffixGoldenValues() {
        MasterworkingPresentationValue strength = resolver.resolveAffix(
                greaterAffix(ImportedItemAffixType.STRENGTH, 225.0d),
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.ordinaryAffix("STRENGTH"))
        );
        MasterworkingPresentationValue fire = resolver.resolveAffix(
                greaterAffix(ImportedItemAffixType.FIRE_RESISTANCE, 787.0d),
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.ordinaryAffix("FIRE_RESISTANCE"))
        );

        assertEquals("360", strength.getDisplayValueLabel());
        assertEquals("1260", fire.getDisplayValueLabel());
    }

    private void assertIntegerGoldenValues(ImportedItemAffixType type, double storedValue, Map<Integer, String> expected) {
        ImportedItemAffix affix = greaterAffix(type, storedValue);
        expected.forEach((quality, value) -> assertEquals(value,
                resolver.resolveAffix(affix, ItemMasterworking.quality(quality)).getDisplayValueLabel()));
    }

    private void assertPresentation(ImportedItemAffixType type, double sourceValue, boolean greaterAffix, String expectedDisplay) {
        MasterworkingPresentationValue value = resolver.resolveAffix(
                new ImportedItemAffix(type, sourceValue, "", greaterAffix, 0, type.getDisplayName(), ImportedItemAffixSource.OCR),
                new ItemMasterworking(25, 25)
        );

        assertEquals(expectedDisplay, value.getDisplayValueLabel(), type.getDisplayName());
        assertNotEquals(MasterworkingPresentationValueResolver.NO_RULE_NOTE, value.getNote(), type.getDisplayName());
        assertEquals(true, value.isSupported(), type.getDisplayName());
    }

    private static ImportedItemAffix greaterAffix(ImportedItemAffixType type, double value) {
        return new ImportedItemAffix(type, value, "", true, 0, type.getDisplayName(), ImportedItemAffixSource.OCR);
    }

    private static ItemTemperingAffix maxAnimusTempering() {
        return new ItemTemperingAffix(
                "defense_max_animus",
                TemperingCategory.DEFENSE,
                5.0d,
                "+5 do maksymalnej liczby kumulacji Animuszu",
                TemperingRuntimeStatus.DATA_ONLY,
                true
        );
    }
}

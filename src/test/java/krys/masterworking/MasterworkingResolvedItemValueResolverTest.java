package krys.masterworking;

import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingRuntimeStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MasterworkingResolvedItemValueResolverTest {
    private final MasterworkingResolvedItemValueResolver resolver = new MasterworkingResolvedItemValueResolver();

    @Test
    void shouldResolveReferenceShieldValuesAtQualityTwentyFiveWithoutPerfectedAffix() {
        ItemMasterworking masterworking = new ItemMasterworking(25, 25);

        assertEquals(1502L, resolver.resolveArmor(1202L, masterworking));
        assertEquals(270.0d, resolver.resolveAffixValue(greaterAffix(ImportedItemAffixType.STRENGTH, 225.0d), masterworking), 0.0000001d);
        assertEquals(945.0d, resolver.resolveAffixValue(greaterAffix(ImportedItemAffixType.FIRE_RESISTANCE, 787.0d), masterworking), 0.0000001d);
        assertEquals(588.0d, resolver.resolveAffixValue(greaterAffix(ImportedItemAffixType.ALL_RESISTANCE, 490.0d), masterworking), 0.0000001d);
        assertEquals(14.3d, resolver.resolveAffixValue(affix(ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, false), masterworking), 0.0000001d);
        assertEquals(7.0d, resolver.resolveTemperingValue(maxAnimusTempering(), masterworking), 0.0000001d);
    }

    @Test
    void shouldResolveGenericNumericAffixesAtQualityTwentyFive() {
        ItemMasterworking masterworking = new ItemMasterworking(25, 25);

        assertEquals(217.0d, resolver.resolveAffixValue(affix(ImportedItemAffixType.STRENGTH, 173.6d, false), masterworking), 0.0000001d);
        assertEquals(11.0d, resolver.resolveAffixValue(affix(ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 8.8d, false), masterworking), 0.0000001d);
        assertEquals(17.6d, resolver.resolveAffixValue(affix(ImportedItemAffixType.DAMAGE_REDUCTION, 14.08d, false), masterworking), 0.0000001d);
        assertEquals(12.3d, resolver.resolveAffixValue(greaterAffix(ImportedItemAffixType.COOLDOWN_REDUCTION, 10.25d), masterworking), 0.0000001d);
    }

    @Test
    void shouldResolveReferenceShieldValuesAtQualityTwentyFiveWithPerfectedMaxAnimus() {
        ItemMasterworking masterworking = new ItemMasterworking(25, 25,
                MasterworkedAffixSelection.temperingAffix("defense_max_animus"));

        assertEquals(1502L, resolver.resolveArmor(1202L, masterworking));
        assertEquals(270.0d, resolver.resolveAffixValue(greaterAffix(ImportedItemAffixType.STRENGTH, 225.0d), masterworking), 0.0000001d);
        assertEquals(945.0d, resolver.resolveAffixValue(greaterAffix(ImportedItemAffixType.FIRE_RESISTANCE, 787.0d), masterworking), 0.0000001d);
        assertEquals(588.0d, resolver.resolveAffixValue(greaterAffix(ImportedItemAffixType.ALL_RESISTANCE, 490.0d), masterworking), 0.0000001d);
        assertEquals(14.3d, resolver.resolveAffixValue(affix(ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, false), masterworking), 0.0000001d);
        assertEquals(12.0d, resolver.resolveTemperingValue(maxAnimusTempering(), masterworking), 0.0000001d);
    }

    @Test
    void shouldResolvePerfectedOrdinaryAffixesOnlyForConfirmedGoldenValues() {
        ItemMasterworking strengthMasterworking = new ItemMasterworking(25, 25,
                MasterworkedAffixSelection.ordinaryAffix("STRENGTH"));
        ItemMasterworking fireMasterworking = new ItemMasterworking(25, 25,
                MasterworkedAffixSelection.ordinaryAffix("FIRE_RESISTANCE"));

        assertEquals(360.0d, resolver.resolveAffixValue(greaterAffix(ImportedItemAffixType.STRENGTH, 225.0d), strengthMasterworking), 0.0000001d);
        assertEquals(7.0d, resolver.resolveTemperingValue(maxAnimusTempering(), strengthMasterworking), 0.0000001d);
        assertEquals(1260.0d, resolver.resolveAffixValue(greaterAffix(ImportedItemAffixType.FIRE_RESISTANCE, 787.0d), fireMasterworking), 0.0000001d);
        assertEquals(7.0d, resolver.resolveTemperingValue(maxAnimusTempering(), fireMasterworking), 0.0000001d);
    }

    @Test
    void shouldKeepSourceValuesAtQualityZero() {
        ItemMasterworking masterworking = ItemMasterworking.defaultState();

        assertEquals(1202L, resolver.resolveArmor(1202L, masterworking));
        assertEquals(225.0d, resolver.resolveAffixValue(greaterAffix(ImportedItemAffixType.STRENGTH, 225.0d), masterworking), 0.0000001d);
        assertEquals(787.0d, resolver.resolveAffixValue(greaterAffix(ImportedItemAffixType.FIRE_RESISTANCE, 787.0d), masterworking), 0.0000001d);
        assertEquals(490.0d, resolver.resolveAffixValue(greaterAffix(ImportedItemAffixType.ALL_RESISTANCE, 490.0d), masterworking), 0.0000001d);
        assertEquals(11.4d, resolver.resolveAffixValue(affix(ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, false), masterworking), 0.0000001d);
        assertEquals(5.0d, resolver.resolveTemperingValue(maxAnimusTempering(), masterworking), 0.0000001d);
    }

    private static ImportedItemAffix greaterAffix(ImportedItemAffixType type, double value) {
        return affix(type, value, true);
    }

    private static ImportedItemAffix affix(ImportedItemAffixType type, double value, boolean greaterAffix) {
        return new ImportedItemAffix(type, value, "", greaterAffix, 0, type.getDisplayName(), ImportedItemAffixSource.OCR);
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

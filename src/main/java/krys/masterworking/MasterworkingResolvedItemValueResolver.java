package krys.masterworking;

import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingRuntimeSupport;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Wspólny resolver finalnych wartości itemu po Doskonaleniu dla UI i runtime. */
public final class MasterworkingResolvedItemValueResolver {
    public long resolveArmor(long baseArmor, ItemMasterworking masterworking) {
        return resolveArmor(baseArmor, quality(masterworking));
    }

    public long resolveArmor(long baseArmor, int qualityCurrent) {
        return (long) Math.floor(baseArmor * (1.0d + qualityCurrent / 100.0d));
    }

    public double resolveAffixValue(ImportedItemAffix affix, ItemMasterworking masterworking) {
        if (affix == null) {
            return 0.0d;
        }
        int quality = quality(masterworking);
        ImportedItemAffixType type = affix.getType();
        if (type == ImportedItemAffixType.STRENGTH && affix.isGreaterAffix()) {
            if (isPerfectedOrdinary(masterworking, type) && quality == ItemMasterworking.DEFAULT_QUALITY_MAX) {
                return 360.0d;
            }
            return resolveStrengthGreaterAffix(affix.getValue(), quality);
        }
        if (type == ImportedItemAffixType.ALL_RESISTANCE && affix.isGreaterAffix()) {
            return resolveAllResistanceGreaterAffix(affix.getValue(), quality);
        }
        if (type == ImportedItemAffixType.FIRE_RESISTANCE && affix.isGreaterAffix()) {
            if (isPerfectedOrdinary(masterworking, type) && quality == ItemMasterworking.DEFAULT_QUALITY_MAX) {
                return 1260.0d;
            }
            return resolveFireResistanceGreaterAffix(affix.getValue(), quality);
        }
        if (type == ImportedItemAffixType.DAMAGE_REDUCTION) {
            return resolveDamageReduction(affix.getValue(), quality);
        }
        return affix.getValue();
    }

    public boolean supportsAffix(ImportedItemAffix affix) {
        if (affix == null) {
            return false;
        }
        return (affix.getType() == ImportedItemAffixType.STRENGTH && affix.isGreaterAffix())
                || (affix.getType() == ImportedItemAffixType.ALL_RESISTANCE && affix.isGreaterAffix())
                || (affix.getType() == ImportedItemAffixType.FIRE_RESISTANCE && affix.isGreaterAffix())
                || affix.getType() == ImportedItemAffixType.DAMAGE_REDUCTION;
    }

    public double resolveTemperingValue(ItemTemperingAffix affix, ItemMasterworking masterworking) {
        if (affix == null) {
            return 0.0d;
        }
        if (!supportsTempering(affix)) {
            return affix.getValue();
        }
        return resolveMaxAnimusTempering(affix, masterworking);
    }

    public boolean supportsTempering(ItemTemperingAffix affix) {
        return TemperingRuntimeSupport.affectsMaximumAnimus(affix);
    }

    public int resolveStrengthGreaterAffix(double storedGreaterAffixValue, int qualityCurrent) {
        double normalMax = Math.round(storedGreaterAffixValue / 1.25d);
        return roundHalfUp(normalMax * (1.25d + qualityCurrent / 100.0d));
    }

    public int resolveAllResistanceGreaterAffix(double storedGreaterAffixValue, int qualityCurrent) {
        double normalMax = Math.round(storedGreaterAffixValue / 1.25d);
        return floor(normalMax * (1.25d + qualityCurrent / 100.0d));
    }

    public int resolveFireResistanceGreaterAffix(double storedGreaterAffixValue, int qualityCurrent) {
        double normalMax = Math.round(storedGreaterAffixValue / 1.25d);
        return floor(normalMax * (1.25d + qualityCurrent / 100.0d));
    }

    public double resolveDamageReduction(double baseValue, int qualityCurrent) {
        double raw = baseValue * (1.0d + qualityCurrent / 100.0d);
        return Math.ceil(raw * 10.0d - 0.000000001d) / 10.0d;
    }

    public int resolveMaxAnimusTempering(ItemTemperingAffix affix, ItemMasterworking masterworking) {
        int storedValue = roundHalfUp(affix.getValue());
        if (!supportsTempering(affix)) {
            return storedValue;
        }
        int quality = quality(masterworking);
        if (quality < ItemMasterworking.DEFAULT_QUALITY_MAX) {
            return storedValue;
        }
        if (isPerfectedTempering(masterworking, affix.getDefinitionId())) {
            return 12;
        }
        return 7;
    }

    public boolean isPerfectedOrdinary(ItemMasterworking masterworking, ImportedItemAffixType type) {
        MasterworkedAffixSelection selection = masterworking == null ? null : masterworking.getPerfectedAffix();
        return selection != null
                && selection.getSource() == MasterworkedAffixSource.ORDINARY_AFFIX
                && type.name().equals(selection.getKey());
    }

    public boolean isPerfectedTempering(ItemMasterworking masterworking, String definitionId) {
        MasterworkedAffixSelection selection = masterworking == null ? null : masterworking.getPerfectedAffix();
        return selection != null
                && selection.getSource() == MasterworkedAffixSource.TEMPERING_AFFIX
                && definitionId.equals(selection.getKey());
    }

    private static int quality(ItemMasterworking masterworking) {
        return masterworking == null ? ItemMasterworking.DEFAULT_QUALITY_CURRENT : masterworking.getQualityCurrent();
    }

    private static int floor(double value) {
        return (int) Math.floor(value + 0.000000001d);
    }

    private static int roundHalfUp(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).intValueExact();
    }
}

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
        if (affix.getReferenceValue() != null) {
            return affix.getValue();
        }
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
        if (supportsGenericNumericAffix(type)) {
            if (affix.isGreaterAffix()) {
                return resolveGenericGreaterAffix(type, affix.getValue(), quality);
            }
            return resolveGenericNonGreaterAffix(type, affix.getValue(), quality);
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
                || affix.getType() == ImportedItemAffixType.DAMAGE_REDUCTION
                || supportsGenericNumericAffix(affix.getType());
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

    public double resolveGenericNonGreaterAffix(ImportedItemAffixType type, double sourceValue, int qualityCurrent) {
        if (qualityCurrent <= ItemMasterworking.DEFAULT_QUALITY_CURRENT) {
            return sourceValue;
        }
        double raw = sourceValue * (1.0d + qualityCurrent / 100.0d);
        return roundForAffixType(type, raw);
    }

    public double resolveGenericGreaterAffix(ImportedItemAffixType type, double storedGreaterAffixValue, int qualityCurrent) {
        if (qualityCurrent <= ItemMasterworking.DEFAULT_QUALITY_CURRENT) {
            return storedGreaterAffixValue;
        }
        double normalMax = storedGreaterAffixValue / 1.25d;
        double raw = normalMax * (1.25d + qualityCurrent / 100.0d);
        return roundForAffixType(type, raw);
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

    private static double roundForAffixType(ImportedItemAffixType type, double value) {
        if (integerLikeAffix(type)) {
            return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).doubleValue();
        }
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private static boolean integerLikeAffix(ImportedItemAffixType type) {
        return switch (type) {
            case STRENGTH, INTELLIGENCE, THORNS, ALL_RESISTANCE, FIRE_RESISTANCE, WEAPON_DAMAGE_FLAT,
                 MAXIMUM_LIFE, LIFE_ON_HIT, LIFE_ON_KILL, LUCKY_HIT_PRIMARY_RESOURCE, CORE_SKILL_RANKS -> true;
            case DAMAGE_REDUCTION, BLOCK_CHANCE, RETRIBUTION_CHANCE, CRITICAL_STRIKE_CHANCE, LUCKY_HIT_CHANCE,
                 COOLDOWN_REDUCTION, MOVEMENT_SPEED, DODGE_CHANCE, DAMAGE_OVER_TIME_MULTIPLIER -> false;
        };
    }

    private static boolean supportsGenericNumericAffix(ImportedItemAffixType type) {
        return switch (type) {
            case STRENGTH, INTELLIGENCE, THORNS, BLOCK_CHANCE, RETRIBUTION_CHANCE, CRITICAL_STRIKE_CHANCE,
                 LUCKY_HIT_CHANCE, WEAPON_DAMAGE_FLAT, MAXIMUM_LIFE, LIFE_ON_HIT, LUCKY_HIT_PRIMARY_RESOURCE,
                 COOLDOWN_REDUCTION, MOVEMENT_SPEED, DODGE_CHANCE -> true;
            case ALL_RESISTANCE, FIRE_RESISTANCE, DAMAGE_REDUCTION, LIFE_ON_KILL, DAMAGE_OVER_TIME_MULTIPLIER,
                 CORE_SKILL_RANKS -> false;
        };
    }
}

package krys.masterworking;

import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.tempering.ItemTemperingAffix;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Przelicza wartosci itemu po Doskonaleniu tylko do prezentacji UI, bez runtime DPS. */
public final class MasterworkingPresentationValueResolver {
    public static final String NO_RULE_NOTE = "Doskonalenie: brak reguły prezentacyjnej dla tego affixu";
    public static final String RUNTIME_INACTIVE_NOTE = "Wartości po Doskonaleniu są prezentacyjne i nie wpływają jeszcze na runtime.";
    public static final String RUNTIME_STORED_TEMPERING_NOTE = "Runtime: nadal używa zapisanej wartości";
    private static final String MAX_ANIMUS_TEMPERING_ID = "defense_max_animus";

    public MasterworkingPresentationValue resolveArmor(long baseArmor, ItemMasterworking masterworking) {
        int quality = quality(masterworking);
        long displayArmor = displayArmor(baseArmor, quality);
        return new MasterworkingPresentationValue(
                "Pancerz",
                Long.toString(baseArmor),
                Long.toString(displayArmor),
                "",
                true,
                false,
                qualityNote(masterworking)
        );
    }

    public MasterworkingPresentationValue resolveAffix(ImportedItemAffix affix, ItemMasterworking masterworking) {
        if (affix == null) {
            return unsupported("", "", "");
        }
        int quality = quality(masterworking);
        ImportedItemAffixType type = affix.getType();
        if (type == ImportedItemAffixType.STRENGTH && affix.isGreaterAffix()) {
            int display = displayStrengthGreaterAffix(affix.getValue(), quality);
            boolean perfected = isPerfectedOrdinary(masterworking, type);
            if (perfected && quality == ItemMasterworking.DEFAULT_QUALITY_MAX) {
                display = 360;
            }
            return integerAffix(type.getDisplayName(), affix.getValue(), display, perfected, masterworking);
        }
        if (type == ImportedItemAffixType.ALL_RESISTANCE && affix.isGreaterAffix()) {
            return integerAffix(type.getDisplayName(), affix.getValue(),
                    displayAllResistanceGreaterAffix(affix.getValue(), quality), false, masterworking);
        }
        if (type == ImportedItemAffixType.FIRE_RESISTANCE && affix.isGreaterAffix()) {
            int display = displayFireResistanceGreaterAffix(affix.getValue(), quality);
            boolean perfected = isPerfectedOrdinary(masterworking, type);
            if (perfected && quality == ItemMasterworking.DEFAULT_QUALITY_MAX) {
                display = 1260;
            }
            return integerAffix(type.getDisplayName(), affix.getValue(), display, perfected, masterworking);
        }
        if (type == ImportedItemAffixType.DAMAGE_REDUCTION) {
            double display = displayDamageReduction(affix.getValue(), quality);
            return new MasterworkingPresentationValue(
                    type.getDisplayName(),
                    formatDecimalOne(affix.getValue()) + "%",
                    formatDecimalOne(display) + "%",
                    "",
                    true,
                    false,
                    qualityNote(masterworking)
            );
        }
        return unsupported(type.getDisplayName(), affix.getValueLabel(), NO_RULE_NOTE);
    }

    public MasterworkingPresentationValue resolveTempering(ItemTemperingAffix affix, ItemMasterworking masterworking) {
        if (affix == null) {
            return unsupported("", "", "");
        }
        String base = formatInteger(affix.getValue());
        if (!MAX_ANIMUS_TEMPERING_ID.equals(affix.getDefinitionId())) {
            return unsupported(affix.getDefinitionId(), "+" + base, NO_RULE_NOTE);
        }
        int quality = quality(masterworking);
        int display = displayMaxAnimusTempering(affix, masterworking);
        boolean perfected = isPerfectedTempering(masterworking, affix.getDefinitionId());
        return new MasterworkingPresentationValue(
                "maksymalna liczba kumulacji Animuszu",
                "+" + base,
                "+" + display,
                "do maksymalnej liczby kumulacji Animuszu",
                true,
                perfected && quality == ItemMasterworking.DEFAULT_QUALITY_MAX,
                RUNTIME_STORED_TEMPERING_NOTE + " +" + base
        );
    }

    public long displayArmor(long baseArmor, int qualityCurrent) {
        return (long) Math.floor(baseArmor * (1.0d + qualityCurrent / 100.0d));
    }

    public int displayStrengthGreaterAffix(double storedGreaterAffixValue, int qualityCurrent) {
        double normalMax = Math.round(storedGreaterAffixValue / 1.25d);
        return roundHalfUp(normalMax * (1.25d + qualityCurrent / 100.0d));
    }

    public int displayAllResistanceGreaterAffix(double storedGreaterAffixValue, int qualityCurrent) {
        double normalMax = Math.round(storedGreaterAffixValue / 1.25d);
        return floor(normalMax * (1.25d + qualityCurrent / 100.0d));
    }

    public int displayFireResistanceGreaterAffix(double storedGreaterAffixValue, int qualityCurrent) {
        double normalMax = Math.round(storedGreaterAffixValue / 1.25d);
        return floor(normalMax * (1.25d + qualityCurrent / 100.0d));
    }

    public double displayDamageReduction(double baseValue, int qualityCurrent) {
        double raw = baseValue * (1.0d + qualityCurrent / 100.0d);
        return Math.ceil(raw * 10.0d - 0.000000001d) / 10.0d;
    }

    public int displayMaxAnimusTempering(ItemTemperingAffix affix, ItemMasterworking masterworking) {
        int storedValue = roundHalfUp(affix.getValue());
        int quality = quality(masterworking);
        if (!MAX_ANIMUS_TEMPERING_ID.equals(affix.getDefinitionId())) {
            return storedValue;
        }
        if (quality < ItemMasterworking.DEFAULT_QUALITY_MAX) {
            return storedValue;
        }
        if (isPerfectedTempering(masterworking, affix.getDefinitionId())) {
            return 12;
        }
        return 7;
    }

    private static MasterworkingPresentationValue integerAffix(String label,
                                                               double baseValue,
                                                               int displayValue,
                                                               boolean perfected,
                                                               ItemMasterworking masterworking) {
        return new MasterworkingPresentationValue(
                label,
                formatInteger(baseValue),
                Integer.toString(displayValue),
                "",
                true,
                perfected,
                qualityNote(masterworking)
        );
    }

    private static MasterworkingPresentationValue unsupported(String label, String baseValue, String note) {
        return new MasterworkingPresentationValue(label, baseValue, baseValue, "", false, false, note);
    }

    private static boolean isPerfectedOrdinary(ItemMasterworking masterworking, ImportedItemAffixType type) {
        MasterworkedAffixSelection selection = masterworking == null ? null : masterworking.getPerfectedAffix();
        return selection != null
                && selection.getSource() == MasterworkedAffixSource.ORDINARY_AFFIX
                && type.name().equals(selection.getKey());
    }

    private static boolean isPerfectedTempering(ItemMasterworking masterworking, String definitionId) {
        MasterworkedAffixSelection selection = masterworking == null ? null : masterworking.getPerfectedAffix();
        return selection != null
                && selection.getSource() == MasterworkedAffixSource.TEMPERING_AFFIX
                && definitionId.equals(selection.getKey());
    }

    private static String qualityNote(ItemMasterworking masterworking) {
        ItemMasterworking safe = masterworking == null ? ItemMasterworking.defaultState() : masterworking;
        return "Doskonalenie: Jakość " + safe.qualityLabel();
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

    private static String formatInteger(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private static String formatDecimalOne(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .toPlainString()
                .replace('.', ',');
    }
}

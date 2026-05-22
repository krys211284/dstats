package krys.masterworking;

import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.tempering.ItemTemperingAffix;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Przelicza finalne wartosci itemu po Doskonaleniu do prezentacji UI. */
public final class MasterworkingPresentationValueResolver {
    public static final String NO_RULE_NOTE = "Doskonalenie: brak reguły prezentacyjnej dla tego affixu";
    public static final String RUNTIME_INACTIVE_NOTE = "Wartości po Doskonaleniu są używane w runtime dla potwierdzonych statystyk.";
    public static final String RUNTIME_STORED_TEMPERING_NOTE = "Runtime: używa wartości po Doskonaleniu";
    private final MasterworkingResolvedItemValueResolver resolvedValueResolver = new MasterworkingResolvedItemValueResolver();

    public MasterworkingPresentationValue resolveArmor(long baseArmor, ItemMasterworking masterworking) {
        int quality = quality(masterworking);
        long displayArmor = resolvedValueResolver.resolveArmor(baseArmor, quality);
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
            int display = (int) resolvedValueResolver.resolveAffixValue(affix, masterworking);
            boolean perfected = resolvedValueResolver.isPerfectedOrdinary(masterworking, type);
            return integerAffix(type.getDisplayName(), affix.getValue(), display, perfected, masterworking);
        }
        if (type == ImportedItemAffixType.ALL_RESISTANCE && affix.isGreaterAffix()) {
            return integerAffix(type.getDisplayName(), affix.getValue(),
                    (int) resolvedValueResolver.resolveAffixValue(affix, masterworking), false, masterworking);
        }
        if (type == ImportedItemAffixType.FIRE_RESISTANCE && affix.isGreaterAffix()) {
            int display = (int) resolvedValueResolver.resolveAffixValue(affix, masterworking);
            boolean perfected = resolvedValueResolver.isPerfectedOrdinary(masterworking, type);
            return integerAffix(type.getDisplayName(), affix.getValue(), display, perfected, masterworking);
        }
        if (type == ImportedItemAffixType.DAMAGE_REDUCTION) {
            double display = resolvedValueResolver.resolveAffixValue(affix, masterworking);
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
        if (!resolvedValueResolver.supportsTempering(affix)) {
            return unsupported(affix.getDefinitionId(), "+" + base, NO_RULE_NOTE);
        }
        int quality = quality(masterworking);
        int display = resolvedValueResolver.resolveMaxAnimusTempering(affix, masterworking);
        boolean perfected = resolvedValueResolver.isPerfectedTempering(masterworking, affix.getDefinitionId());
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
        return resolvedValueResolver.resolveArmor(baseArmor, qualityCurrent);
    }

    public int displayStrengthGreaterAffix(double storedGreaterAffixValue, int qualityCurrent) {
        return resolvedValueResolver.resolveStrengthGreaterAffix(storedGreaterAffixValue, qualityCurrent);
    }

    public int displayAllResistanceGreaterAffix(double storedGreaterAffixValue, int qualityCurrent) {
        return resolvedValueResolver.resolveAllResistanceGreaterAffix(storedGreaterAffixValue, qualityCurrent);
    }

    public int displayFireResistanceGreaterAffix(double storedGreaterAffixValue, int qualityCurrent) {
        return resolvedValueResolver.resolveFireResistanceGreaterAffix(storedGreaterAffixValue, qualityCurrent);
    }

    public double displayDamageReduction(double baseValue, int qualityCurrent) {
        return resolvedValueResolver.resolveDamageReduction(baseValue, qualityCurrent);
    }

    public int displayMaxAnimusTempering(ItemTemperingAffix affix, ItemMasterworking masterworking) {
        return resolvedValueResolver.resolveMaxAnimusTempering(affix, masterworking);
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

    private static String qualityNote(ItemMasterworking masterworking) {
        ItemMasterworking safe = masterworking == null ? ItemMasterworking.defaultState() : masterworking;
        return "Doskonalenie: Jakość " + safe.qualityLabel();
    }

    private static int quality(ItemMasterworking masterworking) {
        return masterworking == null ? ItemMasterworking.DEFAULT_QUALITY_CURRENT : masterworking.getQualityCurrent();
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

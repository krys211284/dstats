package krys.web;

import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportValueMode;
import krys.itemimport.image.DisplayedItemValueSourceNormalizer;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.masterworking.MasterworkingResolvedItemValueResolver;
import krys.tempering.ApplicationTemperingAffixRegistry;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingAffixDefinition;
import krys.tempering.TemperingAffixRegistry;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;

/** Zamienia wartości widoczne w grze z formularza weryfikacji importu na source values do zapisu. */
final class ItemImportDisplayedValueFormNormalizer {
    private static final double EPSILON = 0.0001d;

    private final DisplayedItemValueSourceNormalizer normalizer = new DisplayedItemValueSourceNormalizer();
    private final MasterworkingResolvedItemValueResolver valueResolver = new MasterworkingResolvedItemValueResolver();
    private final TemperingAffixRegistry temperingRegistry = ApplicationTemperingAffixRegistry.get();

    NormalizedForm normalize(ItemImportEditableForm displayedForm) {
        if (displayedForm == null || !displayedForm.usesGameDisplayedValues()) {
            return new NormalizedForm(displayedForm, List.of());
        }

        List<String> errors = new ArrayList<>();
        ItemMasterworking baseMasterworking = displayedForm.getMasterworking();
        List<DisplayedItemValueSourceNormalizer.DisplayedAffix> displayedAffixes = new ArrayList<>();
        List<ImportedItemAffix> sourceAffixes = new ArrayList<>();
        for (ImportedItemAffix displayedAffix : displayedForm.getAffixes()) {
            double displayedValue = displayedAffix.getValue();
            OptionalDouble sourceValue = displayedForm.isMythicUnique()
                    ? OptionalDouble.of(displayedValue)
                    : normalizer.reverseAffix(displayedAffix, displayedValue, baseMasterworking);
            if (sourceValue.isEmpty()) {
                errors.add("Affix " + displayedAffix.getName() + ": nie udało się jednoznacznie przeliczyć wartości z gry na source value.");
                sourceValue = OptionalDouble.of(displayedValue);
            }
            ImportedItemAffix sourceAffix = new ImportedItemAffix(
                    displayedAffix.getType(),
                    sourceValue.getAsDouble(),
                    displayedAffix.getUnit(),
                    displayedAffix.isGreaterAffix(),
                    displayedAffix.getDisplayOrder(),
                    displayedAffix.getRawOcrLine(),
                    displayedAffix.getSource(),
                    displayedAffix.getAffixDefinitionId(),
                    displayedAffix.getRollRangeMin(),
                    displayedAffix.getRollRangeMax(),
                    displayedAffix.getReferenceValue(),
                    displayValueLabel(displayedAffix, displayedValue)
            );
            sourceAffixes.add(sourceAffix);
            displayedAffixes.add(new DisplayedItemValueSourceNormalizer.DisplayedAffix(sourceAffix, displayedValue));
        }

        List<DisplayedItemValueSourceNormalizer.DisplayedTempering> displayedTempering = new ArrayList<>();
        List<ItemTemperingAffix> sourceTempering = new ArrayList<>();
        for (ItemTemperingAffix displayedAffix : displayedForm.getTemperingAffixes()) {
            double displayedValue = displayedAffix.getValue();
            TemperingAffixDefinition definition = temperingRegistry.findById(displayedAffix.getDefinitionId()).orElse(null);
            Optional<ItemTemperingAffix> sourceAffix = normalizer.reverseTempering(
                    displayedAffix,
                    displayedValue,
                    baseMasterworking,
                    definition
            );
            if (sourceAffix.isEmpty()) {
                errors.add("Hartowanie " + displayedAffix.getDefinitionId() + ": nie udało się jednoznacznie przeliczyć wartości z gry na source value.");
                sourceAffix = Optional.of(displayedAffix);
            }
            ItemTemperingAffix normalizedTempering = new ItemTemperingAffix(
                    sourceAffix.get().getDefinitionId(),
                    sourceAffix.get().getCategory(),
                    sourceAffix.get().getValue(),
                    displayedTemperingText(displayedAffix, displayedValue),
                    displayedTemperingText(displayedAffix, displayedValue),
                    sourceAffix.get().getRuntimeStatus(),
                    sourceAffix.get().isGreaterAffix()
            );
            sourceTempering.add(normalizedTempering);
            displayedTempering.add(new DisplayedItemValueSourceNormalizer.DisplayedTempering(normalizedTempering, displayedValue));
        }

        ItemMasterworking masterworking = detectMasterworking(
                baseMasterworking,
                displayedAffixes,
                displayedTempering,
                displayedForm.isMythicUnique(),
                errors
        );
        ItemImportDetails sourceDetails = sourceDetails(displayedForm, masterworking, errors);
        ItemImportEditableForm sourceForm = new ItemImportEditableForm(
                displayedForm.getSourceImageName(),
                displayedForm.getSlot(),
                displayedForm.getWeaponDamage(),
                displayedForm.getStrength(),
                displayedForm.getIntelligence(),
                displayedForm.getThorns(),
                displayedForm.getBlockChance(),
                displayedForm.getRetributionChance(),
                displayedForm.getFullItemRead(),
                sourceAffixes,
                displayedForm.getSelectedAspectId(),
                sourceDetails,
                sourceTempering,
                masterworking,
                displayedForm.getTransfiguration(),
                displayedForm.getSocketing(),
                ItemImportValueMode.SOURCE_VALUE
        );
        return new NormalizedForm(sourceForm, errors);
    }

    private ItemImportDetails sourceDetails(ItemImportEditableForm displayedForm,
                                            ItemMasterworking masterworking,
                                            List<String> errors) {
        ItemImportDetails displayed = displayedForm.getDetails();
        Long sourceArmor = displayed.getItemArmor();
        if (sourceArmor != null && sourceArmor > 0L && !displayed.isMythicUnique()) {
            OptionalLong reversedArmor = normalizer.reverseArmor(sourceArmor, masterworking);
            if (reversedArmor.isPresent()) {
                sourceArmor = reversedArmor.getAsLong();
            } else {
                errors.add("Pancerz: nie udało się jednoznacznie przeliczyć wartości z gry na source value.");
            }
        }
        return new ItemImportDetails(
                displayed.getItemName(),
                displayed.getItemType(),
                displayed.getItemRarity(),
                displayed.isAncient(),
                displayed.getEquipmentSlot(),
                displayed.getItemPower(),
                displayed.getWeaponDps(),
                displayed.getWeaponDamageMin(),
                displayed.getWeaponDamageMax(),
                displayed.getAverageWeaponDamage(),
                displayed.getAttacksPerSecond(),
                sourceArmor,
                displayed.getUniqueEffectText(),
                displayed.isMythicUnique()
        );
    }

    private ItemMasterworking detectMasterworking(ItemMasterworking baseMasterworking,
                                                  List<DisplayedItemValueSourceNormalizer.DisplayedAffix> displayedAffixes,
                                                  List<DisplayedItemValueSourceNormalizer.DisplayedTempering> displayedTempering,
                                                  boolean mythicUnique,
                                                  List<String> errors) {
        if (baseMasterworking == null || baseMasterworking.getQualityCurrent() != ItemMasterworking.DEFAULT_QUALITY_MAX) {
            return baseMasterworking == null ? ItemMasterworking.defaultState() : baseMasterworking;
        }
        if (baseMasterworking.getPerfectedAffix() != null) {
            if (!matchesDisplayedValues(baseMasterworking, displayedAffixes, displayedTempering, mythicUnique)) {
                errors.add("Doskonalenie: jawnie wybrany perfectedAffix nie odtwarza wartości widocznych w grze.");
            }
            return baseMasterworking;
        }
        Optional<MasterworkedAffixSelection> perfectedAffix = normalizer.detectPerfectedAffix(
                displayedAffixes,
                displayedTempering,
                baseMasterworking
        );
        if (perfectedAffix.isPresent()) {
            return new ItemMasterworking(baseMasterworking.getQualityCurrent(), baseMasterworking.getQualityMax(), perfectedAffix.get());
        }
        if (normalizer.hasAmbiguousPerfectedAffix(displayedAffixes, displayedTempering, baseMasterworking)) {
            errors.add("Doskonalenie: więcej niż jeden affix pasuje do wartości widocznych w grze jako aktualnie doskonalony.");
        }
        return new ItemMasterworking(baseMasterworking.getQualityCurrent(), baseMasterworking.getQualityMax());
    }

    private boolean matchesDisplayedValues(ItemMasterworking masterworking,
                                           List<DisplayedItemValueSourceNormalizer.DisplayedAffix> displayedAffixes,
                                           List<DisplayedItemValueSourceNormalizer.DisplayedTempering> displayedTempering,
                                           boolean mythicUnique) {
        for (DisplayedItemValueSourceNormalizer.DisplayedAffix displayedAffix
                : displayedAffixes == null ? List.<DisplayedItemValueSourceNormalizer.DisplayedAffix>of() : displayedAffixes) {
            if (displayedAffix.affix() == null) {
                continue;
            }
            if (!sameValue(valueResolver.resolveAffixValue(displayedAffix.affix(), masterworking, mythicUnique),
                    displayedAffix.displayedValue())) {
                return false;
            }
        }
        for (DisplayedItemValueSourceNormalizer.DisplayedTempering displayedTemperingAffix
                : displayedTempering == null ? List.<DisplayedItemValueSourceNormalizer.DisplayedTempering>of() : displayedTempering) {
            if (displayedTemperingAffix.affix() == null) {
                continue;
            }
            if (!sameValue(valueResolver.resolveTemperingValue(displayedTemperingAffix.affix(), masterworking),
                    displayedTemperingAffix.displayedValue())) {
                return false;
            }
        }
        return true;
    }

    private static String displayValueLabel(ImportedItemAffix affix, double displayedValue) {
        String existing = affix.getDisplayValue();
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        return decimalLabel(displayedValue) + affix.getUnit();
    }

    private static String displayedTemperingText(ItemTemperingAffix affix, double displayedValue) {
        String existing = affix.getDisplayText();
        if (existing != null && !existing.isBlank() && existing.contains(decimalLabel(displayedValue))) {
            return existing;
        }
        return "+" + decimalLabel(displayedValue) + " " + affix.getDefinitionId();
    }

    private static String decimalLabel(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().replace('.', ',');
    }

    private static boolean sameValue(double left, double right) {
        return Math.abs(left - right) < EPSILON;
    }

    record NormalizedForm(ItemImportEditableForm form, List<String> errors) {
        NormalizedForm {
            errors = List.copyOf(errors == null ? List.of() : errors);
        }
    }
}

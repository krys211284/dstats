package krys.itemimport;

import krys.item.EquipmentSlot;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.masterworking.MasterworkedAffixSource;
import krys.tempering.ApplicationTemperingAffixRegistry;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingAffixDefinition;
import krys.tempering.TemperingAffixRegistry;
import krys.tempering.TemperingEligibilityRegistry;
import krys.tempering.TemperingPresentationSupport;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Waliduje ręcznie poprawiony formularz itemu i buduje zatwierdzony model domenowy. */
public final class ItemImportFormMapper {
    private final AspectRegistry aspectRegistry;
    private final TemperingAffixRegistry temperingAffixRegistry;

    public ItemImportFormMapper() {
        this(ApplicationAspectRegistry.get(), ApplicationTemperingAffixRegistry.get());
    }

    ItemImportFormMapper(AspectRegistry aspectRegistry) {
        this(aspectRegistry, ApplicationTemperingAffixRegistry.get());
    }

    ItemImportFormMapper(AspectRegistry aspectRegistry, TemperingAffixRegistry temperingAffixRegistry) {
        this.aspectRegistry = aspectRegistry;
        this.temperingAffixRegistry = temperingAffixRegistry;
    }

    public MappingResult map(ItemImportEditableForm form) {
        List<String> errors = new ArrayList<>();
        EquipmentSlot slot = parseSlot(form.getSlot(), errors);
        Long weaponDamage = parseLong(resolveVisibleWeaponDamage(form), "Weapon damage", errors);
        RuntimeProjection projection = projectAffixes(form.getAffixes());
        Double strength = projection.strength();
        Double intelligence = projection.intelligence();
        Double thorns = projection.thorns();
        Double blockChance = projection.blockChance() + visibleImplicitBlockChance(form.getFullItemRead());
        Double retributionChance = projection.retributionChance();

        if (slot == null || weaponDamage == null || strength == null || intelligence == null
                || thorns == null || blockChance == null || retributionChance == null) {
            return new MappingResult(null, errors);
        }

        if (slot != EquipmentSlot.MAIN_HAND && weaponDamage > 0L) {
            errors.add("Weapon damage można ustawić wyłącznie dla slotu MAIN_HAND.");
        }
        String selectedAspectId = validateAspect(form.getSelectedAspectId(), slot, errors);
        ItemImportDetails details = buildDetails(form, slot, errors);
        List<ItemTemperingAffix> temperingAffixes = validateTempering(form.getTemperingAffixes(), slot, details.getItemType(), details.getItemPower(), errors);
        ItemMasterworking masterworking = validateMasterworking(form.getMasterworking(), form.getAffixes(), temperingAffixes, errors);

        if (!errors.isEmpty()) {
            return new MappingResult(null, errors);
        }

        return new MappingResult(new ValidatedImportedItem(
                form.getSourceImageName(),
                slot,
                weaponDamage,
                strength,
                intelligence,
                thorns,
                blockChance,
                retributionChance,
                form.getAffixes(),
                selectedAspectId,
                details,
                temperingAffixes,
                masterworking
        ), errors);
    }

    private ItemMasterworking validateMasterworking(ItemMasterworking masterworking,
                                                    List<ImportedItemAffix> affixes,
                                                    List<ItemTemperingAffix> temperingAffixes,
                                                    List<String> errors) {
        ItemMasterworking safe = masterworking == null ? ItemMasterworking.defaultState() : masterworking;
        if (!ItemMasterworking.isAllowedQualityStep(safe.getQualityCurrent())) {
            errors.add("Doskonalenie: Jakość Doskonalenia musi być jednym z progów: "
                    + ItemMasterworking.allowedQualityStepsLabel()
                    + ".");
        }
        if (safe.getQualityMax() != ItemMasterworking.DEFAULT_QUALITY_MAX) {
            errors.add("Doskonalenie: Jakość maksymalna musi wynosić 25.");
        }
        validatePerfectedAffix(safe, affixes, temperingAffixes, errors);
        return safe;
    }

    private void validatePerfectedAffix(ItemMasterworking masterworking,
                                        List<ImportedItemAffix> affixes,
                                        List<ItemTemperingAffix> temperingAffixes,
                                        List<String> errors) {
        MasterworkedAffixSelection selection = masterworking.getPerfectedAffix();
        if (selection == null) {
            return;
        }
        if (masterworking.getQualityCurrent() < ItemMasterworking.DEFAULT_QUALITY_MAX) {
            errors.add("Doskonalenie: aktualny doskonalony afiks można wskazać dopiero przy jakości 25/25.");
            return;
        }
        if (!selection.hasRecognizedSource()) {
            errors.add("Doskonalenie: aktualny doskonalony afiks ma nieznany typ.");
            return;
        }
        if (selection.getKey().isBlank()) {
            errors.add("Doskonalenie: aktualny doskonalony afiks wymaga klucza.");
            return;
        }
        if (selection.getSource() == MasterworkedAffixSource.ORDINARY_AFFIX) {
            validateOrdinaryPerfectedAffix(selection, affixes, errors);
            return;
        }
        if (selection.getSource() == MasterworkedAffixSource.TEMPERING_AFFIX) {
            validateTemperingPerfectedAffix(selection, temperingAffixes, errors);
        }
    }

    private static void validateOrdinaryPerfectedAffix(MasterworkedAffixSelection selection,
                                                       List<ImportedItemAffix> affixes,
                                                       List<String> errors) {
        ImportedItemAffixType type;
        try {
            type = ImportedItemAffixType.valueOf(selection.getKey());
        } catch (IllegalArgumentException exception) {
            errors.add("Doskonalenie: wskazany zwykły affix nie istnieje w katalogu itemu.");
            return;
        }
        boolean itemHasAffix = affixes.stream().anyMatch(affix -> affix.getType() == type);
        if (!itemHasAffix) {
            errors.add("Doskonalenie: wskazany zwykły affix nie występuje na itemie.");
        }
    }

    private void validateTemperingPerfectedAffix(MasterworkedAffixSelection selection,
                                                 List<ItemTemperingAffix> temperingAffixes,
                                                 List<String> errors) {
        if (temperingAffixRegistry.findById(selection.getKey()).isEmpty()) {
            errors.add("Doskonalenie: wskazane hartowanie nie istnieje w katalogu.");
            return;
        }
        boolean itemHasTempering = temperingAffixes.stream()
                .anyMatch(affix -> affix.getDefinitionId().equals(selection.getKey()));
        if (!itemHasTempering) {
            errors.add("Doskonalenie: wskazane hartowanie nie występuje na itemie.");
        }
    }

    private List<ItemTemperingAffix> validateTempering(List<ItemTemperingAffix> affixes,
                                                       EquipmentSlot slot,
                                                       String itemType,
                                                       Long itemPower,
                                                       List<String> errors) {
        List<ItemTemperingAffix> validated = new ArrayList<>();
        if (affixes.size() > 1) {
            errors.add("Hartowanie: limit hartowania dla tego przedmiotu wynosi 1.");
            return List.of();
        }
        int index = 0;
        for (ItemTemperingAffix affix : affixes) {
            index++;
            if (!TemperingEligibilityRegistry.isCategoryAvailable(slot, itemType, affix.getCategory())) {
                errors.add("Hartowanie #" + index + ": kategoria " + affix.getCategory().getDisplayName()
                        + " nie jest dostępna dla tego typu itemu.");
                continue;
            }
            TemperingAffixDefinition definition = temperingAffixRegistry.findById(affix.getDefinitionId()).orElse(null);
            if (definition == null) {
                errors.add("Hartowanie #" + index + ": wybrany affix nie istnieje w katalogu hartowania.");
                continue;
            }
            if (definition.getCategory() != affix.getCategory()) {
                errors.add("Hartowanie #" + index + ": affix nie należy do wybranej kategorii.");
                continue;
            }
            if (affix.isGreaterAffix()) {
                if (itemPower == null || itemPower != 900L) {
                    errors.add("Hartowanie #" + index + ": Greater Affix przy hartowaniu jest dostępny tylko dla przedmiotów o mocy 900.");
                    continue;
                }
                if (!sameValue(affix.getValue(), definition.greaterAffixValue())) {
                    errors.add("Hartowanie #" + index + ": wartość Greater Affix musi wynosić "
                            + TemperingPresentationSupport.formatGreaterAffixValue(definition) + ".");
                    continue;
                }
            } else if (!definition.accepts(affix.getValue())) {
                errors.add("Hartowanie #" + index + ": wartość musi być w zakresie "
                        + TemperingPresentationSupport.formatRange(definition) + ".");
                continue;
            }
            String displayText = affix.getDisplayText().isBlank()
                    ? TemperingPresentationSupport.formatAffix(affix, temperingAffixRegistry)
                    : affix.getDisplayText();
            validated.add(new ItemTemperingAffix(
                    affix.getDefinitionId(),
                    affix.getCategory(),
                    affix.getValue(),
                    displayText,
                    definition.getRuntimeStatus(),
                    affix.isGreaterAffix()
            ));
        }
        return List.copyOf(validated);
    }

    private static boolean sameValue(double left, double right) {
        return Math.abs(left - right) < 0.0000001d;
    }

    private static ItemImportDetails buildDetails(ItemImportEditableForm form,
                                                  EquipmentSlot slot,
                                                  List<String> errors) {
        Long itemPower = parseOptionalLong(form.getItemPower(), "Moc przedmiotu", errors);
        Long weaponDps = parseOptionalLong(form.getWeaponDps(), "DPS broni", errors);
        Long weaponDamageMin = parseOptionalLong(form.getWeaponDamageMin(), "Minimalne obrażenia za trafienie", errors);
        Long weaponDamageMax = parseOptionalLong(form.getWeaponDamageMax(), "Maksymalne obrażenia za trafienie", errors);
        Long averageWeaponDamage = parseOptionalLong(form.getAverageWeaponDamage(), "Średnie obrażenia za trafienie", errors);
        Double attacksPerSecond = parseOptionalDouble(form.getAttacksPerSecond(), "Ataki na sekundę", errors);
        Long itemArmor = parseOptionalLong(form.getItemArmor(), "Pancerz", errors);

        if (weaponDamageMin != null && weaponDamageMax != null) {
            long calculatedAverage = Math.round((weaponDamageMin + weaponDamageMax) / 2.0d);
            if (averageWeaponDamage == null || averageWeaponDamage == 0L) {
                averageWeaponDamage = calculatedAverage;
            }
        }

        EquipmentSlot detailSlot = slot == null ? form.getDetails().getEquipmentSlot() : slot;
        return new ItemImportDetails(
                form.getItemName(),
                form.getItemType(),
                form.getItemRarity(),
                form.isAncient(),
                detailSlot,
                itemPower,
                weaponDps,
                weaponDamageMin,
                weaponDamageMax,
                averageWeaponDamage,
                attacksPerSecond,
                itemArmor,
                form.getUniqueEffectText()
        );
    }

    private String validateAspect(String rawAspectId, EquipmentSlot slot, List<String> errors) {
        if (rawAspectId == null || rawAspectId.isBlank()) {
            return "";
        }
        AspectDefinition aspect = aspectRegistry.findById(rawAspectId)
                .orElse(null);
        if (aspect == null) {
            errors.add("Wybrany aspekt nie istnieje w rejestrze aspektów.");
            return "";
        }
        if (!aspect.allowsSlot(slot)) {
            errors.add("Wybrany aspekt nie pasuje do slotu itemu.");
            return "";
        }
        return aspect.getId();
    }

    private static RuntimeProjection projectAffixes(List<ImportedItemAffix> affixes) {
        double strength = 0.0d;
        double intelligence = 0.0d;
        double thorns = 0.0d;
        double blockChance = 0.0d;
        double retributionChance = 0.0d;
        for (ImportedItemAffix affix : affixes) {
            switch (affix.getType().getRuntimeProjection()) {
                case STRENGTH -> strength += affix.getValue();
                case INTELLIGENCE -> intelligence += affix.getValue();
                case THORNS -> thorns += affix.getValue();
                case BLOCK_CHANCE -> blockChance += affix.getValue();
                case RETRIBUTION_CHANCE -> retributionChance += affix.getValue();
                case NONE -> {
                }
            }
        }
        return new RuntimeProjection(strength, intelligence, thorns, blockChance, retributionChance);
    }

    private static EquipmentSlot parseSlot(String rawValue, List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            errors.add("Slot itemu jest wymagany.");
            return null;
        }
        try {
            return EquipmentSlot.valueOf(rawValue.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            errors.add("Niepoprawny slot itemu.");
            return null;
        }
    }

    private static Long parseLong(String rawValue, String label, List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0L;
        }
        try {
            long value = Long.parseLong(rawValue.replace(" ", ""));
            if (value < 0L) {
                errors.add(label + " nie może być ujemny.");
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            errors.add(label + " musi być liczbą całkowitą.");
            return null;
        }
    }

    private static Long parseOptionalLong(String rawValue, String label, List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return parseLong(rawValue, label, errors);
    }

    private static Double parseDouble(String rawValue, String label, List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0.0d;
        }
        try {
            double value = Double.parseDouble(rawValue.replace(',', '.'));
            if (value < 0.0d) {
                errors.add(label + " nie może być ujemny.");
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            errors.add(label + " musi być liczbą.");
            return null;
        }
    }

    private static Double parseOptionalDouble(String rawValue, String label, List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        return parseDouble(rawValue, label, errors);
    }

    private static String resolveVisibleWeaponDamage(ItemImportEditableForm form) {
        if (form.getWeaponDamage() != null && !form.getWeaponDamage().isBlank()) {
            return form.getWeaponDamage();
        }
        String baseItemValue = form.getFullItemRead().getBaseItemValue();
        String normalized = normalize(baseItemValue);
        if (!normalized.contains("OBRAZEN") && !normalized.contains("DAMAGE")) {
            return "";
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+(?:\\s\\d{3})*").matcher(baseItemValue);
        return matcher.find() ? matcher.group().replace(" ", "") : "";
    }

    private static double visibleImplicitBlockChance(FullItemRead fullItemRead) {
        for (FullItemReadLine line : fullItemRead.getLines()) {
            String normalized = normalize(line.getText());
            if (!normalized.contains("SZANSY NA BLOK") && !normalized.contains("BLOCK CHANCE")) {
                continue;
            }
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("([0-9]+(?:[,.][0-9]+)?)").matcher(line.getText());
            if (matcher.find()) {
                return Double.parseDouble(matcher.group(1).replace(',', '.'));
            }
        }
        return 0.0d;
    }

    private static String normalize(String value) {
        return java.text.Normalizer.normalize(value == null ? "" : value, java.text.Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    public static final class MappingResult {
        private final ValidatedImportedItem item;
        private final List<String> errors;

        public MappingResult(ValidatedImportedItem item, List<String> errors) {
            this.item = item;
            this.errors = List.copyOf(errors);
        }

        public ValidatedImportedItem getItem() {
            return item;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    private record RuntimeProjection(double strength,
                                     double intelligence,
                                     double thorns,
                                     double blockChance,
                                     double retributionChance) {
    }
}

package krys.itemimport;

import krys.item.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Waliduje ręcznie poprawiony formularz itemu i buduje zatwierdzony model domenowy. */
public final class ItemImportFormMapper {
    private final AspectRegistry aspectRegistry;

    public ItemImportFormMapper() {
        this(ApplicationAspectRegistry.get());
    }

    ItemImportFormMapper(AspectRegistry aspectRegistry) {
        this.aspectRegistry = aspectRegistry;
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
                details
        ), errors);
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

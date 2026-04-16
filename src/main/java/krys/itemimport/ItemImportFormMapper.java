package krys.itemimport;

import krys.item.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Waliduje ręcznie poprawiony formularz itemu i buduje zatwierdzony model domenowy. */
public final class ItemImportFormMapper {
    public MappingResult map(ItemImportEditableForm form) {
        List<String> errors = new ArrayList<>();
        EquipmentSlot slot = parseSlot(form.getSlot(), errors);
        Long weaponDamage = parseLong(form.getWeaponDamage(), "Weapon damage", errors);
        Double strength = parseDouble(form.getStrength(), "Strength", errors);
        Double intelligence = parseDouble(form.getIntelligence(), "Intelligence", errors);
        Double thorns = parseDouble(form.getThorns(), "Thorns", errors);
        Double blockChance = parseDouble(form.getBlockChance(), "Block chance", errors);
        Double retributionChance = parseDouble(form.getRetributionChance(), "Retribution chance", errors);

        if (slot == null || weaponDamage == null || strength == null || intelligence == null
                || thorns == null || blockChance == null || retributionChance == null) {
            return new MappingResult(null, errors);
        }

        if (slot == EquipmentSlot.MAIN_HAND && weaponDamage <= 0L) {
            errors.add("Weapon damage jest wymagany dla slotu MAIN_HAND.");
        }
        if (slot != EquipmentSlot.MAIN_HAND && weaponDamage > 0L) {
            errors.add("Weapon damage można ustawić wyłącznie dla slotu MAIN_HAND.");
        }

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
                retributionChance
        ), errors);
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
            long value = Long.parseLong(rawValue);
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

    private static Double parseDouble(String rawValue, String label, List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0.0d;
        }
        try {
            double value = Double.parseDouble(rawValue);
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
}

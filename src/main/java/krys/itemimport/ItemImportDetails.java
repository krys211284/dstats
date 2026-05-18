package krys.itemimport;

import krys.item.EquipmentSlot;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strukturalne dane itemu odczytane z OCR albo poprawione ręcznie przed zapisem. */
public final class ItemImportDetails {
    private final String itemName;
    private final String itemType;
    private final String itemRarity;
    private final boolean ancient;
    private final EquipmentSlot equipmentSlot;
    private final Long itemPower;
    private final Long weaponDps;
    private final Long weaponDamageMin;
    private final Long weaponDamageMax;
    private final Long averageWeaponDamage;
    private final Double attacksPerSecond;
    private final Long itemArmor;
    private final String uniqueEffectText;

    public ItemImportDetails(String itemName,
                             String itemType,
                             String itemRarity,
                             boolean ancient,
                             EquipmentSlot equipmentSlot,
                             Long itemPower,
                             Long weaponDps,
                             Long weaponDamageMin,
                             Long weaponDamageMax,
                             Long averageWeaponDamage,
                             Double attacksPerSecond,
                             String uniqueEffectText) {
        this(itemName, itemType, itemRarity, ancient, equipmentSlot, itemPower, weaponDps,
                weaponDamageMin, weaponDamageMax, averageWeaponDamage, attacksPerSecond, null, uniqueEffectText);
    }

    public ItemImportDetails(String itemName,
                             String itemType,
                             String itemRarity,
                             boolean ancient,
                             EquipmentSlot equipmentSlot,
                             Long itemPower,
                             Long weaponDps,
                             Long weaponDamageMin,
                             Long weaponDamageMax,
                             Long averageWeaponDamage,
                             Double attacksPerSecond,
                             Long itemArmor,
                             String uniqueEffectText) {
        this.itemName = normalize(itemName);
        this.itemType = normalize(itemType);
        this.itemRarity = normalize(itemRarity);
        this.ancient = ancient;
        this.equipmentSlot = equipmentSlot;
        this.itemPower = nonNegativeOrNull("Moc przedmiotu", itemPower);
        this.weaponDps = nonNegativeOrNull("DPS broni", weaponDps);
        this.weaponDamageMin = nonNegativeOrNull("Minimalne obrażenia broni", weaponDamageMin);
        this.weaponDamageMax = nonNegativeOrNull("Maksymalne obrażenia broni", weaponDamageMax);
        this.averageWeaponDamage = nonNegativeOrNull("Średnie obrażenia broni",
                averageWeaponDamage == null ? calculateAverage(weaponDamageMin, weaponDamageMax) : averageWeaponDamage);
        this.attacksPerSecond = nonNegativeOrNull("Ataki na sekundę", attacksPerSecond);
        this.itemArmor = nonNegativeOrNull("Pancerz", itemArmor);
        this.uniqueEffectText = normalizeFortifyLegendaryEffect(uniqueEffectText).orElse(normalize(uniqueEffectText));
    }

    public static ItemImportDetails empty() {
        return new ItemImportDetails("", "", "", false, null, null, null, null, null, null, null, "");
    }

    private static Long calculateAverage(Long min, Long max) {
        if (min == null || max == null) {
            return null;
        }
        return Math.round((min + max) / 2.0d);
    }

    private static Long nonNegativeOrNull(String label, Long value) {
        if (value != null && value < 0L) {
            throw new IllegalArgumentException(label + " nie może być ujemna.");
        }
        return value;
    }

    private static Double nonNegativeOrNull(String label, Double value) {
        if (value != null && value < 0.0d) {
            throw new IllegalArgumentException(label + " nie może być ujemne.");
        }
        return value;
    }

    public String getItemName() {
        return itemName;
    }

    public String getItemType() {
        return itemType;
    }

    public String getItemRarity() {
        return itemRarity;
    }

    public boolean isAncient() {
        return ancient;
    }

    public EquipmentSlot getEquipmentSlot() {
        return equipmentSlot;
    }

    public Long getItemPower() {
        return itemPower;
    }

    public Long getWeaponDps() {
        return weaponDps;
    }

    public Long getWeaponDamageMin() {
        return weaponDamageMin;
    }

    public Long getWeaponDamageMax() {
        return weaponDamageMax;
    }

    public Long getAverageWeaponDamage() {
        return averageWeaponDamage;
    }

    public Double getAttacksPerSecond() {
        return attacksPerSecond;
    }

    public String getUniqueEffectText() {
        return uniqueEffectText;
    }

    public Long getItemArmor() {
        return itemArmor;
    }

    public boolean hasAnyData() {
        return !itemName.isBlank()
                || !itemType.isBlank()
                || !itemRarity.isBlank()
                || ancient
                || equipmentSlot != null
                || itemPower != null
                || weaponDps != null
                || weaponDamageMin != null
                || weaponDamageMax != null
                || averageWeaponDamage != null
                || attacksPerSecond != null
                || itemArmor != null
                || !uniqueEffectText.isBlank();
    }

    public String getRarityOrUnknown() {
        return itemRarity.isBlank() ? "UNKNOWN" : itemRarity.toUpperCase(Locale.ROOT);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static Optional<String> normalizeFortifyLegendaryEffect(String text) {
        String normalized = normalizeLineForPatternKeepingPlus(text);
        String collapsed = normalized.replaceAll("[^A-Z0-9]", "");
        if (!collapsed.contains("GDYMASZUMOCNIENIE")
                || !collapsed.contains("ZADAJESZOBRAZENIAZWIEKSZONE")) {
            return Optional.empty();
        }
        Optional<RollRange> range = parseFortifyRollRange(normalized);
        Optional<Integer> roll = parseFortifyRoll(normalized, range);
        if (range.isEmpty() || roll.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("Gdy masz umocnienie, zadajesz obrażenia zwiększone o "
                + roll.get()
                + "%[x] ["
                + range.get().min()
                + " - "
                + range.get().max()
                + "]%.");
    }

    private static Optional<RollRange> parseFortifyRollRange(String normalizedText) {
        Matcher matcher = Pattern.compile("\\[\\s*([0-9OISBL]{1,3})\\s*[-–—−]\\s*([0-9OISBL]{1,3})\\s*]?\\s*%?").matcher(normalizedText);
        while (matcher.find()) {
            Optional<Integer> min = parseFortifyInteger(matcher.group(1));
            Optional<Integer> max = parseFortifyInteger(matcher.group(2));
            if (min.isPresent() && max.isPresent() && min.get() < max.get()) {
                return Optional.of(new RollRange(min.get(), max.get()));
            }
        }
        return Optional.empty();
    }

    private static Optional<Integer> parseFortifyRoll(String normalizedText, Optional<RollRange> range) {
        Matcher matcher = Pattern.compile("ZWIEKSZONE\\s+O\\s+([0-9OISBL]+(?:\\s+[0-9OISBL]+)?)(?:\\s*%?\\s*\\[?\\s*X\\s*]?|\\s*%\\s*X)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE).matcher(normalizedText);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String compactToken = matcher.group(1).replaceAll("\\s+", "");
        Optional<Integer> parsed = parseFortifyInteger(compactToken);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }
        int value = parsed.get();
        if (range.isPresent() && !range.get().contains(value) && compactToken.length() > 1) {
            for (int trimmedLength = compactToken.length() - 1; trimmedLength >= 1; trimmedLength--) {
                Optional<Integer> repaired = parseFortifyInteger(compactToken.substring(0, trimmedLength));
                if (repaired.isPresent() && range.get().contains(repaired.get())) {
                    return repaired;
                }
            }
        }
        return Optional.of(value);
    }

    private static Optional<Integer> parseFortifyInteger(String rawToken) {
        try {
            return Optional.of(Integer.parseInt(rawToken
                    .replace(" ", "")
                    .replace('O', '0')
                    .replace('I', '1')
                    .replace('S', '5')
                    .replace('B', '8')
                    .replace('L', '1')));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static String normalizeLineForPatternKeepingPlus(String line) {
        if (line == null) {
            return "";
        }
        return java.text.Normalizer.normalize(line, java.text.Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private record RollRange(int min, int max) {
        private boolean contains(int value) {
            return value >= min && value <= max;
        }
    }
}

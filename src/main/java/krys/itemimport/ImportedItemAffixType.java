package krys.itemimport;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

/** Katalog znanych affixów itemu używany przez ręczną walidację i projekcję do runtime. */
public enum ImportedItemAffixType {
    STRENGTH("Siła", "+%s siły", RuntimeProjection.STRENGTH),
    INTELLIGENCE("Inteligencja", "+%s inteligencji", RuntimeProjection.INTELLIGENCE),
    THORNS("Ciernie", "+%s cierni", RuntimeProjection.THORNS),
    BLOCK_CHANCE("Szansa na blok", "%s%% szansy na blok", RuntimeProjection.BLOCK_CHANCE),
    RETRIBUTION_CHANCE("Szansa retribution", "%s%% szansy retribution", RuntimeProjection.RETRIBUTION_CHANCE),
    LUCKY_HIT_CHANCE("Szansa na szczęśliwy traf", "+%s%% szansy na szczęśliwy traf", RuntimeProjection.NONE),
    COOLDOWN_REDUCTION("Redukcja czasu odnowienia", "%s%% redukcji czasu odnowienia", RuntimeProjection.NONE);

    private final String displayName;
    private final String linePattern;
    private final RuntimeProjection runtimeProjection;

    ImportedItemAffixType(String displayName, String linePattern, RuntimeProjection runtimeProjection) {
        this.displayName = displayName;
        this.linePattern = linePattern;
        this.runtimeProjection = runtimeProjection;
    }

    public String getDisplayName() {
        return displayName;
    }

    public RuntimeProjection getRuntimeProjection() {
        return runtimeProjection;
    }

    public String formatLine(double value) {
        return linePattern.formatted(formatValue(value));
    }

    public static Optional<ImportedItemAffixType> detectFromLine(String line) {
        String normalized = normalize(line);
        if (normalized.contains("SILY") || normalized.contains("STRENGTH")) {
            return Optional.of(STRENGTH);
        }
        if (normalized.contains("INTELIGENCJI") || normalized.contains("INTELLIGENCE")) {
            return Optional.of(INTELLIGENCE);
        }
        if (normalized.contains("CIERNI") || normalized.contains("THORNS")) {
            return Optional.of(THORNS);
        }
        if (normalized.contains("SZANSY NA BLOK") || normalized.contains("BLOCK CHANCE")) {
            return Optional.of(BLOCK_CHANCE);
        }
        if (normalized.contains("RETRIBUTION") || normalized.contains("ODWET")) {
            return Optional.of(RETRIBUTION_CHANCE);
        }
        if (normalized.contains("SZCZESLIWY TRAF") || normalized.contains("LUCKY HIT")) {
            return Optional.of(LUCKY_HIT_CHANCE);
        }
        if (normalized.contains("CZASU ODNOWIENIA") || normalized.contains("COOLDOWN")) {
            return Optional.of(COOLDOWN_REDUCTION);
        }
        return Optional.empty();
    }

    private static String formatValue(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.1f", value).replace('.', ',');
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT);
    }

    public enum RuntimeProjection {
        NONE,
        STRENGTH,
        INTELLIGENCE,
        THORNS,
        BLOCK_CHANCE,
        RETRIBUTION_CHANCE
    }
}

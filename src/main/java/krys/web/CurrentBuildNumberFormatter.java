package krys.web;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formatuje liczby widoczne w HTML current build bez zmiany wartości runtime. */
final class CurrentBuildNumberFormatter {
    private CurrentBuildNumberFormatter() {
    }

    static String dps(double value) {
        return formatMaxScale(value, 2);
    }

    static String resource(double value) {
        return formatMaxScale(value, 2);
    }

    static String signedResource(double value) {
        if (value > 0.0d) {
            return "+" + resource(value);
        }
        return resource(value);
    }

    static String resourceRegenPerSecond(double value) {
        return formatFixedScale(value, 2);
    }

    static String multiplier(double value) {
        return formatFixedScale(value, 2);
    }

    static String percentOneDecimal(double value) {
        return formatFixedScale(value, 1);
    }

    static String percentWhole(double value) {
        return formatFixedScale(value, 0);
    }

    private static String formatMaxScale(double value, int scale) {
        return decimal(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString()
                .replace('.', ',');
    }

    private static String formatFixedScale(double value, int scale) {
        return decimal(value)
                .setScale(scale, RoundingMode.HALF_UP)
                .toPlainString()
                .replace('.', ',');
    }

    private static BigDecimal decimal(double value) {
        return BigDecimal.valueOf(value);
    }
}

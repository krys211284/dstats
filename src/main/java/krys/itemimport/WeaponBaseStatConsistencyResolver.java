package krys.itemimport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Ustala zależne statystyki bazowe broni bez znajomości klasy, typu itemu ani affixów. */
public final class WeaponBaseStatConsistencyResolver {
    public static final String DPS_INCONSISTENT = "WEAPON_DPS_OCR_INCONSISTENT";
    public static final String SOURCE_UNCERTAIN = "WEAPON_BASE_STATS_UNCERTAIN";
    public static final String SOURCE_INVALID = "WEAPON_BASE_STATS_INVALID";

    private WeaponBaseStatConsistencyResolver() {
    }

    /** Dane wpisane lub potwierdzone ręcznie nie mają niepewności odczytu OCR. */
    public static Result resolve(Long min, Long max, Double aps, Long observedDps) {
        return resolve(min, max, aps, observedDps, Certainty.confirmed());
    }

    public static Result resolve(Long min, Long max, Double aps, Long observedDps, Certainty certainty) {
        Objects.requireNonNull(certainty, "certainty");
        if ((min != null && min < 0L) || (max != null && max < 0L)
                || (min != null && max != null && min > max)
                || (aps != null && (!Double.isFinite(aps) || aps < 0.0d))
                || (observedDps != null && observedDps < 0L)) {
            return new Result(observedDps, null, null, null, null, false, false,
                    DpsConsistency.INVALID_SOURCE, List.of(SOURCE_INVALID + " min=" + min + " max=" + max + " aps=" + aps));
        }

        boolean rangePresent = min != null && max != null;
        boolean rangeCertain = rangePresent && certainty.min() && certainty.max();
        BigDecimal exactAverage = rangePresent
                ? BigDecimal.valueOf(min).add(BigDecimal.valueOf(max)).divide(BigDecimal.valueOf(2L))
                : null;
        Long average = exactAverage == null ? null : rounded(exactAverage);
        boolean sourceUncertain = (min != null && !certainty.min()) || (max != null && !certainty.max())
                || (aps != null && !certainty.aps());
        List<String> diagnostics = new ArrayList<>();
        if (sourceUncertain) {
            diagnostics.add(SOURCE_UNCERTAIN + " minCertain=" + certainty.min()
                    + " maxCertain=" + certainty.max() + " apsCertain=" + certainty.aps());
        }

        // Brak składnika blokuje wyliczenie; niepewność obecnej wartości obniża tylko pewność wyniku.
        if (!rangePresent || aps == null) {
            return new Result(observedDps, average, observedDps, exactAverage, null, rangeCertain,
                    observedDps != null && certainty.observedDps() && !sourceUncertain,
                    sourceUncertain ? DpsConsistency.SOURCE_UNCERTAIN : DpsConsistency.INCOMPLETE_SOURCE,
                    diagnostics);
        }

        // DPS korzysta z dokładnego midpointu, nie z wcześniej zaokrąglonej średniej modelowej.
        BigDecimal exactDps = exactAverage.multiply(BigDecimal.valueOf(aps));
        boolean dpsCertain = rangeCertain && certainty.aps();
        final long displayedDps;
        try {
            displayedDps = rounded(exactDps);
        } catch (ArithmeticException exception) {
            diagnostics.add(SOURCE_INVALID + " derivedDps_out_of_long_range=" + exactDps);
            return new Result(observedDps, average, null, exactAverage, exactDps, rangeCertain, false,
                    DpsConsistency.INVALID_SOURCE, diagnostics);
        }
        if (observedDps == null) {
            return new Result(null, average, displayedDps, exactAverage, exactDps, rangeCertain, dpsCertain,
                    DpsConsistency.OCR_MISSING, diagnostics);
        }
        if (observedDps != displayedDps) {
            diagnostics.add(DPS_INCONSISTENT + " ocr=" + observedDps + " derived=" + displayedDps);
            return new Result(observedDps, average, displayedDps, exactAverage, exactDps, rangeCertain, dpsCertain,
                    DpsConsistency.INCONSISTENT, diagnostics);
        }
        return new Result(observedDps, average, displayedDps, exactAverage, exactDps, rangeCertain, dpsCertain,
                sourceUncertain ? DpsConsistency.SOURCE_UNCERTAIN
                        : certainty.observedDps() ? DpsConsistency.CONSISTENT : DpsConsistency.OCR_UNCERTAIN,
                diagnostics);
    }

    private static long rounded(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP).longValueExact();
    }

    /** Pewność każdego składnika jest niezależna od pewności zaobserwowanego DPS. */
    public record Certainty(boolean min, boolean max, boolean aps, boolean observedDps) {
        public static Certainty confirmed() {
            return new Certainty(true, true, true, true);
        }
    }

    public enum DpsConsistency {
        CONSISTENT, INCONSISTENT, OCR_MISSING, OCR_UNCERTAIN, INCOMPLETE_SOURCE, SOURCE_UNCERTAIN, INVALID_SOURCE
    }

    /** Odczytany DPS pozostaje evidence także wtedy, gdy kanoniczny wynik jest inny. */
    public record Result(Long observedDps, Long canonicalAverage, Long canonicalDps,
                         BigDecimal exactAverage, BigDecimal exactDps,
                         boolean averageCertain, boolean dpsCertain,
                         DpsConsistency consistency, List<String> diagnostics) {
        public Result {
            diagnostics = List.copyOf(diagnostics);
        }

        public boolean isOcrDpsConsistent() {
            return consistency == DpsConsistency.CONSISTENT;
        }
    }
}

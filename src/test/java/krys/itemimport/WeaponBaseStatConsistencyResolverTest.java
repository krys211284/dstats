package krys.itemimport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static krys.itemimport.WeaponBaseStatConsistencyResolver.DpsConsistency.*;
import static org.junit.jupiter.api.Assertions.*;

/** Kontrakt liczbowy oparty na zgłoszeniu użytkownika i istniejących danych Verathiela. */
class WeaponBaseStatConsistencyResolverTest {
    @Test
    void shouldConfirmMatchingObservedDps() {
        var result = WeaponBaseStatConsistencyResolver.resolve(4269L, 5805L, 0.90d, 4533L);
        assertEquals(5037L, result.canonicalAverage());
        assertEquals(4533L, result.canonicalDps());
        assertEquals(new BigDecimal("4533.3"), result.exactDps().stripTrailingZeros());
        assertTrue(result.isOcrDpsConsistent());
        assertTrue(result.averageCertain());
        assertTrue(result.dpsCertain());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void shouldCorrectInconsistentDpsAndKeepObservedValue() {
        var result = WeaponBaseStatConsistencyResolver.resolve(4269L, 5805L, 0.90d, 533L);
        assertEquals(5037L, result.canonicalAverage());
        assertEquals(4533L, result.canonicalDps());
        assertEquals(533L, result.observedDps());
        assertFalse(result.isOcrDpsConsistent());
        assertEquals(INCONSISTENT, result.consistency());
        assertEquals("WEAPON_DPS_OCR_INCONSISTENT ocr=533 derived=4533", result.diagnostics().getFirst());
    }

    @Test
    void shouldDeriveDpsWithoutObservedDps() {
        var result = WeaponBaseStatConsistencyResolver.resolve(4269L, 5805L, 0.90d, null);
        assertEquals(4533L, result.canonicalDps());
        assertEquals(OCR_MISSING, result.consistency());
        assertNull(result.observedDps());
        assertTrue(result.dpsCertain());
    }

    @Test
    void shouldDeriveOnlyAverageWithoutAps() {
        var result = WeaponBaseStatConsistencyResolver.resolve(4269L, 5805L, null, null);
        assertEquals(5037L, result.canonicalAverage());
        assertNull(result.canonicalDps());
        assertNull(result.exactDps());
        assertEquals(INCOMPLETE_SOURCE, result.consistency());
        assertFalse(result.dpsCertain());
    }

    @Test
    void shouldKeepIndependentObservedDpsWhenSourceSetIsIncomplete() {
        var result = WeaponBaseStatConsistencyResolver.resolve(4269L, 5805L, null, 4533L);
        assertEquals(4533L, result.canonicalDps());
        assertNull(result.exactDps());
        assertFalse(result.isOcrDpsConsistent());
    }

    @ParameterizedTest
    @CsvSource({",5805", "4269,"})
    void shouldNotInventAverageFromIncompleteRange(Long min, Long max) {
        var result = WeaponBaseStatConsistencyResolver.resolve(min, max, 0.90d, null);
        assertNull(result.canonicalAverage());
        assertNull(result.canonicalDps());
        assertFalse(result.averageCertain());
    }

    @ParameterizedTest
    @CsvSource({"false,true,true", "true,false,true", "true,true,false", "false,false,true",
            "false,true,false", "true,false,false", "false,false,false"})
    void shouldDeriveValuesAndKeepCertaintyFromUncertainSource(boolean minCertain, boolean maxCertain, boolean apsCertain) {
        var result = WeaponBaseStatConsistencyResolver.resolve(4269L, 5805L, 0.90d, 533L,
                new WeaponBaseStatConsistencyResolver.Certainty(minCertain, maxCertain, apsCertain, true));
        assertEquals(5037L, result.canonicalAverage());
        assertEquals(4533L, result.canonicalDps());
        assertEquals(533L, result.observedDps());
        assertEquals(new BigDecimal("5037"), result.exactAverage());
        assertEquals(new BigDecimal("4533.3"), result.exactDps().stripTrailingZeros());
        assertEquals(minCertain && maxCertain, result.averageCertain());
        assertFalse(result.dpsCertain());
        assertEquals(INCONSISTENT, result.consistency());
        assertTrue(result.diagnostics().stream().anyMatch(note -> note.startsWith("WEAPON_BASE_STATS_UNCERTAIN")));
        assertTrue(result.diagnostics().contains("WEAPON_DPS_OCR_INCONSISTENT ocr=533 derived=4533"));
    }

    @Test
    void shouldKeepUncertaintyEvenWhenObservedDpsMatchesDerivedValue() {
        var result = WeaponBaseStatConsistencyResolver.resolve(4269L, 5805L, 0.90d, 4533L,
                new WeaponBaseStatConsistencyResolver.Certainty(false, true, true, true));
        assertEquals(5037L, result.canonicalAverage());
        assertEquals(4533L, result.canonicalDps());
        assertFalse(result.averageCertain());
        assertFalse(result.dpsCertain());
        assertEquals(SOURCE_UNCERTAIN, result.consistency());
        assertFalse(result.isOcrDpsConsistent());
        assertTrue(result.diagnostics().stream().anyMatch(note -> note.startsWith("WEAPON_BASE_STATS_UNCERTAIN")));
    }

    @Test
    void shouldDeriveUncertainAverageWithMissingApsButNotInventDps() {
        var result = WeaponBaseStatConsistencyResolver.resolve(4269L, 5805L, null, null,
                new WeaponBaseStatConsistencyResolver.Certainty(false, true, true, true));
        assertEquals(5037L, result.canonicalAverage());
        assertFalse(result.averageCertain());
        assertNull(result.canonicalDps());
        assertNull(result.exactDps());
        assertFalse(result.dpsCertain());
    }

    @Test
    void shouldDeriveUncertainDpsWithoutObservedDps() {
        var result = WeaponBaseStatConsistencyResolver.resolve(4269L, 5805L, 0.90d, null,
                new WeaponBaseStatConsistencyResolver.Certainty(true, true, false, true));
        assertEquals(5037L, result.canonicalAverage());
        assertEquals(4533L, result.canonicalDps());
        assertTrue(result.averageCertain());
        assertFalse(result.dpsCertain());
        assertEquals(OCR_MISSING, result.consistency());
        assertTrue(result.diagnostics().stream().anyMatch(note -> note.startsWith("WEAPON_BASE_STATS_UNCERTAIN")));
    }

    @Test
    void shouldNotLetUncertainObservedDpsPreventDerivationFromCertainSource() {
        var result = WeaponBaseStatConsistencyResolver.resolve(4269L, 5805L, 0.90d, 533L,
                new WeaponBaseStatConsistencyResolver.Certainty(true, true, true, false));
        assertEquals(4533L, result.canonicalDps());
        assertEquals(533L, result.observedDps());
        assertTrue(result.dpsCertain());
        assertEquals(INCONSISTENT, result.consistency());
    }

    @Test
    void shouldNotTreatUncertainMatchingOcrAsConfirmation() {
        var result = WeaponBaseStatConsistencyResolver.resolve(4269L, 5805L, 0.90d, 4533L,
                new WeaponBaseStatConsistencyResolver.Certainty(true, true, true, false));
        assertEquals(4533L, result.canonicalDps());
        assertEquals(OCR_UNCERTAIN, result.consistency());
        assertFalse(result.isOcrDpsConsistent());
    }

    @ParameterizedTest
    @CsvSource({"1390,2018,1.10,1704,1874", "1390,2018,0.90,1704,1534", "4269,5805,0.50,5037,2519",
            "1390,2019,1.10,1705,1875"})
    void shouldRoundHalfUpUsingExactMidpointForDps(long min, long max, double aps, long average, long dps) {
        // 1874.4, 1533.6, 2518.5 oraz 1874.95: ostatni przypadek używa midpointu 1704.5.
        var result = WeaponBaseStatConsistencyResolver.resolve(min, max, aps, null);
        assertEquals(average, result.canonicalAverage());
        assertEquals(dps, result.canonicalDps());
    }

    @Test
    void shouldKeepExistingVerathielBaseStats() {
        var result = WeaponBaseStatConsistencyResolver.resolve(1350L, 1978L, 1.10d, 1830L);
        assertEquals(1664L, result.canonicalAverage());
        assertEquals(1830L, result.canonicalDps());
        assertTrue(result.isOcrDpsConsistent());
    }

    @Test
    void shouldAvoidOverflowWhileAddingRangeBounds() {
        var result = WeaponBaseStatConsistencyResolver.resolve(Long.MAX_VALUE, Long.MAX_VALUE, 1.0d, Long.MAX_VALUE);
        assertEquals(Long.MAX_VALUE, result.canonicalAverage());
        assertEquals(Long.MAX_VALUE, result.canonicalDps());
    }

    @Test
    void shouldRejectReversedRangeAndNonFiniteAps() {
        assertEquals(INVALID_SOURCE,
                WeaponBaseStatConsistencyResolver.resolve(5805L, 4269L, 0.90d, 533L).consistency());
        for (double aps : new double[]{Double.NaN, Double.POSITIVE_INFINITY}) {
            var result = WeaponBaseStatConsistencyResolver.resolve(4269L, 5805L, aps, 533L);
            assertEquals(INVALID_SOURCE, result.consistency());
            assertNull(result.canonicalDps());
        }
        var overflow = WeaponBaseStatConsistencyResolver.resolve(Long.MAX_VALUE, Long.MAX_VALUE, 1.10d, null);
        assertEquals(INVALID_SOURCE, overflow.consistency());
        assertNull(overflow.canonicalDps());
    }
}

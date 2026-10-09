package krys.combat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DirectDamageCalculatorTest {
    private final DirectDamageCalculator calculator = new DirectDamageCalculator();

    @ParameterizedTest
    @CsvSource({"100,100", "62.5,63", "7.5,8"})
    void shouldPreserveIndependentSimpleAndDecimalGoldens(String percent, long golden) {
        // Goldeny A–C z kontraktu N8A: broń 100, wszystkie mnożniki 1, Math.round na końcu.
        var result = calculator.calculate(unitContext(100), List.of(component(percent, 1, true, true)));
        assertEquals(golden, result.rawDamage());
        assertEquals(golden, result.finalDamage());
        assertEquals(golden, result.rawCriticalDamage());
        assertEquals(golden, result.criticalDamage());
        assertEquals(new BigDecimal(percent), result.components().getFirst().spec().damagePercent());
        assertEquals(new BigDecimal(percent).doubleValue(), result.rawDamageExact(), 0.0d);
    }

    @Test
    void shouldCountThreeHitsBeforeRounding() {
        // Golden D: 100 × 30% × 3 = 90.
        var result = calculator.calculate(unitContext(100), List.of(component("30", 3, true, true)));
        assertEquals(90.0d, result.rawDamageExact(), 0.0d);
        assertEquals(90L, result.finalDamage());
        assertEquals(3, result.components().getFirst().spec().hitCount());
    }

    @Test
    void shouldApplyEveryResolvedMultiplierIncludingCriticalAndMitigation() {
        // Niezależny golden E: 100 × 2 × 1.2 × 1.5 × 1.2 × 2 = 864; kryt ×1.5 =1296; redukcja ×0.5.
        var context = new DirectDamageContext(100, 2, 1.2, 1.5, 1.2, 2, 1.5, 0.5);
        var result = calculator.calculate(context, List.of(component("100", 1, true, true)));
        assertEquals(864.0d, result.rawDamageExact(), 0.0000001d);
        assertEquals(1296.0d, result.rawCriticalDamageExact(), 0.0000001d);
        assertEquals(864L, result.rawDamage());
        assertEquals(432L, result.finalDamage());
        assertEquals(1296L, result.rawCriticalDamage());
        assertEquals(648L, result.criticalDamage());
        assertEquals(100L, result.components().getFirst().baseDamage());
    }

    @Test
    void excludedComponentShouldRemainVisibleWithDamageButOutsideTotal() {
        var excluded = new DirectDamageComponentSpec("Boczny komponent", "opis", new BigDecimal("100"),
                3, "Warunek rozwiązany wcześniej", true, false, "Nie trafia głównego celu");
        var result = calculator.calculate(unitContext(100), List.of(component("100", 1, true, true), excluded));
        assertEquals(2, result.components().size());
        assertEquals(100L, result.rawDamage());
        assertEquals(100L, result.rawCriticalDamage());
        var debug = result.components().get(1);
        assertEquals(300L, debug.rawDamage());
        assertEquals(300L, debug.finalDamage());
        assertSame(excluded, debug.spec());
        assertEquals("Nie trafia głównego celu", debug.spec().exclusionReason());
        assertEquals("Warunek rozwiązany wcześniej", debug.spec().conditionLabel());
    }

    @Test
    void inactiveComponentShouldRemainVisibleWithZeroDamageOutsideTotal() {
        var inactive = component("100", 3, false, true);
        var result = calculator.calculate(unitContext(100), List.of(component("100", 1, true, true), inactive));
        assertEquals(2, result.components().size());
        assertEquals(100L, result.rawDamage());
        assertEquals(100L, result.rawCriticalDamage());
        var debug = result.components().get(1);
        assertEquals(0.0d, debug.rawDamageExact(), 0.0d);
        assertEquals(0L, debug.rawDamage());
        assertEquals(0L, debug.finalDamage());
        assertEquals(0L, debug.rawCriticalDamage());
        assertEquals(0L, debug.criticalDamage());
        assertFalse(debug.spec().active());
    }

    @Test
    void shouldRoundTotalsAfterExactComponentSumAndAfterMitigation() {
        // Golden H oparty o 62.5% z kontraktu: 62.5 + 62.5 =125; 125 ×0.5 =62.5 →63.
        var context = new DirectDamageContext(100, 1, 1, 1, 1, 1, 1, 0.5);
        var result = calculator.calculate(context,
                List.of(component("62.5", 1, true, true), component("62.5", 1, true, true)));
        assertEquals(125.0d, result.rawDamageExact(), 0.0d);
        assertEquals(125L, result.rawDamage());
        assertEquals(63L, result.finalDamage());
        assertEquals(125L, result.rawCriticalDamage());
        assertEquals(63L, result.criticalDamage());
        assertEquals(126L, result.components().stream().mapToLong(DirectDamageComponentResult::rawDamage).sum());
        assertEquals(62L, result.components().stream().mapToLong(DirectDamageComponentResult::finalDamage).sum());
        assertNotEquals(result.rawDamage(), result.components().stream().mapToLong(DirectDamageComponentResult::rawDamage).sum());
        assertNotEquals(result.finalDamage(), result.components().stream().mapToLong(DirectDamageComponentResult::finalDamage).sum());
    }

    @Test
    void shouldSupportResolvedRoundedRawCriticalPolicyWithoutSkillIdentity() {
        // 7.5 →8; kryt od exact: 7.5 ×1.5 =11.25 →11; kryt od rounded raw: 8 ×1.5 =12.
        var exact = new DirectDamageContext(100, 1, 1, 1, 1, 1, 1.5, 0.5);
        var rounded = new DirectDamageContext(100, 1, 1, 1, 1, 1, 1.5, 0.5,
                DirectDamageContext.CriticalTotalRoundingPolicy.ROUNDED_RAW_TOTAL);
        var specs = List.of(component("7.5", 1, true, true));
        assertEquals(11L, calculator.calculate(exact, specs).rawCriticalDamage());
        var result = calculator.calculate(rounded, specs);
        assertEquals(8L, result.rawDamage());
        assertEquals(12L, result.rawCriticalDamage());
        assertEquals(6L, result.criticalDamage());
        assertEquals(11.25d, result.rawCriticalDamageExact(), 0.0d);
        assertEquals(11L, result.components().getFirst().rawCriticalDamage());
    }

    @Test
    void shouldAcceptNoWeaponAsZeroDamage() {
        var result = calculator.calculate(unitContext(0), List.of(component("62.5", 3, true, true)));
        assertEquals(0L, result.components().getFirst().baseDamage());
        assertEquals(0L, result.rawDamage());
        assertEquals(0L, result.finalDamage());
        assertEquals(0L, result.rawCriticalDamage());
        assertEquals(0L, result.criticalDamage());
    }

    @Test
    void shouldAcceptZeroCoefficientMultipliersAndCompleteMitigation() {
        assertEquals(0L, calculator.calculate(unitContext(100), List.of(component("0", 1, true, true))).rawDamage());
        var zeroMultiplier = new DirectDamageContext(100, 0, 1, 1, 1, 1, 1, 1);
        assertEquals(0L, calculator.calculate(zeroMultiplier, List.of(component("100", 1, true, true))).rawDamage());
        var mitigated = new DirectDamageContext(100, 1, 1, 1, 1, 1, 1, 0);
        assertEquals(0L, calculator.calculate(mitigated, List.of(component("100", 1, true, true))).finalDamage());
        assertEquals(0L, calculator.calculate(unitContext(100), List.of()).rawDamage());
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, -1.0d})
    void shouldRejectNonFiniteOrNegativeNumbersInEveryContextField(double invalid) {
        for (int field = 0; field < 8; field++) {
            double[] values = {100, 1, 1, 1, 1, 1, 1, 1};
            values[field] = invalid;
            assertThrows(IllegalArgumentException.class, () -> new DirectDamageContext(values[0], values[1],
                    values[2], values[3], values[4], values[5], values[6], values[7]), "Pole " + field);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void shouldRejectNonPositiveHitCount(int hits) {
        assertThrows(IllegalArgumentException.class, () -> component("100", hits, true, true));
    }

    @Test
    void shouldRejectMissingNegativeOrUnrepresentableCoefficient() {
        assertThrows(NullPointerException.class, () -> new DirectDamageComponentSpec(null, null, null, 1, null, true, true, null));
        assertThrows(IllegalArgumentException.class, () -> component("-7.5", 1, true, true));
        assertThrows(IllegalArgumentException.class, () -> component("1E+1000", 1, true, true));
    }

    @Test
    void shouldRejectInvalidMitigationAndMissingRoundingPolicy() {
        assertThrows(IllegalArgumentException.class, () -> new DirectDamageContext(100, 1, 1, 1, 1, 1, 1, 1.5));
        assertThrows(NullPointerException.class, () -> new DirectDamageContext(100, 1, 1, 1, 1, 1, 1, 1, null));
    }

    @Test
    void shouldRejectNonFiniteProductsAndOverflowingSum() {
        var productOverflow = new DirectDamageContext(Double.MAX_VALUE, 2, 1, 1, 1, 1, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(productOverflow,
                List.of(component("100", 1, true, true))));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(unitContext(Double.MAX_VALUE),
                List.of(component("100", 1, true, true), component("100", 1, true, true))));
    }

    @Test
    void shouldDefensivelyCopyResultCollectionsAndPreserveDecimalMetadata() {
        var spec = new DirectDamageComponentSpec(null, null, new BigDecimal("62.50"), 1, null, true, true, null);
        var input = new ArrayList<>(List.of(spec));
        var result = calculator.calculate(unitContext(100), input);
        input.clear();
        assertEquals(1, result.components().size());
        assertEquals(new BigDecimal("62.50"), result.components().getFirst().spec().damagePercent());
        assertThrows(UnsupportedOperationException.class, () -> result.components().clear());
        var mutableResults = new ArrayList<>(result.components());
        var copy = new DirectDamageResult(result.context(), mutableResults, result.rawDamageExact(),
                result.rawCriticalDamageExact(), result.rawDamage(), result.finalDamage(), result.rawCriticalDamage(), result.criticalDamage());
        mutableResults.clear();
        assertEquals(result, copy);
        assertThrows(UnsupportedOperationException.class, () -> copy.components().clear());
    }

    private static DirectDamageContext unitContext(double weaponDamage) {
        return new DirectDamageContext(weaponDamage, 1, 1, 1, 1, 1, 1, 1);
    }

    private static DirectDamageComponentSpec component(String percent, int hits, boolean active, boolean included) {
        return new DirectDamageComponentSpec("Komponent", "golden", new BigDecimal(percent), hits,
                null, active, included, null);
    }
}

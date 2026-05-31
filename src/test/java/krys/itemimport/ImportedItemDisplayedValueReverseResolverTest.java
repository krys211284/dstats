package krys.itemimport;

import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkingResolvedItemValueResolver;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje ogólne odwracanie wartości widocznych po Doskonaleniu do referenceValue. */
class ImportedItemDisplayedValueReverseResolverTest {
    private final ImportedItemDisplayedValueReverseResolver reverseResolver = new ImportedItemDisplayedValueReverseResolver();
    private final MasterworkingResolvedItemValueResolver runtimeResolver = new MasterworkingResolvedItemValueResolver();

    @Test
    void shouldReverseResolveMythicDisplayedCoreSkillRanksAtQualityTwentyFive() {
        Optional<ImportedItemDisplayedValueReverseResolver.ReverseResolvedReferenceValue> result =
                reverseResolver.resolveReferenceValue(
                        ImportedItemAffixType.CORE_SKILL_RANKS,
                        3.0d,
                        false,
                        new ItemMasterworking(25, 25)
                );

        assertTrue(result.isPresent());
        assertEquals(2.0d, result.get().referenceValue(), 0.0001d);
        ImportedItemAffix displayedAffix = new ImportedItemAffix(
                ImportedItemAffixType.CORE_SKILL_RANKS,
                3.0d,
                "",
                false,
                0,
                "+3 do umiejętności: Główne",
                ImportedItemAffixSource.OCR,
                "core_skill_ranks",
                null,
                null,
                result.get().referenceValue(),
                ""
        );
        assertEquals(3.0d, runtimeResolver.resolveAffixValue(displayedAffix, new ItemMasterworking(25, 25), true), 0.0001d);
    }

    @Test
    void shouldNotGuessWhenAffixTypeHasNoReverseContract() {
        Optional<ImportedItemDisplayedValueReverseResolver.ReverseResolvedReferenceValue> result =
                reverseResolver.resolveReferenceValue(
                        ImportedItemAffixType.DAMAGE_OVER_TIME_MULTIPLIER,
                        16.0d,
                        false,
                        new ItemMasterworking(25, 25)
                );

        assertTrue(result.isEmpty());
    }
}

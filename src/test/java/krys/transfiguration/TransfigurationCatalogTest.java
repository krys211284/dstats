package krys.transfiguration;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje katalogi źródłowe Przeistoczenia z Kostki Horadrimów. */
class TransfigurationCatalogTest {
    @Test
    void shouldExposeRegularTransfigurationOutcomesWithSampleChances() {
        assertEquals(5, HoradricTransfigurationOutcomeCatalog.definitions().size());
        assertEquals("~20%", HoradricTransfigurationOutcomeCatalog.find(HoradricTransfigurationOutcome.INDESTRUCTIBLE).orElseThrow().getSampleChanceLabel());
        assertEquals("~15%", HoradricTransfigurationOutcomeCatalog.find(HoradricTransfigurationOutcome.UPGRADE_TO_GREATER_AFFIX).orElseThrow().getSampleChanceLabel());
        assertEquals("~35%", HoradricTransfigurationOutcomeCatalog.find(HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX).orElseThrow().getSampleChanceLabel());
        assertEquals("~10%", HoradricTransfigurationOutcomeCatalog.find(HoradricTransfigurationOutcome.REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX).orElseThrow().getSampleChanceLabel());
        assertEquals("~20%", HoradricTransfigurationOutcomeCatalog.find(HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY).orElseThrow().getSampleChanceLabel());
    }

    @Test
    void shouldValidateTransfigurationAffixCatalogRanges() {
        assertEquals("do wszystkich współczynników", TransfigurationAffixCatalog.findById("ALL_STATS").orElseThrow().getDisplayName());
        assertEquals("główny atrybut", TransfigurationAffixCatalog.findById("PRIMARY_STAT").orElseThrow().getDisplayName());
        assertEquals("szansa na trafienie krytyczne", TransfigurationAffixCatalog.findById("CRITICAL_STRIKE_CHANCE").orElseThrow().getDisplayName());
        assertEquals("obrażeń (Fizyczne)", TransfigurationAffixCatalog.findById("PHYSICAL_DAMAGE_MULTIPLIER").orElseThrow().getDisplayName());

        TransfigurationAffixDefinition primaryStat = TransfigurationAffixCatalog.findById("PRIMARY_STAT").orElseThrow();
        assertTrue(primaryStat.accepts(150.0d));
        assertTrue(primaryStat.accepts(180.0d));
        assertFalse(primaryStat.accepts(149.0d));
        assertFalse(primaryStat.accepts(181.0d));
        assertTrue(primaryStat.isDoublesOnTwoHandedWeapon());

        TransfigurationAffixDefinition criticalChance = TransfigurationAffixCatalog.findById("CRITICAL_STRIKE_CHANCE").orElseThrow();
        assertTrue(criticalChance.accepts(3.5d));
        assertTrue(criticalChance.accepts(5.0d));
        assertFalse(criticalChance.accepts(3.4d));
        assertFalse(criticalChance.accepts(5.1d));
    }

    @Test
    void shouldExposePaladinSkillRankAffixesBySlot() {
        assertEquals(java.util.List.of("Aura", "Valor"), TransfigurationSkillRankCatalog.paladinTagsFor(EquipmentSlot.HELMET));
        assertEquals(java.util.List.of("Core"), TransfigurationSkillRankCatalog.paladinTagsFor(EquipmentSlot.GLOVES));
        assertEquals(java.util.List.of("All Skills", "Valor"), TransfigurationSkillRankCatalog.paladinTagsFor(EquipmentSlot.RING));
        assertEquals(java.util.List.of("Basic", "Aura"), TransfigurationSkillRankCatalog.paladinTagsFor(EquipmentSlot.OFF_HAND));
    }
}

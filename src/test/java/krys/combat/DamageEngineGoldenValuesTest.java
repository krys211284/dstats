package krys.combat;

import krys.hero.Hero;
import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.item.Item;
import krys.item.ItemStat;
import krys.item.ItemStatType;
import krys.simulation.HeroBuildSnapshot;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import krys.skill.StatusId;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageEngineGoldenValuesTest {
    private final DamageEngine damageEngine = new DamageEngine();

    @Test
    void powinien_liczyc_brandish_rank_1_zgodnie_z_golden_values() {
        DamageBreakdown breakdown = damageEngine.calculate(referenceSnapshot(new SkillState(SkillId.BRANDISH, 1, false, SkillUpgradeChoice.NONE)),
                SkillId.BRANDISH,
                EnumSet.noneOf(StatusId.class));

        assertEquals(6L, breakdown.getBaseDamage());
        assertEquals(13L, breakdown.getRawDamage());
        assertEquals(8L, breakdown.getFinalDamage());
        assertEquals(19L, breakdown.getRawCriticalDamage());
        assertEquals(12L, breakdown.getCriticalDamage());
    }

    @Test
    void powinien_liczyc_brandish_rank_5_zgodnie_z_golden_values() {
        DamageBreakdown breakdown = damageEngine.calculate(referenceSnapshot(new SkillState(SkillId.BRANDISH, 5, false, SkillUpgradeChoice.NONE)),
                SkillId.BRANDISH,
                EnumSet.noneOf(StatusId.class));

        assertEquals(8L, breakdown.getBaseDamage());
        assertEquals(18L, breakdown.getRawDamage());
        assertEquals(11L, breakdown.getFinalDamage());
        assertEquals(27L, breakdown.getRawCriticalDamage());
        assertEquals(17L, breakdown.getCriticalDamage());
    }

    @Test
    void powinien_liczyc_brandish_rank_5_z_powrotem_swiatlosci() {
        DamageBreakdown breakdown = damageEngine.calculate(referenceSnapshot(new SkillState(SkillId.BRANDISH, 5, true, SkillUpgradeChoice.LEFT)),
                SkillId.BRANDISH,
                EnumSet.noneOf(StatusId.class));

        assertEquals(6L, breakdown.getBaseDamage());
        assertEquals(25L, breakdown.getRawDamage());
        assertEquals(15L, breakdown.getFinalDamage());
        assertEquals(37L, breakdown.getRawCriticalDamage());
        assertEquals(23L, breakdown.getCriticalDamage());
        assertEquals(2, breakdown.getComponents().stream().filter(DamageComponentBreakdown::isIncludedInSingleTarget).count());
        assertEquals(12L, breakdown.getComponents().get(0).getRawDamage());
        assertEquals(8L, breakdown.getComponents().get(0).getFinalDamage());
        assertEquals(12L, breakdown.getComponents().get(1).getRawDamage());
        assertEquals(8L, breakdown.getComponents().get(1).getFinalDamage());
    }

    @Test
    void powinien_liczyc_brandish_rank_5_z_krzyzowym_uderzeniem_w_modelu_single_target() {
        DamageBreakdown breakdown = damageEngine.calculate(referenceSnapshot(new SkillState(SkillId.BRANDISH, 5, true, SkillUpgradeChoice.RIGHT)),
                SkillId.BRANDISH,
                EnumSet.of(StatusId.VULNERABLE));

        assertEquals(34L, breakdown.getRawDamage());
        assertEquals(21L, breakdown.getFinalDamage());
        assertEquals(52L, breakdown.getRawCriticalDamage());
        assertEquals(32L, breakdown.getCriticalDamage());
        assertEquals(2, breakdown.getComponents().size());

        DamageComponentBreakdown mainHit = breakdown.getComponents().get(0);
        DamageComponentBreakdown sideArcs = breakdown.getComponents().get(1);

        assertTrue(mainHit.isIncludedInSingleTarget());
        assertTrue(sideArcs.isActive());
        assertFalse(sideArcs.isIncludedInSingleTarget());
        assertEquals("Komponent nie trafia głównego celu w modelu single target", sideArcs.getExclusionReason());
    }

    @Test
    void powinien_liczyc_starcie_rank_1_z_bazowym_percentem_z_drzewa_paladyna() {
        DamageBreakdown breakdown = damageEngine.calculate(
                snapshot(new SkillState(SkillId.CLASH, 1, false, SkillUpgradeChoice.NONE), 1664L),
                SkillId.CLASH,
                EnumSet.noneOf(StatusId.class)
        );

        DamageComponentBreakdown mainHit = breakdown.getComponents().getFirst();
        assertEquals("Główny hit", mainHit.getName());
        assertEquals(115L, mainHit.getSkillDamagePercent());
        assertTrue(mainHit.getRawDamage() > 0L);
        assertTrue(mainHit.getFinalDamage() > 0L);
        assertTrue(breakdown.getRawDamage() > 0L);
        assertTrue(breakdown.getFinalDamage() > 0L);
    }

    static HeroBuildSnapshot referenceSnapshot(SkillState brandishState) {
        return snapshot(brandishState, 8L);
    }

    static HeroBuildSnapshot level70ClashSnapshot(double totalStrength, List<String> activeAspectIds) {
        Hero hero = new Hero(1, "Paladyn 70", 70, HeroClass.PALADIN);
        List<Item> items = List.of(
                new Item(1, "Odłamek Verathiela", EquipmentSlot.MAIN_HAND, List.of(
                        new ItemStat(ItemStatType.CRIT_DAMAGE, 0.0d)
                )),
                new Item(2, "Miażdżąca Tarcza Kościanych Łusek", EquipmentSlot.OFF_HAND, List.of(
                        new ItemStat(ItemStatType.MAIN_HAND_WEAPON_DAMAGE, 100.0d),
                        new ItemStat(ItemStatType.STRENGTH, Math.max(0.0d, totalStrength - 79.0d)),
                        new ItemStat(ItemStatType.BLOCK_CHANCE, 20.0d)
                ))
        );
        SkillState clash = new SkillState(SkillId.CLASH, 1, false, SkillUpgradeChoice.NONE);
        return new HeroBuildSnapshot(
                hero,
                0,
                1664L,
                0.0d,
                items,
                true,
                true,
                Map.of(SkillId.CLASH, clash),
                List.of(SkillId.CLASH),
                activeAspectIds
        );
    }

    private static HeroBuildSnapshot snapshot(SkillState skillState, long weaponDamage) {
        Hero hero = new Hero(1, "Krys", 13, HeroClass.PALADIN);
        List<Item> items = List.of(
                new Item(1, "Short Sword", EquipmentSlot.MAIN_HAND, List.of(
                        new ItemStat(ItemStatType.CRIT_DAMAGE, 1.5d)
                )),
                new Item(2, "Shield", EquipmentSlot.OFF_HAND, List.of(
                        new ItemStat(ItemStatType.MAIN_HAND_WEAPON_DAMAGE, 100.0d),
                        new ItemStat(ItemStatType.STRENGTH, 7.0d)
                )),
                new Item(3, "Armor", EquipmentSlot.CHEST, List.of(
                        new ItemStat(ItemStatType.STRENGTH, 8.0d)
                )),
                new Item(4, "Ring of Strength", EquipmentSlot.RING, List.of(
                        new ItemStat(ItemStatType.STRENGTH, 3.0d)
                ))
        );

        return new HeroBuildSnapshot(
                hero,
                0,
                weaponDamage,
                0.0d,
                items,
                Map.of(skillState.getSkillId(), skillState),
                List.of(skillState.getSkillId())
        );
    }
}

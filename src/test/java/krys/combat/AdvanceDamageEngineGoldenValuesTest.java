package krys.combat;

import krys.app.SampleBuildFactory;
import krys.simulation.HeroBuildSnapshot;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import krys.skill.StatusId;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AdvanceDamageEngineGoldenValuesTest {
    private final DamageEngine damageEngine = new DamageEngine();

    @Test
    void powinien_liczyc_bazowy_advance_rank_5_zgodnie_z_kontraktem_m7() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                new SkillState(SkillId.ADVANCE, 5, false, SkillUpgradeChoice.NONE)
        );

        DamageBreakdown breakdown = damageEngine.calculate(snapshot, SkillId.ADVANCE, EnumSet.noneOf(StatusId.class));

        assertEquals(12L, breakdown.getBaseDamage());
        assertEquals(24L, breakdown.getRawDamage());
        assertEquals(15L, breakdown.getFinalDamage());
        assertEquals(37L, breakdown.getRawCriticalDamage());
        assertEquals(23L, breakdown.getCriticalDamage());
    }

    @Test
    void wave_dash_powinno_dodawac_drugi_direct_hit_na_tym_samym_celu() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.LEFT)
        );

        DamageBreakdown breakdown = damageEngine.calculate(snapshot, SkillId.ADVANCE, EnumSet.noneOf(StatusId.class));

        assertEquals(12L, breakdown.getBaseDamage());
        assertEquals(56L, breakdown.getRawDamage());
        assertEquals(35L, breakdown.getFinalDamage());
        assertEquals(86L, breakdown.getRawCriticalDamage());
        assertEquals(53L, breakdown.getCriticalDamage());
        assertEquals(2, breakdown.getComponents().size());
        assertEquals("Główny hit", breakdown.getComponents().get(0).getName());
        assertEquals(24L, breakdown.getComponents().get(0).getRawDamage());
        assertEquals(15L, breakdown.getComponents().get(0).getFinalDamage());
        assertEquals("Wave Dash", breakdown.getComponents().get(1).getName());
        assertEquals(32L, breakdown.getComponents().get(1).getRawDamage());
        assertEquals(20L, breakdown.getComponents().get(1).getFinalDamage());
    }

    @Test
    void flash_of_the_blade_powinno_podmieniac_bazowy_hit_na_322_procent() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT)
        );

        DamageBreakdown breakdown = damageEngine.calculate(snapshot, SkillId.ADVANCE, EnumSet.noneOf(StatusId.class));

        assertEquals(26L, breakdown.getBaseDamage());
        assertEquals(54L, breakdown.getRawDamage());
        assertEquals(33L, breakdown.getFinalDamage());
        assertEquals(82L, breakdown.getRawCriticalDamage());
        assertEquals(51L, breakdown.getCriticalDamage());
        assertEquals(1, breakdown.getComponents().size());
        assertEquals(322L, breakdown.getComponents().get(0).getSkillDamagePercent());
        assertEquals("Flash of the Blade", breakdown.getSelectedModifierName());
    }
}

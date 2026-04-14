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

class HolyBoltDamageEngineGoldenValuesTest {
    private final DamageEngine damageEngine = new DamageEngine();

    @Test
    void powinien_liczyc_bazowy_holy_bolt_rank_5_zgodnie_z_golden_values() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                new SkillState(SkillId.HOLY_BOLT, 5, false, SkillUpgradeChoice.NONE)
        );

        DamageBreakdown breakdown = damageEngine.calculate(snapshot, SkillId.HOLY_BOLT, EnumSet.noneOf(StatusId.class));

        assertEquals(10L, breakdown.getBaseDamage());
        assertEquals(21L, breakdown.getRawDamage());
        assertEquals(13L, breakdown.getFinalDamage());
        assertEquals(32L, breakdown.getRawCriticalDamage());
        assertEquals(20L, breakdown.getCriticalDamage());
    }

    @Test
    void bazowe_rozszerzenie_judgement_nie_modyfikuje_natychmiastowego_holy_bolt() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE)
        );

        DamageBreakdown breakdown = damageEngine.calculate(snapshot, SkillId.HOLY_BOLT, EnumSet.noneOf(StatusId.class));

        assertEquals(21L, breakdown.getRawDamage());
        assertEquals(13L, breakdown.getFinalDamage());
        assertEquals(32L, breakdown.getRawCriticalDamage());
        assertEquals(20L, breakdown.getCriticalDamage());
    }

    @Test
    void powinien_liczyc_judgement_jako_osobny_delayed_hit_pelnym_pipeline() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(
                new SkillState(SkillId.HOLY_BOLT, 5, true, SkillUpgradeChoice.NONE)
        );

        DamageBreakdown breakdown = damageEngine.calculateStandaloneHit(snapshot, 80, "Judgement", "delayed", EnumSet.noneOf(StatusId.class));

        assertEquals(6L, breakdown.getBaseDamage());
        assertEquals(13L, breakdown.getRawDamage());
        assertEquals(8L, breakdown.getFinalDamage());
        assertEquals(20L, breakdown.getRawCriticalDamage());
        assertEquals(13L, breakdown.getCriticalDamage());
    }
}

package krys.combat;

import krys.app.SampleBuildFactory;
import krys.simulation.HeroBuildSnapshot;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReactiveDamageFoundationTest {
    private final DamageEngine damageEngine = new DamageEngine();

    @Test
    void powinien_policzyc_deterministyczny_expected_value_dla_retribution() {
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceReactiveCurrentBuild(
                new SkillState(SkillId.HOLY_BOLT, 5, false, SkillUpgradeChoice.NONE)
        );

        ReactiveHitBreakdown breakdown = damageEngine.calculateReactiveHit(snapshot, 3);

        assertTrue(damageEngine.hasReactiveFoundation(snapshot));
        assertEquals(50.0d, breakdown.getBaseThornsFromBuild());
        assertEquals(1.04d, breakdown.getMainStatMultiplier(), 0.0000001d);
        assertEquals(0.50d, breakdown.getBlockChance(), 0.0000001d);
        assertEquals(0.50d, breakdown.getRetributionChance(), 0.0000001d);
        assertEquals(52L, breakdown.getThornsRawDamage());
        assertEquals(32L, breakdown.getThornsFinalDamage());
        assertEquals(13L, breakdown.getRetributionExpectedRawDamage());
        assertEquals(8L, breakdown.getRetributionExpectedFinalDamage());
        assertEquals(40L, breakdown.getReactiveFinalDamage());
    }
}

package krys.hero;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeroClassCoreStatProgressionsTest {
    @Test
    void necromancerShouldMatchVerifiedLevelOneTwoAndSeventyStats() {
        var progression = HeroClassCoreStatProgressions.get(HeroClass.NECROMANCER);
        assertEquals(new HeroCoreStats(7, 10, 8, 7), progression.atLevel(1));
        assertEquals(new HeroCoreStats(8, 11, 9, 8), progression.atLevel(2));
        assertEquals(new HeroCoreStats(76, 79, 77, 76), progression.atLevel(70));
    }

    @Test
    void paladinShouldMatchItsExistingLevelOneAndSeventyStatsAndTheSameProgression() {
        var progression = HeroClassCoreStatProgressions.get(HeroClass.PALADIN);
        assertEquals(new HeroCoreStats(10, 7, 7, 8), progression.atLevel(1));
        assertEquals(new HeroCoreStats(11, 8, 8, 9), progression.atLevel(2));
        assertEquals(new HeroCoreStats(79, 76, 76, 77), progression.atLevel(70));
    }

    @Test
    void progressionShouldRejectInvalidLevelsAndOverflowInsteadOfClamping() {
        var progression = HeroClassCoreStatProgressions.get(HeroClass.NECROMANCER);
        assertThrows(IllegalArgumentException.class, () -> progression.atLevel(0));
        assertThrows(IllegalArgumentException.class, () -> progression.atLevel(-1));
        assertThrows(ArithmeticException.class, () -> progression.atLevel(Integer.MAX_VALUE));
    }
}

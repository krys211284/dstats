package krys.simulation;

import krys.hero.Hero;
import krys.hero.HeroClass;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroBuildSnapshotTest {

    @Test
    void powinien_bezpiecznie_kopiowac_pusty_stan_skilli() {
        HeroBuildSnapshot snapshot = new HeroBuildSnapshot(
                new Hero(1, "Krys", 13, HeroClass.PALADIN),
                0,
                8,
                0.0d,
                List.of(),
                Map.of(),
                List.of()
        );

        assertTrue(snapshot.getLearnedSkills().isEmpty());
        assertTrue(snapshot.getSelectedSkillBar().isEmpty());
    }
}

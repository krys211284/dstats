package krys.skill;

import krys.paladin.PaladinSkillTreeRegistry;
import krys.paladin.PaladinTreeSkill;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaladinSkillDefsTest {
    @Test
    void clash_runtime_uzywa_pierwszych_pieciu_rankow_starcia_z_rejestru_paladyna() {
        PaladinTreeSkill starcie = PaladinSkillTreeRegistry.requireSkill("starcie");
        SkillDef clash = PaladinSkillDefs.get(SkillId.CLASH);

        assertEquals(115L, clash.getBaseSkillDamagePercent(1));
        assertEquals(126L, clash.getBaseSkillDamagePercent(2));
        assertEquals(138L, clash.getBaseSkillDamagePercent(3));
        assertEquals(149L, clash.getBaseSkillDamagePercent(4));
        assertEquals(167L, clash.getBaseSkillDamagePercent(5));
        for (int rank = 1; rank <= 5; rank++) {
            assertEquals(starcie.damagePercentAtRank(rank).longValue(), clash.getBaseSkillDamagePercent(rank));
        }
    }
}

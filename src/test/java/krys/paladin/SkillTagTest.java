package krys.paladin;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillTagTest {
    @Test
    void kazdy_skill_paladyna_powinien_miec_niepuste_unikalne_tagi_z_enuma() {
        for (PaladinTreeSkill skill : PaladinSkillTreeRegistry.allSkills()) {
            Set<SkillTag> tags = skill.getTags();

            assertFalse(tags.isEmpty(), skill.getSkillId());
            assertEquals(tags.size(), Set.copyOf(tags).size(), skill.getSkillId());
            assertTrue(Set.of(SkillTag.values()).containsAll(tags), skill.getSkillId());
        }
    }

    @Test
    void tagi_powinny_wynikac_z_lokalnych_grup_typow_i_ulepszen() {
        Set<SkillTag> brandishTags = PaladinSkillTreeRegistry.requireSkill("wymach").getTags();

        assertTrue(brandishTags.contains(SkillTag.DAMAGE));
        assertTrue(brandishTags.contains(SkillTag.BASIC));
        assertTrue(brandishTags.contains(SkillTag.HOLY_DAMAGE));
        assertTrue(brandishTags.contains(SkillTag.FAITH_GENERATION));
        assertTrue(brandishTags.contains(SkillTag.CAST_SPEED));
        assertTrue(brandishTags.contains(SkillTag.EXPOSED));
        assertTrue(brandishTags.contains(SkillTag.MULTI_HIT));

        assertTrue(PaladinSkillTreeRegistry.allSkills().stream().anyMatch(skill -> skill.getTags().contains(SkillTag.FAITH_GENERATION)));
        assertTrue(PaladinSkillTreeRegistry.allSkills().stream().anyMatch(skill -> skill.getTags().contains(SkillTag.CAST_SPEED)));
        assertTrue(PaladinSkillTreeRegistry.allSkills().stream().anyMatch(skill -> skill.getTags().contains(SkillTag.EXPOSED)
                || skill.getTags().contains(SkillTag.VULNERABLE)));
        assertTrue(PaladinSkillTreeRegistry.allSkills().stream().anyMatch(skill -> skill.getTags().contains(SkillTag.SHIELD)));
        assertTrue(PaladinSkillTreeRegistry.allSkills().stream().anyMatch(skill -> skill.getTags().contains(SkillTag.AURA)));
    }
}

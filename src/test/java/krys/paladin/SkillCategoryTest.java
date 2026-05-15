package krys.paladin;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillCategoryTest {

    @Test
    void basicSkillsShouldExposeSourceCategoriesSeparatelyFromTreeGroup() {
        PaladinTreeSkill brandish = PaladinSkillTreeRegistry.requireSkill("wymach");
        PaladinTreeSkill holyBolt = PaladinSkillTreeRegistry.requireSkill("swiety_pocisk");
        PaladinTreeSkill clash = PaladinSkillTreeRegistry.requireSkill("starcie");
        PaladinTreeSkill advance = PaladinSkillTreeRegistry.requireSkill("natarcie");

        assertEquals("basic", brandish.getSkillGroup());
        assertEquals(Set.of(SkillCategory.PODSTAWOWE, SkillCategory.ADEPT), brandish.getSkillCategories());
        assertEquals("Podstawowe, Adept", brandish.getSkillCategoriesDisplay());

        assertEquals("basic", holyBolt.getSkillGroup());
        assertEquals(Set.of(SkillCategory.PODSTAWOWE, SkillCategory.SEDZIA), holyBolt.getSkillCategories());

        assertEquals("basic", clash.getSkillGroup());
        assertEquals(Set.of(SkillCategory.PODSTAWOWE, SkillCategory.MOLOCH), clash.getSkillCategories());

        assertEquals("basic", advance.getSkillGroup());
        assertEquals(Set.of(SkillCategory.PODSTAWOWE, SkillCategory.MOBILNOSC, SkillCategory.ZELOTY), advance.getSkillCategories());
    }

    @Test
    void sourceCategoriesShouldBeImmutableAndUnique() {
        Set<SkillCategory> categories = PaladinSkillTreeRegistry.requireSkill("wymach").getSkillCategories();

        assertEquals(2, categories.size());
        assertThrows(UnsupportedOperationException.class, () -> categories.add(SkillCategory.MOLOCH));
        assertTrue(PaladinSkillTreeRegistry.requireSkill("wymach").hasSkillCategory(SkillCategory.PODSTAWOWE));
        assertTrue(PaladinSkillTreeRegistry.requireSkill("wymach").hasSkillCategory(SkillCategory.ADEPT));
    }
}

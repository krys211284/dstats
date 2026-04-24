package krys.web;

import krys.hero.HeroClass;
import krys.item.HeroEquipmentSlot;
import krys.skill.SkillId;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pokrywa podstawowe operacje na bohaterach, poziomie i przypisanych umiejętnościach. */
class HeroServiceTest {
    @Test
    void shouldCreateFirstHeroAsActiveAndAllowSwitchingDeletingIndependentSelectionsAndAssignedSkills() throws Exception {
        Path tempDirectory = Files.createTempDirectory("hero-service");
        HeroService service = new HeroService(new FileHeroProfileRepository(tempDirectory));

        HeroProfile firstHero = service.createHero("Alaric", HeroClass.PALADIN, 13);
        HeroProfile secondHero = service.createHero("Gregor", HeroClass.PALADIN, 25);

        assertEquals(2, service.getHeroes().size());
        assertEquals(firstHero.getHeroId(), service.requireActiveHero().getHeroId());
        assertEquals("13", service.requireActiveHero().getCurrentBuildFormData().getLevel());
        assertEquals(List.of(SkillId.ADVANCE), service.requireActiveHero().getSkillLoadout().getAssignedSkillIds());

        service.setActiveHero(secondHero.getHeroId());
        assertEquals(secondHero.getHeroId(), service.requireActiveHero().getHeroId());
        assertEquals("25", service.requireActiveHero().getCurrentBuildFormData().getLevel());
        service.updateActiveHeroLevel(30);
        assertEquals("30", service.requireActiveHero().getCurrentBuildFormData().getLevel());

        service.setActiveHeroItem(HeroEquipmentSlot.MAIN_HAND, 101L);
        assertEquals(101L, service.requireActiveHero().getItemSelection().getSelectedItemId(HeroEquipmentSlot.MAIN_HAND));
        service.addSkillToActiveHero(SkillId.CLASH);
        assertTrue(service.requireActiveHero().getSkillLoadout().isAssigned(SkillId.CLASH));

        service.setActiveHero(firstHero.getHeroId());
        assertTrue(service.requireActiveHero().getItemSelection().getSelectedItemIdsBySlot().isEmpty());
        service.setActiveHeroItem(HeroEquipmentSlot.OFF_HAND, 202L);
        assertEquals(202L, service.requireActiveHero().getItemSelection().getSelectedItemId(HeroEquipmentSlot.OFF_HAND));
        assertTrue(service.requireActiveHero().getSkillLoadout().isAssigned(SkillId.ADVANCE));
        assertTrue(!service.requireActiveHero().getSkillLoadout().isAssigned(SkillId.CLASH));

        service.setActiveHero(secondHero.getHeroId());
        assertEquals(101L, service.requireActiveHero().getItemSelection().getSelectedItemId(HeroEquipmentSlot.MAIN_HAND));
        assertTrue(service.requireActiveHero().getItemSelection().getSelectedItemId(HeroEquipmentSlot.OFF_HAND) == null);
        assertTrue(service.requireActiveHero().getSkillLoadout().isAssigned(SkillId.CLASH));
        service.removeSkillFromActiveHero(SkillId.CLASH);
        assertTrue(!service.requireActiveHero().getSkillLoadout().isAssigned(SkillId.CLASH));

        service.clearItemFromAllHeroes(101L);
        assertTrue(service.requireActiveHero().getItemSelection().getSelectedItemIdsBySlot().isEmpty());

        service.deleteHero(secondHero.getHeroId());
        assertEquals(1, service.getHeroes().size());
        assertEquals(firstHero.getHeroId(), service.requireActiveHero().getHeroId());
        assertEquals("Alaric", service.requireActiveHero().getName());
    }
}

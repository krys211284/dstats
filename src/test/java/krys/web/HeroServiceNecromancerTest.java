package krys.web;

import krys.hero.HeroClass;
import krys.item.HeroEquipmentSlot;
import krys.skill.SkillId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class HeroServiceNecromancerTest {
    @TempDir Path directory;

    @Test
    void necromancerShouldStartEmptyAndRemainTheSameActiveProfileAfterRestart() {
        var service = new HeroService(new FileHeroProfileRepository(directory));
        var hero = service.createHero(" Mortis ", HeroClass.NECROMANCER, 70);
        assertEquals("Mortis", hero.getName());
        assertEquals(hero.getHeroId(), service.requireActiveHero().getHeroId());
        assertTrue(hero.getItemSelection().getSelectedItemIdsBySlot().isEmpty());
        assertEquals("necromancer", hero.getRuntimeSkillLoadout().classId());
        assertTrue(hero.getRuntimeSkillLoadout().configurations().isEmpty());
        assertTrue(hero.getRuntimeSkillLoadout().actionBar().isEmpty());

        var restarted = new HeroService(new FileHeroProfileRepository(directory));
        var restored = restarted.requireActiveHero();
        assertEquals(hero.getHeroId(), restored.getHeroId());
        assertEquals(hero.getName(), restored.getName());
        assertEquals(HeroClass.NECROMANCER, restored.getHeroClass());
        assertEquals("70", restored.getCurrentBuildFormData().getLevel());
        assertEquals(hero.getRuntimeSkillLoadout(), restored.getRuntimeSkillLoadout());
    }

    @Test
    void genericNameLevelAndItemOperationsShouldPreserveTheExactCanonicalStateForBothClasses() {
        var repository = new FileHeroProfileRepository(directory);
        var service = new HeroService(repository);
        for (var heroClass : HeroClass.values()) {
            var hero = service.createHero("Tester", heroClass, 1);
            repository.save(hero.withCurrentBuildQuery(hero.getCurrentBuildQuery() + "&futureField=raw%20value"));
            service.setActiveHero(hero.getHeroId());
            service.setActiveHeroItem(HeroEquipmentSlot.MAIN_HAND, 101);
            var before = service.requireActiveHero();
            var updated = service.updateHeroDetails(hero.getHeroId(), " Nowa nazwa ", 70);
            assertEquals("Nowa nazwa", updated.getName());
            assertEquals(heroClass, updated.getHeroClass());
            assertEquals("70", updated.getCurrentBuildFormData().getLevel());
            assertEquals(before.getRuntimeSkillLoadout(), updated.getRuntimeSkillLoadout());
            assertEquals(before.getItemSelection().getSelectedItemIdsBySlot(), updated.getItemSelection().getSelectedItemIdsBySlot());
            var oldFields = UrlEncodedFormSupport.parseQuery(before.getCurrentBuildQuery());
            oldFields.put("level", "70");
            assertEquals(oldFields, UrlEncodedFormSupport.parseQuery(updated.getCurrentBuildQuery()));
            service.updateActiveHeroLevel(20);
            assertEquals("20", service.requireActiveHero().getCurrentBuildFormData().getLevel());
            assertEquals(before.getRuntimeSkillLoadout(), service.requireActiveHero().getRuntimeSkillLoadout());
            service.clearActiveHeroItem(HeroEquipmentSlot.MAIN_HAND);
            assertEquals(before.getRuntimeSkillLoadout(), service.requireActiveHero().getRuntimeSkillLoadout());
        }
    }

    @Test
    void legacyPaladinOperationsShouldExplicitlyRejectNecromancerWithoutChangingState() {
        var service = new HeroService(new FileHeroProfileRepository(directory));
        var hero = service.createHero("Mortis", HeroClass.NECROMANCER, 70);
        var form = CurrentBuildFormData.defaultValues();
        var loadout = HeroSkillLoadout.foundationDefault();
        assertThrows(UnsupportedOperationException.class, () -> service.addSkillToActiveHero(SkillId.HOLY_BOLT));
        assertThrows(UnsupportedOperationException.class, () -> service.removeSkillFromActiveHero(SkillId.HOLY_BOLT));
        assertThrows(UnsupportedOperationException.class, () -> service.saveActiveHeroState(form, loadout));
        assertThrows(UnsupportedOperationException.class, () -> service.updateActiveHeroBuildFormData(form));
        assertEquals(hero.getRuntimeSkillLoadout(), service.requireActiveHero().getRuntimeSkillLoadout());
        assertEquals(hero.getCurrentBuildQuery(), service.requireActiveHero().getCurrentBuildQuery());
    }

    @Test
    void paladinDefaultAndActiveHeroFallbackShouldStayUnchangedAlongsideNecromancer() {
        var service = new HeroService(new FileHeroProfileRepository(directory));
        var paladin = service.createHero("Alaric", HeroClass.PALADIN, 13);
        var necro = service.createHero("Mortis", HeroClass.NECROMANCER, 70);
        assertEquals(paladin.getHeroId(), service.requireActiveHero().getHeroId());
        assertEquals(List.of(SkillId.ADVANCE), paladin.getSkillLoadout().getAssignedSkillIds());
        var defaultLoadout = HeroSkillLoadout.fromCurrentBuildFormData(CurrentBuildFormData.defaultValues());
        assertEquals(defaultLoadout.getAssignedSkill(SkillId.ADVANCE).getRank(), paladin.getSkillLoadout().getAssignedSkill(SkillId.ADVANCE).getRank());
        assertEquals(defaultLoadout.getActionBarSkills(), paladin.getSkillLoadout().getActionBarSkills());
        service.setActiveHero(necro.getHeroId());
        service.deleteHero(necro.getHeroId());
        assertEquals(paladin.getHeroId(), service.requireActiveHero().getHeroId());
        service.deleteHero(paladin.getHeroId());
        assertTrue(service.getActiveHero().isEmpty());
    }

    @Test
    void createAndEditShouldValidateTheSameNameAndLevelRange() {
        var service = new HeroService(new FileHeroProfileRepository(directory));
        var hero = service.createHero("Mortis", HeroClass.NECROMANCER, 70);
        for (int level : List.of(0, 71)) {
            assertThrows(IllegalArgumentException.class, () -> service.createHero("Tester", HeroClass.NECROMANCER, level));
            assertThrows(IllegalArgumentException.class, () -> service.updateHeroDetails(hero.getHeroId(), "Tester", level));
        }
        assertThrows(IllegalArgumentException.class, () -> service.createHero(" ", HeroClass.PALADIN, 13));
        assertThrows(IllegalArgumentException.class, () -> service.updateHeroDetails(hero.getHeroId(), " ", 13));
        assertThrows(IllegalArgumentException.class, () -> service.createHero("Tester", null, 13));
        assertEquals("Mortis", service.requireActiveHero().getName());
    }
}

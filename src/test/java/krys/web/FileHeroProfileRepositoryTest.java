package krys.web;

import krys.hero.HeroClass;
import krys.item.HeroEquipmentSlot;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje trwałość bohatera wraz z przypisanymi umiejętnościami i selekcją slotów. */
class FileHeroProfileRepositoryTest {
    @Test
    void shouldPersistHeroWithAssignedSkillsActionBarAndItemSelection() throws Exception {
        Path tempDirectory = Files.createTempDirectory("hero-profile-repository");
        FileHeroProfileRepository repository = new FileHeroProfileRepository(tempDirectory);

        EnumMap<SkillId, HeroAssignedSkill> assignedSkills = new EnumMap<>(SkillId.class);
        assignedSkills.put(SkillId.ADVANCE, new HeroAssignedSkill(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT));
        assignedSkills.put(SkillId.CLASH, new HeroAssignedSkill(
                SkillId.CLASH,
                3,
                true,
                SkillUpgradeChoice.LEFT,
                SkillRuntimeModifierChoice.ANIMUS
        ));
        HeroSkillLoadout loadout = new HeroSkillLoadout(assignedSkills, List.of(SkillId.ADVANCE, SkillId.CLASH));
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(
                java.util.Map.of("level", "70", "questSkillPoints", "14"),
                loadout.applyToFormData(CurrentBuildFormData.defaultValues())
        );
        HeroProfile hero = new HeroProfile(
                1L,
                "Alaric",
                HeroClass.PALADIN,
                CurrentBuildFormQuerySupport.toQuery(formData),
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.MAIN_HAND, 101L).withSelectedItem(HeroEquipmentSlot.OFF_HAND, 202L),
                loadout
        );

        repository.save(hero);
        repository.saveActiveHeroId(hero.getHeroId());

        HeroProfile restoredHero = repository.findById(hero.getHeroId()).orElseThrow();

        assertEquals(hero.getHeroId(), restoredHero.getHeroId());
        assertEquals("Alaric", restoredHero.getName());
        assertEquals(HeroClass.PALADIN, restoredHero.getHeroClass());
        assertEquals("70", restoredHero.getCurrentBuildFormData().getLevel());
        assertEquals("14", restoredHero.getCurrentBuildFormData().getQuestSkillPoints());
        assertEquals(101L, restoredHero.getItemSelection().getSelectedItemId(HeroEquipmentSlot.MAIN_HAND));
        assertEquals(202L, restoredHero.getItemSelection().getSelectedItemId(HeroEquipmentSlot.OFF_HAND));
        assertEquals(Set.of(SkillId.ADVANCE, SkillId.CLASH), Set.copyOf(restoredHero.getSkillLoadout().getAssignedSkillIds()));
        assertTrue(restoredHero.getSkillLoadout().isAssigned(SkillId.ADVANCE));
        assertTrue(restoredHero.getSkillLoadout().isAssigned(SkillId.CLASH));
        assertEquals(5, restoredHero.getSkillLoadout().getAssignedSkill(SkillId.ADVANCE).getRank());
        assertEquals(SkillUpgradeChoice.RIGHT, restoredHero.getSkillLoadout().getAssignedSkill(SkillId.ADVANCE).getChoiceUpgrade());
        assertEquals(3, restoredHero.getSkillLoadout().getAssignedSkill(SkillId.CLASH).getRank());
        assertEquals(SkillUpgradeChoice.LEFT, restoredHero.getSkillLoadout().getAssignedSkill(SkillId.CLASH).getChoiceUpgrade());
        assertEquals(SkillRuntimeModifierChoice.ANIMUS, restoredHero.getSkillLoadout().getAssignedSkill(SkillId.CLASH).getRuntimeModifierChoice());
        assertEquals("animusz", restoredHero.getSkillLoadout().getAssignedSkill(SkillId.CLASH).getChoiceGroup1());
        assertEquals("kara", restoredHero.getSkillLoadout().getAssignedSkill(SkillId.CLASH).getChoiceGroup3());
        assertEquals(List.of(SkillId.ADVANCE, SkillId.CLASH), restoredHero.getSkillLoadout().getActionBarSkills());
        assertEquals(hero.getHeroId(), repository.loadActiveHeroId().orElseThrow());
    }
}

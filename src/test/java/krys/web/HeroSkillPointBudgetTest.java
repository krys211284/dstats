package krys.web;

import krys.hero.HeroClass;
import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje budżet punktów umiejętności bez dotykania runtime DPS. */
class HeroSkillPointBudgetTest {
    @Test
    void shouldCalculateAvailableSkillPointsFromLevelAndQuestBonus() {
        assertEquals(0, budget("1", "0", emptyLoadout()).getAvailableSkillPoints());
        assertEquals(69, budget("70", "0", emptyLoadout()).getAvailableSkillPoints());
        assertEquals(83, budget("70", "14", emptyLoadout()).getAvailableSkillPoints());
    }

    @Test
    void shouldRejectSkillPointInputsOutsideAllowedRange() {
        HeroSkillPointBudget tooHighQuest = budget("70", "15", emptyLoadout());
        assertFalse(tooHighQuest.isValid());
        assertTrue(tooHighQuest.getValidationErrors().contains("Dodatkowe punkty z zadań musi być w zakresie 0..14."));

        HeroSkillPointBudget tooHighLevel = budget("71", "0", emptyLoadout());
        assertFalse(tooHighLevel.isValid());
        assertTrue(tooHighLevel.getValidationErrors().contains("Poziom bohatera musi być w zakresie 1..70."));
    }

    @Test
    void shouldCalculateSpentSkillPointsFromAssignedSkillConfiguration() {
        assertEquals(0, budget("70", "0", loadout(new HeroAssignedSkill(SkillId.CLASH, 0, false, SkillUpgradeChoice.NONE))).getSpentSkillPoints());
        assertEquals(1, budget("70", "0", loadout(new HeroAssignedSkill(SkillId.CLASH, 1, false, SkillUpgradeChoice.NONE))).getSpentSkillPoints());
        assertEquals(2, budget("70", "0", loadout(new HeroAssignedSkill(SkillId.CLASH, 1, true, SkillUpgradeChoice.NONE))).getSpentSkillPoints());
        assertEquals(3, budget("70", "0", loadout(new HeroAssignedSkill(SkillId.CLASH, 1, true, SkillUpgradeChoice.LEFT))).getSpentSkillPoints());
        assertEquals(8, budget("70", "0", loadout(
                new HeroAssignedSkill(SkillId.CLASH, 1, true, SkillUpgradeChoice.LEFT),
                new HeroAssignedSkill(SkillId.ADVANCE, 5, false, SkillUpgradeChoice.NONE)
        )).getSpentSkillPoints());
    }

    @Test
    void shouldMarkOverspentConfigurationAsInvalid() {
        HeroSkillPointBudget budget = budget("1", "0", loadout(new HeroAssignedSkill(SkillId.CLASH, 1, false, SkillUpgradeChoice.NONE)));

        assertFalse(budget.isValid());
        assertTrue(budget.getValidationErrors().contains("Konfiguracja przekracza budżet punktów umiejętności: wydano 1, dostępne 0."));
    }

    @Test
    void shouldPersistQuestSkillPointsInHeroProfileQuery() throws Exception {
        Path tempDirectory = Files.createTempDirectory("hero-profile-skill-points");
        FileHeroProfileRepository repository = new FileHeroProfileRepository(tempDirectory);
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(
                Map.of("level", "70", "questSkillPoints", "14"),
                CurrentBuildFormData.defaultValues()
        );
        HeroProfile hero = new HeroProfile(
                1L,
                "Alaric",
                HeroClass.PALADIN,
                CurrentBuildFormQuerySupport.toQuery(formData),
                HeroItemSelection.empty(),
                emptyLoadout()
        );

        repository.save(hero);

        HeroProfile restoredHero = repository.findById(1L).orElseThrow();
        assertEquals("70", restoredHero.getCurrentBuildFormData().getLevel());
        assertEquals("14", restoredHero.getCurrentBuildFormData().getQuestSkillPoints());
    }

    private static HeroSkillPointBudget budget(String level, String questSkillPoints, HeroSkillLoadout loadout) {
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(
                Map.of("level", level, "questSkillPoints", questSkillPoints),
                CurrentBuildFormData.defaultValues()
        );
        return HeroSkillPointBudget.from(formData, loadout);
    }

    private static HeroSkillLoadout emptyLoadout() {
        return new HeroSkillLoadout(Map.of(), List.of());
    }

    private static HeroSkillLoadout loadout(HeroAssignedSkill... assignedSkills) {
        EnumMap<SkillId, HeroAssignedSkill> skills = new EnumMap<>(SkillId.class);
        for (HeroAssignedSkill assignedSkill : assignedSkills) {
            skills.put(assignedSkill.getSkillId(), assignedSkill);
        }
        return new HeroSkillLoadout(skills, List.of());
    }
}

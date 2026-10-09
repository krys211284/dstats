package krys.web;

import krys.paladin.PaladinSkillTreeRegistry;
import krys.skill.SkillId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HeroSkillCatalogAdapterTest {
    @ParameterizedTest
    @CsvSource({"BRANDISH, wymach, Wymach, 75", "HOLY_BOLT, swiety_pocisk, Święty Pocisk, 90",
            "CLASH, starcie, Starcie, 115", "ADVANCE, natarcie, Natarcie, 105"})
    void fourLegacySkillsShouldKeepTheirCatalogPresentation(SkillId legacyId, String catalogId, String name, int damage) {
        // Wartości przechwycono sondą zastanego adaptera przed centralizacją mapowania N9A.
        var presentation = HeroSkillCatalogAdapter.present(legacyId, config("1", "NONE", "NONE"));
        assertEquals(legacyId, presentation.getLegacySkillId());
        assertEquals(name, presentation.getDisplayName());
        assertEquals(name, HeroSkillCatalogAdapter.displayName(legacyId));
        assertEquals(name, HeroSkillCatalogAdapter.displayName(legacyId.name()));
        assertTrue(presentation.hasTreeSkill());
        assertSame(PaladinSkillTreeRegistry.requireSkill(catalogId), presentation.getTreeSkill());
        assertEquals(1, presentation.getCurrentRank());
        assertEquals(damage, presentation.getCurrentDamagePercent());
        assertTrue(presentation.getActiveModifiers().isEmpty());
        assertEquals(legacyId == SkillId.CLASH ? List.of("Marsz Krzyżowca") : List.of(), names(presentation.getBaseEffects()));
        if (legacyId == SkillId.ADVANCE) {
            assertNull(presentation.getFaithGenerationModifier());
        } else {
            assertEquals("Generowanie Wiary", presentation.getFaithGenerationModifier().getName());
        }
        var inactive = HeroSkillCatalogAdapter.present(legacyId);
        assertEquals(name, inactive.getDisplayName());
        assertSame(presentation.getTreeSkill(), inactive.getTreeSkill());
        assertEquals(0, inactive.getCurrentRank());
        assertNull(inactive.getCurrentDamagePercent());
    }

    @Test
    void clashLegacyChoicesShouldKeepPunishmentAnimusAndBaseEffect() {
        var presentation = HeroSkillCatalogAdapter.present(SkillId.CLASH, config("1", "LEFT", "ANIMUS"));
        assertEquals(List.of("Kara", "Animusz"), names(presentation.getActiveModifiers()));
        assertEquals(List.of("Marsz Krzyżowca"), names(presentation.getBaseEffects()));
    }

    @Test
    void clashTreeChoicesShouldKeepOrderingAndAvoidDuplicateAnimus() {
        var presentation = HeroSkillCatalogAdapter.present(SkillId.CLASH, treeChoices("1"));
        assertEquals(List.of("Kara", "Animusz", "Zwiększenie Obrażeń"), names(presentation.getActiveModifiers()));
        assertEquals(List.of("Marsz Krzyżowca"), names(presentation.getBaseEffects()));
    }

    @Test
    void inactiveClashShouldKeepChoicesOutOfActivePresentation() {
        var presentation = HeroSkillCatalogAdapter.present(SkillId.CLASH, treeChoices("0"));
        assertEquals("Starcie", presentation.getDisplayName());
        assertNull(presentation.getCurrentDamagePercent());
        assertTrue(presentation.getActiveModifiers().isEmpty());
        assertTrue(presentation.getBaseEffects().isEmpty());
        assertEquals("Generowanie Wiary", presentation.getFaithGenerationModifier().getName());
    }

    private static CurrentBuildFormData.SkillConfigFormData config(String rank, String choice, String runtimeModifier) {
        return new CurrentBuildFormData.SkillConfigFormData(rank, true, choice, runtimeModifier);
    }

    private static CurrentBuildFormData.SkillConfigFormData treeChoices(String rank) {
        return new CurrentBuildFormData.SkillConfigFormData(rank, true, "NONE", "ANIMUS", "animusz", "zwiekszenie_obrazen", "kara");
    }

    private static List<String> names(List<HeroAssignedSkillPresentation.ModifierPresentation> modifiers) {
        return modifiers.stream().map(HeroAssignedSkillPresentation.ModifierPresentation::getName).toList();
    }
}

package krys.web;

import krys.hero.HeroClass;
import krys.runtime.RuntimeSkillLoadout;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class HeroProfileRuntimeSkillLoadoutTest {
    @Test
    void profileShouldHaveExactlyOneCanonicalSkillPayloadAndKeepItWhenCopyingGenericState() {
        var loadout = new RuntimeSkillLoadout("necromancer", Map.of(), List.of());
        var hero = new HeroProfile(1, "Mortis", HeroClass.NECROMANCER, "level=70&rankAdvance=5", HeroItemSelection.empty(), loadout);
        var fields = Arrays.stream(HeroProfile.class.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).toList();
        assertEquals(1, fields.stream().filter(field -> field.getType() == RuntimeSkillLoadout.class).count());
        assertTrue(fields.stream().noneMatch(field -> field.getType() == HeroSkillLoadout.class));
        assertSame(loadout, hero.getRuntimeSkillLoadout());
        assertSame(loadout, hero.withCurrentBuildQuery("level=20").getRuntimeSkillLoadout());
        assertSame(loadout, hero.withItemSelection(HeroItemSelection.empty()).getRuntimeSkillLoadout());
        assertSame(loadout, hero.withDetails("Morta", "level=20").getRuntimeSkillLoadout());
        assertEquals("70", hero.getCurrentBuildFormData().getLevel());
        assertThrows(UnsupportedOperationException.class, hero::getSkillLoadout);
    }

    @Test
    void bothDirectionsOfClassMismatchShouldBeRejectedWithoutRepair() {
        for (var heroClass : HeroClass.values()) {
            String wrongClass = heroClass == HeroClass.PALADIN ? "necromancer" : "paladin";
            assertThrows(IllegalArgumentException.class, () -> new HeroProfile(1, "Tester", heroClass,
                    "level=70", HeroItemSelection.empty(), new RuntimeSkillLoadout(wrongClass, Map.of(), List.of())));
        }
        assertThrows(NullPointerException.class, () -> new HeroProfile(1, "Tester", HeroClass.NECROMANCER,
                "level=70", HeroItemSelection.empty(), (RuntimeSkillLoadout) null));
    }

    @Test
    void legacyConstructorsShouldRemainPaladinOnlyAndApplyTheStoredSkillStateToTheForm() {
        var legacy = LegacyPaladinHeroSkillLoadoutAdapterTest.paladinLoadout();
        var paladin = new HeroProfile(1, "Alaric", HeroClass.PALADIN, "level=70", HeroItemSelection.empty(), legacy);
        assertEquals(LegacyPaladinHeroSkillLoadoutAdapter.toRuntimeLoadout(legacy), paladin.getRuntimeSkillLoadout());
        assertEquals("3", paladin.getCurrentBuildFormData().getSkillConfig(krys.skill.SkillId.CLASH).getRank());
        assertThrows(UnsupportedOperationException.class, () -> new HeroProfile(2, "Mortis", HeroClass.NECROMANCER,
                "level=70", HeroItemSelection.empty(), legacy));
        assertThrows(UnsupportedOperationException.class, () -> new HeroProfile(2, "Mortis", HeroClass.NECROMANCER,
                "level=70", HeroItemSelection.empty()));
    }

    @Test
    void unsupportedCurrentBuildModelShouldHaveNoLegacySkillsOrBudget() {
        var hero = new HeroProfile(1, "Mortis", HeroClass.NECROMANCER, "level=70", HeroItemSelection.empty(),
                new RuntimeSkillLoadout("necromancer", Map.of(), List.of()));
        var model = new CurrentBuildPageModel(hero.getCurrentBuildFormData(), List.of(), List.of(), List.of(),
                List.of(), List.of(), null, null, hero, List.of(hero), List.of(),
                "/biblioteka-itemow", "/importuj-item-ze-screena", "/bohaterowie", "");
        assertFalse(model.isCurrentBuildRuntimeSupported());
        assertTrue(model.getAssignedSkillIds().isEmpty());
        assertTrue(model.getActionBarEligibleSkillIds().isEmpty());
        assertNull(model.getSkillPointBudget());
        assertFalse(model.hasResult());
    }
}

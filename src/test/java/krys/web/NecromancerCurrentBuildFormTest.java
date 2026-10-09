package krys.web;

import krys.hero.HeroClass;
import krys.necromancer.app.NecromancerCurrentBuildCalculationService;
import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import krys.runtime.RuntimeSkillLoadout;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NecromancerCurrentBuildFormTest {
    private final RuntimeSkillKey key = NecromancerCurrentBuildCalculationService.KRWOTOK;
    private final RuntimeSkillLoadout empty = new RuntimeSkillLoadout("necromancer", Map.of(), List.of());

    @Test
    void shouldAcceptBlankZeroAndFiniteIntelligenceAndRejectInvalidValues() {
        assertEquals(0, new NecromancerCurrentBuildFormData("", false, "NONE").parsedNonItemIntelligence());
        assertEquals(900, new NecromancerCurrentBuildFormData("900", false, "krwotok").parsedNonItemIntelligence());
        for (String value : List.of("NaN", "Infinity", "-Infinity", "-1", "abc", "1e309")) {
            assertFalse(new NecromancerCurrentBuildFormData(value, false, "krwotok").validationErrors().isEmpty(), value);
        }
    }

    @Test
    void shouldRejectRawLegacySkillIdsOtherSkillsRankAndSelectionsAndInvalidBoolean() {
        for (String skill : List.of("HOLY_BOLT", "KRWOTOK", "mor", "necromancer:krwotok")) {
            assertFalse(new NecromancerCurrentBuildFormData("900", false, skill).validationErrors().isEmpty(), skill);
        }
        for (var fields : List.of(Map.of("rank", "2"), Map.of("rank_krwotok", "2"), Map.of("selections", "grupa_1=szybkosc_uzycia"),
                Map.of("selectedPaladinOathId", "juggernaut"), Map.of("targetVulnerable", "on"))) {
            assertFalse(NecromancerCurrentBuildFormData.postValidationErrors(fields).isEmpty());
        }
        assertTrue(NecromancerCurrentBuildFormData.postValidationErrors(Map.of("targetVulnerable", "false")).isEmpty());
    }

    @Test
    void queryShouldPreserveAllOtherFieldsAndTakeSkillOnlyFromCanonicalLoadout() {
        var query = "level=70&unknown=a%2Bb&intelligence=777&selectedSkillId=krwotok";
        var form = new NecromancerCurrentBuildFormData("900", true, "krwotok");
        var merged = NecromancerCurrentBuildFormQuerySupport.withFormData(query, form);
        var fields = UrlEncodedFormSupport.parseQuery(merged);
        assertEquals("a+b", fields.get("unknown"));
        assertEquals("777", fields.get("intelligence"));
        assertEquals("70", fields.get("level"));
        var read = NecromancerCurrentBuildFormQuerySupport.fromSerializedQuery(merged, empty);
        assertEquals("", read.selectedSkillId());
        assertEquals("900", read.nonItemIntelligence());
        assertTrue(read.targetVulnerable());
        var newQuery = UrlEncodedFormSupport.parseQuery(NecromancerCurrentBuildFormQuerySupport.withFormData("level=70", form));
        assertFalse(newQuery.containsKey("selectedSkillId"));
        assertEquals("krwotok", NecromancerCurrentBuildFormQuerySupport.fromSerializedQuery(merged,
                NecromancerCurrentBuildLoadoutEditor.apply(empty, form)).selectedSkillId());
    }

    @Test
    void editorShouldPreserveOtherConfigurationsAndActionBarWhenAddingAndRemovingKrwotok() {
        var other = new RuntimeSkillKey("necromancer", "mor");
        var otherConfiguration = new RuntimeSkillConfiguration(other, 2, Map.of("grupa_1", "premia"));
        var original = new RuntimeSkillLoadout("necromancer", Map.of(other, otherConfiguration), List.of(other));
        var added = NecromancerCurrentBuildLoadoutEditor.apply(original, new NecromancerCurrentBuildFormData("0", false, "krwotok"));
        assertSame(otherConfiguration, added.configurations().get(other));
        assertEquals(List.of(other, key), added.actionBar());
        assertEquals(new RuntimeSkillConfiguration(key, 1, Map.of()), added.configurations().get(key));
        assertEquals(original, NecromancerCurrentBuildLoadoutEditor.apply(added, new NecromancerCurrentBuildFormData("0", false, "")));
    }

    @Test
    void editorShouldNeverStripUpgradesRaiseRankZeroOrRepairAnOffBarConfiguration() {
        for (var configuration : List.of(new RuntimeSkillConfiguration(key, 1, Map.of("grupa_1", "szybkosc_uzycia")),
                new RuntimeSkillConfiguration(key, 0, Map.of()), new RuntimeSkillConfiguration(key, 2, Map.of()))) {
            var original = new RuntimeSkillLoadout("necromancer", Map.of(key, configuration), List.of());
            assertEquals(original, NecromancerCurrentBuildLoadoutEditor.apply(original, new NecromancerCurrentBuildFormData("0", false, "krwotok")));
        }
    }

    @Test
    void pageModelShouldKeepLegacyStateEmptyAndRuntimeModesSeparate() {
        var hero = new HeroProfile(1, "Mortis", HeroClass.NECROMANCER, "level=70", HeroItemSelection.empty(), empty);
        var model = CurrentBuildPageModel.forNecromancer(new NecromancerCurrentBuildFormData("0", false, ""), List.of(), List.of(), null, null, hero, List.of(hero), List.of());
        assertEquals(CurrentBuildRuntimeMode.NECROMANCER_DIRECT_HIT, model.getRuntimeMode());
        assertFalse(model.isCurrentBuildRuntimeSupported());
        assertNull(model.getFormData());
        assertNull(model.getCalculation());
        assertNull(model.getSkillPointBudget());
        assertTrue(model.getAssignedSkillIds().isEmpty());
        assertTrue(model.getActionBarEligibleSkillIds().isEmpty());
        assertFalse(CurrentBuildRuntimeSupport.isSupported(HeroClass.NECROMANCER));
    }

    @Test
    void neutralProfileMutationShouldValidateClassAndPreserveOtherState() {
        var hero = new HeroProfile(1, "Mortis", HeroClass.NECROMANCER, "level=70&custom=a", HeroItemSelection.empty(), empty);
        var added = NecromancerCurrentBuildLoadoutEditor.apply(empty, new NecromancerCurrentBuildFormData("0", false, "krwotok"));
        assertSame(added, hero.withRuntimeSkillLoadout(added).getRuntimeSkillLoadout());
        assertEquals(hero.getCurrentBuildQuery(), hero.withRuntimeSkillLoadout(added).getCurrentBuildQuery());
        assertThrows(IllegalArgumentException.class, () -> hero.withCurrentBuildRuntimeState("level=70", new RuntimeSkillLoadout("paladin", Map.of(), List.of())));
    }
}

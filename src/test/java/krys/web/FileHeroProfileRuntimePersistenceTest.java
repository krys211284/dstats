package krys.web;

import krys.hero.HeroClass;
import krys.item.HeroEquipmentSlot;
import krys.necromancer.NecromancerSkillTreeRegistry;
import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import krys.runtime.RuntimeSkillLoadout;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class FileHeroProfileRuntimePersistenceTest {
    @TempDir Path directory;

    @Test
    void paladinShouldPersistNeutralPayloadAndRestoreAllEightLegacyFieldsAndRankZero() throws Exception {
        var legacy = LegacyPaladinHeroSkillLoadoutAdapterTest.paladinLoadout();
        var hero = new HeroProfile(1, "Alaric", HeroClass.PALADIN, "level=70", HeroItemSelection.empty(), legacy);
        var repository = new FileHeroProfileRepository(directory);
        repository.save(hero);
        var restored = new FileHeroProfileRepository(directory).findById(1).orElseThrow();
        assertEquals(hero.getRuntimeSkillLoadout(), restored.getRuntimeSkillLoadout());
        for (var skill : legacy.getAssignedSkills().values()) {
            LegacyPaladinHeroSkillLoadoutAdapterTest.assertFields(skill, restored.getSkillLoadout().getAssignedSkill(skill.getSkillId()));
        }
        assertEquals(legacy.getActionBarSkills(), restored.getSkillLoadout().getActionBarSkills());
        assertTrue(savedPayload().startsWith("runtime-v1~paladin~"));
    }

    @Test
    void emptyNecromancerShouldPreserveClassNameLevelAndItemSelectionWithoutALegacyView() throws Exception {
        var hero = new HeroProfile(1, "Mortis", HeroClass.NECROMANCER, "level=70",
                HeroItemSelection.empty().withSelectedItem(HeroEquipmentSlot.MAIN_HAND, 101),
                new RuntimeSkillLoadout("necromancer", Map.of(), List.of()));
        new FileHeroProfileRepository(directory).save(hero);
        var restored = new FileHeroProfileRepository(directory).findById(1).orElseThrow();
        assertEquals(HeroClass.NECROMANCER, restored.getHeroClass());
        assertEquals("Mortis", restored.getName());
        assertEquals("70", restored.getCurrentBuildFormData().getLevel());
        assertEquals("necromancer", restored.getRuntimeSkillLoadout().classId());
        assertTrue(restored.getRuntimeSkillLoadout().configurations().isEmpty());
        assertTrue(restored.getRuntimeSkillLoadout().actionBar().isEmpty());
        assertEquals(hero.getItemSelection().getSelectedItemIdsBySlot(), restored.getItemSelection().getSelectedItemIdsBySlot());
        assertThrows(UnsupportedOperationException.class, restored::getSkillLoadout);
        assertEquals("runtime-v1~necromancer~~", savedPayload());
    }

    @Test
    void technicalKrwotokConfigurationOnActionBarShouldRoundTripWithoutEnablingWebRuntime() {
        var key = new RuntimeSkillKey("necromancer", "krwotok");
        var configuration = new RuntimeSkillConfiguration(key, 1, Map.of());
        var loadout = new RuntimeSkillLoadout("necromancer", Map.of(key, configuration), List.of(key));
        var hero = new HeroProfile(1, "Mortis", HeroClass.NECROMANCER, "level=70", HeroItemSelection.empty(), loadout);
        new FileHeroProfileRepository(directory).save(hero);
        var restored = new FileHeroProfileRepository(directory).findById(1).orElseThrow();
        assertEquals(loadout, restored.getRuntimeSkillLoadout());
        assertEquals(configuration, restored.getRuntimeSkillLoadout().configuration(key).orElseThrow());
        assertEquals(List.of(key), restored.getRuntimeSkillLoadout().actionBar());
        assertFalse(CurrentBuildRuntimeSupport.isSupported(restored.getHeroClass()));
    }

    @Test
    void selectionsFromTheSourceCatalogShouldAlsoRoundTripAsTechnicalData() {
        var key = new RuntimeSkillKey("necromancer", "krwotok");
        var group = NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill("krwotok").getUpgradeGroups().getFirst();
        var configuration = new RuntimeSkillConfiguration(key, 1, Map.of(group.getId(), group.getUpgrades().getFirst().getId()));
        var loadout = new RuntimeSkillLoadout("necromancer", Map.of(key, configuration), List.of(key));
        new FileHeroProfileRepository(directory).save(new HeroProfile(1, "Mortis", HeroClass.NECROMANCER,
                "level=70", HeroItemSelection.empty(), loadout));
        assertEquals(loadout, new FileHeroProfileRepository(directory).findById(1).orElseThrow().getRuntimeSkillLoadout());
    }

    @Test
    void sixFieldPaladinRecordShouldDeriveItsLoadoutFromQueryAndMigrateOnSave() throws Exception {
        // Format sześciopolowy odtwarza historyczny formularz, który wyznacza modyfikatory CLASH z grup drzewa.
        var legacy = new HeroSkillLoadout(Map.of(SkillId.ADVANCE,
                new HeroAssignedSkill(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT),
                SkillId.CLASH, new HeroAssignedSkill(SkillId.CLASH, 3, true, SkillUpgradeChoice.LEFT, SkillRuntimeModifierChoice.ANIMUS)),
                List.of(SkillId.CLASH, SkillId.ADVANCE));
        var form = legacy.applyToFormData(CurrentBuildFormData.defaultValues());
        String query = CurrentBuildFormQuerySupport.toQuery(form);
        Files.writeString(directory.resolve("heroes.db"), line(HeroClass.PALADIN, query, null));
        var repository = new FileHeroProfileRepository(directory);
        var restored = repository.findById(1).orElseThrow();
        assertEquals(HeroClass.PALADIN, restored.getHeroClass());
        assertEquals(LegacyPaladinHeroSkillLoadoutAdapter.toRuntimeLoadout(HeroSkillLoadout.fromCurrentBuildFormData(form)),
                restored.getRuntimeSkillLoadout());
        repository.save(restored);
        assertTrue(savedPayload().startsWith("runtime-v1~paladin~"));
        assertEquals(restored.getRuntimeSkillLoadout(), new FileHeroProfileRepository(directory).findById(1).orElseThrow().getRuntimeSkillLoadout());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ADVANCE:1:false:NONE~ADVANCE", "CLASH:3:true:LEFT:ANIMUS~CLASH",
            "CLASH:3:true:LEFT:ANIMUS:generowanie_wiary:zwiekszenie_obrazen:potyczka;ADVANCE:0:false:NONE:NONE:NONE:NONE:NONE~CLASH"})
    void sevenFieldLegacyPaladinRecordsShouldReadAllHistoricalSkillEntryVersionsAndMigrate(String payload) throws Exception {
        Files.writeString(directory.resolve("heroes.db"), line(HeroClass.PALADIN, "level=70", payload));
        var repository = new FileHeroProfileRepository(directory);
        var restored = repository.findById(1).orElseThrow();
        assertEquals(HeroClass.PALADIN, restored.getHeroClass());
        assertEquals("paladin", restored.getRuntimeSkillLoadout().classId());
        if (payload.startsWith("ADVANCE")) {
            LegacyPaladinHeroSkillLoadoutAdapterTest.assertFields(new HeroAssignedSkill(SkillId.ADVANCE, 1, false, SkillUpgradeChoice.NONE),
                    restored.getSkillLoadout().getAssignedSkill(SkillId.ADVANCE));
            assertEquals(List.of(SkillId.ADVANCE), restored.getSkillLoadout().getActionBarSkills());
        } else {
            var expected = payload.contains("potyczka")
                    ? LegacyPaladinHeroSkillLoadoutAdapterTest.paladinLoadout().getAssignedSkill(SkillId.CLASH)
                    : new HeroAssignedSkill(SkillId.CLASH, 3, true, SkillUpgradeChoice.LEFT, SkillRuntimeModifierChoice.ANIMUS);
            LegacyPaladinHeroSkillLoadoutAdapterTest.assertFields(expected, restored.getSkillLoadout().getAssignedSkill(SkillId.CLASH));
            assertEquals(List.of(SkillId.CLASH), restored.getSkillLoadout().getActionBarSkills());
            if (payload.contains("ADVANCE:0")) {
                assertEquals(0, restored.getSkillLoadout().getAssignedSkill(SkillId.ADVANCE).getRank());
            }
        }
        repository.save(restored);
        assertTrue(savedPayload().startsWith("runtime-v1~paladin~"));
        assertEquals(restored.getRuntimeSkillLoadout(), new FileHeroProfileRepository(directory).findById(1).orElseThrow().getRuntimeSkillLoadout());
    }

    @Test
    void legacyRecordsShouldNeverBeReinterpretedAsNecromancerAndNewRecordsShouldRejectClassMismatch() throws Exception {
        for (String payload : new String[]{null, "ADVANCE:1:false:NONE~ADVANCE", "runtime-v1~paladin~~"}) {
            Files.writeString(directory.resolve("heroes.db"), line(HeroClass.NECROMANCER, "level=70", payload));
            assertThrows(IllegalStateException.class, () -> new FileHeroProfileRepository(directory).findAll());
        }
        Files.writeString(directory.resolve("heroes.db"), line(HeroClass.PALADIN, "level=70", "runtime-v1~necromancer~~"));
        assertThrows(IllegalStateException.class, () -> new FileHeroProfileRepository(directory).findAll());
    }

    @ParameterizedTest
    @ValueSource(strings = {"broken~", "ADVANCE:1:false:NONE;broken~ADVANCE", "ADVANCE:1:invalid:NONE~ADVANCE",
            "ADVANCE:1:false:NONE~HOLY_BOLT", "HOLY_BOLT:0:false:NONE~HOLY_BOLT",
            "ADVANCE:1:false:NONE;ADVANCE:1:false:NONE~ADVANCE",
            "runtime-v1~paladin~~paladin:swiety_pocisk", "runtime-v1~paladin~paladin:swiety_pocisk@0@~paladin:swiety_pocisk"})
    void malformedAndInconsistentFilePayloadsShouldRaiseAnExplicitDataError(String payload) throws Exception {
        Files.writeString(directory.resolve("heroes.db"), line(HeroClass.PALADIN, "level=70", payload));
        var error = assertThrows(IllegalStateException.class, () -> new FileHeroProfileRepository(directory).findAll());
        assertTrue(error.getMessage().contains("Niepoprawne dane bohatera"));
        assertEquals(line(HeroClass.PALADIN, "level=70", payload), Files.readString(directory.resolve("heroes.db")));
    }

    private String savedPayload() throws Exception {
        String encoded = Files.readAllLines(directory.resolve("heroes.db")).getFirst().split("\\|", -1)[6];
        return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
    }

    private static String line(HeroClass heroClass, String query, String payload) {
        String base = "HERO|1|" + encode("Mortis") + "|" + heroClass.name() + "|" + encode(query) + "|" + encode("MAIN_HAND:101");
        return payload == null ? base : base + "|" + encode(payload);
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}

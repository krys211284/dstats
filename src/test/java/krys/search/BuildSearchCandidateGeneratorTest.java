package krys.search;

import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildSearchCandidateGeneratorTest {
    private final BuildSearchCandidateGenerator generator = new BuildSearchCandidateGenerator();

    @Test
    void powinien_generowac_wylacznie_legalnych_kandydatow_searcha() {
        BuildSearchRequest request = new BuildSearchRequest(
                List.of(13),
                List.of(8L),
                List.of(18.0d),
                List.of(0.0d),
                List.of(0.0d),
                List.of(0.0d),
                List.of(0.0d),
                createSkillSpaces(),
                List.of(1),
                9,
                5
        );

        List<BuildSearchCandidate> candidates = generator.generate(request);

        assertEquals(2, candidates.size());
        assertTrue(candidates.stream().allMatch(candidate -> candidate.getCurrentBuildRequest().getActionBar().size() == 1));
        assertTrue(candidates.stream().allMatch(candidate -> candidate.getCurrentBuildRequest().getLearnedSkills().size() == 1));
        assertTrue(candidates.stream().allMatch(candidate ->
                candidate.getCurrentBuildRequest().getActionBar().stream()
                        .allMatch(skillId -> candidate.getCurrentBuildRequest().getLearnedSkills().containsKey(skillId))
        ));
        assertTrue(candidates.stream().noneMatch(candidate -> candidate.getCurrentBuildRequest().getLearnedSkills().containsKey(SkillId.HOLY_BOLT)));

        Set<String> keys = candidates.stream()
                .map(BuildSearchCandidate::toDeterministicKey)
                .collect(Collectors.toSet());
        assertEquals(2, keys.size());
    }

    @Test
    void powinien_traktowac_kolejnosc_action_bara_jako_semantyczna_i_nie_generowac_nielegalnych_slotow() {
        BuildSearchRequest request = new BuildSearchRequest(
                List.of(13),
                List.of(8L),
                List.of(18.0d),
                List.of(0.0d),
                List.of(0.0d),
                List.of(0.0d),
                List.of(0.0d),
                createDualSkillSpaces(),
                List.of(2),
                9,
                5
        );

        List<BuildSearchCandidate> candidates = generator.generate(request);

        assertEquals(2, candidates.size());
        assertTrue(candidates.stream().anyMatch(candidate ->
                candidate.getCurrentBuildRequest().getActionBar().equals(List.of(SkillId.BRANDISH, SkillId.HOLY_BOLT))
        ));
        assertTrue(candidates.stream().anyMatch(candidate ->
                candidate.getCurrentBuildRequest().getActionBar().equals(List.of(SkillId.HOLY_BOLT, SkillId.BRANDISH))
        ));
        assertFalse(candidates.stream().anyMatch(candidate ->
                candidate.getCurrentBuildRequest().getActionBar().contains(SkillId.CLASH)
        ));
    }

    @Test
    void powinien_budowac_kandydatow_z_biblioteki_itemow_do_tego_samego_current_build_request() throws Exception {
        Path tempDirectory = Files.createTempDirectory("search-library-candidates");
        ItemLibraryService itemLibraryService = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        itemLibraryService.saveImportedItem(new krys.itemimport.ValidatedImportedItem(
                "weapon-a.png",
                EquipmentSlot.MAIN_HAND,
                300L,
                55.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        ));
        itemLibraryService.saveImportedItem(new krys.itemimport.ValidatedImportedItem(
                "weapon-b.png",
                EquipmentSlot.MAIN_HAND,
                321L,
                60.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        ));
        itemLibraryService.saveImportedItem(new krys.itemimport.ValidatedImportedItem(
                "shield-a.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                11.0d,
                90.0d,
                18.0d,
                25.0d
        ));
        BuildSearchCandidateGenerator generatorWithLibrary = new BuildSearchCandidateGenerator(itemLibraryService);
        BuildSearchRequest request = new BuildSearchRequest(
                true,
                List.of(13),
                List.of(0L),
                List.of(30.0d),
                List.of(11.0d),
                List.of(70.0d),
                List.of(10.0d),
                List.of(15.0d),
                createSingleSkillSpace(),
                List.of(1),
                9,
                5
        );

        List<BuildSearchCandidate> candidates = generatorWithLibrary.generate(request);

        assertEquals(4, candidates.size());
        assertTrue(candidates.stream().allMatch(BuildSearchCandidate::usesItemLibrary));
        assertTrue(candidates.stream().allMatch(candidate ->
                candidate.getItemLibraryCombination().getSelectedItems().stream()
                        .map(assignment -> assignment.getHeroSlot())
                        .distinct()
                        .count() == candidate.getItemLibraryCombination().getSelectedItems().size()
        ));
        assertFalse(candidates.stream().anyMatch(candidate -> candidate.getCurrentBuildRequest().getWeaponDamage() == 0L));
        assertTrue(candidates.stream().anyMatch(candidate ->
                candidate.getCurrentBuildRequest().getWeaponDamage() == 300L
                        && candidate.getCurrentBuildRequest().getStrength() == 85.0d
                        && candidate.getSelectedItemLibraryItemsDescription().contains("Broń")
        ));
        assertTrue(candidates.stream().anyMatch(candidate ->
                candidate.getCurrentBuildRequest().getWeaponDamage() == 321L
                        && candidate.getCurrentBuildRequest().getStrength() == 90.0d
                        && candidate.getCurrentBuildRequest().getIntelligence() == 22.0d
                        && candidate.getCurrentBuildRequest().getThorns() == 160.0d
                        && candidate.getCurrentBuildRequest().getBlockChance() == 28.0d
                        && candidate.getCurrentBuildRequest().getRetributionChance() == 40.0d
                        && candidate.getItemLibraryContributionDescription().contains("obrażenia broni=321")
                        && candidate.getItemLibraryContributionDescription().contains("inteligencja=11")
        ));
        assertTrue(candidates.stream().anyMatch(candidate ->
                candidate.getItemLibraryCombination().getSelectedItems().stream()
                        .anyMatch(assignment -> assignment.getHeroSlot() == HeroEquipmentSlot.OFF_HAND)
        ));
    }

    private static Map<SkillId, BuildSearchSkillSpace> createSkillSpaces() {
        Map<SkillId, BuildSearchSkillSpace> skillSpaces = offOnlySkillSpaces();
        skillSpaces.put(SkillId.BRANDISH, new BuildSearchSkillSpace(
                SkillId.BRANDISH,
                List.of(0, 5),
                List.of(false, true),
                List.of(SkillUpgradeChoice.NONE)
        ));
        return skillSpaces;
    }

    private static Map<SkillId, BuildSearchSkillSpace> createSingleSkillSpace() {
        Map<SkillId, BuildSearchSkillSpace> skillSpaces = offOnlySkillSpaces();
        skillSpaces.put(SkillId.BRANDISH, new BuildSearchSkillSpace(
                SkillId.BRANDISH,
                List.of(5),
                List.of(false),
                List.of(SkillUpgradeChoice.NONE)
        ));
        return skillSpaces;
    }

    private static Map<SkillId, BuildSearchSkillSpace> createDualSkillSpaces() {
        Map<SkillId, BuildSearchSkillSpace> skillSpaces = offOnlySkillSpaces();
        skillSpaces.put(SkillId.BRANDISH, new BuildSearchSkillSpace(
                SkillId.BRANDISH,
                List.of(5),
                List.of(false),
                List.of(SkillUpgradeChoice.NONE)
        ));
        skillSpaces.put(SkillId.HOLY_BOLT, new BuildSearchSkillSpace(
                SkillId.HOLY_BOLT,
                List.of(5),
                List.of(false),
                List.of(SkillUpgradeChoice.NONE)
        ));
        return skillSpaces;
    }

    private static Map<SkillId, BuildSearchSkillSpace> offOnlySkillSpaces() {
        Map<SkillId, BuildSearchSkillSpace> skillSpaces = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            skillSpaces.put(skillId, new BuildSearchSkillSpace(
                    skillId,
                    List.of(0),
                    List.of(false),
                    List.of(SkillUpgradeChoice.NONE)
            ));
        }
        return skillSpaces;
    }
}

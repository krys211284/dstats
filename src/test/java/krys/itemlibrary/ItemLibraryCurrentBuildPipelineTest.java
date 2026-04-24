package krys.itemlibrary;

import krys.app.CurrentBuildRequest;
import krys.app.CurrentBuildSnapshotFactory;
import krys.item.Item;
import krys.item.ItemStatType;
import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.simulation.HeroBuildSnapshot;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import krys.web.HeroItemSelection;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Potwierdza, że biblioteka itemów nadal kończy się zwykłym CurrentBuildRequest i tym samym runtime snapshot. */
class ItemLibraryCurrentBuildPipelineTest {
    @Test
    void shouldStillUseCurrentBuildRequestAndSnapshotFactoryAfterResolvingLibraryItems() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-pipeline");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        service.saveImportedItem(new krys.itemimport.ValidatedImportedItem(
                "shield.png",
                EquipmentSlot.OFF_HAND,
                0L,
                114.0d,
                13.0d,
                494.0d,
                20.0d,
                25.0d
        ));
        long itemId = service.getSavedItems().getFirst().getItemId();
        HeroItemSelection selection = HeroItemSelection.empty()
                .withSelectedItem(HeroEquipmentSlot.OFF_HAND, itemId);

        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(
                new CurrentBuildImportableStats(200L, 30.0d, 11.0d, 70.0d, 10.0d, 15.0d),
                selection
        );
        CurrentBuildRequest request = new CurrentBuildRequest(
                13,
                resolution.getEffectiveStats().getWeaponDamage(),
                resolution.getEffectiveStats().getStrength(),
                resolution.getEffectiveStats().getIntelligence(),
                resolution.getEffectiveStats().getThorns(),
                resolution.getEffectiveStats().getBlockChance(),
                resolution.getEffectiveStats().getRetributionChance(),
                Map.of(SkillId.ADVANCE, new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT)),
                List.of(SkillId.ADVANCE),
                10
        );

        HeroBuildSnapshot snapshot = new CurrentBuildSnapshotFactory().create(request);

        assertEquals(200L, snapshot.getAverageWeaponDamage());
        assertEquals(144.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.STRENGTH), 0.0000001d);
        assertEquals(24.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.INTELLIGENCE), 0.0000001d);
        assertEquals(564.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.THORNS), 0.0000001d);
        assertEquals(30.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.BLOCK_CHANCE), 0.0000001d);
        assertEquals(40.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.RETRIBUTION_CHANCE), 0.0000001d);
    }

    @Test
    void shouldAllowZeroManualBaseBeforeBuildingEffectiveCurrentBuildRequest() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-library-zero-base");
        ItemLibraryService service = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        SavedImportedItem mainHand = service.saveImportedItem(new krys.itemimport.ValidatedImportedItem(
                "weapon.png",
                EquipmentSlot.MAIN_HAND,
                321L,
                55.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        ));
        SavedImportedItem offHand = service.saveImportedItem(new krys.itemimport.ValidatedImportedItem(
                "shield.png",
                EquipmentSlot.OFF_HAND,
                0L,
                114.0d,
                13.0d,
                494.0d,
                20.0d,
                25.0d
        ));
        HeroItemSelection selection = new HeroItemSelection(Map.of(
                HeroEquipmentSlot.MAIN_HAND, mainHand.getItemId(),
                HeroEquipmentSlot.OFF_HAND, offHand.getItemId()
        ));

        EffectiveCurrentBuildResolution resolution = service.resolveEffectiveCurrentBuild(
                new CurrentBuildImportableStats(0L, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d),
                selection
        );
        CurrentBuildRequest request = new CurrentBuildRequest(
                13,
                resolution.getEffectiveStats().getWeaponDamage(),
                resolution.getEffectiveStats().getStrength(),
                resolution.getEffectiveStats().getIntelligence(),
                resolution.getEffectiveStats().getThorns(),
                resolution.getEffectiveStats().getBlockChance(),
                resolution.getEffectiveStats().getRetributionChance(),
                Map.of(SkillId.ADVANCE, new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT)),
                List.of(SkillId.ADVANCE),
                10
        );

        HeroBuildSnapshot snapshot = new CurrentBuildSnapshotFactory().create(request);

        assertEquals(321L, snapshot.getAverageWeaponDamage());
        assertEquals(169.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.STRENGTH), 0.0000001d);
        assertEquals(13.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.INTELLIGENCE), 0.0000001d);
        assertEquals(494.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.THORNS), 0.0000001d);
        assertEquals(20.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.BLOCK_CHANCE), 0.0000001d);
        assertEquals(25.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.RETRIBUTION_CHANCE), 0.0000001d);
    }
}

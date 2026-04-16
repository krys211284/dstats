package krys.app;

import krys.item.Item;
import krys.item.ItemStatType;
import krys.simulation.HeroBuildSnapshot;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CurrentBuildSnapshotFactoryTest {
    private final CurrentBuildSnapshotFactory snapshotFactory = new CurrentBuildSnapshotFactory();

    @Test
    void powinien_zbudowac_snapshot_z_realnych_danych_wejsciowych_uzytkownika() {
        CurrentBuildRequest request = new CurrentBuildRequest(
                20,
                11,
                30.0d,
                5.0d,
                60.0d,
                40.0d,
                20.0d,
                Map.of(
                        SkillId.BRANDISH, new SkillState(SkillId.BRANDISH, 5, true, SkillUpgradeChoice.LEFT),
                        SkillId.ADVANCE, new SkillState(SkillId.ADVANCE, 5, true, SkillUpgradeChoice.RIGHT)
                ),
                List.of(SkillId.ADVANCE, SkillId.BRANDISH),
                15
        );

        HeroBuildSnapshot snapshot = snapshotFactory.create(request);

        assertEquals(20, snapshot.getHero().getLevel());
        assertEquals(11L, snapshot.getAverageWeaponDamage());
        assertEquals(30.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.STRENGTH), 0.0000001d);
        assertEquals(5.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.INTELLIGENCE), 0.0000001d);
        assertEquals(60.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.THORNS), 0.0000001d);
        assertEquals(40.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.BLOCK_CHANCE), 0.0000001d);
        assertEquals(20.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.RETRIBUTION_CHANCE), 0.0000001d);
        assertEquals(1.5d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.CRIT_DAMAGE), 0.0000001d);
        assertEquals(100.0d, Item.sumStat(snapshot.getEquippedItems(), ItemStatType.MAIN_HAND_WEAPON_DAMAGE), 0.0000001d);
        assertEquals(List.of(SkillId.ADVANCE, SkillId.BRANDISH), snapshot.getSelectedSkillBar());
        assertEquals(2, snapshot.getLearnedSkills().size());
    }
}

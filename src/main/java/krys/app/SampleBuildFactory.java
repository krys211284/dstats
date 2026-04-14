package krys.app;

import krys.hero.Hero;
import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.item.Item;
import krys.item.ItemStat;
import krys.item.ItemStatType;
import krys.simulation.HeroBuildSnapshot;
import krys.skill.SkillId;
import krys.skill.SkillState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Fabryka minimalnych buildów testowych używanych przez pierwszy ręczny slice użytkownika. */
public final class SampleBuildFactory {
    private SampleBuildFactory() {
    }

    public static HeroBuildSnapshot createReferenceCurrentBuild(SkillState selectedSkillState) {
        return createReferenceCurrentBuild(List.of(selectedSkillState), List.of(selectedSkillState.getSkillId()));
    }

    public static HeroBuildSnapshot createReferenceCurrentBuild(List<SkillState> learnedSkills, List<SkillId> selectedSkillBar) {
        Hero hero = new Hero(1, "Krys", 13, HeroClass.PALADIN);
        List<Item> items = List.of(
                new Item(1, "Short Sword", EquipmentSlot.MAIN_HAND, List.of(
                        new ItemStat(ItemStatType.CRIT_DAMAGE, 1.5d)
                )),
                new Item(2, "Shield", EquipmentSlot.OFF_HAND, List.of(
                        new ItemStat(ItemStatType.MAIN_HAND_WEAPON_DAMAGE, 100.0d),
                        new ItemStat(ItemStatType.STRENGTH, 7.0d)
                )),
                new Item(3, "Armor", EquipmentSlot.CHEST, List.of(
                        new ItemStat(ItemStatType.STRENGTH, 8.0d)
                )),
                new Item(4, "Ring of Strength", EquipmentSlot.RING, List.of(
                new ItemStat(ItemStatType.STRENGTH, 3.0d)
                ))
        );

        Map<SkillId, SkillState> learnedSkillsMap = new LinkedHashMap<>();
        for (SkillState learnedSkill : learnedSkills) {
            learnedSkillsMap.put(learnedSkill.getSkillId(), learnedSkill);
        }

        return new HeroBuildSnapshot(
                hero,
                0,
                8,
                0.0d,
                items,
                learnedSkillsMap,
                selectedSkillBar
        );
    }
}

package krys.simulation;

import krys.hero.Hero;
import krys.item.EquipmentSlot;
import krys.item.Item;
import krys.skill.SkillId;
import krys.skill.SkillState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Wspólne wejście runtime dla manual simulation i build search.
 * Na tym etapie zawiera wyłącznie pola potrzebne do foundation Damage Engine.
 */
public final class HeroBuildSnapshot {
    private final Hero hero;
    private final int bonusSkillPoints;
    private final long averageWeaponDamage;
    private final double totalPercentDamageBonus;
    private final List<Item> equippedItems;
    private final boolean hasActiveWeapon;
    private final boolean hasActiveShield;
    private final Map<SkillId, SkillState> learnedSkills;
    private final List<SkillId> selectedSkillBar;

    public HeroBuildSnapshot(Hero hero,
                             int bonusSkillPoints,
                             long averageWeaponDamage,
                             double totalPercentDamageBonus,
                             List<Item> equippedItems,
                             Map<SkillId, SkillState> learnedSkills,
                             List<SkillId> selectedSkillBar) {
        this(hero, bonusSkillPoints, averageWeaponDamage, totalPercentDamageBonus, equippedItems,
                hasEquippedSlot(equippedItems, EquipmentSlot.MAIN_HAND),
                hasEquippedSlot(equippedItems, EquipmentSlot.OFF_HAND),
                learnedSkills, selectedSkillBar);
    }

    public HeroBuildSnapshot(Hero hero,
                             int bonusSkillPoints,
                             long averageWeaponDamage,
                             double totalPercentDamageBonus,
                             List<Item> equippedItems,
                             boolean hasActiveWeapon,
                             boolean hasActiveShield,
                             Map<SkillId, SkillState> learnedSkills,
                             List<SkillId> selectedSkillBar) {
        this.hero = hero;
        this.bonusSkillPoints = bonusSkillPoints;
        this.averageWeaponDamage = averageWeaponDamage;
        this.totalPercentDamageBonus = totalPercentDamageBonus;
        this.equippedItems = Collections.unmodifiableList(new ArrayList<>(equippedItems == null ? List.of() : equippedItems));
        this.hasActiveWeapon = hasActiveWeapon;
        this.hasActiveShield = hasActiveShield;

        EnumMap<SkillId, SkillState> learnedSkillsCopy = new EnumMap<>(SkillId.class);
        if (learnedSkills != null && !learnedSkills.isEmpty()) {
            learnedSkillsCopy.putAll(learnedSkills);
        }
        this.learnedSkills = Collections.unmodifiableMap(learnedSkillsCopy);

        List<SkillId> selectedBar = selectedSkillBar == null ? List.of() : selectedSkillBar;
        this.selectedSkillBar = Collections.unmodifiableList(new ArrayList<>(new LinkedHashSet<>(selectedBar)));
    }

    public Hero getHero() {
        return hero;
    }

    public int getBonusSkillPoints() {
        return bonusSkillPoints;
    }

    public long getAverageWeaponDamage() {
        return averageWeaponDamage;
    }

    public double getTotalPercentDamageBonus() {
        return totalPercentDamageBonus;
    }

    public List<Item> getEquippedItems() {
        return equippedItems;
    }

    public boolean hasActiveWeapon() {
        return hasActiveWeapon;
    }

    public boolean hasActiveShield() {
        return hasActiveShield;
    }

    public Map<SkillId, SkillState> getLearnedSkills() {
        return learnedSkills;
    }

    public List<SkillId> getSelectedSkillBar() {
        return selectedSkillBar;
    }

    public SkillState getSkillState(SkillId skillId) {
        return learnedSkills.get(skillId);
    }

    private static boolean hasEquippedSlot(List<Item> equippedItems, EquipmentSlot slot) {
        if (equippedItems == null) {
            return false;
        }
        for (Item item : equippedItems) {
            if (item != null && item.getSlot() == slot) {
                return true;
            }
        }
        return false;
    }
}

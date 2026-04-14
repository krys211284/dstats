package krys.simulation;

import krys.hero.Hero;
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
    private final Map<SkillId, SkillState> learnedSkills;
    private final List<SkillId> selectedSkillBar;

    public HeroBuildSnapshot(Hero hero,
                             int bonusSkillPoints,
                             long averageWeaponDamage,
                             double totalPercentDamageBonus,
                             List<Item> equippedItems,
                             Map<SkillId, SkillState> learnedSkills,
                             List<SkillId> selectedSkillBar) {
        this.hero = hero;
        this.bonusSkillPoints = bonusSkillPoints;
        this.averageWeaponDamage = averageWeaponDamage;
        this.totalPercentDamageBonus = totalPercentDamageBonus;
        this.equippedItems = Collections.unmodifiableList(new ArrayList<>(equippedItems == null ? List.of() : equippedItems));

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

    public Map<SkillId, SkillState> getLearnedSkills() {
        return learnedSkills;
    }

    public List<SkillId> getSelectedSkillBar() {
        return selectedSkillBar;
    }

    public SkillState getSkillState(SkillId skillId) {
        return learnedSkills.get(skillId);
    }
}

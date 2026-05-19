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
    public static final double DEFAULT_INITIAL_PRIMARY_RESOURCE = 100.0d;
    public static final double DEFAULT_MAX_PRIMARY_RESOURCE = 100.0d;
    public static final double DEFAULT_PRIMARY_RESOURCE_REGEN_PER_SECOND = 1.50d;

    private final Hero hero;
    private final int bonusSkillPoints;
    private final long averageWeaponDamage;
    private final double totalPercentDamageBonus;
    private final List<Item> equippedItems;
    private final boolean hasActiveWeapon;
    private final boolean hasActiveShield;
    private final Map<SkillId, SkillState> learnedSkills;
    private final List<SkillId> selectedSkillBar;
    private final double initialPrimaryResource;
    private final double maxPrimaryResource;
    private final double primaryResourceRegenPerSecond;
    private final List<String> activeAspectIds;

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
                learnedSkills, selectedSkillBar, List.of());
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
        this(hero, bonusSkillPoints, averageWeaponDamage, totalPercentDamageBonus, equippedItems,
                hasActiveWeapon, hasActiveShield, learnedSkills, selectedSkillBar, List.of());
    }

    public HeroBuildSnapshot(Hero hero,
                             int bonusSkillPoints,
                             long averageWeaponDamage,
                             double totalPercentDamageBonus,
                             List<Item> equippedItems,
                             boolean hasActiveWeapon,
                             boolean hasActiveShield,
                             Map<SkillId, SkillState> learnedSkills,
                             List<SkillId> selectedSkillBar,
                             List<String> activeAspectIds) {
        this(hero, bonusSkillPoints, averageWeaponDamage, totalPercentDamageBonus, equippedItems,
                hasActiveWeapon, hasActiveShield, learnedSkills, selectedSkillBar,
                DEFAULT_INITIAL_PRIMARY_RESOURCE, DEFAULT_MAX_PRIMARY_RESOURCE,
                DEFAULT_PRIMARY_RESOURCE_REGEN_PER_SECOND, activeAspectIds);
    }

    public HeroBuildSnapshot(Hero hero,
                             int bonusSkillPoints,
                             long averageWeaponDamage,
                             double totalPercentDamageBonus,
                             List<Item> equippedItems,
                             boolean hasActiveWeapon,
                             boolean hasActiveShield,
                             Map<SkillId, SkillState> learnedSkills,
                             List<SkillId> selectedSkillBar,
                             double initialPrimaryResource,
                             double maxPrimaryResource,
                             double primaryResourceRegenPerSecond,
                             List<String> activeAspectIds) {
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
        this.initialPrimaryResource = initialPrimaryResource;
        this.maxPrimaryResource = maxPrimaryResource;
        this.primaryResourceRegenPerSecond = primaryResourceRegenPerSecond;
        this.activeAspectIds = Collections.unmodifiableList(new ArrayList<>(new LinkedHashSet<>(activeAspectIds == null ? List.of() : activeAspectIds)));
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

    public List<String> getActiveAspectIds() {
        return activeAspectIds;
    }

    public double getInitialPrimaryResource() {
        return initialPrimaryResource;
    }

    public double getMaxPrimaryResource() {
        return maxPrimaryResource;
    }

    public double getPrimaryResourceRegenPerSecond() {
        return primaryResourceRegenPerSecond;
    }

    public boolean hasActiveAspect(String aspectId) {
        return activeAspectIds.contains(aspectId);
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

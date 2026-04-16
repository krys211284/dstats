package krys.web;

import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Surowe dane formularza GUI M8, zachowywane także przy błędach walidacji. */
public final class CurrentBuildFormData {
    private final String level;
    private final String weaponDamage;
    private final String strength;
    private final String intelligence;
    private final String thorns;
    private final String blockChance;
    private final String retributionChance;
    private final String horizonSeconds;
    private final Map<SkillId, SkillConfigFormData> skillConfigs;
    private final List<String> actionBarSlots;

    public CurrentBuildFormData(String level,
                                String weaponDamage,
                                String strength,
                                String intelligence,
                                String thorns,
                                String blockChance,
                                String retributionChance,
                                String horizonSeconds,
                                Map<SkillId, SkillConfigFormData> skillConfigs,
                                List<String> actionBarSlots) {
        this.level = level;
        this.weaponDamage = weaponDamage;
        this.strength = strength;
        this.intelligence = intelligence;
        this.thorns = thorns;
        this.blockChance = blockChance;
        this.retributionChance = retributionChance;
        this.horizonSeconds = horizonSeconds;
        this.skillConfigs = Collections.unmodifiableMap(new EnumMap<>(skillConfigs));
        this.actionBarSlots = Collections.unmodifiableList(new ArrayList<>(actionBarSlots));
    }

    public static CurrentBuildFormData defaultValues() {
        Map<SkillId, SkillConfigFormData> skillConfigs = createEmptySkillConfigs();
        skillConfigs.put(SkillId.ADVANCE, new SkillConfigFormData("5", true, SkillUpgradeChoice.RIGHT.name()));
        return new CurrentBuildFormData("13", "8", "18", "0", "50", "50", "50", "10",
                skillConfigs,
                List.of(SkillId.ADVANCE.name(), "NONE", "NONE", "NONE"));
    }

    public static CurrentBuildFormData fromFormFields(Map<String, String> fields) {
        CurrentBuildFormData defaults = defaultValues();
        Map<SkillId, SkillConfigFormData> skillConfigs = createEmptySkillConfigs();
        for (SkillId skillId : SkillId.values()) {
            SkillConfigFormData defaultSkillConfig = defaults.getSkillConfig(skillId);
            skillConfigs.put(skillId, new SkillConfigFormData(
                    fields.getOrDefault(rankFieldName(skillId), defaultSkillConfig.getRank()),
                    fields.containsKey(baseUpgradeFieldName(skillId)),
                    fields.getOrDefault(choiceFieldName(skillId), defaultSkillConfig.getChoiceUpgrade())
            ));
        }

        List<String> actionBarSlots = new ArrayList<>();
        for (int slot = 1; slot <= 4; slot++) {
            actionBarSlots.add(fields.getOrDefault(actionBarFieldName(slot), defaults.getActionBarSlot(slot)));
        }

        return new CurrentBuildFormData(
                fields.getOrDefault("level", defaults.getLevel()),
                fields.getOrDefault("weaponDamage", defaults.getWeaponDamage()),
                fields.getOrDefault("strength", defaults.getStrength()),
                fields.getOrDefault("intelligence", defaults.getIntelligence()),
                fields.getOrDefault("thorns", defaults.getThorns()),
                fields.getOrDefault("blockChance", defaults.getBlockChance()),
                fields.getOrDefault("retributionChance", defaults.getRetributionChance()),
                fields.getOrDefault("horizonSeconds", defaults.getHorizonSeconds()),
                skillConfigs,
                actionBarSlots
        );
    }

    private static Map<SkillId, SkillConfigFormData> createEmptySkillConfigs() {
        Map<SkillId, SkillConfigFormData> skillConfigs = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            skillConfigs.put(skillId, new SkillConfigFormData("0", false, SkillUpgradeChoice.NONE.name()));
        }
        return skillConfigs;
    }

    public static String rankFieldName(SkillId skillId) {
        return "rank_" + skillId.name();
    }

    public static String baseUpgradeFieldName(SkillId skillId) {
        return "baseUpgrade_" + skillId.name();
    }

    public static String choiceFieldName(SkillId skillId) {
        return "choiceUpgrade_" + skillId.name();
    }

    public static String actionBarFieldName(int slot) {
        return "actionBar" + slot;
    }

    public String getLevel() {
        return level;
    }

    public String getWeaponDamage() {
        return weaponDamage;
    }

    public String getStrength() {
        return strength;
    }

    public String getIntelligence() {
        return intelligence;
    }

    public String getThorns() {
        return thorns;
    }

    public String getBlockChance() {
        return blockChance;
    }

    public String getRetributionChance() {
        return retributionChance;
    }

    public String getHorizonSeconds() {
        return horizonSeconds;
    }

    public SkillConfigFormData getSkillConfig(SkillId skillId) {
        return skillConfigs.get(skillId);
    }

    public List<String> getActionBarSlots() {
        return actionBarSlots;
    }

    public String getActionBarSlot(int slot) {
        return actionBarSlots.get(slot - 1);
    }

    public static final class SkillConfigFormData {
        private final String rank;
        private final boolean baseUpgrade;
        private final String choiceUpgrade;

        public SkillConfigFormData(String rank, boolean baseUpgrade, String choiceUpgrade) {
            this.rank = rank;
            this.baseUpgrade = baseUpgrade;
            this.choiceUpgrade = choiceUpgrade;
        }

        public String getRank() {
            return rank;
        }

        public boolean isBaseUpgrade() {
            return baseUpgrade;
        }

        public String getChoiceUpgrade() {
            return choiceUpgrade;
        }
    }
}

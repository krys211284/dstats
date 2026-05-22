package krys.web;

import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Surowe dane formularza GUI M8, zachowywane także przy błędach walidacji. */
public final class CurrentBuildFormData {
    public static final int ACTION_BAR_SLOT_COUNT = 6;
    public static final String DEFAULT_SIMULATION_STEP_COUNT = "10";

    private final String level;
    private final String questSkillPoints;
    private final String weaponDamage;
    private final String strength;
    private final String intelligence;
    private final String thorns;
    private final String blockChance;
    private final String retributionChance;
    private final String simulationStepCount;
    private final String initialPrimaryResource;
    private final String maxPrimaryResource;
    private final String primaryResourceRegenPerSecond;
    private final String selectedPaladinOathId;
    private final String initialAnimus;
    private final String maxAnimus;
    private final Map<SkillId, SkillConfigFormData> skillConfigs;
    private final List<String> actionBarSlots;

    public CurrentBuildFormData(String level,
                                String questSkillPoints,
                                String weaponDamage,
                                String strength,
                                String intelligence,
                                String thorns,
                                String blockChance,
                                String retributionChance,
                                String horizonSeconds,
                                Map<SkillId, SkillConfigFormData> skillConfigs,
                                List<String> actionBarSlots) {
        this(level, questSkillPoints, weaponDamage, strength, intelligence, thorns, blockChance,
                retributionChance, horizonSeconds, "100", "100", "1.50", "NONE", "8", "8", skillConfigs, actionBarSlots);
    }

    public CurrentBuildFormData(String level,
                                String questSkillPoints,
                                String weaponDamage,
                                String strength,
                                String intelligence,
                                String thorns,
                                String blockChance,
                                String retributionChance,
                                String horizonSeconds,
                                String initialPrimaryResource,
                                String maxPrimaryResource,
                                String primaryResourceRegenPerSecond,
                                String selectedPaladinOathId,
                                String initialAnimus,
                                String maxAnimus,
                                Map<SkillId, SkillConfigFormData> skillConfigs,
                                List<String> actionBarSlots) {
        this.level = level;
        this.questSkillPoints = questSkillPoints;
        this.weaponDamage = weaponDamage;
        this.strength = strength;
        this.intelligence = intelligence;
        this.thorns = thorns;
        this.blockChance = blockChance;
        this.retributionChance = retributionChance;
        this.simulationStepCount = horizonSeconds;
        this.initialPrimaryResource = initialPrimaryResource;
        this.maxPrimaryResource = maxPrimaryResource;
        this.primaryResourceRegenPerSecond = primaryResourceRegenPerSecond;
        this.selectedPaladinOathId = selectedPaladinOathId == null || selectedPaladinOathId.isBlank() ? "NONE" : selectedPaladinOathId;
        this.initialAnimus = initialAnimus == null || initialAnimus.isBlank() ? "8" : initialAnimus;
        this.maxAnimus = maxAnimus == null || maxAnimus.isBlank() ? "8" : maxAnimus;
        this.skillConfigs = Collections.unmodifiableMap(new EnumMap<>(skillConfigs));
        this.actionBarSlots = Collections.unmodifiableList(normalizeActionBarSlots(actionBarSlots));
    }

    public static CurrentBuildFormData defaultValues() {
        Map<SkillId, SkillConfigFormData> skillConfigs = createEmptySkillConfigs();
        skillConfigs.put(SkillId.ADVANCE, new SkillConfigFormData("5", true, SkillUpgradeChoice.RIGHT.name()));
        return new CurrentBuildFormData("13", "0", "8", "18", "0", "50", "50", "50", DEFAULT_SIMULATION_STEP_COUNT, "100", "100", "1.50", "NONE", "8", "8",
                skillConfigs,
                List.of(SkillId.ADVANCE.name(), "NONE", "NONE", "NONE", "NONE", "NONE"));
    }

    public static CurrentBuildFormData fromFormFields(Map<String, String> fields) {
        return fromFormFields(fields, defaultValues());
    }

    public static CurrentBuildFormData fromFormFields(Map<String, String> fields, CurrentBuildFormData defaults) {
        Map<SkillId, SkillConfigFormData> skillConfigs = createEmptySkillConfigs();
        for (SkillId skillId : SkillId.values()) {
            SkillConfigFormData defaultSkillConfig = defaults.getSkillConfig(skillId);
            String rawChoiceUpgrade = fields.getOrDefault(choiceFieldName(skillId), defaultSkillConfig.getChoiceUpgrade());
            String choiceGroup1 = fields.getOrDefault(choiceGroupFieldName(skillId, 1), defaultSkillConfig.getChoiceGroup1());
            String choiceGroup2 = fields.getOrDefault(choiceGroupFieldName(skillId, 2), defaultSkillConfig.getChoiceGroup2());
            String choiceGroup3 = fields.getOrDefault(choiceGroupFieldName(skillId, 3), defaultSkillConfig.getChoiceGroup3());
            String runtimeModifierChoice = fields.getOrDefault(runtimeModifierFieldName(skillId), defaultSkillConfig.getRuntimeModifierChoice());
            if (skillId == SkillId.CLASH) {
                if (isNoTreeChoice(choiceGroup1) && SkillRuntimeModifierChoice.ANIMUS.name().equalsIgnoreCase(runtimeModifierChoice)) {
                    choiceGroup1 = SkillState.CLASH_ANIMUS_CHOICE;
                }
                if (isNoTreeChoice(choiceGroup3) && SkillUpgradeChoice.LEFT.name().equalsIgnoreCase(rawChoiceUpgrade)
                        && !fields.containsKey(choiceGroupFieldName(skillId, 3))) {
                    choiceGroup3 = SkillState.CLASH_PUNISHMENT_CHOICE;
                }
                runtimeModifierChoice = SkillState.CLASH_ANIMUS_CHOICE.equals(choiceGroup1)
                        ? SkillRuntimeModifierChoice.ANIMUS.name()
                        : SkillRuntimeModifierChoice.NONE.name();
                if (fields.containsKey(choiceGroupFieldName(skillId, 3))) {
                    rawChoiceUpgrade = SkillState.CLASH_PUNISHMENT_CHOICE.equals(choiceGroup3)
                            ? SkillUpgradeChoice.LEFT.name()
                            : SkillUpgradeChoice.NONE.name();
                }
            }
            skillConfigs.put(skillId, new SkillConfigFormData(
                    fields.getOrDefault(rankFieldName(skillId), defaultSkillConfig.getRank()),
                    fields.containsKey(baseUpgradeFieldName(skillId)),
                    rawChoiceUpgrade,
                    runtimeModifierChoice,
                    choiceGroup1,
                    choiceGroup2,
                    choiceGroup3
            ));
        }

        List<String> actionBarSlots = new ArrayList<>();
        for (int slot = 1; slot <= ACTION_BAR_SLOT_COUNT; slot++) {
            actionBarSlots.add(fields.getOrDefault(actionBarFieldName(slot), defaults.getActionBarSlot(slot)));
        }

        return new CurrentBuildFormData(
                fields.getOrDefault("level", defaults.getLevel()),
                fields.getOrDefault("questSkillPoints", defaults.getQuestSkillPoints()),
                fields.getOrDefault("weaponDamage", defaults.getWeaponDamage()),
                fields.getOrDefault("strength", defaults.getStrength()),
                fields.getOrDefault("intelligence", defaults.getIntelligence()),
                fields.getOrDefault("thorns", defaults.getThorns()),
                fields.getOrDefault("blockChance", defaults.getBlockChance()),
                fields.getOrDefault("retributionChance", defaults.getRetributionChance()),
                simulationStepCountFromFields(fields, defaults),
                fields.getOrDefault("initialPrimaryResource", defaults.getInitialPrimaryResource()),
                fields.getOrDefault("maxPrimaryResource", defaults.getMaxPrimaryResource()),
                fields.getOrDefault("primaryResourceRegenPerSecond", defaults.getPrimaryResourceRegenPerSecond()),
                fields.getOrDefault("selectedPaladinOathId", defaults.getSelectedPaladinOathId()),
                fields.getOrDefault("initialAnimus", defaults.getInitialAnimus()),
                fields.getOrDefault("maxAnimus", defaults.getMaxAnimus()),
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

    public static String runtimeModifierFieldName(SkillId skillId) {
        return "choiceModifier_" + skillId.name();
    }

    public static String choiceGroupFieldName(SkillId skillId, int groupIndex) {
        return "choiceGroup" + groupIndex + "_" + skillId.name();
    }

    public static String actionBarFieldName(int slot) {
        return "actionBar" + slot;
    }

    public String getLevel() {
        return level;
    }

    public String getQuestSkillPoints() {
        return questSkillPoints;
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
        return simulationStepCount;
    }

    public String getSimulationStepCount() {
        return simulationStepCount;
    }

    public String getInitialPrimaryResource() {
        return initialPrimaryResource;
    }

    public String getMaxPrimaryResource() {
        return maxPrimaryResource;
    }

    public String getPrimaryResourceRegenPerSecond() {
        return primaryResourceRegenPerSecond;
    }

    public String getSelectedPaladinOathId() {
        return selectedPaladinOathId;
    }

    public String getInitialAnimus() {
        return initialAnimus;
    }

    public String getMaxAnimus() {
        return maxAnimus;
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

    private static List<String> normalizeActionBarSlots(List<String> rawSlots) {
        List<String> normalized = new ArrayList<>();
        if (rawSlots != null) {
            for (String rawSlot : rawSlots) {
                if (normalized.size() == ACTION_BAR_SLOT_COUNT) {
                    break;
                }
                normalized.add(rawSlot == null || rawSlot.isBlank() ? "NONE" : rawSlot);
            }
        }
        while (normalized.size() < ACTION_BAR_SLOT_COUNT) {
            normalized.add("NONE");
        }
        return normalized;
    }

    private static String simulationStepCountFromFields(Map<String, String> fields, CurrentBuildFormData defaults) {
        if (fields.containsKey("simulationStepCount")) {
            return fields.get("simulationStepCount");
        }
        if (fields.containsKey("horizonSeconds")) {
            return fields.get("horizonSeconds");
        }
        return defaults.getSimulationStepCount();
    }

    public static final class SkillConfigFormData {
        private final String rank;
        private final boolean baseUpgrade;
        private final String choiceUpgrade;
        private final String runtimeModifierChoice;
        private final String choiceGroup1;
        private final String choiceGroup2;
        private final String choiceGroup3;

        public SkillConfigFormData(String rank, boolean baseUpgrade, String choiceUpgrade) {
            this(rank, baseUpgrade, choiceUpgrade, SkillRuntimeModifierChoice.NONE.name());
        }

        public SkillConfigFormData(String rank,
                                   boolean baseUpgrade,
                                   String choiceUpgrade,
                                   String runtimeModifierChoice) {
            this(rank, baseUpgrade, choiceUpgrade, runtimeModifierChoice,
                    SkillState.NO_TREE_CHOICE,
                    SkillState.NO_TREE_CHOICE,
                    SkillState.NO_TREE_CHOICE);
        }

        public SkillConfigFormData(String rank,
                                   boolean baseUpgrade,
                                   String choiceUpgrade,
                                   String runtimeModifierChoice,
                                   String choiceGroup1,
                                   String choiceGroup2,
                                   String choiceGroup3) {
            this.rank = rank;
            this.baseUpgrade = baseUpgrade;
            this.choiceUpgrade = choiceUpgrade;
            this.runtimeModifierChoice = runtimeModifierChoice == null || runtimeModifierChoice.isBlank()
                    ? SkillRuntimeModifierChoice.NONE.name()
                    : runtimeModifierChoice;
            this.choiceGroup1 = normalizeTreeChoice(choiceGroup1);
            this.choiceGroup2 = normalizeTreeChoice(choiceGroup2);
            this.choiceGroup3 = normalizeTreeChoice(choiceGroup3);
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

        public String getRuntimeModifierChoice() {
            return runtimeModifierChoice;
        }

        public String getChoiceGroup1() {
            return choiceGroup1;
        }

        public String getChoiceGroup2() {
            return choiceGroup2;
        }

        public String getChoiceGroup3() {
            return choiceGroup3;
        }

        public String getChoiceGroup(int groupIndex) {
            return switch (groupIndex) {
                case 1 -> choiceGroup1;
                case 2 -> choiceGroup2;
                case 3 -> choiceGroup3;
                default -> SkillState.NO_TREE_CHOICE;
            };
        }
    }

    private static boolean isNoTreeChoice(String value) {
        return value == null || value.isBlank() || SkillState.NO_TREE_CHOICE.equalsIgnoreCase(value);
    }

    private static String normalizeTreeChoice(String value) {
        return isNoTreeChoice(value) ? SkillState.NO_TREE_CHOICE : value;
    }
}

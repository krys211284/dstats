package krys.web;

import krys.search.BuildSearchReferenceRequests;
import krys.search.BuildSearchRequest;
import krys.search.BuildSearchSkillSpace;
import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;
import krys.skill.SkillState;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Surowe dane formularza GUI searcha M12, zachowywane także przy błędach walidacji. */
public final class SearchBuildFormData {
    private final boolean useItemLibrary;
    private final String levelValues;
    private final String weaponDamageValues;
    private final String strengthValues;
    private final String intelligenceValues;
    private final String thornsValues;
    private final String blockChanceValues;
    private final String retributionChanceValues;
    private final String actionBarSizes;
    private final String horizonSeconds;
    private final String topResultsLimit;
    private final Map<SkillId, SkillSearchFormData> skillConfigs;

    public SearchBuildFormData(boolean useItemLibrary,
                               String levelValues,
                               String weaponDamageValues,
                               String strengthValues,
                               String intelligenceValues,
                               String thornsValues,
                               String blockChanceValues,
                               String retributionChanceValues,
                               String actionBarSizes,
                               String horizonSeconds,
                               String topResultsLimit,
                               Map<SkillId, SkillSearchFormData> skillConfigs) {
        this.useItemLibrary = useItemLibrary;
        this.levelValues = levelValues;
        this.weaponDamageValues = weaponDamageValues;
        this.strengthValues = strengthValues;
        this.intelligenceValues = intelligenceValues;
        this.thornsValues = thornsValues;
        this.blockChanceValues = blockChanceValues;
        this.retributionChanceValues = retributionChanceValues;
        this.actionBarSizes = actionBarSizes;
        this.horizonSeconds = horizonSeconds;
        this.topResultsLimit = topResultsLimit;
        this.skillConfigs = Collections.unmodifiableMap(new EnumMap<>(skillConfigs));
    }

    public static SearchBuildFormData defaultValues() {
        BuildSearchRequest referenceRequest = BuildSearchReferenceRequests.createFoundationM9();
        Map<SkillId, SkillSearchFormData> skillConfigs = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            BuildSearchSkillSpace skillSpace = referenceRequest.getSkillSpace(skillId);
            skillConfigs.put(skillId, new SkillSearchFormData(
                    joinIntegers(skillSpace.getRankValues()),
                    joinBooleans(skillSpace.getBaseUpgradeValues()),
                    joinChoices(skillSpace.getChoiceUpgradeValues())
            ));
        }

        return new SearchBuildFormData(
                referenceRequest.isUseItemLibrary(),
                joinIntegers(referenceRequest.getLevelValues()),
                joinLongs(referenceRequest.getWeaponDamageValues()),
                joinWholeDoubles(referenceRequest.getStrengthValues()),
                joinWholeDoubles(referenceRequest.getIntelligenceValues()),
                joinWholeDoubles(referenceRequest.getThornsValues()),
                joinWholeDoubles(referenceRequest.getBlockChanceValues()),
                joinWholeDoubles(referenceRequest.getRetributionChanceValues()),
                joinIntegers(referenceRequest.getActionBarSizes()),
                Integer.toString(referenceRequest.getHorizonSeconds()),
                Integer.toString(referenceRequest.getTopResultsLimit()),
                skillConfigs
        );
    }

    public static SearchBuildFormData fromFormFields(Map<String, String> fields) {
        return fromFormFields(fields, defaultValues());
    }

    public static SearchBuildFormData fromFormFields(Map<String, String> fields, SearchBuildFormData defaults) {
        Map<SkillId, SkillSearchFormData> skillConfigs = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            SkillSearchFormData defaultSkillConfig = defaults.getSkillConfig(skillId);
            skillConfigs.put(skillId, new SkillSearchFormData(
                    fields.getOrDefault(rankValuesFieldName(skillId), defaultSkillConfig.getRankValues()),
                    fields.getOrDefault(baseUpgradeValuesFieldName(skillId), defaultSkillConfig.getBaseUpgradeValues()),
                    fields.getOrDefault(choiceValuesFieldName(skillId), defaultSkillConfig.getChoiceValues())
            ));
        }

        return new SearchBuildFormData(
                fields.containsKey("useItemLibrary"),
                fields.getOrDefault("levelValues", defaults.getLevelValues()),
                fields.getOrDefault("weaponDamageValues", defaults.getWeaponDamageValues()),
                fields.getOrDefault("strengthValues", defaults.getStrengthValues()),
                fields.getOrDefault("intelligenceValues", defaults.getIntelligenceValues()),
                fields.getOrDefault("thornsValues", defaults.getThornsValues()),
                fields.getOrDefault("blockChanceValues", defaults.getBlockChanceValues()),
                fields.getOrDefault("retributionChanceValues", defaults.getRetributionChanceValues()),
                fields.getOrDefault("actionBarSizes", defaults.getActionBarSizes()),
                fields.getOrDefault("horizonSeconds", defaults.getHorizonSeconds()),
                fields.getOrDefault("topResultsLimit", defaults.getTopResultsLimit()),
                skillConfigs
        );
    }

    public static SearchBuildFormData fromHeroProfile(HeroProfile heroProfile) {
        CurrentBuildFormData formData = heroProfile.getCurrentBuildFormData();
        Map<SkillId, SkillSearchFormData> skillConfigs = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            CurrentBuildFormData.SkillConfigFormData skillConfig = formData.getSkillConfig(skillId);
            String rankValue = skillConfig.getRank();
            String baseUpgradeValue = skillConfig.isBaseUpgrade() ? "true" : "false";
            String choiceValue = skillConfig.getChoiceUpgrade();
            skillConfigs.put(skillId, new SkillSearchFormData(rankValue, baseUpgradeValue, choiceValue));
        }

        return new SearchBuildFormData(
                false,
                formData.getLevel(),
                formData.getWeaponDamage(),
                formData.getStrength(),
                formData.getIntelligence(),
                formData.getThorns(),
                formData.getBlockChance(),
                formData.getRetributionChance(),
                Integer.toString(countActiveActionBarSlots(formData)),
                formData.getHorizonSeconds(),
                "5",
                skillConfigs
        );
    }

    public static String rankValuesFieldName(SkillId skillId) {
        return "rankValues_" + skillId.name();
    }

    public static String baseUpgradeValuesFieldName(SkillId skillId) {
        return "baseUpgradeValues_" + skillId.name();
    }

    public static String choiceValuesFieldName(SkillId skillId) {
        return "choiceValues_" + skillId.name();
    }

    public boolean isUseItemLibrary() {
        return useItemLibrary;
    }

    public String getLevelValues() {
        return levelValues;
    }

    public String getWeaponDamageValues() {
        return weaponDamageValues;
    }

    public String getStrengthValues() {
        return strengthValues;
    }

    public String getIntelligenceValues() {
        return intelligenceValues;
    }

    public String getThornsValues() {
        return thornsValues;
    }

    public String getBlockChanceValues() {
        return blockChanceValues;
    }

    public String getRetributionChanceValues() {
        return retributionChanceValues;
    }

    public String getActionBarSizes() {
        return actionBarSizes;
    }

    public String getHorizonSeconds() {
        return horizonSeconds;
    }

    public String getTopResultsLimit() {
        return topResultsLimit;
    }

    public SkillSearchFormData getSkillConfig(SkillId skillId) {
        return skillConfigs.get(skillId);
    }

    public SearchBuildFormData restrictedToAssignedSkills(List<SkillId> assignedSkillIds) {
        java.util.LinkedHashSet<SkillId> allowedSkills = new java.util.LinkedHashSet<>(assignedSkillIds);
        Map<SkillId, SkillSearchFormData> restrictedSkillConfigs = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            if (allowedSkills.contains(skillId)) {
                restrictedSkillConfigs.put(skillId, getSkillConfig(skillId));
                continue;
            }
            restrictedSkillConfigs.put(skillId, new SkillSearchFormData("0", "false", SkillUpgradeChoice.NONE.name()));
        }
        return new SearchBuildFormData(
                useItemLibrary,
                levelValues,
                weaponDamageValues,
                strengthValues,
                intelligenceValues,
                thornsValues,
                blockChanceValues,
                retributionChanceValues,
                actionBarSizes,
                horizonSeconds,
                topResultsLimit,
                restrictedSkillConfigs
        );
    }

    public static final class SkillSearchFormData {
        private final String rankValues;
        private final String baseUpgradeValues;
        private final String choiceValues;

        public SkillSearchFormData(String rankValues, String baseUpgradeValues, String choiceValues) {
            this.rankValues = rankValues;
            this.baseUpgradeValues = baseUpgradeValues;
            this.choiceValues = choiceValues;
        }

        public String getRankValues() {
            return rankValues;
        }

        public String getBaseUpgradeValues() {
            return baseUpgradeValues;
        }

        public String getChoiceValues() {
            return choiceValues;
        }
    }

    private static String joinIntegers(java.util.List<Integer> values) {
        return values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    private static String joinLongs(java.util.List<Long> values) {
        return values.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    private static String joinWholeDoubles(java.util.List<Double> values) {
        return values.stream()
                .map(value -> String.format(java.util.Locale.US, "%.0f", value))
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static String joinBooleans(java.util.List<Boolean> values) {
        return values.stream()
                .map(value -> Boolean.TRUE.equals(value) ? "true" : "false")
                .collect(java.util.stream.Collectors.joining(","));
    }

    private static String joinChoices(java.util.List<SkillUpgradeChoice> values) {
        return values.stream().map(Enum::name).collect(java.util.stream.Collectors.joining(","));
    }

    private static int countActiveActionBarSlots(CurrentBuildFormData formData) {
        int count = 0;
        for (String actionBarSlot : formData.getActionBarSlots()) {
            if (!"NONE".equals(actionBarSlot)) {
                count++;
            }
        }
        return Math.max(count, 1);
    }
}

package krys.web;

import krys.itemimport.CurrentBuildImportableStats;
import krys.skill.SkillId;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/** Wspólna serializacja i odtwarzanie pełnego kontekstu current build przez query string. */
final class CurrentBuildFormQuerySupport {
    private CurrentBuildFormQuerySupport() {
    }

    static CurrentBuildFormData resolveImportContext(Map<String, String> rawFields) {
        if (rawFields == null || rawFields.isEmpty()) {
            return defaultImportContext();
        }
        return CurrentBuildFormData.fromFormFields(rawFields);
    }

    static CurrentBuildFormData fromSerializedQuery(String query) {
        return resolveImportContext(UrlEncodedFormSupport.parseQuery(query));
    }

    static String toQuery(CurrentBuildFormData formData) {
        StringJoiner query = new StringJoiner("&");
        append(query, "level", formData.getLevel());
        append(query, "questSkillPoints", formData.getQuestSkillPoints());
        append(query, "weaponDamage", formData.getWeaponDamage());
        append(query, "strength", formData.getStrength());
        append(query, "intelligence", formData.getIntelligence());
        append(query, "thorns", formData.getThorns());
        append(query, "blockChance", formData.getBlockChance());
        append(query, "retributionChance", formData.getRetributionChance());
        append(query, "horizonSeconds", formData.getHorizonSeconds());
        append(query, "initialPrimaryResource", formData.getInitialPrimaryResource());
        append(query, "maxPrimaryResource", formData.getMaxPrimaryResource());
        append(query, "primaryResourceRegenPerSecond", formData.getPrimaryResourceRegenPerSecond());
        append(query, "selectedPaladinOathId", formData.getSelectedPaladinOathId());
        append(query, "initialAnimus", formData.getInitialAnimus());
        append(query, "maxAnimus", formData.getMaxAnimus());

        for (SkillId skillId : SkillId.values()) {
            CurrentBuildFormData.SkillConfigFormData skillConfig = formData.getSkillConfig(skillId);
            append(query, CurrentBuildFormData.rankFieldName(skillId), skillConfig.getRank());
            if (skillConfig.isBaseUpgrade()) {
                append(query, CurrentBuildFormData.baseUpgradeFieldName(skillId), "true");
            }
            append(query, CurrentBuildFormData.choiceFieldName(skillId), skillConfig.getChoiceUpgrade());
            append(query, CurrentBuildFormData.runtimeModifierFieldName(skillId), skillConfig.getRuntimeModifierChoice());
            append(query, CurrentBuildFormData.choiceGroupFieldName(skillId, 1), skillConfig.getChoiceGroup1());
            append(query, CurrentBuildFormData.choiceGroupFieldName(skillId, 2), skillConfig.getChoiceGroup2());
            append(query, CurrentBuildFormData.choiceGroupFieldName(skillId, 3), skillConfig.getChoiceGroup3());
        }
        for (int slot = 1; slot <= CurrentBuildFormData.ACTION_BAR_SLOT_COUNT; slot++) {
            append(query, CurrentBuildFormData.actionBarFieldName(slot), formData.getActionBarSlot(slot));
        }
        return query.toString();
    }

    static CurrentBuildImportableStats importableStats(CurrentBuildFormData formData) {
        return new CurrentBuildImportableStats(
                Long.parseLong(formData.getWeaponDamage()),
                Double.parseDouble(formData.getStrength()),
                Double.parseDouble(formData.getIntelligence()),
                Double.parseDouble(formData.getThorns()),
                Double.parseDouble(formData.getBlockChance()),
                Double.parseDouble(formData.getRetributionChance())
        );
    }

    static CurrentBuildFormData withAppliedStats(CurrentBuildFormData baseFormData,
                                                 CurrentBuildImportableStats appliedStats) {
        return withAppliedStatsAndMaxAnimus(baseFormData, appliedStats, baseFormData.getMaxAnimus());
    }

    static CurrentBuildFormData withAppliedStatsAndMaxAnimus(CurrentBuildFormData baseFormData,
                                                             CurrentBuildImportableStats appliedStats,
                                                             String maxAnimus) {
        Map<SkillId, CurrentBuildFormData.SkillConfigFormData> copiedSkillConfigs = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            CurrentBuildFormData.SkillConfigFormData skillConfig = baseFormData.getSkillConfig(skillId);
            copiedSkillConfigs.put(skillId, new CurrentBuildFormData.SkillConfigFormData(
                    skillConfig.getRank(),
                    skillConfig.isBaseUpgrade(),
                    skillConfig.getChoiceUpgrade(),
                    skillConfig.getRuntimeModifierChoice(),
                    skillConfig.getChoiceGroup1(),
                    skillConfig.getChoiceGroup2(),
                    skillConfig.getChoiceGroup3()
            ));
        }

        List<String> actionBarSlots = new ArrayList<>();
        for (int slot = 1; slot <= CurrentBuildFormData.ACTION_BAR_SLOT_COUNT; slot++) {
            actionBarSlots.add(baseFormData.getActionBarSlot(slot));
        }

        return new CurrentBuildFormData(
                baseFormData.getLevel(),
                baseFormData.getQuestSkillPoints(),
                Long.toString(appliedStats.getWeaponDamage()),
                formatWhole(appliedStats.getStrength()),
                formatWhole(appliedStats.getIntelligence()),
                formatWhole(appliedStats.getThorns()),
                formatWhole(appliedStats.getBlockChance()),
                formatWhole(appliedStats.getRetributionChance()),
                baseFormData.getHorizonSeconds(),
                baseFormData.getInitialPrimaryResource(),
                baseFormData.getMaxPrimaryResource(),
                baseFormData.getPrimaryResourceRegenPerSecond(),
                baseFormData.getSelectedPaladinOathId(),
                baseFormData.getInitialAnimus(),
                maxAnimus,
                copiedSkillConfigs,
                actionBarSlots
        );
    }

    static CurrentBuildFormData withMaxAnimus(CurrentBuildFormData baseFormData, String maxAnimus) {
        CurrentBuildImportableStats currentStats = new CurrentBuildImportableStats(
                Long.parseLong(baseFormData.getWeaponDamage()),
                Double.parseDouble(baseFormData.getStrength()),
                Double.parseDouble(baseFormData.getIntelligence()),
                Double.parseDouble(baseFormData.getThorns()),
                Double.parseDouble(baseFormData.getBlockChance()),
                Double.parseDouble(baseFormData.getRetributionChance())
        );
        return withAppliedStatsAndMaxAnimus(baseFormData, currentStats, maxAnimus);
    }

    static CurrentBuildFormData withHeroLevel(CurrentBuildFormData baseFormData, int heroLevel) {
        Map<SkillId, CurrentBuildFormData.SkillConfigFormData> copiedSkillConfigs = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            CurrentBuildFormData.SkillConfigFormData skillConfig = baseFormData.getSkillConfig(skillId);
            copiedSkillConfigs.put(skillId, new CurrentBuildFormData.SkillConfigFormData(
                    skillConfig.getRank(),
                    skillConfig.isBaseUpgrade(),
                    skillConfig.getChoiceUpgrade(),
                    skillConfig.getRuntimeModifierChoice(),
                    skillConfig.getChoiceGroup1(),
                    skillConfig.getChoiceGroup2(),
                    skillConfig.getChoiceGroup3()
            ));
        }
        return new CurrentBuildFormData(
                Integer.toString(heroLevel),
                baseFormData.getQuestSkillPoints(),
                baseFormData.getWeaponDamage(),
                baseFormData.getStrength(),
                baseFormData.getIntelligence(),
                baseFormData.getThorns(),
                baseFormData.getBlockChance(),
                baseFormData.getRetributionChance(),
                baseFormData.getHorizonSeconds(),
                baseFormData.getInitialPrimaryResource(),
                baseFormData.getMaxPrimaryResource(),
                baseFormData.getPrimaryResourceRegenPerSecond(),
                baseFormData.getSelectedPaladinOathId(),
                baseFormData.getInitialAnimus(),
                baseFormData.getMaxAnimus(),
                copiedSkillConfigs,
                baseFormData.getActionBarSlots()
        );
    }

    private static CurrentBuildFormData defaultImportContext() {
        CurrentBuildFormData defaults = CurrentBuildFormData.defaultValues();
        Map<SkillId, CurrentBuildFormData.SkillConfigFormData> copiedSkillConfigs = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            CurrentBuildFormData.SkillConfigFormData skillConfig = defaults.getSkillConfig(skillId);
            copiedSkillConfigs.put(skillId, new CurrentBuildFormData.SkillConfigFormData(
                    skillConfig.getRank(),
                    skillConfig.isBaseUpgrade(),
                    skillConfig.getChoiceUpgrade(),
                    skillConfig.getRuntimeModifierChoice(),
                    skillConfig.getChoiceGroup1(),
                    skillConfig.getChoiceGroup2(),
                    skillConfig.getChoiceGroup3()
            ));
        }
        return new CurrentBuildFormData(
                defaults.getLevel(),
                defaults.getQuestSkillPoints(),
                defaults.getWeaponDamage(),
                "0",
                "0",
                "0",
                "0",
                "0",
                defaults.getHorizonSeconds(),
                defaults.getInitialPrimaryResource(),
                defaults.getMaxPrimaryResource(),
                defaults.getPrimaryResourceRegenPerSecond(),
                defaults.getSelectedPaladinOathId(),
                defaults.getInitialAnimus(),
                defaults.getMaxAnimus(),
                copiedSkillConfigs,
                defaults.getActionBarSlots()
        );
    }

    private static void append(StringJoiner query, String key, String value) {
        query.add(encode(key) + "=" + encode(value));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String formatWhole(double value) {
        if (Math.rint(value) == value) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }
}

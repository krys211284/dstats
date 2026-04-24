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
        append(query, "weaponDamage", formData.getWeaponDamage());
        append(query, "strength", formData.getStrength());
        append(query, "intelligence", formData.getIntelligence());
        append(query, "thorns", formData.getThorns());
        append(query, "blockChance", formData.getBlockChance());
        append(query, "retributionChance", formData.getRetributionChance());
        append(query, "horizonSeconds", formData.getHorizonSeconds());

        for (SkillId skillId : SkillId.values()) {
            CurrentBuildFormData.SkillConfigFormData skillConfig = formData.getSkillConfig(skillId);
            append(query, CurrentBuildFormData.rankFieldName(skillId), skillConfig.getRank());
            if (skillConfig.isBaseUpgrade()) {
                append(query, CurrentBuildFormData.baseUpgradeFieldName(skillId), "true");
            }
            append(query, CurrentBuildFormData.choiceFieldName(skillId), skillConfig.getChoiceUpgrade());
        }
        for (int slot = 1; slot <= 4; slot++) {
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
        Map<SkillId, CurrentBuildFormData.SkillConfigFormData> copiedSkillConfigs = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            CurrentBuildFormData.SkillConfigFormData skillConfig = baseFormData.getSkillConfig(skillId);
            copiedSkillConfigs.put(skillId, new CurrentBuildFormData.SkillConfigFormData(
                    skillConfig.getRank(),
                    skillConfig.isBaseUpgrade(),
                    skillConfig.getChoiceUpgrade()
            ));
        }

        List<String> actionBarSlots = new ArrayList<>();
        for (int slot = 1; slot <= 4; slot++) {
            actionBarSlots.add(baseFormData.getActionBarSlot(slot));
        }

        return new CurrentBuildFormData(
                baseFormData.getLevel(),
                Long.toString(appliedStats.getWeaponDamage()),
                formatWhole(appliedStats.getStrength()),
                formatWhole(appliedStats.getIntelligence()),
                formatWhole(appliedStats.getThorns()),
                formatWhole(appliedStats.getBlockChance()),
                formatWhole(appliedStats.getRetributionChance()),
                baseFormData.getHorizonSeconds(),
                copiedSkillConfigs,
                actionBarSlots
        );
    }

    static CurrentBuildFormData withHeroLevel(CurrentBuildFormData baseFormData, int heroLevel) {
        Map<SkillId, CurrentBuildFormData.SkillConfigFormData> copiedSkillConfigs = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            CurrentBuildFormData.SkillConfigFormData skillConfig = baseFormData.getSkillConfig(skillId);
            copiedSkillConfigs.put(skillId, new CurrentBuildFormData.SkillConfigFormData(
                    skillConfig.getRank(),
                    skillConfig.isBaseUpgrade(),
                    skillConfig.getChoiceUpgrade()
            ));
        }
        return new CurrentBuildFormData(
                Integer.toString(heroLevel),
                baseFormData.getWeaponDamage(),
                baseFormData.getStrength(),
                baseFormData.getIntelligence(),
                baseFormData.getThorns(),
                baseFormData.getBlockChance(),
                baseFormData.getRetributionChance(),
                baseFormData.getHorizonSeconds(),
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
                    skillConfig.getChoiceUpgrade()
            ));
        }
        return new CurrentBuildFormData(
                defaults.getLevel(),
                defaults.getWeaponDamage(),
                "0",
                "0",
                "0",
                "0",
                "0",
                defaults.getHorizonSeconds(),
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

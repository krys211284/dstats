package krys.web;

import krys.skilltree.DamagePercentComponent;
import krys.skilltree.SourceSkillCategory;
import krys.skilltree.UpgradeDamageModifier;

import java.text.Normalizer;
import java.util.Locale;

/** Buduje tekst używany wyłącznie przez filtrowanie widoku rankingu obrażeń. */
public final class DamageRankingSearchText {
    private DamageRankingSearchText() {
    }

    public static String normalizedRowText(DamageRankingRow row) {
        return normalize(rowText(row));
    }

    public static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String polishLettersExpanded = value
                .replace('ł', 'l')
                .replace('Ł', 'L');
        String decomposed = Normalizer.normalize(polishLettersExpanded, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return decomposed.toLowerCase(Locale.ROOT).trim();
    }

    private static String rowText(DamageRankingRow row) {
        StringBuilder text = new StringBuilder();
        append(text, row.getEntry().getSkillName());
        append(text, row.getEntry().getSkillId());
        append(text, row.getEntry().getSkillGroup());
        append(text, row.getTreeGroupDisplayName());
        append(text, row.getEntry().getVerificationStatus().name());
        append(text, row.getType().name());
        append(text, row.getDamageProfile());
        append(text, row.getDamageComponentsDescription());
        append(text, row.getEntry().getNotes());
        append(text, row.getSourceCategoriesDisplay());
        for (SourceSkillCategory category : row.getSkillCategories()) {
            append(text, category.getId());
            append(text, category.getDisplayName());
        }
        for (String tag : row.getTags()) {
            append(text, tag);
        }
        append(text, row.getResourceCostSummary());
        append(text, row.getResourceGenerationSummary());
        append(text, row.getLuckyHitSummary());
        if (row.getEntry().getCooldownSeconds() != null || row.getChargeData() != null) {
            append(text, "Odnowienie " + row.getCooldownSummary());
        }
        append(text, row.getActivationRequirementsSummary());
        for (var requirement : row.getActivationRequirements()) {
            append(text, requirement.id());
            append(text, requirement.consumptionMode().name());
        }
        if (row.getLuckyHitPercent() != null) {
            append(text, "Lucky Hit");
        }
        if (row.hasResourceCost()) {
            append(text, row.getResourceType().costLabel());
        }
        if (row.hasResourceGeneration()) {
            append(text, row.getResourceType().generationLabel());
        }
        appendPercent(text, row.getBaseDamagePercentAtRank1());
        appendPercent(text, row.getBaseDamagePercentAtTreeMaxRank());
        for (DamagePercentComponent component : DamagePercentComponent.values()) {
            Integer rankOne = row.getComponentDamagePercentRanks().damagePercentAt(component, 1);
            Integer treeMax = row.getComponentDamagePercentRanks().damagePercentAt(component, 15);
            if (rankOne != null || treeMax != null) {
                append(text, component.name());
                appendPercent(text, rankOne);
                appendPercent(text, treeMax);
                Integer durationSeconds = row.getComponentDamagePercentRanks().durationSecondsFor(component);
                if (durationSeconds != null) {
                    append(text, "w ciągu " + durationSeconds + " s");
                }
                appendComponentTiming(text, row.getComponentDamagePercentRanks(), component);
            }
        }
        appendModifiers(text, row.damageMultiplierModifiers());
        appendModifiers(text, row.damageBonusModifiers());
        appendModifiers(text, row.extraHitOrComponentModifiers());
        appendModifiers(text, row.damageOverTimeModifiers());
        appendModifiers(text, row.statusDamageModifiers());
        appendModifiers(text, row.resourceModifiers());
        appendModifiers(text, row.cooldownOrCastSpeedModifiers());
        appendModifiers(text, row.defenseOrUtilityModifiers());
        appendModifiers(text, row.manualReviewModifiers());
        return text.toString();
    }

    private static void appendModifiers(StringBuilder text, Iterable<UpgradeDamageModifier> modifiers) {
        for (UpgradeDamageModifier modifier : modifiers) {
            append(text, modifier.getUpgradeGroup());
            append(text, modifier.getUpgradeName());
            append(text, modifier.getType().name());
            append(text, modifier.getValue());
            append(text, modifier.getCondition());
            append(text, modifier.getAffectedComponent());
            append(text, modifier.getSafeForRankingDisplay().name());
            append(text, modifier.getNotes());
            var tables = modifier.getComponentDamagePercentRanks();
            for (var component : DamagePercentComponent.values()) {
                if (!tables.hasComponent(component)) continue;
                append(text, component.name());
                appendPercent(text, tables.damagePercentAt(component, 1));
                appendPercent(text, tables.damagePercentAt(component, 15));
                appendComponentTiming(text, tables, component);
            }
        }
    }

    private static void appendComponentTiming(StringBuilder text, krys.skilltree.DamagePercentComponentRankTable tables,
                                              DamagePercentComponent component) {
        Integer interval = tables.intervalSecondsFor(component);
        if (interval != null) append(text, "co " + interval + " s");
        if (component == DamagePercentComponent.EVENT_TRIGGERED_DAMAGE) append(text, "na zdarzenie");
    }

    private static void appendPercent(StringBuilder text, Integer value) {
        if (value != null) {
            append(text, value + "%");
        }
    }

    private static void append(StringBuilder text, String value) {
        if (value != null && !value.isBlank()) {
            text.append(' ').append(value);
        }
    }
}

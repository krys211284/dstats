package krys.web;

import krys.paladin.DamagePercentComponent;
import krys.paladin.SkillCategory;
import krys.paladin.SkillTag;
import krys.paladin.UpgradeDamageModifier;

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
        append(text, row.getSourceCategoriesDisplay());
        for (SkillCategory category : row.getSkillCategories()) {
            append(text, category.name());
            append(text, category.getDisplayName());
        }
        for (SkillTag tag : row.getTags()) {
            append(text, tag.name());
        }
        append(text, row.getFaithCostSummary());
        append(text, row.getFaithGenerationSummary());
        if (row.hasFaithCost()) {
            append(text, "Koszt Wiary");
        }
        if (row.hasResourceGeneration()) {
            append(text, "Generowanie Wiary");
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
            }
        }
        appendModifiers(text, row.damageMultiplierModifiers());
        appendModifiers(text, row.damageBonusModifiers());
        appendModifiers(text, row.extraHitOrComponentModifiers());
        appendModifiers(text, row.damageOverTimeModifiers());
        appendModifiers(text, row.statusDamageModifiers());
        appendModifiers(text, row.resourceModifiers());
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
        }
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

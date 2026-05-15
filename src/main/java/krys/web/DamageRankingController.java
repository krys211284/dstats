package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.paladin.PaladinSkillTreeType;
import krys.paladin.PaladinTreeSkill;
import krys.paladin.SkillCategory;
import krys.paladin.SkillTag;
import krys.ranking.CharacterSkillTreeRegistry;
import krys.ranking.PaladinDamageRankingMetric;
import krys.ranking.PaladinSkillDamageRankingEntry;
import krys.ranking.DamageRankingService;
import krys.ranking.PaladinSkillDamageVerificationStatus;
import krys.ranking.SkillTreeRegistryProvider;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Kontroler ogólnego widoku rankingu obrażeń wybierający rejestr po klasie postaci. */
public final class DamageRankingController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final DamageRankingService rankingService;
    private final DamageRankingPageRenderer renderer;
    private final SkillTreeRegistryProvider skillTreeRegistryProvider;

    public DamageRankingController(DamageRankingService rankingService,
                                   DamageRankingPageRenderer renderer,
                                   SkillTreeRegistryProvider skillTreeRegistryProvider) {
        this.rankingService = rankingService;
        this.renderer = renderer;
        this.skillTreeRegistryProvider = skillTreeRegistryProvider;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if (!"GET".equals(method)) {
                exchange.getResponseHeaders().set("Allow", "GET");
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            DamageRankingFilter filter = DamageRankingFilter.fromQuery(
                    UrlEncodedFormSupport.parseQuery(exchange.getRequestURI().getRawQuery())
            );
            renderPage(exchange, buildPageModel(filter));
        } finally {
            exchange.close();
        }
    }

    private DamageRankingPageModel buildPageModel(DamageRankingFilter filter) {
        CharacterSkillTreeRegistry registry = skillTreeRegistryProvider.registryFor(filter.getCharacter());
        Map<String, PaladinTreeSkill> skillsById = registry.allSkills().stream()
                .collect(Collectors.toMap(PaladinTreeSkill::getSkillId, Function.identity()));
        List<DamageRankingRow> rows = rankingService.describeTreeSkills(registry.getPlayableClass()).stream()
                .map(entry -> toRow(entry, registry, skillsById))
                .filter(row -> matchesFilter(row, filter))
                .sorted(sortComparator(filter))
                .toList();

        List<String> skillGroups = registry.allSkills().stream()
                .map(PaladinTreeSkill::getSkillGroup)
                .distinct()
                .sorted()
                .toList();
        List<SkillTag> tags = registry.allSkills().stream()
                .flatMap(skill -> skill.getTags().stream())
                .distinct()
                .sorted(Comparator.comparing(Enum::name))
                .toList();
        List<SkillCategory> sourceCategories = registry.allSkills().stream()
                .flatMap(skill -> skill.getSkillCategories().stream())
                .distinct()
                .sorted(Comparator.comparing(Enum::name))
                .toList();

        return new DamageRankingPageModel(
                filter,
                registry,
                skillTreeRegistryProvider.supportedClasses(),
                rows,
                registry.allSkills().size(),
                skillGroups,
                List.of(PaladinSkillDamageVerificationStatus.SUPPORTED,
                        PaladinSkillDamageVerificationStatus.NEEDS_VERIFICATION,
                        PaladinSkillDamageVerificationStatus.UNSUPPORTED,
                        PaladinSkillDamageVerificationStatus.NON_DAMAGE),
                List.of(PaladinSkillTreeType.DAMAGE,
                        PaladinSkillTreeType.NON_DAMAGE,
                        PaladinSkillTreeType.MOBILITY,
                        PaladinSkillTreeType.DEFENSIVE,
                        PaladinSkillTreeType.SUPPORT,
                        PaladinSkillTreeType.SPECIAL,
                        PaladinSkillTreeType.UNCLASSIFIED),
                sourceCategories,
                tags,
                List.of(PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_RANK_1,
                        PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_TREE_MAX)
        );
    }

    private static DamageRankingRow toRow(PaladinSkillDamageRankingEntry entry,
                                          CharacterSkillTreeRegistry registry,
                                          Map<String, PaladinTreeSkill> skillsById) {
        PaladinTreeSkill treeSkill = skillsById.get(entry.getSkillId());
        if (treeSkill == null) {
            throw new IllegalStateException("Ranking zwrócił skill spoza " + registry.getRegistryName() + ": " + entry.getSkillId());
        }
        return new DamageRankingRow(entry, treeSkill);
    }

    private static boolean matchesFilter(DamageRankingRow row,
                                         DamageRankingFilter filter) {
        if (filter.hasSkillGroup() && !row.getEntry().getSkillGroup().equals(filter.getSkillGroup())) {
            return false;
        }
        if (filter.hasVerificationStatus()
                && row.getEntry().getVerificationStatus() != filter.getVerificationStatus()) {
            return false;
        }
        if (filter.hasType() && row.getType() != filter.getType()) {
            return false;
        }
        if (filter.hasTag() && !row.hasTag(filter.getTag())) {
            return false;
        }
        if (filter.hasSourceCategory() && !row.hasSkillCategory(filter.getSourceCategory())) {
            return false;
        }
        if (filter.hasSearchQuery()
                && !DamageRankingSearchText.normalizedRowText(row)
                .contains(DamageRankingSearchText.normalize(filter.getQ()))) {
            return false;
        }
        return matchesFacet(filter.getHasDirectUpgradeDamage(), row.hasDirectUpgradeDamage())
                && matchesFacet(filter.getHasNewDamageComponent(), row.hasNewDamageComponent())
                && matchesFacet(filter.getHasStatusDamageEnabler(), row.hasStatusDamageEnabler())
                && matchesFacet(filter.getHasFaithCost(), row.hasFaithCost())
                && matchesFacet(filter.getHasResourceGeneration(), row.hasResourceGeneration())
                && matchesFacet(filter.getHasCooldownOrCastSpeed(), row.hasCooldownOrCastSpeed())
                && matchesFacet(filter.getHasDefenseOrUtility(), row.hasDefenseOrUtility())
                && matchesFacet(filter.getHasManualReviewUpgrade(), row.hasManualReviewUpgrade());
    }

    private static boolean matchesFacet(DamageRankingFilter.FacetFilter filter, boolean value) {
        return switch (filter) {
            case ALL -> true;
            case YES -> value;
            case NO -> !value;
        };
    }

    private static Comparator<DamageRankingRow> sortComparator(DamageRankingFilter filter) {
        if (filter.getSort().equals("baseDamageRank1")) {
            return numericComparator(DamageRankingRow::getBaseDamagePercentAtRank1, filter.getDirection())
                    .thenComparing(row -> row.getEntry().getSkillName(), String.CASE_INSENSITIVE_ORDER);
        }
        if (filter.getSort().equals("baseDamageTreeMax")) {
            return numericComparator(DamageRankingRow::getBaseDamagePercentAtTreeMaxRank, filter.getDirection())
                    .thenComparing(row -> row.getEntry().getSkillName(), String.CASE_INSENSITIVE_ORDER);
        }
        if (filter.getSort().equals("faithCost")) {
            return numericComparator(DamageRankingRow::getFaithCostSortValue, filter.getDirection())
                    .thenComparing(row -> row.getEntry().getSkillName(), String.CASE_INSENSITIVE_ORDER);
        }
        if (filter.getSort().equals("faithGeneratedBase")) {
            return numericComparator(DamageRankingRow::getFaithGenerationBaseSortValue, filter.getDirection())
                    .thenComparing(row -> row.getEntry().getSkillName(), String.CASE_INSENSITIVE_ORDER);
        }
        if (filter.getSort().equals("faithGeneratedMaxKnown")) {
            return numericComparator(DamageRankingRow::getFaithGenerationMaxKnownSortValue, filter.getDirection())
                    .thenComparing(row -> row.getEntry().getSkillName(), String.CASE_INSENSITIVE_ORDER);
        }
        if (filter.getSort().equals("maxDamageMultiplierPercent")) {
            return numericComparator(DamageRankingRow::maxDamageMultiplierPercent, filter.getDirection())
                    .thenComparing(row -> row.getEntry().getSkillName(), String.CASE_INSENSITIVE_ORDER);
        }
        if (filter.getSort().equals("maxDamageBonusPercent")) {
            return numericComparator(DamageRankingRow::maxDamageBonusPercent, filter.getDirection())
                    .thenComparing(row -> row.getEntry().getSkillName(), String.CASE_INSENSITIVE_ORDER);
        }
        if (filter.getSort().equals("maxExtraHitOrComponentPercent")) {
            return numericComparator(DamageRankingRow::maxExtraHitOrComponentPercent, filter.getDirection())
                    .thenComparing(row -> row.getEntry().getSkillName(), String.CASE_INSENSITIVE_ORDER);
        }
        if (filter.getSort().equals("maxDamageOverTimePercent")) {
            return numericComparator(DamageRankingRow::maxDamageOverTimePercent, filter.getDirection())
                    .thenComparing(row -> row.getEntry().getSkillName(), String.CASE_INSENSITIVE_ORDER);
        }
        Comparator<DamageRankingRow> primary = switch (filter.getSort()) {
            case "skillName" -> Comparator.comparing(row -> row.getEntry().getSkillName(), String.CASE_INSENSITIVE_ORDER);
            case "sourceCategories" -> Comparator.comparing(DamageRankingRow::getSkillCategoriesDisplay, String.CASE_INSENSITIVE_ORDER);
            case "skillGroup" -> Comparator.comparing(row -> row.getEntry().getSkillGroup(), String.CASE_INSENSITIVE_ORDER);
            case "type" -> Comparator.comparing(row -> row.getType().name());
            case "damageProfile" -> Comparator.comparing(DamageRankingRow::getDamageProfile);
            case "hasDirectUpgradeDamage" -> booleanComparator(DamageRankingRow::hasDirectUpgradeDamage);
            case "hasNewDamageComponent" -> booleanComparator(DamageRankingRow::hasNewDamageComponent);
            case "hasStatusDamageEnabler" -> booleanComparator(DamageRankingRow::hasStatusDamageEnabler);
            case "hasResourceGeneration" -> booleanComparator(DamageRankingRow::hasResourceGeneration);
            case "hasCooldownOrCastSpeed" -> booleanComparator(DamageRankingRow::hasCooldownOrCastSpeed);
            case "hasDefenseOrUtility" -> booleanComparator(DamageRankingRow::hasDefenseOrUtility);
            case "hasManualReviewUpgrade" -> booleanComparator(DamageRankingRow::hasManualReviewUpgrade);
            case "tags" -> Comparator.comparing(DamageRankingController::tagSortValue);
            default -> Comparator.comparing(row -> row.getEntry().getSkillName(), String.CASE_INSENSITIVE_ORDER);
        };
        if (filter.getDirection() == DamageRankingFilter.SortDirection.DESC) {
            primary = primary.reversed();
        }
        return primary.thenComparing(row -> row.getEntry().getSkillName(), String.CASE_INSENSITIVE_ORDER);
    }

    private static Comparator<DamageRankingRow> numericComparator(Function<DamageRankingRow, Integer> valueExtractor,
                                                                  DamageRankingFilter.SortDirection direction) {
        return (left, right) -> {
            Integer leftValue = valueExtractor.apply(left);
            Integer rightValue = valueExtractor.apply(right);
            if (leftValue == null && rightValue == null) {
                return 0;
            }
            if (leftValue == null) {
                return 1;
            }
            if (rightValue == null) {
                return -1;
            }
            int result = Integer.compare(leftValue, rightValue);
            return direction == DamageRankingFilter.SortDirection.ASC ? result : -result;
        };
    }

    private static Comparator<DamageRankingRow> booleanComparator(Function<DamageRankingRow, Boolean> valueExtractor) {
        return Comparator.comparing(row -> valueExtractor.apply(row) ? 1 : 0);
    }

    private static String tagSortValue(DamageRankingRow row) {
        Set<SkillTag> tags = row.getTags();
        return tags.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
    }

    private void renderPage(HttpExchange exchange, DamageRankingPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

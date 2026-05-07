package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.paladin.PaladinSkillTreeType;
import krys.paladin.PaladinTreeSkill;
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
                .sorted(defaultComparator(filter.getMetric()))
                .toList();

        List<String> skillGroups = registry.allSkills().stream()
                .map(PaladinTreeSkill::getSkillGroup)
                .distinct()
                .sorted()
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
                List.of(PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_RANK_1,
                        PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_TREE_MAX,
                        PaladinDamageRankingMetric.DAMAGE_PER_USE,
                        PaladinDamageRankingMetric.THEORETICAL_DPS,
                        PaladinDamageRankingMetric.SINGLE_TARGET_DPS)
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
        return !filter.hasType() || row.getType() == filter.getType();
    }

    private static Comparator<DamageRankingRow> defaultComparator(PaladinDamageRankingMetric metric) {
        if (isBaseDamagePercentMetric(metric)) {
            return metricComparator(metric).reversed()
                    .thenComparingInt(DamageRankingController::statusSortRank)
                    .thenComparing(row -> row.getEntry().getSkillName());
        }
        return Comparator
                .comparingInt(DamageRankingController::statusSortRank)
                .thenComparing(metricComparator(metric).reversed())
                .thenComparing(row -> row.getEntry().getSkillName());
    }

    private static boolean isBaseDamagePercentMetric(PaladinDamageRankingMetric metric) {
        return metric == PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_RANK_1
                || metric == PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_TREE_MAX;
    }

    private static int statusSortRank(DamageRankingRow row) {
        if (row.isDpsCalculable()) {
            return 0;
        }
        return switch (row.getEntry().getVerificationStatus()) {
            case SUPPORTED, PARTIAL -> 1;
            case NEEDS_VERIFICATION -> 2;
            case UNSUPPORTED -> 3;
            case NON_DAMAGE -> 4;
        };
    }

    private static Comparator<DamageRankingRow> metricComparator(PaladinDamageRankingMetric metric) {
        return switch (metric) {
            case BASE_DAMAGE_PERCENT_RANK_1 -> Comparator.comparingInt(row -> row.getBaseDamagePercentAtRank1() == null
                    ? Integer.MIN_VALUE
                    : row.getBaseDamagePercentAtRank1());
            case BASE_DAMAGE_PERCENT_TREE_MAX -> Comparator.comparingInt(row -> row.getBaseDamagePercentAtTreeMaxRank() == null
                    ? Integer.MIN_VALUE
                    : row.getBaseDamagePercentAtTreeMaxRank());
            case DAMAGE_PER_USE -> Comparator.comparingLong(row -> row.getEntry().getDamagePerUse() == null
                    ? Long.MIN_VALUE
                    : row.getEntry().getDamagePerUse());
            case THEORETICAL_DPS -> Comparator.comparingDouble(row -> row.getEntry().getTheoreticalDps() == null
                    ? Double.NEGATIVE_INFINITY
                    : row.getEntry().getTheoreticalDps());
            case SINGLE_TARGET_DPS -> Comparator.comparingDouble(row -> row.getSingleTargetDps() == null
                    ? Double.NEGATIVE_INFINITY
                    : row.getSingleTargetDps());
        };
    }

    private void renderPage(HttpExchange exchange, DamageRankingPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

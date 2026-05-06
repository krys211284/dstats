package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.paladin.PaladinSkillTreeRegistry;
import krys.paladin.PaladinSkillTreeType;
import krys.paladin.PaladinTreeSkill;
import krys.ranking.PaladinDamageRankingMetric;
import krys.ranking.PaladinSkillDamageRankingEntry;
import krys.ranking.PaladinSkillDamageRankingService;
import krys.ranking.PaladinSkillDamageVerificationStatus;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Kontroler widoku opisowego rankingu obrażeń umiejętności Paladyna z nowego rejestru PDF. */
public final class PaladinSkillDamageRankingController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final PaladinSkillDamageRankingService rankingService;
    private final PaladinSkillDamageRankingPageRenderer renderer;

    public PaladinSkillDamageRankingController(PaladinSkillDamageRankingService rankingService,
                                               PaladinSkillDamageRankingPageRenderer renderer) {
        this.rankingService = rankingService;
        this.renderer = renderer;
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
            PaladinSkillDamageRankingFilter filter = PaladinSkillDamageRankingFilter.fromQuery(
                    UrlEncodedFormSupport.parseQuery(exchange.getRequestURI().getRawQuery())
            );
            renderPage(exchange, buildPageModel(filter));
        } finally {
            exchange.close();
        }
    }

    private PaladinSkillDamageRankingPageModel buildPageModel(PaladinSkillDamageRankingFilter filter) {
        Map<String, PaladinTreeSkill> skillsById = PaladinSkillTreeRegistry.allSkills().stream()
                .collect(Collectors.toMap(PaladinTreeSkill::getSkillId, Function.identity()));
        List<PaladinSkillDamageRankingRow> rows = rankingService.describePaladinTreeSkills().stream()
                .map(entry -> toRow(entry, skillsById))
                .filter(row -> matchesFilter(row, filter))
                .sorted(defaultComparator(filter.getMetric()))
                .toList();

        List<String> skillGroups = PaladinSkillTreeRegistry.allSkills().stream()
                .map(PaladinTreeSkill::getSkillGroup)
                .distinct()
                .sorted()
                .toList();

        return new PaladinSkillDamageRankingPageModel(
                filter,
                rows,
                PaladinSkillTreeRegistry.allSkills().size(),
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
                List.of(PaladinDamageRankingMetric.DAMAGE_PER_USE,
                        PaladinDamageRankingMetric.THEORETICAL_DPS,
                        PaladinDamageRankingMetric.SINGLE_TARGET_DPS)
        );
    }

    private static PaladinSkillDamageRankingRow toRow(PaladinSkillDamageRankingEntry entry,
                                                      Map<String, PaladinTreeSkill> skillsById) {
        PaladinTreeSkill treeSkill = skillsById.get(entry.getSkillId());
        if (treeSkill == null) {
            throw new IllegalStateException("Ranking zwrócił skill spoza PaladinSkillTreeRegistry: " + entry.getSkillId());
        }
        return new PaladinSkillDamageRankingRow(entry, treeSkill.getType());
    }

    private static boolean matchesFilter(PaladinSkillDamageRankingRow row,
                                         PaladinSkillDamageRankingFilter filter) {
        if (filter.hasSkillGroup() && !row.getEntry().getSkillGroup().equals(filter.getSkillGroup())) {
            return false;
        }
        if (filter.hasVerificationStatus()
                && row.getEntry().getVerificationStatus() != filter.getVerificationStatus()) {
            return false;
        }
        return !filter.hasType() || row.getType() == filter.getType();
    }

    private static Comparator<PaladinSkillDamageRankingRow> defaultComparator(PaladinDamageRankingMetric metric) {
        return Comparator
                .comparingInt(PaladinSkillDamageRankingController::statusSortRank)
                .thenComparing(metricComparator(metric).reversed())
                .thenComparing(row -> row.getEntry().getSkillName());
    }

    private static int statusSortRank(PaladinSkillDamageRankingRow row) {
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

    private static Comparator<PaladinSkillDamageRankingRow> metricComparator(PaladinDamageRankingMetric metric) {
        return switch (metric) {
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

    private void renderPage(HttpExchange exchange, PaladinSkillDamageRankingPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

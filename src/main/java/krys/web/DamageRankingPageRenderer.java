package krys.web;

import krys.paladin.DamagePercentComponent;
import krys.paladin.DamagePercentComponentRankTable;
import krys.paladin.PaladinSkillTreeType;
import krys.paladin.UpgradeDamageImpact;
import krys.ranking.PaladinDamageRankingMetric;
import krys.ranking.PaladinSkillDamageRankingEntry;
import krys.ranking.PaladinSkillDamageVerificationStatus;
import krys.ranking.PlayableClass;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Renderuje ogólny ranking obrażeń z blokadą niezweryfikowanych mechanik DPS. */
public final class DamageRankingPageRenderer {
    private static final String PAGE_PATH = "/ranking-obrazen";
    private final String template;

    public DamageRankingPageRenderer() {
        this.template = loadTemplate();
    }

    public String render(DamageRankingPageModel model) {
        return template
                .replace("{{APP_SHELL_STYLES}}", AppShellRendererSupport.renderSharedStyles())
                .replace("{{GLOBAL_NAV}}", AppShellRendererSupport.renderGlobalNavigation(PAGE_PATH))
                .replace("{{REGISTRY_NAME}}", escapeHtml(model.getRegistry().getRegistryName()))
                .replace("{{CHARACTER_NAME}}", escapeHtml(model.getFilter().getCharacter().getDisplayName()))
                .replace("{{SUMMARY}}", renderSummary(model))
                .replace("{{FILTERS}}", renderFilters(model))
                .replace("{{TABLE}}", renderTable(model));
    }

    private static String renderSummary(DamageRankingPageModel model) {
        long needsVerification = countStatus(model, PaladinSkillDamageVerificationStatus.NEEDS_VERIFICATION);
        long unsupported = countStatus(model, PaladinSkillDamageVerificationStatus.UNSUPPORTED);
        long nonDamage = countStatus(model, PaladinSkillDamageVerificationStatus.NON_DAMAGE);
        return new StringBuilder("<div class=\"summary-grid\">")
                .append(renderSummaryCard("Postać", model.getFilter().getCharacter().getDisplayName()))
                .append(renderSummaryCard("Wpisy w rejestrze", Integer.toString(model.getTotalSkillCount())))
                .append(renderSummaryCard("Widoczne po filtrach", Integer.toString(model.getRows().size())))
                .append(renderSummaryCard("Policzalne teraz", Long.toString(model.getCalculableCount())))
                .append(renderSummaryCard("NEEDS_VERIFICATION", Long.toString(needsVerification)))
                .append(renderSummaryCard("UNSUPPORTED", Long.toString(unsupported)))
                .append(renderSummaryCard("NON_DAMAGE", Long.toString(nonDamage)))
                .append("</div>")
                .toString();
    }

    private static long countStatus(DamageRankingPageModel model,
                                    PaladinSkillDamageVerificationStatus status) {
        return model.getRows().stream()
                .filter(row -> row.getEntry().getVerificationStatus() == status)
                .count();
    }

    private static String renderSummaryCard(String label, String value) {
        return """
                <div class="summary-card">
                    <div class="summary-label">%s</div>
                    <div class="summary-value">%s</div>
                </div>
                """.formatted(escapeHtml(label), escapeHtml(value));
    }

    private static String renderFilters(DamageRankingPageModel model) {
        DamageRankingFilter filter = model.getFilter();
        StringBuilder html = new StringBuilder("""
                <form class="ranking-filters" method="get" action="/ranking-obrazen">
                    <label>Postać
                        <select name="character">
                """);
        for (PlayableClass playableClass : model.getSupportedClasses()) {
            html.append(renderOption(
                    playableClass.getQueryValue(),
                    playableClass.getDisplayName(),
                    playableClass == filter.getCharacter()
            ));
        }
        html.append("""
                        </select>
                    </label>
                    <label>Grupa umiejętności
                        <select name="skillGroup">
                """);
        html.append(renderOption("ALL", "Wszystkie grupy", !filter.hasSkillGroup()));
        for (String skillGroup : model.getSkillGroups()) {
            html.append(renderOption(skillGroup, skillGroup, skillGroup.equals(filter.getSkillGroup())));
        }
        html.append("""
                        </select>
                    </label>
                    <label>Status weryfikacji
                        <select name="verificationStatus">
                """);
        html.append(renderOption("ALL", "Wszystkie statusy", !filter.hasVerificationStatus()));
        for (PaladinSkillDamageVerificationStatus status : model.getVerificationStatuses()) {
            html.append(renderOption(status.name(), status.name(), status == filter.getVerificationStatus()));
        }
        html.append("""
                        </select>
                    </label>
                    <label>Typ umiejętności
                        <select name="type">
                """);
        html.append(renderOption("ALL", "Wszystkie typy", !filter.hasType()));
        for (PaladinSkillTreeType type : model.getTypes()) {
            html.append(renderOption(type.name(), type.name(), type == filter.getType()));
        }
        html.append("""
                        </select>
                    </label>
                    <label>Metryka rankingu
                        <select name="metric">
                """);
        for (PaladinDamageRankingMetric metric : model.getMetrics()) {
            html.append(renderOption(metric.name(), metric.name(), metric == filter.getMetric()));
        }
        html.append("""
                        </select>
                    </label>
                    <div class="filter-actions">
                        <button type="submit">Filtruj</button>
                        <a class="secondary-link" href="/ranking-obrazen">Wyczyść</a>
                    </div>
                </form>
                """);
        return html.toString();
    }

    private static String renderOption(String value, String label, boolean selected) {
        return "<option value=\"" + escapeHtml(value) + "\"" + (selected ? " selected" : "") + ">"
                + escapeHtml(label)
                + "</option>";
    }

    private static String renderTable(DamageRankingPageModel model) {
        if (model.getRows().isEmpty()) {
            return """
                    <div class="empty-state">
                        <h2>Brak wpisów dla wybranych filtrów</h2>
                        <p>Zmień filtry, aby wrócić do opisowego rankingu wybranej postaci.</p>
                    </div>
                    """;
        }
        StringBuilder html = new StringBuilder("""
                <div class="verification-legend" aria-label="Legenda statusów weryfikacji">
                    <span class="legend-item verification-supported">SUPPORTED - zweryfikowane</span>
                    <span class="legend-item verification-needs-verification">NEEDS_VERIFICATION - wymaga weryfikacji</span>
                    <span class="legend-item verification-non-damage">NON_DAMAGE - bez bezpośrednich obrażeń</span>
                    <span class="legend-item verification-unsupported">UNSUPPORTED - nieobsługiwane</span>
                </div>
                <div class="ranking-table-wrap">
                    <table class="data-table ranking-table">
                        <thead>
                            <tr>
                                <th>skillName</th>
                                <th>skillGroup</th>
                                <th>type</th>
                                <th>Obrażenia % R1</th>
                                <th>Obrażenia % max drzewo</th>
                                <th>grupa_1: wpływ na obrażenia</th>
                                <th>grupa_2: wpływ na obrażenia</th>
                                <th>grupa_3: wpływ na obrażenia</th>
                            </tr>
                        </thead>
                        <tbody>
                """);
        for (DamageRankingRow row : model.getRows()) {
            html.append(renderRow(row));
        }
        html.append("""
                        </tbody>
                    </table>
                </div>
                """);
        return html.toString();
    }

    private static String renderRow(DamageRankingRow row) {
        PaladinSkillDamageRankingEntry entry = row.getEntry();
        PaladinSkillDamageVerificationStatus status = entry.getVerificationStatus();
        return new StringBuilder("<tr class=\"damage-ranking-row verification-row ")
                .append(verificationRowCssClass(status))
                .append("\" data-skill-row=\"true\" data-skill-id=\"")
                .append(escapeHtml(entry.getSkillId()))
                .append("\" data-verification-status=\"")
                .append(escapeHtml(status.name()))
                .append("\" data-skill-type=\"")
                .append(escapeHtml(row.getType().name()))
                .append("\" title=\"Status weryfikacji: ")
                .append(escapeHtml(status.name()))
                .append("\" aria-label=\"")
                .append(escapeHtml(entry.getSkillName()))
                .append(", status weryfikacji: ")
                .append(escapeHtml(status.name()))
                .append("\"><td>")
                .append(escapeHtml(entry.getSkillName()))
                .append("</td><td>")
                .append(escapeHtml(entry.getSkillGroup()))
                .append("</td><td>")
                .append(escapeHtml(row.getType().name()))
                .append("</td><td>")
                .append(renderDamagePercentCell(row, 1))
                .append("</td><td>")
                .append(renderDamagePercentCell(row, 15))
                .append("</td><td>")
                .append(renderUpgradeGroup(row, "grupa_1"))
                .append("</td><td>")
                .append(renderUpgradeGroup(row, "grupa_2"))
                .append("</td><td>")
                .append(renderUpgradeGroup(row, "grupa_3"))
                .append("</td></tr>")
                .toString();
    }

    private static String renderDamagePercentCell(DamageRankingRow row, int rank) {
        Integer simpleValue = rank == 1
                ? row.getBaseDamagePercentAtRank1()
                : row.getBaseDamagePercentAtTreeMaxRank();
        if (simpleValue != null) {
            return simpleValue + "%";
        }

        DamagePercentComponentRankTable componentTable = row.getComponentDamagePercentRanks();
        if (!componentTable.isEmpty()) {
            StringBuilder html = new StringBuilder("<ul class=\"compact-list component-percent-list\">");
            for (DamagePercentComponent component : DamagePercentComponent.values()) {
                Integer componentValue = componentTable.damagePercentAt(component, rank);
                if (componentValue != null) {
                    html.append("<li><code>")
                            .append(escapeHtml(component.name()))
                            .append("</code>: ")
                            .append(componentValue)
                            .append("%</li>");
                }
            }
            html.append("</ul>");
            return html.toString();
        }

        if (isNonDamageDisplay(row)) {
            return "<span class=\"not-applicable-value\">nie dotyczy</span>";
        }
        return "<span class=\"needs-review-value\">wymaga weryfikacji</span>";
    }

    private static boolean isNonDamageDisplay(DamageRankingRow row) {
        if (row.getEntry().getVerificationStatus() == PaladinSkillDamageVerificationStatus.NON_DAMAGE) {
            return true;
        }
        return row.getType() != PaladinSkillTreeType.DAMAGE && row.getType() != PaladinSkillTreeType.SPECIAL;
    }

    private static String renderUpgradeGroup(DamageRankingRow row, String groupId) {
        var impacts = row.getUpgradeDamageImpactsForGroup(groupId);
        if (impacts.isEmpty()) {
            return "<span class=\"missing-source-value\">brak danych</span>";
        }
        StringBuilder html = new StringBuilder("<ul class=\"compact-list upgrade-impact-list\">");
        for (UpgradeDamageImpact impact : impacts) {
            html.append("<li>")
                    .append(escapeHtml(impact.getUpgradeName()))
                    .append(" &mdash; <code>")
                    .append(escapeHtml(impact.getType().name()))
                    .append("</code> &mdash; ")
                    .append(escapeHtml(shortImpactDescription(impact)))
                    .append("</li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private static String shortImpactDescription(UpgradeDamageImpact impact) {
        if (impact.getDamagePercent() != null) {
            return "+" + impact.getDamagePercent() + "%";
        }
        return switch (impact.getType()) {
            case DIRECT_DAMAGE_PERCENT -> "bezpośredni procent obrażeń";
            case ADDITIONAL_HIT -> "dodatkowe trafienie";
            case DAMAGE_OVER_TIME -> "obrażenia w czasie";
            case BURST_DAMAGE -> "obrażenia wybuchowe";
            case CONDITIONAL_DAMAGE -> "obrażenia warunkowe";
            case STATUS_OR_UTILITY -> "utility/status";
            case COOLDOWN_OR_COST -> "koszt/odnowienie/zasób";
            case NO_DAMAGE_IMPACT -> "brak wpływu na obrażenia";
            case NEEDS_VERIFICATION -> "wymaga weryfikacji";
        };
    }

    private static String renderStatus(PaladinSkillDamageVerificationStatus status) {
        return "<span class=\"status-badge " + statusCssClass(status) + "\">"
                + escapeHtml(status.name())
                + "</span>";
    }

    private static String statusCssClass(PaladinSkillDamageVerificationStatus status) {
        return switch (status) {
            case SUPPORTED, PARTIAL -> "status-active";
            case NEEDS_VERIFICATION -> "status-warning";
            case UNSUPPORTED -> "status-error";
            case NON_DAMAGE -> "status-inactive";
        };
    }

    private static String verificationRowCssClass(PaladinSkillDamageVerificationStatus status) {
        return switch (status) {
            case SUPPORTED -> "verification-supported";
            case PARTIAL -> "verification-partial";
            case NEEDS_VERIFICATION -> "verification-needs-verification";
            case UNSUPPORTED -> "verification-unsupported";
            case NON_DAMAGE -> "verification-non-damage";
        };
    }

    private static String escapeHtml(String value) {
        return AppShellRendererSupport.escapeHtml(value);
    }

    private static String loadTemplate() {
        try (InputStream inputStream = DamageRankingPageRenderer.class.getResourceAsStream("/templates/damage-ranking.html")) {
            if (inputStream == null) {
                throw new IllegalStateException("Brak szablonu /templates/damage-ranking.html");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się wczytać szablonu rankingu obrażeń.", exception);
        }
    }
}

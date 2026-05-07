package krys.web;

import krys.paladin.DamagePercentComponent;
import krys.paladin.DamagePercentComponentRankTable;
import krys.paladin.PaladinSkillTreeType;
import krys.paladin.SkillTag;
import krys.paladin.UpgradeDamageModifier;
import krys.paladin.UpgradeDamageModifierType;
import krys.paladin.UpgradeDamageSafety;
import krys.ranking.PaladinDamageRankingMetric;
import krys.ranking.PaladinSkillDamageRankingEntry;
import krys.ranking.PaladinSkillDamageVerificationStatus;
import krys.ranking.PlayableClass;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

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
                """);
        html.append(renderTagFilter(model));
        html.append(renderFacetFilter("hasDirectUpgradeDamage", "Direct dmg", filter.getHasDirectUpgradeDamage()));
        html.append(renderFacetFilter("hasNewDamageComponent", "New component", filter.getHasNewDamageComponent()));
        html.append(renderFacetFilter("hasStatusDamageEnabler", "Status / debuff", filter.getHasStatusDamageEnabler()));
        html.append(renderFacetFilter("hasResourceGeneration", "Resource", filter.getHasResourceGeneration()));
        html.append(renderFacetFilter("hasCooldownOrCastSpeed", "Speed / cooldown", filter.getHasCooldownOrCastSpeed()));
        html.append(renderFacetFilter("hasDefenseOrUtility", "Defense / utility", filter.getHasDefenseOrUtility()));
        html.append(renderFacetFilter("hasManualReviewUpgrade", "Manual review", filter.getHasManualReviewUpgrade()));
        html.append("""
                    <input type="hidden" name="sort" value="%s">
                    <input type="hidden" name="direction" value="%s">
                    <div class="filter-actions">
                        <button type="submit">Filtruj</button>
                        <a class="secondary-link" href="/ranking-obrazen">Wyczyść</a>
                    </div>
                </form>
                """.formatted(escapeHtml(filter.getSort()), escapeHtml(filter.getDirection().name().toLowerCase())));
        return html.toString();
    }

    private static String renderTagFilter(DamageRankingPageModel model) {
        StringBuilder html = new StringBuilder("""
                    <label>tag
                        <select name="tag">
                """);
        html.append(renderOption("ALL", "Wszystkie tagi", !model.getFilter().hasTag()));
        for (SkillTag tag : model.getTags()) {
            html.append(renderOption(tag.name(), tag.name(), tag == model.getFilter().getTag()));
        }
        html.append("""
                        </select>
                    </label>
                """);
        return html.toString();
    }

    private static String renderFacetFilter(String name,
                                            String label,
                                            DamageRankingFilter.FacetFilter selectedValue) {
        StringBuilder html = new StringBuilder("<label>")
                .append(escapeHtml(label))
                .append("<select name=\"")
                .append(escapeHtml(name))
                .append("\">");
        for (DamageRankingFilter.FacetFilter value : DamageRankingFilter.FacetFilter.values()) {
            html.append(renderOption(value.name(), value.name(), value == selectedValue));
        }
        html.append("</select></label>");
        return html.toString();
    }

    private static String renderOption(String value, String label, boolean selected) {
        return "<option value=\"" + escapeHtml(value) + "\"" + (selected ? " selected" : "") + ">"
                + escapeHtml(label)
                + "</option>";
    }

    private static String renderSortableHeader(DamageRankingPageModel model, String sortKey, String label) {
        DamageRankingFilter filter = model.getFilter();
        boolean active = filter.getSort().equals(sortKey);
        DamageRankingFilter.SortDirection nextDirection = active && filter.getDirection() == DamageRankingFilter.SortDirection.ASC
                ? DamageRankingFilter.SortDirection.DESC
                : DamageRankingFilter.SortDirection.ASC;
        String ariaSort = active
                ? " aria-sort=\"" + (filter.getDirection() == DamageRankingFilter.SortDirection.ASC ? "ascending" : "descending") + "\""
                : "";
        String indicator = active
                ? (filter.getDirection() == DamageRankingFilter.SortDirection.ASC ? " ▲" : " ▼")
                : "";
        return "<th" + ariaSort + "><a class=\"sort-link\" href=\""
                + escapeHtml(sortUrl(filter, sortKey, nextDirection))
                + "\">"
                + escapeHtml(label)
                + "<span class=\"sort-indicator\">"
                + escapeHtml(indicator)
                + "</span></a></th>";
    }

    private static String sortUrl(DamageRankingFilter filter,
                                  String sortKey,
                                  DamageRankingFilter.SortDirection direction) {
        StringBuilder query = new StringBuilder("/ranking-obrazen?character=")
                .append(urlEncode(filter.getCharacter().getQueryValue()));
        appendQuery(query, "skillGroup", filter.getSkillGroup());
        if (filter.getVerificationStatus() != null) {
            appendQuery(query, "verificationStatus", filter.getVerificationStatus().name());
        }
        if (filter.getType() != null) {
            appendQuery(query, "type", filter.getType().name());
        }
        if (filter.getTag() != null) {
            appendQuery(query, "tag", filter.getTag().name());
        }
        appendFacetQuery(query, "hasDirectUpgradeDamage", filter.getHasDirectUpgradeDamage());
        appendFacetQuery(query, "hasNewDamageComponent", filter.getHasNewDamageComponent());
        appendFacetQuery(query, "hasStatusDamageEnabler", filter.getHasStatusDamageEnabler());
        appendFacetQuery(query, "hasResourceGeneration", filter.getHasResourceGeneration());
        appendFacetQuery(query, "hasCooldownOrCastSpeed", filter.getHasCooldownOrCastSpeed());
        appendFacetQuery(query, "hasDefenseOrUtility", filter.getHasDefenseOrUtility());
        appendFacetQuery(query, "hasManualReviewUpgrade", filter.getHasManualReviewUpgrade());
        appendQuery(query, "sort", sortKey);
        appendQuery(query, "direction", direction.name().toLowerCase());
        return query.toString();
    }

    private static void appendFacetQuery(StringBuilder query,
                                         String name,
                                         DamageRankingFilter.FacetFilter value) {
        if (value != DamageRankingFilter.FacetFilter.ALL) {
            appendQuery(query, name, value.name());
        }
    }

    private static void appendQuery(StringBuilder query, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        query.append("&")
                .append(urlEncode(name))
                .append("=")
                .append(urlEncode(value));
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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
                        <colgroup>
                            <col class="col-skill-name">
                            <col class="col-tags">
                            <col class="col-group">
                            <col class="col-type">
                            <col class="col-damage">
                            <col class="col-damage">
                            <col class="col-facet">
                            <col class="col-facet">
                            <col class="col-facet">
                            <col class="col-facet">
                            <col class="col-facet">
                            <col class="col-facet">
                            <col class="col-facet">
                        </colgroup>
                        <thead>
                            <tr>
                                %s
                                %s
                                %s
                                %s
                                %s
                                %s
                                %s
                                %s
                                %s
                                %s
                                %s
                                %s
                                %s
                            </tr>
                        </thead>
                        <tbody>
                """.formatted(
                renderSortableHeader(model, "skillName", "skillName"),
                renderSortableHeader(model, "tags", "tags"),
                renderSortableHeader(model, "skillGroup", "skillGroup"),
                renderSortableHeader(model, "type", "type"),
                renderSortableHeader(model, "baseDamageRank1", "Obrażenia % R1"),
                renderSortableHeader(model, "baseDamageTreeMax", "Obrażenia % max drzewo"),
                renderSortableHeader(model, "hasDirectUpgradeDamage", "Direct dmg upgrade"),
                renderSortableHeader(model, "hasNewDamageComponent", "New dmg component"),
                renderSortableHeader(model, "hasStatusDamageEnabler", "Status / debuff"),
                renderSortableHeader(model, "hasResourceGeneration", "Resource"),
                renderSortableHeader(model, "hasCooldownOrCastSpeed", "Speed / cooldown"),
                renderSortableHeader(model, "hasDefenseOrUtility", "Defense / utility"),
                renderSortableHeader(model, "hasManualReviewUpgrade", "Manual review")
        ));
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
                .append(renderTags(row))
                .append("</td><td>")
                .append(escapeHtml(entry.getSkillGroup()))
                .append("</td><td>")
                .append(escapeHtml(row.getType().name()))
                .append("</td><td>")
                .append(renderDamagePercentCell(row, 1))
                .append("</td><td>")
                .append(renderDamagePercentCell(row, 15))
                .append("</td><td>")
                .append(renderModifierSummary(row.directUpgradeDamageModifiers()))
                .append("</td><td>")
                .append(renderModifierSummary(row.newDamageComponentModifiers()))
                .append("</td><td>")
                .append(renderModifierSummary(row.statusDamageModifiers()))
                .append("</td><td>")
                .append(renderModifierSummary(row.resourceModifiers()))
                .append("</td><td>")
                .append(renderModifierSummary(row.cooldownOrCastSpeedModifiers()))
                .append("</td><td>")
                .append(renderModifierSummary(row.defenseOrUtilityModifiers()))
                .append("</td><td>")
                .append(renderModifierSummary(row.manualReviewModifiers()))
                .append("</td></tr>")
                .toString();
    }

    private static String renderTags(DamageRankingRow row) {
        StringBuilder html = new StringBuilder("<div class=\"tag-list\">");
        row.getTags().stream()
                .map(Enum::name)
                .sorted()
                .forEach(tag -> html.append("<span class=\"tag-chip\">")
                        .append(escapeHtml(tag))
                        .append("</span>"));
        html.append("</div>");
        return html.toString();
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
        var modifiers = row.getUpgradeDamageModifiersForGroup(groupId);
        if (modifiers.isEmpty()) {
            return "<span class=\"missing-source-value\">brak danych</span>";
        }
        StringBuilder html = new StringBuilder("<ul class=\"compact-list upgrade-impact-list\">");
        for (UpgradeDamageModifier modifier : modifiers) {
            html.append("<li>")
                    .append(escapeHtml(modifier.getUpgradeName()))
                    .append(" &mdash; <code>")
                    .append(escapeHtml(modifier.getType().name()))
                    .append("</code> &mdash; ")
                    .append(escapeHtml(shortModifierDescription(modifier)))
                    .append("</li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private static String renderModifierSummary(List<UpgradeDamageModifier> modifiers) {
        if (modifiers.isEmpty()) {
            return "<span class=\"missing-source-value\">-</span>";
        }
        StringBuilder html = new StringBuilder("<ul class=\"compact-list facet-list\">");
        for (UpgradeDamageModifier modifier : modifiers) {
            html.append("<li title=\"")
                    .append(escapeHtml(modifier.getUpgradeGroup()))
                    .append(": ")
                    .append(escapeHtml(modifier.getNotes()))
                    .append("\"><span class=\"facet-name\">")
                    .append(escapeHtml(modifier.getUpgradeName()))
                    .append("</span>");
            String description = shortModifierDescription(modifier);
            if (!description.isBlank()) {
                html.append(" <span class=\"facet-value\">")
                        .append(escapeHtml(description))
                        .append("</span>");
            }
            html.append("</li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private static String shortModifierDescription(UpgradeDamageModifier modifier) {
        if (modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.NEEDS_MANUAL_REVIEW) {
            return "wymaga weryfikacji";
        }
        if (!modifier.getValue().equals("brak")
                && !modifier.getValue().equals("tekst źródłowy")
                && !modifier.getValue().equals("brak bezpośredniego damage")
                && !modifier.getValue().equals("status")) {
            return modifier.getValue() + valueSuffix(modifier);
        }
        return switch (modifier.getType()) {
            case MULTIPLICATIVE_DAMAGE_PERCENT -> "mnożnik obrażeń";
            case ADDITIVE_DAMAGE_PERCENT -> "addytywny procent obrażeń";
            case FLAT_COMPONENT_PERCENT -> "komponent obrażeń";
            case RANK_SCALING_COMPONENT_PERCENT -> "komponent skalowany rangą";
            case ADDITIONAL_HIT_OR_STRIKE -> "dodatkowe trafienie/uderzenie";
            case DAMAGE_OVER_TIME -> "obrażenia w czasie";
            case THORNS_DAMAGE_MODIFIER -> "ciernie, wymaga weryfikacji";
            case STATUS_DAMAGE_ENABLER -> "status / pośredni wpływ";
            case CAST_SPEED_OR_COOLDOWN -> "tempo użycia / cooldown";
            case RESOURCE_OR_COST -> "zasób / koszt";
            case DEFENSE_OR_UTILITY -> "utility/defense";
            case NO_DAMAGE_IMPACT -> "brak wpływu na obrażenia";
            case NEEDS_MANUAL_REVIEW -> "wymaga weryfikacji";
        };
    }

    private static String valueSuffix(UpgradeDamageModifier modifier) {
        if (modifier.getType() == UpgradeDamageModifierType.CAST_SPEED_OR_COOLDOWN) {
            return ", tempo użycia";
        }
        if (modifier.getType() == UpgradeDamageModifierType.STATUS_DAMAGE_ENABLER) {
            return ", status / pośredni wpływ";
        }
        if (modifier.getType() == UpgradeDamageModifierType.RESOURCE_OR_COST) {
            return ", zasób";
        }
        if (modifier.createsNewDamageComponent()) {
            return ", nowy komponent";
        }
        return "";
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

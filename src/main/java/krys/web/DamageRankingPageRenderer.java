package krys.web;

import krys.ranking.DamageRankingMetric;
import krys.ranking.PlayableClass;
import krys.ranking.SkillDamageRankingEntry;
import krys.ranking.SkillDamageVerificationStatus;
import krys.skilltree.DamagePercentComponent;
import krys.skilltree.DamagePercentComponentRankTable;
import krys.skilltree.SkillTreeType;
import krys.skilltree.SourceSkillCategory;
import krys.skilltree.UpgradeDamageModifier;
import krys.skilltree.UpgradeDamageModifierType;
import krys.skilltree.UpgradeDamageSafety;

import java.io.InputStream;
import java.io.IOException;
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
        long needsVerification = countStatus(model, SkillDamageVerificationStatus.NEEDS_VERIFICATION);
        long unsupported = countStatus(model, SkillDamageVerificationStatus.UNSUPPORTED);
        long nonDamage = countStatus(model, SkillDamageVerificationStatus.NON_DAMAGE);
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
                                    SkillDamageVerificationStatus status) {
        return model.getRows().stream()
                .filter(row -> row.getEntry().getVerificationStatus() == status)
                .count();
    }

    private static String renderSummaryCard(String label, String value) {
        String valueAttributes = label.equals("Widoczne po filtrach")
                ? " id=\"ranking-visible-count\" role=\"status\" aria-live=\"polite\" aria-atomic=\"true\""
                : "";
        return """
                <div class="summary-card">
                    <div class="summary-label">%s</div>
                    <div class="summary-value"%s>%s</div>
                </div>
                """.formatted(escapeHtml(label), valueAttributes, escapeHtml(value));
    }

    private static String renderFilters(DamageRankingPageModel model) {
        DamageRankingFilter filter = model.getFilter();
        StringBuilder html = new StringBuilder("""
                <form class="ranking-filters" method="get" action="/ranking-obrazen">
                    <label class="filter-field filter-field-search">Szukaj
                        <input class="filter-control filter-input" id="ranking-search-query" type="search" name="q" value="%s" placeholder="%s" aria-describedby="ranking-search-help">
                        <span id="ranking-search-help" class="visually-hidden">Filtruje po nazwie, id, kategoriach i widocznych cechach.</span>
                    </label>
                    <label class="filter-field">Postać
                        <select class="filter-control filter-select" name="character">
                """.formatted(escapeHtml(filter.getQ() == null ? "" : filter.getQ()),
                escapeHtml(model.getRegistry().getSearchPlaceholder())));
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
                    <label class="filter-field">Grupa drzewa
                        <select class="filter-control filter-select" name="skillGroup">
                """);
        html.append(renderOption("ALL", "Wszystkie grupy drzewa", !filter.hasSkillGroup()));
        for (String skillGroup : model.getSkillGroups()) {
            html.append(renderOption(skillGroup, model.getRegistry().treeGroupDisplayName(skillGroup), skillGroup.equals(filter.getSkillGroup())));
        }
        html.append("""
                        </select>
                    </label>
                    <label class="filter-field">Status weryfikacji
                        <select class="filter-control filter-select" name="verificationStatus">
                """);
        html.append(renderOption("ALL", "Wszystkie statusy", !filter.hasVerificationStatus()));
        for (SkillDamageVerificationStatus status : model.getVerificationStatuses()) {
            html.append(renderOption(status.name(), status.name(), status == filter.getVerificationStatus()));
        }
        html.append("""
                        </select>
                    </label>
                """);
        html.append(renderSourceCategoryFilter(model));
        html.append(renderFacetFilter("hasDirectUpgradeDamage", "Damage modifier", filter.getHasDirectUpgradeDamage()));
        html.append(renderFacetFilter("hasNewDamageComponent", "Extra component", filter.getHasNewDamageComponent()));
        html.append(renderFacetFilter("hasStatusDamageEnabler", "Status / debuff", filter.getHasStatusDamageEnabler()));
        html.append(renderFacetFilter("hasFaithCost", model.getRegistry().getResourceType().costLabel(), filter.getHasResourceCost()));
        html.append(renderFacetFilter("hasResourceGeneration", model.getRegistry().getResourceType().generationLabel(), filter.getHasResourceGeneration()));
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

    private static String renderSourceCategoryFilter(DamageRankingPageModel model) {
        StringBuilder html = new StringBuilder("""
                    <label class="filter-field">Kategoria z gry
                        <select class="filter-control filter-select" name="sourceCategory">
                """);
        html.append(renderOption("ALL", "Wszystkie kategorie", !model.getFilter().hasSourceCategory()));
        for (SourceSkillCategory category : model.getSourceCategories()) {
            html.append(renderOption(category.getId(), category.getDisplayName(), category.getId().equalsIgnoreCase(model.getFilter().getSourceCategory())));
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
        StringBuilder html = new StringBuilder("<label class=\"filter-field\">")
                .append(escapeHtml(label))
                .append("<select class=\"filter-control filter-select\" name=\"")
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
        boolean active = filter.matchesSort(sortKey);
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

    private static String renderResourceGenerationHeader(DamageRankingPageModel model) {
        DamageRankingFilter filter = model.getFilter();
        boolean active = filter.matchesSort("faithGeneratedBase")
                || filter.matchesSort("faithGeneratedMaxKnown");
        DamageRankingFilter.SortDirection nextMaxDirection = filter.matchesSort("faithGeneratedMaxKnown")
                && filter.getDirection() == DamageRankingFilter.SortDirection.ASC
                ? DamageRankingFilter.SortDirection.DESC
                : DamageRankingFilter.SortDirection.ASC;
        DamageRankingFilter.SortDirection nextBaseDirection = filter.matchesSort("faithGeneratedBase")
                && filter.getDirection() == DamageRankingFilter.SortDirection.ASC
                ? DamageRankingFilter.SortDirection.DESC
                : DamageRankingFilter.SortDirection.ASC;
        String ariaSort = active
                ? " aria-sort=\"" + (filter.getDirection() == DamageRankingFilter.SortDirection.ASC ? "ascending" : "descending") + "\""
                : "";
        String indicator = active
                ? (filter.getDirection() == DamageRankingFilter.SortDirection.ASC ? " ▲" : " ▼")
                : "";
        return "<th" + ariaSort + "><a class=\"sort-link\" href=\""
                + escapeHtml(sortUrl(filter, "faithGeneratedMaxKnown", nextMaxDirection))
                + "\">" + escapeHtml(model.getRegistry().getResourceType().generationLabel()) + "<span class=\"sort-indicator\">"
                + escapeHtml(indicator)
                + "</span></a><a class=\"sort-link secondary-sort-link\" href=\""
                + escapeHtml(sortUrl(filter, "faithGeneratedBase", nextBaseDirection))
                + "\">bazowo</a></th>";
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
        if (filter.getSourceCategory() != null) {
            appendQuery(query, "sourceCategory", filter.getSourceCategory());
        }
        appendQuery(query, "q", filter.getQ());
        appendFacetQuery(query, "hasDirectUpgradeDamage", filter.getHasDirectUpgradeDamage());
        appendFacetQuery(query, "hasNewDamageComponent", filter.getHasNewDamageComponent());
        appendFacetQuery(query, "hasStatusDamageEnabler", filter.getHasStatusDamageEnabler());
        appendFacetQuery(query, "hasFaithCost", filter.getHasResourceCost());
        appendFacetQuery(query, "hasResourceGeneration", filter.getHasResourceGeneration());
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
                        <h2>Brak umiejętności pasujących do filtrów.</h2>
                        <p>Zmień filtr tekstowy albo pozostałe filtry, aby wrócić do opisowego rankingu wybranej postaci.</p>
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
                <div class="damage-modifier-legend" aria-label="Legenda kolumn modyfikatorów obrażeń">
                    <span>Dmg multiplier = mnożnik, np. 20%%[X]</span>
                    <span>Dmg bonus = bonus addytywny, np. 20%%[+]</span>
                    <span>Extra hit / component = osobny hit lub komponent</span>
                    <span>Wartości nie są sumowane i nie są DPS.</span>
                </div>
                <div class="ranking-table-wrap">
                    <table class="data-table ranking-table">
                        <colgroup>
                            <col class="col-skill-name">
                            <col class="col-source-categories">
                            <col class="col-damage">
                            <col class="col-damage">
                            <col class="col-faith">
                            <col class="col-faith wide-faith">
                            <col class="col-faith">
                            <col class="col-facet">
                            <col class="col-facet">
                            <col class="col-facet wide-facet">
                            <col class="col-facet">
                            <col class="col-facet">
                            <col class="col-facet">
                            <col class="col-facet">
                            <col class="col-faith">
                            <col class="col-facet wide-facet">
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
                                %s
                                <th>Odnowienie</th>
                                <th>Wymaganie użycia</th>
                            </tr>
                        </thead>
                        <tbody>
                """.formatted(
                renderSortableHeader(model, "skillName", "skillName"),
                renderSortableHeader(model, "sourceCategories", "Kategorie z gry"),
                renderSortableHeader(model, "baseDamageRank1", "Obrażenia % R1"),
                renderSortableHeader(model, "baseDamageTreeMax", "Obrażenia % max drzewo"),
                renderSortableHeader(model, "faithCost", model.getRegistry().getResourceType().costLabel()),
                renderResourceGenerationHeader(model),
                "<th>Lucky Hit</th>",
                renderSortableHeader(model, "maxDamageMultiplierPercent", "Dmg multiplier"),
                renderSortableHeader(model, "maxDamageBonusPercent", "Dmg bonus"),
                renderSortableHeader(model, "maxExtraHitOrComponentPercent", "Extra hit / component"),
                renderSortableHeader(model, "maxDamageOverTimePercent", "Damage over time"),
                renderSortableHeader(model, "hasStatusDamageEnabler", "Status / debuff"),
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
                <p class="empty-state ranking-live-empty" id="ranking-live-empty" hidden>Brak umiejętności pasujących do filtrów.</p>
                """);
        return html.toString();
    }

    private static String renderRow(DamageRankingRow row) {
        SkillDamageRankingEntry entry = row.getEntry();
        SkillDamageVerificationStatus status = entry.getVerificationStatus();
        return new StringBuilder("<tr class=\"damage-ranking-row verification-row ")
                .append(verificationRowCssClass(status))
                .append("\" data-skill-row=\"true\" data-skill-id=\"")
                .append(escapeHtml(entry.getSkillId()))
                .append("\" data-verification-status=\"")
                .append(escapeHtml(status.name()))
                .append("\" data-skill-group=\"")
                .append(escapeHtml(entry.getSkillGroup()))
                .append("\" data-mechanic-tags=\"")
                .append(escapeHtml(row.getMechanicTagsDisplay()))
                .append("\" data-search-text=\"")
                .append(escapeHtml(DamageRankingSearchText.normalizedRowText(row)))
                .append("\" title=\"Status weryfikacji: ")
                .append(escapeHtml(status.name()))
                .append("\" aria-label=\"")
                .append(escapeHtml(entry.getSkillName()))
                .append(", status weryfikacji: ")
                .append(escapeHtml(status.name()))
                .append("\"><td><span class=\"ranking-tooltip\" title=\"")
                .append(escapeHtml(entry.getNotes()))
                .append("\">")
                .append(escapeHtml(entry.getSkillName()))
                .append("</span></td><td>")
                .append(escapeHtml(row.getSourceCategoriesDisplay()))
                .append("</td><td>")
                .append(renderDamagePercentCell(row, 1))
                .append("</td><td>")
                .append(renderDamagePercentCell(row, 15))
                .append("</td><td>")
                .append(escapeHtml(row.getResourceCostSummary()))
                .append("</td><td>")
                .append(renderResourceGenerationCell(row))
                .append("</td><td>")
                .append(renderLuckyHitCell(row))
                .append("</td><td>")
                .append(renderModifierSummary(row, row.damageMultiplierModifiers()))
                .append("</td><td>")
                .append(renderModifierSummary(row, row.damageBonusModifiers()))
                .append("</td><td>")
                .append(renderModifierSummary(row, row.extraHitOrComponentModifiers()))
                .append("</td><td>")
                .append(renderModifierSummary(row, row.damageOverTimeModifiers()))
                .append("</td><td>")
                .append(renderModifierSummary(row, row.statusDamageModifiers()))
                .append("</td><td>")
                .append(renderModifierSummary(row, row.defenseOrUtilityModifiers()))
                .append("</td><td>")
                .append(renderModifierSummary(row, row.manualReviewModifiers()))
                .append("</td><td>")
                .append(renderCooldownCell(row))
                .append("</td><td>")
                .append(escapeHtml(row.getActivationRequirementsSummary()))
                .append("</td></tr>")
                .toString();
    }

    private static String renderResourceGenerationCell(DamageRankingRow row) {
        if (row.getResourceGenerationBonusKnown() == null) {
            return escapeHtml(row.getResourceGenerationSummary());
        }
        String summary = row.getResourceGenerationBaseSummary() + "; " + row.getResourceType().generationLabel();
        String tooltip = row.resourceModifiers().stream()
                .filter(modifier -> modifier.getUpgradeName().equals(row.getResourceType().generationLabel()))
                .findFirst()
                .map(DamageRankingPageRenderer::modifierTooltip)
                .orElse("Modyfikator: " + row.getResourceType().generationLabel() + " — dodatkowe "
                        + row.getResourceGenerationBonusKnown()
                        + " " + row.getResourceType().unitLabel() + ".");
        return "<span class=\"ranking-tooltip\" title=\"" + escapeHtml(tooltip) + "\" aria-label=\"" + escapeHtml(tooltip) + "\">"
                + escapeHtml(summary)
                + "</span>";
    }

    private static String renderLuckyHitCell(DamageRankingRow row) {
        if (row.getLuckyHitPercent() == null) {
            return "<span class=\"missing-source-value ranking-tooltip\" title=\"Brak źródłowej wartości Lucky Hit\" aria-label=\"Brak źródłowej wartości Lucky Hit\">-</span>";
        }
        String tooltip = "Umiejętność: "
                + row.getEntry().getSkillName()
                + " — Lucky Hit "
                + row.getLuckyHitPercent()
                + "%.";
        return "<span class=\"ranking-tooltip\" title=\"" + escapeHtml(tooltip) + "\" aria-label=\"" + escapeHtml(tooltip) + "\">"
                + escapeHtml(row.getLuckyHitSummary())
                + "</span>";
    }

    private static String renderCooldownCell(DamageRankingRow row) {
        if (row.getChargeData() == null) return escapeHtml(row.getCooldownSummary());
        String variants = row.cooldownOrCastSpeedModifiers().stream()
                .map(modifier -> modifier.getUpgradeGroup() + " / " + modifier.getUpgradeName() + ": "
                        + modifier.getValue() + ". " + modifier.getRankingTooltipDescription())
                .collect(java.util.stream.Collectors.joining(" "));
        String tooltip = "Bazowe ładunki: " + row.getCooldownSummary()
                + ". Alternatywne ulepszenia zmieniają wyłącznie wybrany wariant. " + variants;
        return "<span class=\"ranking-tooltip\" title=\"" + escapeHtml(tooltip) + "\" aria-label=\""
                + escapeHtml(tooltip) + "\">" + escapeHtml(row.getCooldownSummary()) + "</span>";
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
                            .append("%");
                    Integer durationSeconds = componentTable.durationSecondsFor(component);
                    if (durationSeconds != null) {
                        html.append(" w ciągu ").append(durationSeconds).append(" s");
                    }
                    html.append(componentTiming(componentTable, component));
                    html.append("</li>");
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
        if (row.getEntry().getVerificationStatus() == SkillDamageVerificationStatus.NON_DAMAGE) {
            return true;
        }
        return row.getType() != SkillTreeType.DAMAGE && row.getType() != SkillTreeType.SPECIAL;
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

    private static String renderModifierSummary(DamageRankingRow row, List<UpgradeDamageModifier> modifiers) {
        if (modifiers.isEmpty()) {
            return "<span class=\"missing-source-value ranking-tooltip\" title=\"Brak bezpośredniego wpływu w tej kategorii\" aria-label=\"Brak bezpośredniego wpływu w tej kategorii\">-</span>";
        }
        StringBuilder html = new StringBuilder("<ul class=\"compact-list facet-list\">");
        for (UpgradeDamageModifier modifier : modifiers) {
            html.append("<li class=\"ranking-tooltip\" title=\"")
                    .append(escapeHtml(modifierTooltip(modifier)))
                    .append("\" aria-label=\"")
                    .append(escapeHtml(modifierTooltip(modifier)))
                    .append("\">")
                    .append(renderModifierName(modifier))
                    .append("</li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private static String renderModifierName(UpgradeDamageModifier modifier) {
        return "<span class=\"facet-name\">" + escapeHtml(modifier.getUpgradeName()) + "</span>";
    }

    private static boolean isConcreteValue(String value) {
        return !value.equals("brak")
                && !value.equals("tekst źródłowy")
                && !value.equals("brak bezpośredniego damage")
                && !value.equals("status");
    }

    private static String shortModifierDescription(UpgradeDamageModifier modifier) {
        if (modifier.getSafeForRankingDisplay() == UpgradeDamageSafety.NEEDS_MANUAL_REVIEW) {
            return "wymaga weryfikacji";
        }
        if (isConcreteValue(modifier.getValue())) {
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
            case STATUS_DAMAGE_ENABLER -> "status";
            case CAST_SPEED_OR_COOLDOWN -> "tempo użycia / cooldown";
            case RESOURCE_OR_COST -> "zasób / koszt";
            case DEFENSE_OR_UTILITY -> "efekt";
            case NO_DAMAGE_IMPACT -> "-";
            case NEEDS_MANUAL_REVIEW -> "wymaga weryfikacji";
        };
    }

    private static String valueSuffix(UpgradeDamageModifier modifier) {
        if (modifier.getType() == UpgradeDamageModifierType.CAST_SPEED_OR_COOLDOWN) {
            return ", tempo użycia";
        }
        if (modifier.getType() == UpgradeDamageModifierType.RESOURCE_OR_COST) {
            return ", zasób";
        }
        if (modifier.createsNewDamageComponent()) {
            return ", nowy komponent";
        }
        return "";
    }

    private static String modifierTooltip(UpgradeDamageModifier modifier) {
        return modifier.getRankingTooltipSourceLabel()
                + ": "
                + modifier.getUpgradeName()
                + " — "
                + modifier.getRankingTooltipDescription()
                + variantTableTooltip(modifier.getComponentDamagePercentRanks());
    }

    private static String variantTableTooltip(DamagePercentComponentRankTable tables) {
        if (tables.isEmpty()) return "";
        StringBuilder text = new StringBuilder(" Tabela wariantu: ");
        for (DamagePercentComponent component : DamagePercentComponent.values()) {
            if (!tables.hasComponent(component)) continue;
            Integer duration = tables.durationSecondsFor(component);
            String durationText = (duration == null ? "" : " w ciągu " + duration + " s") + componentTiming(tables, component);
            text.append(component.name()).append("; R1 ").append(tables.damagePercentAt(component, 1)).append("%")
                    .append(durationText).append("; max-view ").append(tables.damagePercentAt(component, 15)).append("%")
                    .append(durationText).append(". ");
        }
        return text.toString().stripTrailing();
    }

    private static String componentTiming(DamagePercentComponentRankTable tables, DamagePercentComponent component) {
        Integer interval = tables.intervalSecondsFor(component);
        String text = interval == null ? "" : " co " + interval + " s";
        return text + (component == DamagePercentComponent.EVENT_TRIGGERED_DAMAGE ? " na zdarzenie" : "");
    }

    private static String renderStatus(SkillDamageVerificationStatus status) {
        return "<span class=\"status-badge " + statusCssClass(status) + "\">"
                + escapeHtml(status.name())
                + "</span>";
    }

    private static String statusCssClass(SkillDamageVerificationStatus status) {
        return switch (status) {
            case SUPPORTED, PARTIAL -> "status-active";
            case NEEDS_VERIFICATION -> "status-warning";
            case UNSUPPORTED -> "status-error";
            case NON_DAMAGE -> "status-inactive";
        };
    }

    private static String verificationRowCssClass(SkillDamageVerificationStatus status) {
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

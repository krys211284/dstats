package krys.web;

import krys.app.CurrentBuildCalculation;
import krys.hero.HeroClassDefs;
import krys.item.HeroEquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.HeroSlotItemAssignment;
import krys.itemlibrary.ItemLibraryPresentationSupport;
import krys.itemlibrary.SavedImportedItem;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Renderuje pojedynczy szablon HTML dla prostego SSR bez zewnętrznego frameworka webowego. */
public final class CurrentBuildPageRenderer {
    private static final List<HeroEquipmentSlot> LEFT_PAPER_DOLL_SLOTS = List.of(
            HeroEquipmentSlot.HELMET,
            HeroEquipmentSlot.CHEST,
            HeroEquipmentSlot.GLOVES,
            HeroEquipmentSlot.PANTS,
            HeroEquipmentSlot.BOOTS,
            HeroEquipmentSlot.MAIN_HAND
    );
    private static final List<HeroEquipmentSlot> RIGHT_PAPER_DOLL_SLOTS = List.of(
            HeroEquipmentSlot.AMULET,
            HeroEquipmentSlot.RING_LEFT,
            HeroEquipmentSlot.RING_RIGHT,
            HeroEquipmentSlot.OFF_HAND
    );

    private final String template;

    public CurrentBuildPageRenderer() {
        this.template = loadTemplate();
    }

    public String render(CurrentBuildPageModel model) {
        return template
                .replace("{{APP_SHELL_STYLES}}", AppShellRendererSupport.renderSharedStyles())
                .replace("{{GLOBAL_NAV}}", AppShellRendererSupport.renderGlobalNavigation("/policz-aktualny-build"))
                .replace("{{HERO_CONTEXT}}", renderHeroContext(model))
                .replace("{{FORM_MESSAGES}}", renderMessages(model.getMessages()))
                .replace("{{ENTRY_SECTION}}", renderEntrySection(model))
                .replace("{{FORM_ERRORS}}", renderErrors(model.getValidationErrors()))
                .replace("{{RESULT_SECTION}}", renderResultSection(model));
    }

    private static String renderHeroContext(CurrentBuildPageModel model) {
        if (!model.hasActiveHero()) {
            return """
                    <section class="panel panel-warning">
                        <h2>Brak aktywnego bohatera</h2>
                        <p>Ten ekran działa w kontekście aktywnego bohatera. Utwórz pierwszego bohatera albo wybierz istniejącego, aby zarządzać ekwipunkiem, skillami, paskiem akcji i wynikiem symulacji.</p>
                        <div class="hero-links">
                            <a class="nav-link" href="/bohaterowie">Przejdź do modułu Bohaterowie</a>
                        </div>
                    </section>
                    """;
        }

        HeroProfile activeHero = model.getActiveHero();
        return new StringBuilder("""
                <details class="current-build-details hero-context-details">
                    <summary>Aktywny bohater</summary>
                    <section class="hero-context-panel hero-context-panel-compact">
                        <div class="hero-context-head">
                        <div>
                            <p class="helper">Pracujesz bezpośrednio na stanie bohatera: jego ekwipunku, przypisanych umiejętnościach, pasku akcji i ręcznych nadpisaniach statów.</p>
                        </div>
                        <a class="nav-link secondary-link" href="
                """)
                .append(escapeHtml(model.getHeroesUrl()))
                .append("\">Zarządzaj bohaterami</a></div><div class=\"hero-context-grid\">")
                .append(renderSummaryCard("Nazwa bohatera", activeHero.getName()))
                .append(renderSummaryCard("Klasa postaci", HeroClassDefs.get(activeHero.getHeroClass()).getDisplayName()))
                .append(renderSummaryCard("Poziom bohatera", model.getFormData().getLevel()))
                .append(renderSummaryCard("Przypisane umiejętności", Integer.toString(model.getAssignedSkillIds().size())))
                .append("""
                    </div>
                    <div class="hero-context-inline-actions hero-context-inline-actions-single">
                        <form method="post" action="/policz-aktualny-build" class="inline-action-form">
                            <input type="hidden" name="heroAction" value="setActiveHeroInline">
                            <label>
                                Zmień aktywnego bohatera
                                <select name="selectedHeroId">
                """)
                .append(renderHeroSelectOptions(model.getHeroes(), activeHero.getHeroId()))
                .append("""
                                </select>
                            </label>
                            <button type="submit">Ustaw aktywnego</button>
                        </form>
                    </div>
                    </section>
                </details>
                """)
                .toString();
    }

    private static String renderHeroSelectOptions(List<HeroProfile> heroes, long activeHeroId) {
        StringBuilder html = new StringBuilder();
        for (HeroProfile hero : heroes) {
            html.append("<option value=\"")
                    .append(hero.getHeroId())
                    .append("\"")
                    .append(hero.getHeroId() == activeHeroId ? " selected" : "")
                    .append(">")
                    .append(escapeHtml(hero.getName()))
                    .append(" (")
                    .append(escapeHtml(HeroClassDefs.get(hero.getHeroClass()).getDisplayName()))
                    .append(")")
                    .append("</option>");
        }
        return html.toString();
    }

    private static String renderMessages(List<String> messages) {
        if (messages.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("""
                <section class="panel panel-success">
                    <h2>Stan aktualnego buildu</h2>
                    <ul class="message-list">
                """);
        for (String message : messages) {
            html.append("<li>").append(escapeHtml(message)).append("</li>");
        }
        html.append("""
                    </ul>
                </section>
                """);
        return html.toString();
    }

    private static String renderErrors(List<String> errors) {
        if (errors.isEmpty()) {
            return "";
        }

        StringBuilder html = new StringBuilder("""
                <section class="panel panel-error">
                    <h2>Błędy formularza</h2>
                    <ul class="error-list">
                """);
        for (String error : errors) {
            html.append("<li>").append(escapeHtml(error)).append("</li>");
        }
        html.append("""
                    </ul>
                </section>
                """);
        return html.toString();
    }

    private static String renderEntrySection(CurrentBuildPageModel model) {
        if (!model.hasActiveHero()) {
            return "";
        }
        return """
                <section class="panel panel-build-workspace">
                    <form method="post" action="/policz-aktualny-build">
                """
                + renderStickyFormActions()
                + renderSkillPointSection(model)
                + renderAssignedSkillsSection(model)
                + renderActionBarSection(model)
                + renderEquipmentSection(model)
                + """
                        <details class="current-build-details advanced-details">
                            <summary>Zaawansowane: ręczne nadpisanie statów</summary>
                """
                + renderManualBaseSection(model)
                + """
                        </details>
                """
                + renderEffectiveStatsSection(model)
                + """
                    </form>
                </section>
                """;
    }

    private static String renderStickyFormActions() {
        return """
                <div class="current-build-sticky-actions">
                    <div>
                        <strong>Zapis konfiguracji</strong>
                        <p>Główne pola aktualnego buildu zapisują się jednym przyciskiem.</p>
                    </div>
                    <div class="current-build-sticky-buttons">
                        <button type="submit">Zapisz zmiany</button>
                        <a class="nav-link secondary-link" href="/policz-aktualny-build">Wycofaj zmiany</a>
                    </div>
                </div>
                """;
    }

    private static String renderEquipmentSection(CurrentBuildPageModel model) {
        String currentBuildQuery = CurrentBuildFormQuerySupport.toQuery(model.getFormData());
        StringBuilder html = new StringBuilder("""
                <details class="current-build-details equipment-details">
                    <summary>Ekwipunek aktualnego buildu</summary>
                    <section class="layer-panel layer-panel-hero">
                    <div class="layer-heading">
                        <span class="layer-index">1</span>
                        <div>
                            <h3>Ekwipunek aktualnego buildu</h3>
                            <p class="helper">Układ slotów jest teraz zorganizowany jak ekran bohatera: lewa i prawa strona ekwipunku. Wybór itemu nadal tylko steruje przypisaniem bibliotecznego itemu do konkretnego bohatera przed tym samym runtime.</p>
                        </div>
                    </div>
                    <div class="equipment-top-actions">
                """);
        html.append("<a class=\"nav-link\" href=\"")
                .append(escapeHtml(model.getItemLibraryUrl()))
                .append("\">Otwórz bibliotekę itemów</a><a class=\"nav-link secondary-link\" href=\"")
                .append(escapeHtml(buildItemImportUrl(currentBuildQuery)))
                .append("\">Importuj nowy item</a></div>");
        html.append("""
                    <div class="equipment-paperdoll">
                        <div class="equipment-column equipment-column-left">
                """);
        for (HeroEquipmentSlot slot : LEFT_PAPER_DOLL_SLOTS) {
            html.append(renderEquipmentSlot(model, slot));
        }
        html.append("""
                        </div>
                        <div class="equipment-column equipment-column-right">
                """);
        for (HeroEquipmentSlot slot : RIGHT_PAPER_DOLL_SLOTS) {
            html.append(renderEquipmentSlot(model, slot));
        }
        html.append("""
                        </div>
                    </div>
                """)
                .append(renderUsedItemsSection(model))
                .append("</section></details>");
        return html.toString();
    }

    private static String renderEquipmentSlot(CurrentBuildPageModel model, HeroEquipmentSlot slot) {
        HeroSlotItemAssignment activeItem = findActiveItem(model, slot);
        List<SavedImportedItem> slotItems = model.getSavedLibraryItems().stream()
                .filter(item -> slot.supports(item.getSlot()))
                .sorted(Comparator.comparingLong(SavedImportedItem::getItemId))
                .toList();
        String currentBuildQuery = CurrentBuildFormQuerySupport.toQuery(model.getFormData());

        StringBuilder html = new StringBuilder("<article class=\"equipment-slot equipment-slot-")
                .append(slot.name().toLowerCase(Locale.ROOT))
                .append("\"><div class=\"slot-header\"><div><span class=\"slot-kicker\">Slot bohatera</span><h4>")
                .append(escapeHtml(ItemLibraryPresentationSupport.heroSlotDisplayName(slot)))
                .append("</h4></div>")
                .append(renderSlotStatusBadge(activeItem))
                .append("</div>");

        if (activeItem == null) {
            html.append("<p class=\"slot-item-name slot-item-empty\">Slot jest pusty</p>")
                    .append("<p class=\"slot-helper\">")
                    .append(slotItems.isEmpty()
                            ? "Nie masz jeszcze zgodnego itemu w bibliotece dla tego slotu."
                            : "W tym slocie nie ustawiono jeszcze aktywnego itemu. Możesz wybrać istniejący item z biblioteki albo zaimportować nowy.")
                    .append("</p>");
        } else {
            html.append("<p class=\"slot-item-name\">")
                    .append(escapeHtml(activeItem.getItem().getDisplayName()))
                    .append("</p><p class=\"slot-helper\">")
                    .append(escapeHtml(ItemLibraryPresentationSupport.userItemIdentifier(activeItem.getItem())))
                    .append("</p><p class=\"slot-contribution\">")
                    .append(escapeHtml(ItemLibraryPresentationSupport.shortContributionLabel(activeItem.getItem())))
                    .append("</p>");
        }

        if (!slotItems.isEmpty()) {
            html.append("<label class=\"slot-select-label\">")
                    .append(activeItem == null ? "Wybierz z biblioteki" : "Zmień item")
                    .append("<select name=\"selectedItemId_")
                    .append(slot.name())
                    .append("\">")
                    .append(renderSlotOption("", activeItem == null ? "Wybierz zapisany item z biblioteki" : "Pozostaw bez zmiany", false));
            for (SavedImportedItem item : slotItems) {
                boolean selected = activeItem != null && item.getItemId() == activeItem.getItem().getItemId();
                html.append(renderSlotOption(Long.toString(item.getItemId()), buildSlotOptionLabel(item), selected));
            }
            html.append("</select></label>");
        }

        html.append("<div class=\"slot-actions\">");
        if (!slotItems.isEmpty()) {
            html.append("<button type=\"submit\" name=\"slotAction\" value=\"setActiveSlotItem:")
                    .append(slot.name())
                    .append("\">")
                    .append(activeItem == null ? "Wybierz z biblioteki" : "Zmień item")
                    .append("</button>");
        } else {
            html.append("<a class=\"nav-link secondary-link\" href=\"")
                    .append(escapeHtml(model.getItemLibraryUrl()))
                    .append("\">Wybierz z biblioteki</a>");
        }
        if (activeItem != null) {
            html.append("<button type=\"submit\" name=\"slotAction\" value=\"clearActiveSlotItem:")
                    .append(slot.name())
                    .append("\" class=\"secondary-button\">Wyczyść slot</button>");
        }
        html.append("<a class=\"nav-link secondary-link\" href=\"")
                .append(escapeHtml(buildItemImportUrl(currentBuildQuery)))
                .append("\">Importuj nowy item</a>");
        html.append("</div></article>");
        return html.toString();
    }

    private static String renderSkillPointSection(CurrentBuildPageModel model) {
        HeroSkillPointBudget budget = model.getSkillPointBudget();
        if (budget == null) {
            return "";
        }
        String budgetStatus = budget.isValid()
                ? "Konfiguracja mieści się w budżecie punktów umiejętności."
                : "Konfiguracja punktów umiejętności wymaga poprawy przed uznaniem buildu za legalny.";
        return new StringBuilder("""
                <details class="current-build-details skill-point-details" open>
                    <summary>Punkty umiejętności</summary>
                    <section class="subpanel skill-point-panel">
                    <p class="helper">Budżet waliduje konfigurację bohatera: poziom 70 daje 69 punktów, a zadania mogą dodać maksymalnie 14 punktów.</p>
                    <div class="form-grid skill-point-fields">
                        <label>
                            Poziom bohatera
                            <input type="number" min="1" max="70" step="1" name="level" value=\"""")
                .append(escapeHtml(model.getFormData().getLevel()))
                .append("""
                ">
                        </label>
                        <label>
                            Dodatkowe punkty z zadań
                            <input type="number" min="0" max="14" step="1" name="questSkillPoints" value=\"""")
                .append(escapeHtml(model.getFormData().getQuestSkillPoints()))
                .append("""
                ">
                        </label>
                    </div>
                    <div class="summary-grid compact-grid skill-point-summary">
                """)
                .append(renderSummaryCard("Punkty z poziomu", Integer.toString(budget.getLevelSkillPoints())))
                .append(renderSummaryCard("Dodatkowe punkty z zadań", formatNullableInteger(budget.getQuestSkillPoints())))
                .append(renderSummaryCard("Dostępne punkty", Integer.toString(budget.getAvailableSkillPoints())))
                .append(renderSummaryCard("Wydane punkty", Integer.toString(budget.getSpentSkillPoints())))
                .append(renderSummaryCard("Pozostałe punkty", Integer.toString(budget.getRemainingSkillPoints())))
                .append("</div><p class=\"skill-point-status ")
                .append(budget.isValid() ? "skill-point-status-ok" : "skill-point-status-error")
                .append("\">")
                .append(escapeHtml(budgetStatus))
                .append("</p></section></details>")
                .toString();
    }

    private static String renderUsedItemsSection(CurrentBuildPageModel model) {
        StringBuilder html = new StringBuilder("""
                <details class="current-build-details used-items-details">
                    <summary>Szczegóły użytych itemów</summary>
                    <section class="subpanel used-items-panel">
                    <p class="helper">Tutaj widać dokładnie, które aktywne itemy z biblioteki składają się na aktualny build i jaki jest ich łączny wkład do wejścia obliczeń.</p>
                    <div class="summary-grid compact-grid">
                """);
        CurrentBuildImportableStats contribution = model.getActiveLibraryContribution();
        html.append(renderSummaryCard("Łączne obrażenia broni", Long.toString(contribution.getWeaponDamage())))
                .append(renderSummaryCard("Łączna siła", ItemLibraryPresentationSupport.formatWhole(contribution.getStrength())))
                .append(renderSummaryCard("Łączna inteligencja", ItemLibraryPresentationSupport.formatWhole(contribution.getIntelligence())))
                .append(renderSummaryCard("Łączne kolce", ItemLibraryPresentationSupport.formatWhole(contribution.getThorns())))
                .append(renderSummaryCard("Łączna szansa bloku [%]", formatPercentage(contribution.getBlockChance())))
                .append(renderSummaryCard("Łączna szansa retribution [%]", formatPercentage(contribution.getRetributionChance())))
                .append("</div>");

        if (!model.hasActiveLibraryItems()) {
            html.append("<p class=\"helper\">Brak aktywnych itemów z biblioteki dla aktualnego buildu.</p></section></details>");
            return html.toString();
        }

        html.append("<table class=\"data-table\"><thead><tr><th>Slot</th><th>Nazwa itemu</th><th>Identyfikator / źródło</th><th>Wkład do buildu</th></tr></thead><tbody>");
        for (HeroSlotItemAssignment assignment : model.getActiveLibraryItems()) {
            html.append("<tr><td>")
                    .append(escapeHtml(ItemLibraryPresentationSupport.heroSlotDisplayName(assignment.getHeroSlot())))
                    .append("</td><td>")
                    .append(escapeHtml(assignment.getItem().getDisplayName()))
                    .append("</td><td>")
                    .append(escapeHtml(ItemLibraryPresentationSupport.userItemIdentifier(assignment.getItem())))
                    .append("</td><td>")
                    .append(escapeHtml(ItemLibraryPresentationSupport.itemContributionLabel(assignment.getItem())))
                    .append("</td></tr>");
        }
        html.append("</tbody></table></section></details>");
        return html.toString();
    }

    private static String renderEffectiveStatsSection(CurrentBuildPageModel model) {
        StringBuilder html = new StringBuilder("""
                <details class="current-build-details effective-stats-details">
                    <summary>Efektywne staty do obliczeń</summary>
                    <section class="layer-panel layer-panel-emphasis">
                    <div class="layer-heading">
                        <span class="layer-index">2</span>
                        <div>
                            <p class="helper">To te finalne staty trafiają do pipeline’u `efektywne staty -&gt; CurrentBuildRequest -&gt; CurrentBuildSnapshotFactory -&gt; runtime`. Sekcja nie buduje alternatywnego flow, tylko pokazuje końcowy stan wejścia do obliczeń.</p>
                        </div>
                    </div>
                    <div class="formula-strip">Ręczne nadpisania statów + aktywne itemy per slot = efektywne staty do obliczeń</div>
                """);
        if (model.getEffectiveStats() == null) {
            html.append("<p class=\"helper\">Efektywne staty nie są jeszcze dostępne, bo ręczna baza zawiera błędy walidacji.</p></section></details>");
            return html.toString();
        }

        CurrentBuildImportableStats effectiveStats = model.getEffectiveStats();
        html.append("<div class=\"summary-grid compact-grid\">")
                .append(renderSummaryCard("Obrażenia broni", Long.toString(effectiveStats.getWeaponDamage())))
                .append(renderSummaryCard("Siła", ItemLibraryPresentationSupport.formatWhole(effectiveStats.getStrength())))
                .append(renderSummaryCard("Inteligencja", ItemLibraryPresentationSupport.formatWhole(effectiveStats.getIntelligence())))
                .append(renderSummaryCard("Kolce", ItemLibraryPresentationSupport.formatWhole(effectiveStats.getThorns())))
                .append(renderSummaryCard("Szansa bloku [%]", formatPercentage(effectiveStats.getBlockChance())))
                .append(renderSummaryCard("Szansa retribution [%]", formatPercentage(effectiveStats.getRetributionChance())))
                .append("</div>")
                .append("<p class=\"helper\">Do runtime trafiają: obrażenia broni=")
                .append(escapeHtml(Long.toString(effectiveStats.getWeaponDamage())))
                .append(", siła=")
                .append(escapeHtml(ItemLibraryPresentationSupport.formatWhole(effectiveStats.getStrength())))
                .append(", inteligencja=")
                .append(escapeHtml(ItemLibraryPresentationSupport.formatWhole(effectiveStats.getIntelligence())))
                .append(", kolce=")
                .append(escapeHtml(ItemLibraryPresentationSupport.formatWhole(effectiveStats.getThorns())))
                .append(", szansa bloku=")
                .append(escapeHtml(formatPercentage(effectiveStats.getBlockChance())))
                .append(", szansa retribution=")
                .append(escapeHtml(formatPercentage(effectiveStats.getRetributionChance())))
                .append(".</p></section></details>");
        return html.toString();
    }

    private static String renderAssignedSkillsSection(CurrentBuildPageModel model) {
        StringBuilder html = new StringBuilder("""
                <details class="current-build-details assigned-skills-details" open>
                    <summary>Umiejętności bohatera</summary>
                    <section class="subpanel">
                    <div class="section-head-inline">
                        <div>
                            <p class="helper">Current build renderuje i edytuje tylko umiejętności przypisane do aktywnego bohatera. To nadal tylko stan wejściowy do tego samego runtime.</p>
                        </div>
                    </div>
                """);

        List<SkillId> addableSkills = new ArrayList<>();
        for (SkillId skillId : SkillId.values()) {
            if (!model.getAssignedSkillIds().contains(skillId)) {
                addableSkills.add(skillId);
            }
        }

        if (!addableSkills.isEmpty()) {
            html.append("""
                    <div class="skill-toolbar">
                        <label>
                            Dodaj umiejętność
                            <select name="skillIdToAdd">
                    """);
            for (SkillId skillId : addableSkills) {
                html.append("<option value=\"")
                        .append(skillId.name())
                        .append("\">")
                        .append(escapeHtml(HeroSkillCatalogAdapter.displayName(skillId)))
                        .append("</option>");
            }
            html.append("""
                            </select>
                        </label>
                        <button type="submit" name="heroAction" value="addAssignedSkill">Dodaj umiejętność</button>
                    </div>
                    """);
        }

        if (model.getAssignedSkillIds().isEmpty()) {
            html.append("<div class=\"empty-state\"><h4>Brak przypisanych umiejętności</h4><p>Dodaj pierwszą umiejętność bohatera, aby skonfigurować jej rangę i pasek akcji.</p></div></section></details>");
            return html.toString();
        }

        html.append("<div class=\"assigned-skills-grid\">");
        for (SkillId skillId : model.getAssignedSkillIds()) {
            html.append(renderAssignedSkillCard(model, skillId));
        }
        html.append("</div></section></details>");
        return html.toString();
    }

    private static String renderAssignedSkillCard(CurrentBuildPageModel model, SkillId skillId) {
        CurrentBuildFormData.SkillConfigFormData skillConfig = model.getFormData().getSkillConfig(skillId);
        HeroAssignedSkillPresentation presentation = HeroSkillCatalogAdapter.present(skillId, skillConfig);
        return new StringBuilder("<article class=\"skill-card\" data-assigned-skill-id=\"")
                .append(skillId.name())
                .append("\">")
                .append("""
                    <div class="skill-card-head">
                        <div>
                            <span class="section-kicker">Przypisana umiejętność</span>
                            <h4>""")
                .append(escapeHtml(presentation.getDisplayName()))
                .append("""
                            </h4>
                        </div>
                        <button type="submit" name="heroAction" value="removeAssignedSkill:""")
                .append(skillId.name())
                .append("\" class=\"secondary-button\">Usuń umiejętność</button></div>")
                .append(renderPaladinTreeData(presentation))
                .append("<div class=\"runtime-config-label\">Konfiguracja runtime legacy</div><div class=\"form-grid\">")
                .append("""
                        <label>
                            Ranga z punktów
                            <select name=\"""")
                .append(CurrentBuildFormData.rankFieldName(skillId))
                .append("\">")
                .append(renderRankOptions(skillConfig.getRank()))
                .append("""
                            </select>
                        </label>
                        <label>
                            Bazowe ulepszenie
                            <span class="checkbox-row">
                                <input type="checkbox" name=\"""")
                .append(CurrentBuildFormData.baseUpgradeFieldName(skillId))
                .append("\" value=\"true\" ")
                .append(skillConfig.isBaseUpgrade() ? "checked" : "")
                .append("""
>
                                Włącz bazowe ulepszenie
                            </span>
                        </label>
                        <label>
                            Dodatkowy modyfikator
                            <select name=\"""")
                .append(CurrentBuildFormData.choiceFieldName(skillId))
                .append("\">")
                .append(renderChoiceOptions(skillId, skillConfig.getChoiceUpgrade()))
                .append("""
                            </select>
                        </label>
                    </div>
                </article>
                """)
                .toString();
    }

    private static String renderPaladinTreeData(HeroAssignedSkillPresentation presentation) {
        if (!presentation.hasTreeSkill()) {
            return "";
        }
        krys.paladin.PaladinTreeSkill treeSkill = presentation.getTreeSkill();
        StringBuilder html = new StringBuilder("""
                <div class="tree-skill-data">
                    <div class="tree-skill-data-head">
                        <span class="section-kicker">Aktualne dane umiejętności</span>
                        <p class="runtime-warning">Opisowe modyfikatory z drzewa Paladyna nie są jeszcze aktywne w runtime DPS.</p>
                    </div>
                    <div class="summary-grid compact-grid">
                """);
        html.append(renderSummaryCard("Nazwa", presentation.getDisplayName()))
                .append(renderSummaryCard("Aktualna ranga", Integer.toString(presentation.getCurrentRank())))
                .append(renderSummaryCard("Kategorie z gry", treeSkill.getSkillCategoriesDisplay()));
        if (presentation.getCurrentRank() <= 0) {
            html.append("</div>")
                    .append("<p class=\"current-skill-state-message\">Ranga 0 — umiejętność przypisana, ale nieaktywna w danych bojowych.</p>")
                    .append("<p class=\"no-active-modifiers\">Brak aktywnych modyfikatorów z konfiguracji.</p>")
                    .append("</div>");
            return html.toString();
        }
        html.append(renderSummaryCard(
                        "Obrażenia na randze " + presentation.getCurrentRank(),
                        currentDamageSummary(presentation)
                ))
                .append(renderSummaryCard("Lucky Hit", formatPercent(treeSkill.getLuckyHitPercent())))
                .append(renderSummaryCard("Bazowe generowanie Wiary", baseFaithGenerationSummary(treeSkill)));
        if (treeSkill.getFaithCost() != null) {
            html.append(renderSummaryCard("Koszt Wiary", treeSkill.getFaithCost().toString()));
        }
        html.append("</div>");
        if (!presentation.getBaseEffects().isEmpty()) {
            html.append("<div class=\"tree-effect-group\"><span class=\"tree-effect-label\">Efekt bazowy umiejętności</span>")
                    .append(renderModifierList(presentation.getBaseEffects()))
                    .append("</div>");
        }
        html.append("<div class=\"tree-effect-group\"><span class=\"tree-effect-label\">Aktywne modyfikatory z konfiguracji</span>");
        if (presentation.getActiveModifiers().isEmpty()) {
            html.append("<p class=\"no-active-modifiers\">Brak aktywnych modyfikatorów z konfiguracji.</p>");
        } else {
            html.append(renderModifierList(presentation.getActiveModifiers()));
        }
        html.append("</div>");
        html.append("</div>");
        return html.toString();
    }

    private static String renderModifierList(List<HeroAssignedSkillPresentation.ModifierPresentation> modifiers) {
        StringBuilder html = new StringBuilder("<ul class=\"tree-modifier-list\">");
        for (HeroAssignedSkillPresentation.ModifierPresentation modifier : modifiers) {
            html.append("<li class=\"tree-modifier\" title=\"")
                    .append(escapeHtml(modifier.getTooltip()))
                    .append("\" aria-label=\"")
                    .append(escapeHtml(modifier.getTooltip()))
                    .append("\">")
                    .append(escapeHtml(modifier.getName()))
                    .append("</li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private static String currentDamageSummary(HeroAssignedSkillPresentation presentation) {
        return presentation.getCurrentDamagePercent() == null
                ? "brak jawnej wartości dla tej rangi"
                : presentation.getCurrentDamagePercent() + "%";
    }

    private static String baseFaithGenerationSummary(krys.paladin.PaladinTreeSkill treeSkill) {
        if (treeSkill.getFaithGenerationBase() == null) {
            return "-";
        }
        return treeSkill.getFaithGenerationBase().toString();
    }

    private static String formatPercent(Integer value) {
        return value == null ? "-" : value + "%";
    }

    private static String formatNullableInteger(Integer value) {
        return value == null ? "-" : value.toString();
    }

    private static String renderActionBarSection(CurrentBuildPageModel model) {
        StringBuilder html = new StringBuilder("""
                <details class="current-build-details action-bar-details" open>
                    <summary>Pasek akcji bohatera</summary>
                    <section class="subpanel">
                    <p class="helper">Pasek akcji wybiera tylko spośród przypisanych i nauczonych umiejętności aktywnego bohatera. Jeśli konfiguracja przestaje być legalna, ekran czyści ją do bezpiecznego podzbioru.</p>
                """);
        if (model.getActionBarEligibleSkillIds().isEmpty()) {
            html.append("<div class=\"empty-state\"><h4>Brak umiejętności gotowych do paska akcji</h4><p>Podnieś rangę co najmniej jednej przypisanej umiejętności powyżej 0, aby dodać ją do paska akcji.</p></div></section></details>");
            return html.toString();
        }
        html.append("<div class=\"form-grid\">")
                .append(renderActionBarFields(model))
                .append("</div></section></details>");
        return html.toString();
    }

    private static String renderManualBaseSection(CurrentBuildPageModel model) {
        CurrentBuildImportableStats manualBaseStats = resolveManualBaseStats(model);
        return new StringBuilder("""
                <section class="subpanel advanced-panel">
                    <div class="layer-heading">
                        <span class="layer-index">A</span>
                        <div>
                            <h3>Ręczne nadpisanie statów</h3>
                            <p class="helper">""")
                .append(escapeHtml(model.getChoiceHelpText()))
                .append("""
                            </p>
                        </div>
                    </div>
                    <div class="summary-grid compact-grid">
                """)
                .append(renderSummaryCard("Obrażenia broni", Long.toString(manualBaseStats.getWeaponDamage())))
                .append(renderSummaryCard("Siła", ItemLibraryPresentationSupport.formatWhole(manualBaseStats.getStrength())))
                .append(renderSummaryCard("Inteligencja", ItemLibraryPresentationSupport.formatWhole(manualBaseStats.getIntelligence())))
                .append(renderSummaryCard("Kolce", ItemLibraryPresentationSupport.formatWhole(manualBaseStats.getThorns())))
                .append(renderSummaryCard("Szansa bloku [%]", formatPercentage(manualBaseStats.getBlockChance())))
                .append(renderSummaryCard("Szansa retribution [%]", formatPercentage(manualBaseStats.getRetributionChance())))
                .append("""
                    </div>
                    <div class="form-grid">
                """)
                .append(renderBuildStatsFields(model.getFormData()))
                .append("""
                    </div>
                </section>
                """)
                .toString();
    }

    private static String renderResultSection(CurrentBuildPageModel model) {
        if (!model.hasActiveHero()) {
            return "";
        }
        if (!model.hasResult()) {
            return """
                    <details class="current-build-details result-details">
                        <summary>Wynik symulacji</summary>
                        <section class="panel result-panel">
                        <p>To jest aktualny foundation manual simulation dla trybu „Policz aktualny build”. Ustaw ekwipunek, przypisane umiejętności, pasek akcji i poziom bohatera, a potem uruchom obliczenie.</p>
                        </section>
                    </details>
                """;
        }

        CurrentBuildCalculation calculation = model.getCalculation();
        StringBuilder html = new StringBuilder("""
                <details class="current-build-details result-details" open>
                    <summary>Wynik symulacji</summary>
                    <section class="panel result-panel">
                    <div class="summary-grid">
                """);
        html.append(renderSummaryCard("Poziom", Integer.toString(calculation.getRequest().getLevel())));
        html.append(renderSummaryCard("Efektywne obrażenia broni", Long.toString(calculation.getRequest().getWeaponDamage())));
        html.append(renderSummaryCard("Efektywna siła", String.format(Locale.US, "%.0f", calculation.getRequest().getStrength())));
        html.append(renderSummaryCard("Efektywna inteligencja", String.format(Locale.US, "%.0f", calculation.getRequest().getIntelligence())));
        html.append(renderSummaryCard("Horyzont symulacji", calculation.getRequest().getHorizonSeconds() + " s"));
        html.append(renderSummaryCard("Pasek akcji", CurrentBuildCalculationSectionsRenderer.buildActionBarLabel(calculation.getRequest().getActionBar())));
        html.append(renderSummaryCard("Łączne obrażenia", Long.toString(calculation.getResult().getTotalDamage())));
        html.append(renderSummaryCard("DPS", String.format(Locale.US, "%.4f", calculation.getResult().getDps())));
        html.append(renderSummaryCard("Wkład obrażeń reaktywnych", Long.toString(calculation.getResult().getTotalReactiveDamage())));
        html.append(renderSummaryCard("Judgement aktywny na końcu", calculation.getResult().isJudgementActiveAtEnd() ? "Tak" : "Nie"));
        html.append(renderSummaryCard("Resolve aktywny na końcu", calculation.getResult().isResolveActiveAtEnd() ? "Tak" : "Nie"));
        html.append(renderSummaryCard("Końcowa szansa bloku", String.format(Locale.US, "%.2f%%", calculation.getResult().getActiveBlockChanceAtEnd() * 100.0d)));
        html.append(renderSummaryCard("Końcowy bonus do kolców", String.format(Locale.US, "%.0f", calculation.getResult().getActiveThornsBonusAtEnd())));
        html.append("""
                    </div>
                </section>
                </details>
                """);
        html.append(CurrentBuildCalculationSectionsRenderer.renderDirectHitDebug(calculation));
        html.append(CurrentBuildCalculationSectionsRenderer.renderDelayedHitDebug(calculation));
        html.append(CurrentBuildCalculationSectionsRenderer.renderReactiveDebug(calculation));
        html.append(CurrentBuildCalculationSectionsRenderer.renderStepTrace(calculation));
        return html.toString();
    }

    private static String renderBuildStatsFields(CurrentBuildFormData formData) {
        return """
                <label>
                    Obrażenia broni w ręcznym nadpisaniu
                    <input type="number" step="1" name="weaponDamage" value="{{WEAPON_DAMAGE}}">
                </label>
                <label>
                    Siła w ręcznym nadpisaniu
                    <input type="number" min="0" step="1" name="strength" value="{{STRENGTH}}">
                </label>
                <label>
                    Inteligencja w ręcznym nadpisaniu
                    <input type="number" min="0" step="1" name="intelligence" value="{{INTELLIGENCE}}">
                </label>
                <label>
                    Kolce w ręcznym nadpisaniu
                    <input type="number" min="0" step="1" name="thorns" value="{{THORNS}}">
                </label>
                <label>
                    Szansa bloku w ręcznym nadpisaniu [%]
                    <input type="number" min="0" step="0.01" name="blockChance" value="{{BLOCK_CHANCE}}">
                </label>
                <label>
                    Szansa retribution w ręcznym nadpisaniu [%]
                    <input type="number" min="0" step="0.01" name="retributionChance" value="{{RETRIBUTION_CHANCE}}">
                </label>
                <label>
                    Horyzont symulacji [s]
                    <input type="number" min="1" step="1" name="horizonSeconds" value="{{HORIZON_SECONDS}}">
                </label>
                """
                .replace("{{WEAPON_DAMAGE}}", escapeHtml(formData.getWeaponDamage()))
                .replace("{{STRENGTH}}", escapeHtml(formData.getStrength()))
                .replace("{{INTELLIGENCE}}", escapeHtml(formData.getIntelligence()))
                .replace("{{THORNS}}", escapeHtml(formData.getThorns()))
                .replace("{{BLOCK_CHANCE}}", escapeHtml(formData.getBlockChance()))
                .replace("{{RETRIBUTION_CHANCE}}", escapeHtml(formData.getRetributionChance()))
                .replace("{{HORIZON_SECONDS}}", escapeHtml(formData.getHorizonSeconds()));
    }

    private static String renderActionBarFields(CurrentBuildPageModel model) {
        StringBuilder html = new StringBuilder();
        for (int slot = 1; slot <= 4; slot++) {
            html.append("""
                    <label>
                        Miejsce """).append(slot).append("""
                        <select name=\"""").append(CurrentBuildFormData.actionBarFieldName(slot)).append("\">")
                    .append(renderActionBarOptions(model.getFormData().getActionBarSlot(slot), model.getActionBarEligibleSkillIds()))
                    .append("""
                        </select>
                    </label>
                    """);
        }
        return html.toString();
    }

    private static String renderRankOptions(String selectedRank) {
        List<CurrentBuildPageModel.SelectOption> options = new ArrayList<>();
        for (int rank = 0; rank <= HeroSkillPointBudget.MAX_BOUGHT_SKILL_RANK; rank++) {
            String value = Integer.toString(rank);
            options.add(new CurrentBuildPageModel.SelectOption(value, value, value.equals(selectedRank)));
        }
        return renderOptions(options);
    }

    private static String renderChoiceOptions(SkillId skillId, String selectedChoice) {
        List<CurrentBuildPageModel.SelectOption> options = new ArrayList<>();
        options.add(new CurrentBuildPageModel.SelectOption(SkillUpgradeChoice.NONE.name(), "Brak", SkillUpgradeChoice.NONE.name().equals(selectedChoice)));
        for (SkillUpgradeChoice choiceUpgrade : PaladinSkillDefs.get(skillId).getAvailableChoiceUpgrades()) {
            if (choiceUpgrade == SkillUpgradeChoice.NONE) {
                continue;
            }
            options.add(new CurrentBuildPageModel.SelectOption(
                    choiceUpgrade.name(),
                    PaladinSkillDefs.getChoiceDisplayName(skillId, choiceUpgrade),
                    choiceUpgrade.name().equals(selectedChoice)
            ));
        }
        return renderOptions(options);
    }

    private static String renderActionBarOptions(String selectedSkillId, List<SkillId> eligibleSkills) {
        List<CurrentBuildPageModel.SelectOption> options = new ArrayList<>();
        boolean selectedSkillStillEligible = "NONE".equals(selectedSkillId);
        options.add(new CurrentBuildPageModel.SelectOption("NONE", "Brak", "NONE".equals(selectedSkillId)));
        for (SkillId skillId : eligibleSkills) {
            boolean selected = skillId.name().equals(selectedSkillId);
            if (selected) {
                selectedSkillStillEligible = true;
            }
            options.add(new CurrentBuildPageModel.SelectOption(
                    skillId.name(),
                    HeroSkillCatalogAdapter.displayName(skillId),
                    selected
            ));
        }
        if (!selectedSkillStillEligible) {
            options.set(0, new CurrentBuildPageModel.SelectOption("NONE", "Brak", true));
        }
        return renderOptions(options);
    }

    private static String renderOptions(List<CurrentBuildPageModel.SelectOption> options) {
        StringBuilder html = new StringBuilder();
        for (CurrentBuildPageModel.SelectOption option : options) {
            html.append("<option value=\"")
                    .append(escapeHtml(option.getValue()))
                    .append("\"");
            if (option.isSelected()) {
                html.append(" selected");
            }
            html.append(">")
                    .append(escapeHtml(option.getLabel()))
                    .append("</option>");
        }
        return html.toString();
    }

    private static String renderSlotOption(String value, String label, boolean selected) {
        return "<option value=\"" + escapeHtml(value) + "\"" + (selected ? " selected" : "") + ">"
                + escapeHtml(label)
                + "</option>";
    }

    private static String buildSlotOptionLabel(SavedImportedItem item) {
        return item.getDisplayName() + " | " + ItemLibraryPresentationSupport.shortContributionLabel(item);
    }

    private static String buildItemImportUrl(String currentBuildQuery) {
        if (currentBuildQuery == null || currentBuildQuery.isBlank()) {
            return "/importuj-item-ze-screena";
        }
        return "/importuj-item-ze-screena?" + currentBuildQuery;
    }

    private static String renderSlotStatusBadge(HeroSlotItemAssignment activeItem) {
        if (activeItem == null) {
            return "<span class=\"status-badge status-empty\">Pusty</span>";
        }
        return "<span class=\"status-badge status-active\">Aktywny</span>";
    }

    private static HeroSlotItemAssignment findActiveItem(CurrentBuildPageModel model, HeroEquipmentSlot slot) {
        for (HeroSlotItemAssignment item : model.getActiveLibraryItems()) {
            if (item.getHeroSlot() == slot) {
                return item;
            }
        }
        return null;
    }

    private static CurrentBuildImportableStats resolveManualBaseStats(CurrentBuildPageModel model) {
        if (model.getEffectiveCurrentBuildResolution() == null || model.getEffectiveCurrentBuildResolution().getManualBaseStats() == null) {
            return new CurrentBuildImportableStats(0L, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
        }
        return model.getEffectiveCurrentBuildResolution().getManualBaseStats();
    }

    private static String renderSummaryCard(String label, String value) {
        return CurrentBuildCalculationSectionsRenderer.renderSummaryCard(label, value);
    }

    private static String formatPercentage(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String escapeHtml(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value);
    }

    private static String loadTemplate() {
        try (InputStream inputStream = CurrentBuildPageRenderer.class.getResourceAsStream("/templates/current-build.html")) {
            if (inputStream == null) {
                throw new IllegalStateException("Brak szablonu /templates/current-build.html");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się wczytać szablonu strony M8", exception);
        }
    }
}

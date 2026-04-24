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
    private final String template;

    public CurrentBuildPageRenderer() {
        this.template = loadTemplate();
    }

    public String render(CurrentBuildPageModel model) {
        return template
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
        return new StringBuilder("""
                <section class="panel hero-context-panel">
                    <div class="layer-heading">
                        <span class="layer-index">B</span>
                        <div>
                            <h2>Aktywny bohater</h2>
                            <p class="helper">Cały ekran aktualnego buildu pracuje na stanie tego bohatera: jego ekwipunku, skillach, pasku akcji i ręcznych nadpisaniach statów.</p>
                        </div>
                    </div>
                    <div class="summary-grid compact-grid">
                """)
                .append(renderSummaryCard("Nazwa bohatera", model.getActiveHero().getName()))
                .append(renderSummaryCard("Klasa postaci", HeroClassDefs.get(model.getActiveHero().getHeroClass()).getDisplayName()))
                .append(renderSummaryCard("Poziom bohatera", model.getFormData().getLevel()))
                .append(renderSummaryCard("Aktywny kontekst", "Build bohatera"))
                .append("""
                    </div>
                    <div class="hero-links">
                        <a class="nav-link secondary-link" href="
                """)
                .append(escapeHtml(model.getHeroesUrl()))
                .append("\">Zmień aktywnego bohatera</a></div></section>")
                .toString();
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

    private static String renderResultSection(CurrentBuildPageModel model) {
        if (!model.hasActiveHero()) {
            return "";
        }
        if (!model.hasResult()) {
            return """
                    <section class="panel result-panel">
                        <h2>Wynik symulacji</h2>
                        <p>To jest aktualny foundation manual simulation dla trybu „Policz aktualny build”. Ustaw bazę ręczną, aktywne itemy, skille i pasek akcji, a potem uruchom obliczenie.</p>
                    </section>
                """;
        }

        CurrentBuildCalculation calculation = model.getCalculation();
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel">
                    <h2>Wynik symulacji</h2>
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
                """);
        html.append(CurrentBuildCalculationSectionsRenderer.renderDirectHitDebug(calculation));
        html.append(CurrentBuildCalculationSectionsRenderer.renderDelayedHitDebug(calculation));
        html.append(CurrentBuildCalculationSectionsRenderer.renderReactiveDebug(calculation));
        html.append(CurrentBuildCalculationSectionsRenderer.renderStepTrace(calculation));
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

    private static String renderEntrySection(CurrentBuildPageModel model) {
        if (!model.hasActiveHero()) {
            return "";
        }
        return """
                <section class="panel">
                    <h2>Ekran buildu bohatera</h2>
                    <form method="post" action="/policz-aktualny-build">
                """
                + renderEquipmentSection(model)
                + renderEffectiveStatsSection(model)
                + """
                        <section class="subpanel">
                            <h3>Nauczone skille</h3>
                """
                + renderSkillConfigFields(model.getFormData())
                + """
                        </section>
                        <section class="subpanel">
                            <h3>Pasek akcji</h3>
                            <div class="form-grid">
                """
                + renderActionBarFields(model.getFormData())
                + """
                            </div>
                        </section>
                        <details class="advanced-details">
                            <summary>Zaawansowane: baza ręczna i ręczne nadpisanie statów</summary>
                """
                + renderManualBaseSection(model)
                + """
                        </details>
                        <div class="submit-row">
                            <button type="submit">Policz aktualny build</button>
                            <button type="submit" formmethod="get" formaction="/importuj-item-ze-screena" class="secondary-button">Importuj item dla aktywnego bohatera</button>
                        </div>
                    </form>
                </section>
                """;
    }

    private static String renderEquipmentSection(CurrentBuildPageModel model) {
        StringBuilder html = new StringBuilder("""
                <section class="layer-panel layer-panel-hero">
                    <div class="layer-heading">
                        <span class="layer-index">1</span>
                        <div>
                            <h3>Ekwipunek aktualnego buildu</h3>
                            <p class="helper">To jest pełny ekran slotów aktywnego bohatera. Wybór itemu w slocie steruje wyłącznie przypisaniem bibliotecznego itemu do konkretnego bohatera i nadal kończy się tym samym pipeline’em runtime.</p>
                        </div>
                    </div>
                    <div class="equipment-shell">
                        <div class="equipment-board">
                """);
        for (HeroEquipmentSlot slot : HeroEquipmentSlot.values()) {
            html.append(renderEquipmentSlot(model, slot));
        }
        html.append("""
                        </div>
                        <aside class="equipment-side-panel">
                            <h4>Biblioteka i import</h4>
                            <p class="helper">Źródłem itemów pozostaje wspólna biblioteka, ale aktywna selekcja per slot należy do tego bohatera. Możesz szybko przejść do biblioteki albo zaimportować kolejny item, a potem wrócić tutaj.</p>
                            <div class="hero-links">
                                <a class="nav-link" href="
                """)
                .append(escapeHtml(model.getItemLibraryUrl()))
                .append("\">Otwórz bibliotekę itemów</a><a class=\"nav-link secondary-link\" href=\"")
                .append(escapeHtml(model.getItemImportUrl()))
                .append("\">Importuj item ze screena</a></div></aside></div>")
                .append(renderUsedItemsSection(model))
                .append("</section>");
        return html.toString();
    }

    private static String renderEquipmentSlot(CurrentBuildPageModel model, HeroEquipmentSlot slot) {
        HeroSlotItemAssignment activeItem = findActiveItem(model, slot);
        List<SavedImportedItem> slotItems = model.getSavedLibraryItems().stream()
                .filter(item -> slot.supports(item.getSlot()))
                .sorted(Comparator.comparingLong(SavedImportedItem::getItemId))
                .toList();

        StringBuilder html = new StringBuilder("<article class=\"equipment-slot equipment-slot-")
                .append(slot.name().toLowerCase(Locale.ROOT))
                .append("\"><div class=\"slot-header\"><div><span class=\"slot-kicker\">")
                .append("Slot bohatera")
                .append("</span><h4>")
                .append(escapeHtml(ItemLibraryPresentationSupport.heroSlotDisplayName(slot)))
                .append("</h4></div>")
                .append(renderSlotStatusBadge(activeItem))
                .append("</div>");

        if (activeItem == null) {
            html.append("<p class=\"slot-item-name slot-item-empty\">Slot jest pusty</p>")
                    .append("<p class=\"slot-helper\">")
                    .append(slotItems.isEmpty()
                            ? "Brak zapisanych itemów dla tego slotu. Zaimportuj item albo zapisz go w bibliotece."
                            : "W tym slocie nie ustawiono jeszcze aktywnego itemu.")
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

        if (slotItems.isEmpty()) {
            html.append("<div class=\"slot-empty-actions\"><a class=\"nav-link secondary-link\" href=\"")
                    .append(escapeHtml(model.getItemImportUrl()))
                    .append("\">Dodaj item do biblioteki</a></div></article>");
            return html.toString();
        }

        html.append("<label class=\"slot-select-label\">")
                .append(activeItem == null ? "Wybierz item dla slotu" : "Zmień item dla slotu")
                .append("<select name=\"selectedItemId_")
                .append(slot.name())
                .append("\">")
                .append(renderSlotOption("", activeItem == null ? "Wybierz zapisany item" : "Pozostaw bez zmiany", false));
        for (SavedImportedItem item : slotItems) {
            boolean selected = activeItem != null && item.getItemId() == activeItem.getItem().getItemId();
            html.append(renderSlotOption(Long.toString(item.getItemId()), buildSlotOptionLabel(item), selected));
        }
        html.append("</select></label><div class=\"slot-actions\">")
                .append("<button type=\"submit\" name=\"slotAction\" value=\"setActiveSlotItem:")
                .append(slot.name())
                .append("\">")
                .append(activeItem == null ? "Wybierz item" : "Zmień item")
                .append("</button>");
        if (activeItem != null) {
            html.append("<button type=\"submit\" name=\"slotAction\" value=\"clearActiveSlotItem:")
                    .append(slot.name())
                    .append("\" class=\"secondary-button\">Wyczyść slot</button>");
        }
        html.append("</div></article>");
        return html.toString();
    }

    private static String renderUsedItemsSection(CurrentBuildPageModel model) {
        StringBuilder html = new StringBuilder("""
                <section class="subpanel">
                    <h3>Użyte itemy</h3>
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
            html.append("<div class=\"empty-state\"><h4>Brak użytych itemów</h4><p>Aktualny build korzysta tylko z bazy ręcznej. Ustaw aktywny item w jednym ze slotów albo przejdź do biblioteki itemów.</p></div></section>");
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
        html.append("</tbody></table></section>");
        return html.toString();
    }

    private static String renderEffectiveStatsSection(CurrentBuildPageModel model) {
        StringBuilder html = new StringBuilder("""
                <section class="layer-panel layer-panel-emphasis">
                    <div class="layer-heading">
                        <span class="layer-index">2</span>
                        <div>
                            <h3>Efektywne staty do obliczeń</h3>
                            <p class="helper">To te finalne staty trafiają do pipeline’u `efektywne staty -&gt; CurrentBuildRequest -&gt; CurrentBuildSnapshotFactory -&gt; runtime`. Sekcja nie buduje alternatywnego flow, tylko pokazuje końcowy stan wejścia do obliczeń.</p>
                        </div>
                    </div>
                    <div class="formula-strip">Baza ręczna + aktywne itemy per slot = efektywne staty do obliczeń</div>
                """);
        if (model.getEffectiveStats() == null) {
            html.append("<p class=\"helper\">Efektywne staty nie są jeszcze dostępne, bo ręczna baza zawiera błędy walidacji.</p></section>");
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
                .append(".</p></section>");
        return html.toString();
    }

    private static String renderSummaryCard(String label, String value) {
        return CurrentBuildCalculationSectionsRenderer.renderSummaryCard(label, value);
    }

    private static String renderBuildStatsFields(CurrentBuildFormData formData) {
        return """
                <label>
                    Poziom bohatera
                    <input type="number" min="1" step="1" name="level" value="{{LEVEL}}">
                </label>
                <label>
                    Obrażenia broni w bazie ręcznej
                    <input type="number" step="1" name="weaponDamage" value="{{WEAPON_DAMAGE}}">
                </label>
                <label>
                    Siła w bazie ręcznej
                    <input type="number" min="0" step="1" name="strength" value="{{STRENGTH}}">
                </label>
                <label>
                    Inteligencja w bazie ręcznej
                    <input type="number" min="0" step="1" name="intelligence" value="{{INTELLIGENCE}}">
                </label>
                <label>
                    Kolce w bazie ręcznej
                    <input type="number" min="0" step="1" name="thorns" value="{{THORNS}}">
                </label>
                <label>
                    Szansa bloku w bazie ręcznej [%]
                    <input type="number" min="0" step="0.01" name="blockChance" value="{{BLOCK_CHANCE}}">
                </label>
                <label>
                    Szansa retribution w bazie ręcznej [%]
                    <input type="number" min="0" step="0.01" name="retributionChance" value="{{RETRIBUTION_CHANCE}}">
                </label>
                <label>
                    Horyzont symulacji [s]
                    <input type="number" min="1" step="1" name="horizonSeconds" value="{{HORIZON_SECONDS}}">
                </label>
                """
                .replace("{{LEVEL}}", escapeHtml(formData.getLevel()))
                .replace("{{WEAPON_DAMAGE}}", escapeHtml(formData.getWeaponDamage()))
                .replace("{{STRENGTH}}", escapeHtml(formData.getStrength()))
                .replace("{{INTELLIGENCE}}", escapeHtml(formData.getIntelligence()))
                .replace("{{THORNS}}", escapeHtml(formData.getThorns()))
                .replace("{{BLOCK_CHANCE}}", escapeHtml(formData.getBlockChance()))
                .replace("{{RETRIBUTION_CHANCE}}", escapeHtml(formData.getRetributionChance()))
                .replace("{{HORIZON_SECONDS}}", escapeHtml(formData.getHorizonSeconds()));
    }

    private static String renderSkillConfigFields(CurrentBuildFormData formData) {
        StringBuilder html = new StringBuilder();
        for (SkillId skillId : SkillId.values()) {
            CurrentBuildFormData.SkillConfigFormData skillConfig = formData.getSkillConfig(skillId);
            html.append("""
                    <article class="subpanel">
                        <h3>""").append(escapeHtml(PaladinSkillDefs.get(skillId).getName())).append("</h3>")
                    .append("""
                        <div class="form-grid">
                            <label>
                                Ranga
                                <select name=\"""").append(CurrentBuildFormData.rankFieldName(skillId)).append("\">")
                    .append(renderRankOptions(skillConfig.getRank()))
                    .append("""
                                </select>
                            </label>
                            <label>
                                Bazowe ulepszenie
                                <span class="checkbox-row">
                                    <input type="checkbox" name=\"""").append(CurrentBuildFormData.baseUpgradeFieldName(skillId)).append("\" value=\"true\" ")
                    .append(skillConfig.isBaseUpgrade() ? "checked" : "")
                    .append("""
>
                                    Włącz bazowe ulepszenie
                                </span>
                            </label>
                            <label>
                                Dodatkowy modyfikator
                                <select name=\"""").append(CurrentBuildFormData.choiceFieldName(skillId)).append("\">")
                    .append(renderChoiceOptions(skillId, skillConfig.getChoiceUpgrade()))
                    .append("""
                                </select>
                            </label>
                        </div>
                    </article>
                    """);
        }
        return html.toString();
    }

    private static String renderActionBarFields(CurrentBuildFormData formData) {
        StringBuilder html = new StringBuilder();
        for (int slot = 1; slot <= 4; slot++) {
            html.append("""
                    <label>
                        Miejsce """).append(slot).append("""
                        <select name=\"""").append(CurrentBuildFormData.actionBarFieldName(slot)).append("\">")
                    .append(renderActionBarOptions(formData.getActionBarSlot(slot)))
                    .append("""
                        </select>
                    </label>
                    """);
        }
        return html.toString();
    }

    private static String renderRankOptions(String selectedRank) {
        List<CurrentBuildPageModel.SelectOption> options = new ArrayList<>();
        for (int rank = 0; rank <= 5; rank++) {
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

    private static String renderActionBarOptions(String selectedSkillId) {
        List<CurrentBuildPageModel.SelectOption> options = new ArrayList<>();
        options.add(new CurrentBuildPageModel.SelectOption("NONE", "Brak", "NONE".equals(selectedSkillId)));
        for (SkillId skillId : SkillId.values()) {
            options.add(new CurrentBuildPageModel.SelectOption(
                    skillId.name(),
                    PaladinSkillDefs.get(skillId).getName(),
                    skillId.name().equals(selectedSkillId)
            ));
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

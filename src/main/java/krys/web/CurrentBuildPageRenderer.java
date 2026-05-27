package krys.web;

import krys.app.CurrentBuildCalculation;
import krys.hero.HeroArmorBreakdown;
import krys.hero.HeroClassDefs;
import krys.hero.HeroCriticalChanceBreakdown;
import krys.hero.HeroClassStatBaseline;
import krys.item.HeroEquipmentSlot;
import krys.itemimport.ApplicationAspectRegistry;
import krys.itemimport.AspectDefinition;
import krys.itemimport.AspectRegistry;
import krys.itemimport.AspectRuntimeStatus;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemlibrary.CurrentHeroActiveItemStats;
import krys.itemlibrary.HeroSlotItemAssignment;
import krys.itemlibrary.ItemLibraryPresentationSupport;
import krys.itemlibrary.SavedImportedItem;
import krys.paladin.PaladinOathDefinition;
import krys.paladin.PaladinOathRegistry;
import krys.paladin.PaladinOathRuntimeStatus;
import krys.paladin.PaladinSkillTreeRegistry;
import krys.paladin.PaladinSkillUpgrade;
import krys.paladin.PaladinSkillUpgradeGroup;
import krys.paladin.PaladinTreeSkill;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;
import krys.tempering.ApplicationTemperingAffixRegistry;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingPresentationSupport;

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
    private static final AspectRegistry ASPECT_REGISTRY = ApplicationAspectRegistry.get();
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
                + renderStickyFormActions(model)
                + renderSkillPointSection(model)
                + renderAssignedSkillsSection(model)
                + renderActionBarSection(model)
                + renderPaladinOathSection(model)
                + renderBasicHeroStatsSection(model)
                + renderEquipmentSection(model)
                + """
                    </form>
                </section>
                """;
    }

    private static String renderStickyFormActions(CurrentBuildPageModel model) {
        return """
                <div class="current-build-sticky-actions">
                    <div>
                        <strong>Akcje aktualnego buildu</strong>
                        <p>Oblicz bieżącą konfigurację albo zapisz zmiany w profilu bohatera.</p>
                    </div>
                    <label class="current-build-step-count">
                        Liczba kroków symulacji
                        <input type="number" min="1" max="200" step="1" name="simulationStepCount" value="{{SIMULATION_STEP_COUNT}}">
                    </label>
                    <div class="current-build-sticky-buttons">
                        <button type="submit" name="formAction" value="calculate">Oblicz aktualny build</button>
                        <button type="submit" name="formAction" value="save">Zapisz zmiany</button>
                        <a class="nav-link secondary-link" href="/policz-aktualny-build">Wycofaj zmiany</a>
                    </div>
                </div>
                """.replace("{{SIMULATION_STEP_COUNT}}", escapeHtml(model.getFormData().getSimulationStepCount()));
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
                    .append("</p>")
                    .append(renderActiveSlotContribution(activeItem.getItem()));
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
        return new StringBuilder("""
                <details class="current-build-details skill-point-details">
                    <summary>Punkty umiejętności</summary>
                    <section class="subpanel skill-point-panel">
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
                .append("</div>")
                .append(renderSkillPointErrors(budget))
                .append("</section></details>")
                .toString();
    }

    private static String renderSkillPointErrors(HeroSkillPointBudget budget) {
        if (budget.isValid()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<div class=\"skill-point-status skill-point-status-error\"><ul class=\"error-list\">");
        for (String error : budget.getValidationErrors()) {
            html.append("<li>").append(escapeHtml(error)).append("</li>");
        }
        html.append("</ul></div>");
        return html.toString();
    }

    private static String renderAssignedSkillsSection(CurrentBuildPageModel model) {
        StringBuilder html = new StringBuilder("""
                <details class="current-build-details assigned-skills-details">
                    <summary>Umiejętności bohatera</summary>
                    <section class="subpanel">
                    <div class="section-head-inline">
                        <div>
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
                .append("<div class=\"runtime-config-label\">")
                .append(skillId == SkillId.CLASH ? "Ulepszenia Starcia" : "Konfiguracja runtime legacy")
                .append("</div>")
                .append(skillId == SkillId.CLASH
                        ? renderClashRuntimeConfiguration(skillConfig)
                        : renderDefaultRuntimeConfiguration(skillId, skillConfig))
                .append("""
                </article>
                """)
                .toString();
    }

    private static String renderDefaultRuntimeConfiguration(SkillId skillId, CurrentBuildFormData.SkillConfigFormData skillConfig) {
        return new StringBuilder("<div class=\"form-grid\">")
                .append(renderRankControl(skillId, skillConfig, ""))
                .append(renderLegacyRuntimeConfiguration(skillId, skillConfig))
                .append("</div>")
                .toString();
    }

    private static String renderRankControl(SkillId skillId, CurrentBuildFormData.SkillConfigFormData skillConfig, String cssClass) {
        return new StringBuilder("<label")
                .append(cssClass == null || cssClass.isBlank() ? "" : " class=\"" + escapeHtml(cssClass) + "\"")
                .append("""
                        >
                            Ranga z punktów
                            <select name=\"""")
                .append(CurrentBuildFormData.rankFieldName(skillId))
                .append("\">")
                .append(renderRankOptions(skillConfig.getRank()))
                .append("""
                            </select>
                        </label>
                """)
                .toString();
    }

    private static String renderLegacyRuntimeConfiguration(SkillId skillId, CurrentBuildFormData.SkillConfigFormData skillConfig) {
        return new StringBuilder("""
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
                """)
                .toString();
    }

    private static String renderClashRuntimeConfiguration(CurrentBuildFormData.SkillConfigFormData skillConfig) {
        PaladinTreeSkill clash = PaladinSkillTreeRegistry.requireSkill("starcie");
        StringBuilder html = new StringBuilder();
        html.append("<input type=\"hidden\" name=\"")
                .append(CurrentBuildFormData.choiceFieldName(SkillId.CLASH))
                .append("\" value=\"NONE\">");
        html.append("<div class=\"skill-upgrade-controls-row\">");
        html.append(renderRankControl(SkillId.CLASH, skillConfig, "skill-upgrade-control"));
        html.append(renderClashBaseEffectControl());
        for (PaladinSkillUpgradeGroup group : clash.getUpgradeGroups()) {
            int groupIndex = parseGroupIndex(group);
            if (groupIndex <= 0) {
                continue;
            }
            String selectedChoice = skillConfig.getChoiceGroup(groupIndex);
            html.append(renderClashGroupControl(group, groupIndex, selectedChoice));
        }
        html.append("</div>");
        return html.toString();
    }

    private static String renderClashBaseEffectControl() {
        return """
                        <div class="skill-upgrade-control readonly-upgrade-control">
                            <span class="control-label">Efekt bazowy</span>
                            <span class="readonly-upgrade-value">Marsz Krzyżowca</span>
                        </div>
                """;
    }

    private static String renderClashGroupControl(PaladinSkillUpgradeGroup group, int groupIndex, String selectedChoice) {
        StringBuilder html = new StringBuilder("""
                        <label class="skill-upgrade-control">
                            """)
                .append(escapeHtml(shortGroupDisplayLabel(groupIndex)))
                .append("""
                            <select name=\"""")
                .append(CurrentBuildFormData.choiceGroupFieldName(SkillId.CLASH, groupIndex))
                .append("\">")
                .append(renderClashGroupOptions(group, selectedChoice))
                .append("""
                            </select>
                        </label>
                """);
        return html.toString();
    }

    private static String renderClashGroupOptions(PaladinSkillUpgradeGroup group, String selectedChoice) {
        List<CurrentBuildPageModel.SelectOption> options = new ArrayList<>();
        options.add(new CurrentBuildPageModel.SelectOption(SkillState.NO_TREE_CHOICE, "Brak", SkillState.NO_TREE_CHOICE.equals(selectedChoice)));
        for (PaladinSkillUpgrade upgrade : group.getUpgrades()) {
            options.add(new CurrentBuildPageModel.SelectOption(upgrade.getId(), upgrade.getName(), upgrade.getId().equals(selectedChoice)));
        }
        return renderOptions(options);
    }

    private static int parseGroupIndex(PaladinSkillUpgradeGroup group) {
        return switch (group.getId()) {
            case "grupa_1" -> 1;
            case "grupa_2" -> 2;
            case "grupa_3" -> 3;
            default -> -1;
        };
    }

    private static String shortGroupDisplayLabel(int groupIndex) {
        return switch (groupIndex) {
            case 1 -> "Grupa 1";
            case 2 -> "Grupa 2";
            case 3 -> "Grupa 3";
            default -> "Grupa";
        };
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
                        <p class="runtime-warning">Efekty oznaczone jako aktywne w runtime wpływają na symulację; pozostałe modyfikatory są obecnie opisowe.</p>
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
                <details class="current-build-details action-bar-details">
                    <summary>Pasek akcji bohatera</summary>
                    <section class="subpanel">
                """);
        if (model.getActionBarEligibleSkillIds().isEmpty()) {
            html.append("<div class=\"empty-state\"><h4>Brak umiejętności gotowych do paska akcji</h4><p>Podnieś rangę co najmniej jednej przypisanej umiejętności powyżej 0, aby dodać ją do paska akcji.</p></div>")
                    .append("<div class=\"form-grid primary-resource-fields\">")
                    .append(renderPrimaryResourceFields(model.getFormData()))
                    .append("</div></section></details>");
            return html.toString();
        }
        html.append("<div class=\"form-grid\">")
                .append(renderActionBarFields(model))
                .append("</div><div class=\"form-grid primary-resource-fields\">")
                .append(renderPrimaryResourceFields(model.getFormData()))
                .append("</div></section></details>");
        return html.toString();
    }

    private static String renderBasicHeroStatsSection(CurrentBuildPageModel model) {
        CurrentHeroStatsPresentation stats = CurrentHeroStatsPresentation.from(model);
        HeroClassStatBaseline baseline = stats.getVerifiedBaseline().orElse(null);
        StringBuilder html = new StringBuilder("""
                <details class="current-build-details hero-stats-details">
                    <summary>Statystyki bohatera</summary>
                    <section class="subpanel hero-stats-panel">
                """)
                .append(renderHeroStatGroup("Główne",
                        renderSummaryCard("Klasa", stats.getHeroClassName())
                                + renderSummaryCard("Poziom", Integer.toString(stats.getLevel()))
                                + renderSummaryCard("Siła", stats.getStrengthDisplay())
                                + renderSummaryCard("Inteligencja", stats.getIntelligenceDisplay())
                                + (baseline == null ? "" : renderSummaryCard("Siła woli", Integer.toString(baseline.getWillpower()))
                                + renderSummaryCard("Zręczność", Integer.toString(baseline.getDexterity())))));
        if (baseline != null) {
            HeroArmorBreakdown armor = baseline.getArmorBreakdown();
            HeroCriticalChanceBreakdown criticalChance = baseline.getCriticalChanceBreakdown();
            boolean hasActiveWeaponDetails = stats.getActiveHeroItemStats().hasActiveWeaponDetails();
            StringBuilder offenseCards = new StringBuilder();
            if (!hasActiveWeaponDetails) {
                offenseCards.append(renderSummaryCard("Podstawowe obrażenia od broni", Long.toString(stats.getWeaponDamage())))
                        .append(renderSummaryCard("Szybkość broni", formatDecimalComma(baseline.getWeaponSpeed(), 2)));
            }
            BigDecimal activeItemCriticalChance = BigDecimal.valueOf(stats.getActiveHeroItemStats().getCriticalChancePercent());
            BigDecimal totalCriticalChance = criticalChance.getTotalCriticalChancePercent().add(activeItemCriticalChance);
            offenseCards.append(renderSummaryCardWithTooltip("Szansa na trafienie krytyczne", formatPercentComma(totalCriticalChance, 1), buildCriticalChanceBreakdownLabel(criticalChance, activeItemCriticalChance)))
                    .append(renderSummaryCard("Obrażenia od trafień krytycznych", formatPercentComma(baseline.getCriticalDamagePercent(), 1)))
                    .append(renderSummaryCard("Obrażenia zadawane odsłoniętym celom", formatPercentComma(baseline.getVulnerableDamagePercent(), 1)))
                    .append(renderSummaryCard("Ciernie", ItemLibraryPresentationSupport.formatWhole(stats.getThorns())));
            html.append(renderHeroStatGroup("Pancerz i defensywa",
                            renderSummaryCard("Wytrzymałość", Integer.toString(baseline.getToughness()))
                                    + renderSummaryCardWithTooltip("Pancerz", Integer.toString(stats.getTotalArmor(baseline)), buildArmorBreakdownLabel(armor, stats))
                                    + renderSummaryCardWithTooltip("Maksimum zdrowia", Integer.toString(stats.getMaxHealth()), buildMaxHealthBreakdownLabel(baseline, stats))
                                    + (stats.getActiveHeroItemStats().getDamageReduction() > 0.0d
                                    ? renderSummaryCard("Redukcja obrażeń z itemów", formatPercentComma(BigDecimal.valueOf(stats.getActiveHeroItemStats().getDamageReduction()), 1))
                                    : "")))
                    .append(renderActiveWeaponGroup(stats.getActiveHeroItemStats()))
                    .append(renderHeroStatGroup("Odporności",
                            renderSummaryCard("Fizyczne", Integer.toString(stats.getPhysicalResistance(baseline)))
                                    + renderSummaryCard("Ogień", Integer.toString(stats.getFireResistance(baseline)))
                                    + renderSummaryCard("Błyskawice", Integer.toString(stats.getLightningResistance(baseline)))
                                    + renderSummaryCard("Zimno", Integer.toString(stats.getColdResistance(baseline)))
                                    + renderSummaryCard("Trucizna", Integer.toString(stats.getPoisonResistance(baseline)))
                                    + renderSummaryCard("Cień", Integer.toString(stats.getShadowResistance(baseline)))))
                    .append(renderHeroStatGroup("Ofensywa", offenseCards.toString()))
                    .append(renderActiveItemAffixesGroup(stats.getActiveHeroItemStats()));
        } else {
            html.append("<p class=\"helper\">Brak jawnego baseline'u gry dla tego poziomu; UI pokazuje tylko statystyki z jawną formułą albo z aktywnych itemów.</p>");
            if (stats.getWeaponDamage() > 0L || stats.getThorns() > 0.0d
                    || stats.hasActiveItemBlockChance() || stats.hasActiveItemRetributionChance()) {
                html.append(renderHeroStatGroup("Aktywne itemy",
                        renderOptionalActiveItemStats(stats)));
            }
            html.append(renderActiveWeaponGroup(stats.getActiveHeroItemStats()))
                    .append(renderActiveItemAffixesGroup(stats.getActiveHeroItemStats()));
        }
        html.append("""
                    </section>
                </details>
                """);
        return html.toString();
    }

    private static String renderHeroStatGroup(String title, String cards) {
        return """
                <div class="hero-stat-group">
                    <h3>""" + escapeHtml(title) + """
                </h3>
                    <div class="summary-grid compact-grid">
                """ + cards + """
                    </div>
                </div>
                """;
    }

    private static String renderHeroStatListGroup(String title, String content) {
        if (content.isBlank()) {
            return "";
        }
        return """
                <div class="hero-stat-group hero-stat-list-group">
                    <h3>""" + escapeHtml(title) + """
                </h3>
                """ + content + """
                </div>
                """;
    }

    private static String buildArmorBreakdownLabel(HeroArmorBreakdown armor, CurrentHeroStatsPresentation stats) {
        return armor.getArmorFromStrength() + " z siły, "
                + (armor.getArmorFromItems() + stats.getActiveHeroItemStats().getItemArmor()) + " z itemów/głównego wyposażenia, "
                + armor.getArmorFromOtherSources() + " z innych źródeł, razem "
                + (armor.getTotalArmor() + stats.getActiveHeroItemStats().getItemArmor()) + ".";
    }

    private static String buildCriticalChanceBreakdownLabel(HeroCriticalChanceBreakdown criticalChance,
                                                            BigDecimal activeItemCriticalChance) {
        return "bazowo " + formatPercentComma(criticalChance.getBaseCriticalChancePercent(), 1)
                + ", +" + formatPercentComma(criticalChance.getCriticalChanceFromIntelligencePercent(), 1) + " z Inteligencji"
                + ", +" + formatPercentComma(criticalChance.getCriticalChanceFromItemsPercent().add(activeItemCriticalChance), 1) + " z itemów"
                + ", +" + formatPercentComma(criticalChance.getCriticalChanceFromOtherSourcesPercent(), 1) + " z innych źródeł"
                + ", razem " + formatPercentComma(criticalChance.getTotalCriticalChancePercent().add(activeItemCriticalChance), 1) + ".";
    }

    private static String buildMaxHealthBreakdownLabel(HeroClassStatBaseline baseline, CurrentHeroStatsPresentation stats) {
        return "Baseline: " + baseline.getMaxHealth()
                + "; aktywne itemy: +" + stats.getMaximumLifeFromItemsDisplay()
                + "; razem: " + stats.getMaxHealth() + ".";
    }

    private static String renderActiveWeaponGroup(CurrentHeroActiveItemStats activeItemStats) {
        if (!activeItemStats.hasActiveWeaponDetails()) {
            return "";
        }
        StringBuilder cards = new StringBuilder();
        if (activeItemStats.getWeaponDps() != null) {
            cards.append(renderSummaryCard("DPS broni", Long.toString(activeItemStats.getWeaponDps())));
        }
        if (activeItemStats.getWeaponDamageMin() != null && activeItemStats.getWeaponDamageMax() != null) {
            cards.append(renderSummaryCard("Obrażenia za trafienie",
                    activeItemStats.getWeaponDamageMin() + " - " + activeItemStats.getWeaponDamageMax()));
        }
        if (activeItemStats.getAverageWeaponDamage() != null) {
            cards.append(renderSummaryCard("Średnie obrażenia trafienia", Long.toString(activeItemStats.getAverageWeaponDamage())));
        }
        if (activeItemStats.getAttacksPerSecond() != null) {
            cards.append(renderSummaryCard("Ataki na sekundę", formatDoubleComma(activeItemStats.getAttacksPerSecond(), 2)));
        }
        return renderHeroStatGroup("Aktywna broń", cards.toString());
    }

    private static String renderActiveItemAffixesGroup(CurrentHeroActiveItemStats activeItemStats) {
        if (!activeItemStats.hasGroupedAffixes()) {
            return "";
        }
        StringBuilder content = new StringBuilder();
        content.append(renderAffixListSection("Wartości itemów", activeItemStats.getStatisticalAffixes()));
        content.append(renderAffixListSection("Efekty opisowe", activeItemStats.getDescriptiveEffectAffixes()));
        return renderHeroStatListGroup("Aktywne affixy itemów", content.toString());
    }

    private static String renderAffixListSection(String title, List<String> affixes) {
        if (affixes.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<div class=\"active-affix-section\"><h4>")
                .append(escapeHtml(title))
                .append("</h4><ul class=\"active-affix-list\">");
        for (String affix : affixes) {
            html.append("<li>").append(escapeHtml(affix)).append("</li>");
        }
        html.append("</ul></div>");
        return html.toString();
    }

    private static String renderSummaryCardWithTooltip(String label, String value, String tooltip) {
        String accessibilityLabel = label + ": " + value + ". " + tooltip;
        return "<article class=\"summary-card\" title=\"" + escapeHtml(tooltip)
                + "\" aria-label=\"" + escapeHtml(accessibilityLabel) + "\">\n"
                + "    <div class=\"summary-label\">" + escapeHtml(label) + "\n"
                + "</div>\n"
                + "    <div class=\"summary-value\">" + escapeHtml(value) + "\n"
                + "</div>\n"
                + "</article>\n";
    }

    private static String renderOptionalActiveItemStats(CurrentHeroStatsPresentation stats) {
        StringBuilder cards = new StringBuilder();
        if (stats.getWeaponDamage() > 0L) {
            cards.append(renderSummaryCard("Podstawowe obrażenia od broni", Long.toString(stats.getWeaponDamage())));
        }
        if (stats.getThorns() > 0.0d) {
            cards.append(renderSummaryCard("Ciernie", ItemLibraryPresentationSupport.formatWhole(stats.getThorns())));
        }
        if (stats.hasActiveItemBlockChance()) {
            cards.append(renderSummaryCard("Szansa na blok z aktywnych itemów [%]", formatPercentage(stats.getActiveItemStats().getBlockChance())));
        }
        if (stats.hasActiveItemRetributionChance()) {
            cards.append(renderSummaryCard("Szansa retribution z aktywnych itemów [%]", formatPercentage(stats.getActiveItemStats().getRetributionChance())));
        }
        return cards.toString();
    }

    private static String formatDecimalComma(BigDecimal value, int scale) {
        return value.setScale(scale).toPlainString().replace('.', ',');
    }

    private static String formatDoubleComma(double value, int scale) {
        return String.format(Locale.US, "%." + scale + "f", value).replace('.', ',');
    }

    private static String renderActiveSlotContribution(SavedImportedItem item) {
        String weaponSection = renderSlotContributionSection("Broń", buildSlotWeaponChips(item));
        String statsSection = renderSlotContributionSection("Wartości itemu", buildSlotStatChips(item));
        String temperingSection = renderSlotContributionSection("Hartowanie", buildSlotTemperingChips(item));
        String masterworkingSection = renderSlotContributionSection("Doskonalenie", buildSlotMasterworkingChips(item));
        String transfigurationSection = renderSlotContributionSection("Przeistoczenie", buildSlotTransfigurationChips(item));
        String socketingSection = renderSlotContributionSection("Gniazda", SocketingSectionRenderer.compactChips(item));
        String aspectSection = renderSlotContributionSection("Aspekt", buildSlotAspectChips(item));
        String effectsSection = renderSlotContributionSection("Efekty opisowe", buildSlotEffectChips(item));
        String content = weaponSection + statsSection + temperingSection + masterworkingSection + transfigurationSection + socketingSection + aspectSection + effectsSection;
        if (content.isBlank()) {
            return "<p class=\"slot-contribution\">Brak wkładu</p>";
        }
        return "<div class=\"slot-contribution-block\">" + content + "</div>";
    }

    private static String renderSlotContributionSection(String title, List<String> chips) {
        if (chips.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<div class=\"slot-contribution-section\"><span class=\"slot-contribution-title\">")
                .append(escapeHtml(title))
                .append("</span><div class=\"slot-chip-list\">");
        for (String chip : chips) {
            html.append("<span class=\"slot-chip\">").append(renderSlotChipContent(chip)).append("</span>");
        }
        html.append("</div></div>");
        return html.toString();
    }

    private static String renderSlotChipContent(String chip) {
        if (chip.contains("class=\"masterworking-value")) {
            return chip;
        }
        return escapeHtml(chip);
    }

    private static List<String> buildSlotWeaponChips(SavedImportedItem item) {
        List<String> chips = new ArrayList<>();
        if (item.getWeaponDps() != null) {
            chips.add("DPS " + item.getWeaponDps());
        }
        if (item.getWeaponDamageMin() != null && item.getWeaponDamageMax() != null) {
            chips.add(item.getWeaponDamageMin() + " - " + item.getWeaponDamageMax() + " obrażeń za trafienie");
        }
        if (item.getAttacksPerSecond() != null) {
            chips.add(formatDoubleComma(item.getAttacksPerSecond(), 2) + " ataku/s");
        }
        return chips;
    }

    private static List<String> buildSlotStatChips(SavedImportedItem item) {
        List<String> chips = new ArrayList<>();
        if (item.getItemArmor() != null) {
            chips.add("Pancerz: " + MasterworkingSectionRenderer.formatArmorReadonlyValue(item.getMasterworking(), item.getItemArmor()));
        }
        if (item.getWeaponDamage() > 0L && !hasAffix(item, ImportedItemAffixType.WEAPON_DAMAGE_FLAT)) {
            chips.add("+" + item.getWeaponDamage() + " obrażeń broni");
        }
        if (item.getStrength() > 0.0d && !hasAffix(item, ImportedItemAffixType.STRENGTH)) {
            chips.add("+" + ItemLibraryPresentationSupport.formatWhole(item.getStrength()) + " siły");
        }
        if (item.getIntelligence() > 0.0d && !hasAffix(item, ImportedItemAffixType.INTELLIGENCE)) {
            chips.add("+" + ItemLibraryPresentationSupport.formatWhole(item.getIntelligence()) + " inteligencji");
        }
        if (item.getThorns() > 0.0d && !hasAffix(item, ImportedItemAffixType.THORNS)) {
            chips.add("+" + ItemLibraryPresentationSupport.formatWhole(item.getThorns()) + " cierni");
        }
        if (item.getBlockChance() > 0.0d && !hasAffix(item, ImportedItemAffixType.BLOCK_CHANCE)) {
            chips.add("+" + ItemLibraryPresentationSupport.formatDecimal(item.getBlockChance()) + "% bloku");
        }
        if (item.getRetributionChance() > 0.0d && !hasAffix(item, ImportedItemAffixType.RETRIBUTION_CHANCE)) {
            chips.add("+" + ItemLibraryPresentationSupport.formatDecimal(item.getRetributionChance()) + "% retribution");
        }
        for (ImportedItemAffix affix : item.getAffixes()) {
            if (!isDescriptiveSlotAffix(affix)) {
                chips.add(formatSlotAffixChip(item, affix));
            }
        }
        return chips;
    }

    private static boolean hasAffix(SavedImportedItem item, ImportedItemAffixType type) {
        return item.getAffixes().stream().anyMatch(affix -> affix.getType() == type);
    }

    private static boolean isDescriptiveSlotAffix(ImportedItemAffix affix) {
        return affix.getType() == ImportedItemAffixType.LIFE_ON_HIT
                || affix.getType() == ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE;
    }

    private static String formatSlotAffixChip(SavedImportedItem item, ImportedItemAffix affix) {
        if (item.getMasterworking() != null && item.getMasterworking().hasVisibleProgress()) {
            return MasterworkingSectionRenderer.formatAffixReadonlyLine(item.getMasterworking(), affix);
        }
        return ItemLibraryPresentationSupport.formatAffixForDetails(affix);
    }

    private static List<String> buildSlotEffectChips(SavedImportedItem item) {
        List<String> chips = new ArrayList<>();
        for (ImportedItemAffix affix : item.getAffixes()) {
            if (affix.getType() == ImportedItemAffixType.LIFE_ON_HIT) {
                chips.add("+" + ItemLibraryPresentationSupport.formatWhole(affix.getValue()) + " zdrowia przy trafieniu");
            } else if (affix.getType() == ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE) {
                chips.add("Lucky Hit 15%: " + luckyHitResourceValue(affix) + " zasobu");
            }
        }
        return chips;
    }

    private static List<String> buildSlotTemperingChips(SavedImportedItem item) {
        List<String> chips = new ArrayList<>();
        for (ItemTemperingAffix affix : item.getTemperingAffixes()) {
            String affixLabel = item.getMasterworking() != null && item.getMasterworking().hasVisibleProgress()
                    ? MasterworkingSectionRenderer.formatTemperingReadonlyLine(item.getMasterworking(), affix)
                    : (affix.isGreaterAffix() ? "★ " : "")
                    + TemperingPresentationSupport.formatSavedAffixEffect(affix, ApplicationTemperingAffixRegistry.get());
            chips.add(affix.getCategory().getDisplayName()
                    + ": "
                    + affixLabel
                    + (affix.isGreaterAffix() ? " | Greater Affix / Gwiazdka" : ""));
        }
        return chips;
    }

    private static List<String> buildSlotMasterworkingChips(SavedImportedItem item) {
        String chip = MasterworkingSectionRenderer.compactChip(
                item.getMasterworking(),
                item.getAffixes(),
                item.getTemperingAffixes());
        if (chip.isBlank()) {
            return List.of();
        }
        List<String> chips = new ArrayList<>();
        chips.add(chip);
        return List.copyOf(chips);
    }

    private static List<String> buildSlotTransfigurationChips(SavedImportedItem item) {
        String chip = TransfigurationSectionRenderer.compactChip(item.getTransfiguration(), item.getAffixes());
        if (chip.isBlank()) {
            return List.of();
        }
        return List.of(chip);
    }

    private static List<String> buildSlotAspectChips(SavedImportedItem item) {
        if (item.getSelectedAspectId() == null || item.getSelectedAspectId().isBlank()) {
            return List.of();
        }
        AspectDefinition aspect = ASPECT_REGISTRY.findById(item.getSelectedAspectId()).orElse(null);
        if (aspect == null) {
            return List.of(item.getSelectedAspectId());
        }
        String runtime = aspect.getRuntimeStatus() == AspectRuntimeStatus.DESCRIPTIVE_ONLY
                ? "Runtime nieaktywny"
                : aspect.getRuntimeStatus().getDisplayName();
        return List.of(aspect.getDisplayName() + " · " + runtime);
    }

    private static String luckyHitResourceValue(ImportedItemAffix affix) {
        if (affix.getDisplayValue() != null && !affix.getDisplayValue().isBlank()) {
            return affix.getDisplayValue();
        }
        return "+" + ItemLibraryPresentationSupport.formatWhole(affix.getValue());
    }

    private static String formatPercentComma(BigDecimal value, int scale) {
        return formatDecimalComma(value, scale) + "%";
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
                        <p>""" + escapeHtml(resultEmptyStateText(model)) + """
                </p>
                        </section>
                    </details>
                """
                    + renderSimulationDebugSection(model, null);
        }

        CurrentBuildCalculation calculation = model.getCalculation();
        StringBuilder html = new StringBuilder("""
                <details class="current-build-details result-details">
                    <summary>Wynik symulacji</summary>
                    <section class="panel result-panel">
                    <div class="summary-grid">
                """);
        html.append(renderSummaryCard("Poziom", Integer.toString(calculation.getRequest().getLevel())));
        html.append(renderSummaryCard("Efektywne obrażenia broni", Long.toString(calculation.getRequest().getWeaponDamage())));
        html.append(renderSummaryCard("Efektywna siła", String.format(Locale.US, "%.0f", calculation.getRequest().getStrength())));
        html.append(renderSummaryCard("Efektywna inteligencja", String.format(Locale.US, "%.0f", calculation.getRequest().getIntelligence())));
        html.append(renderSummaryCard("Liczba kroków symulacji", Integer.toString(calculation.getRequest().getSimulationStepCount())));
        html.append(renderSummaryCard("Pasek akcji", CurrentBuildCalculationSectionsRenderer.buildActionBarLabel(calculation.getRequest().getActionBar())));
        html.append(renderSummaryCard("Aktywna Przysięga", activePaladinOathSummary(model.getFormData())));
        html.append(renderSummaryCard("Łączne obrażenia", Long.toString(calculation.getResult().getTotalDamage())));
        html.append(renderSummaryCard("DPS", CurrentBuildNumberFormatter.dps(calculation.getResult().getDps())));
        html.append(renderSummaryCard("Początkowa Wiara", CurrentBuildNumberFormatter.resource(calculation.getResult().getInitialPrimaryResource())));
        html.append(renderSummaryCard("Końcowa Wiara", CurrentBuildNumberFormatter.resource(calculation.getResult().getFinalPrimaryResource())));
        html.append(renderSummaryCard("Maksymalna Wiara", CurrentBuildNumberFormatter.resource(calculation.getResult().getMaxPrimaryResource())));
        html.append(renderSummaryCard("Regeneracja Wiary/s", CurrentBuildNumberFormatter.resourceRegenPerSecond(calculation.getResult().getPrimaryResourceRegenPerSecond())));
        html.append(renderSummaryCard("Łączny koszt Wiary", CurrentBuildNumberFormatter.resource(calculation.getResult().getTotalPrimaryResourceCost())));
        html.append(renderSummaryCard("Łączna generacja Wiary", CurrentBuildNumberFormatter.resource(calculation.getResult().getTotalPrimaryResourceGenerated())));
        html.append(renderSummaryCard("Łączna regeneracja Wiary", CurrentBuildNumberFormatter.resource(calculation.getResult().getTotalPrimaryResourceRegenerated())));
        if (calculation.getResult().hasAnimusRuntimeData()) {
            html.append(renderSummaryCard("Początkowy Animusz", CurrentBuildNumberFormatter.resource(calculation.getResult().getInitialAnimus())));
            html.append(renderSummaryCard("Końcowy Animusz", CurrentBuildNumberFormatter.resource(calculation.getResult().getFinalAnimus())));
            html.append(renderSummaryCard("Maksymalny Animusz", CurrentBuildNumberFormatter.resource(calculation.getResult().getMaxAnimus())));
            html.append(renderSummaryCard("Buff Molocha aktywowany", calculation.getResult().getMolochBuffActivationCount() > 0 ? "Tak" : "Nie"));
            html.append(renderSummaryCard("Aktywacje Molocha", Integer.toString(calculation.getResult().getMolochBuffActivationCount())));
            html.append(renderSummaryCard("Animusz ze Starcia", CurrentBuildNumberFormatter.signedResource(calculation.getResult().getTotalClashAnimusGenerated())));
        }
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
        html.append(renderSimulationDebugSection(model, calculation));
        return html.toString();
    }

    private static String resultEmptyStateText(CurrentBuildPageModel model) {
        if (model.getFormData().getActionBarSlots().stream().noneMatch(CurrentBuildPageRenderer::hasSelectedActionBarSkill)) {
            return "Brak obliczeń: pasek akcji jest pusty. Dodaj umiejętność do paska albo kliknij Oblicz aktualny build po uzupełnieniu konfiguracji.";
        }
        CurrentBuildImportableStats effectiveStats = model.getEffectiveStats();
        if (effectiveStats == null || effectiveStats.getWeaponDamage() <= 0L) {
            return "Brak aktywnej broni. Obliczenia mogą dać 0 obrażeń, dopóki nie wybierzesz broni.";
        }
        return "Kliknij Oblicz aktualny build, aby uruchomić symulację dla bieżącej konfiguracji.";
    }

    private static boolean hasSelectedActionBarSkill(String value) {
        return value != null && !value.isBlank() && !"NONE".equals(value);
    }

    private static String renderSimulationDebugSection(CurrentBuildPageModel model, CurrentBuildCalculation calculation) {
        StringBuilder html = new StringBuilder("""
                <details class="current-build-details simulation-debug-details">
                    <summary>Debug symulacji</summary>
                """);
        html.append(renderTechnicalRuntimeInput(model));
        if (calculation != null) {
            if (calculation.getRequest().getActionBar().isEmpty()) {
                html.append("""
                        <section class="panel result-panel empty-simulation-panel">
                            <p class="helper">Symulacja nie wykonała trafień, ponieważ pasek akcji jest pusty.</p>
                        </section>
                        """);
            }
            html.append(CurrentBuildCalculationSectionsRenderer.renderDirectHitDebug(calculation));
            html.append(CurrentBuildCalculationSectionsRenderer.renderDelayedHitDebug(calculation));
            html.append(CurrentBuildCalculationSectionsRenderer.renderReactiveDebug(calculation));
            html.append(CurrentBuildCalculationSectionsRenderer.renderStepTrace(calculation));
        }
        html.append("</details>");
        return html.toString();
    }

    private static String renderTechnicalRuntimeInput(CurrentBuildPageModel model) {
        CurrentBuildRuntimeInputPresentation presentation = CurrentBuildRuntimeInputPresentation.from(model);
        StringBuilder html = new StringBuilder("""
                <section class="panel result-panel technical-runtime-input">
                    <h2>Techniczne wejście runtime</h2>
                    <div class="summary-grid compact-grid">
                """);
        if (!presentation.isEmpty()) {
            for (CurrentBuildRuntimeInputPresentation.Field field : presentation.getFields()) {
                html.append(renderRuntimeInputCard(field));
            }
            html.append("</div></section>");
            return html.toString();
        }
        html.append("</div><p class=\"helper\">Wejście runtime current build nie jest dostępne, bo formularz zawiera błędy walidacji.</p></section>");
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
                """
                .replace("{{WEAPON_DAMAGE}}", escapeHtml(formData.getWeaponDamage()))
                .replace("{{STRENGTH}}", escapeHtml(formData.getStrength()))
                .replace("{{INTELLIGENCE}}", escapeHtml(formData.getIntelligence()))
                .replace("{{THORNS}}", escapeHtml(formData.getThorns()))
                .replace("{{BLOCK_CHANCE}}", escapeHtml(formData.getBlockChance()))
                .replace("{{RETRIBUTION_CHANCE}}", escapeHtml(formData.getRetributionChance()));
    }

    private static String renderPrimaryResourceFields(CurrentBuildFormData formData) {
        return """
                <label>
                    Początkowa Wiara
                    <input type="number" min="0" step="0.01" name="initialPrimaryResource" value="{{INITIAL_PRIMARY_RESOURCE}}">
                </label>
                <label>
                    Maksymalna Wiara
                    <input type="number" min="0" step="0.01" name="maxPrimaryResource" value="{{MAX_PRIMARY_RESOURCE}}">
                </label>
                <label>
                    Regeneracja Wiary/s
                    <input type="number" min="0" step="0.01" name="primaryResourceRegenPerSecond" value="{{PRIMARY_RESOURCE_REGEN_PER_SECOND}}">
                </label>
                """
                .replace("{{INITIAL_PRIMARY_RESOURCE}}", escapeHtml(formData.getInitialPrimaryResource()))
                .replace("{{MAX_PRIMARY_RESOURCE}}", escapeHtml(formData.getMaxPrimaryResource()))
                .replace("{{PRIMARY_RESOURCE_REGEN_PER_SECOND}}", escapeHtml(formData.getPrimaryResourceRegenPerSecond()));
    }

    private static String renderRuntimeInputCard(CurrentBuildRuntimeInputPresentation.Field field) {
        return """
                <article class="summary-card runtime-input-card">
                    <div class="summary-label">""" + escapeHtml(field.getLabel()) + """
                </div>
                    <div class="summary-value">""" + escapeHtml(field.getValue()) + """
                </div>
                    <div class="summary-source">Źródło: """ + escapeHtml(field.getSource()) + """
                </div>
                </article>
                """;
    }

    private static String renderActionBarFields(CurrentBuildPageModel model) {
        StringBuilder html = new StringBuilder();
        for (int slot = 1; slot <= CurrentBuildFormData.ACTION_BAR_SLOT_COUNT; slot++) {
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

    private static String renderPaladinOathSection(CurrentBuildPageModel model) {
        String selectedOathId = model.getFormData().getSelectedPaladinOathId();
        StringBuilder html = new StringBuilder("""
                <details class="current-build-details paladin-oath-details">
                    <summary>Przysięga Paladyna</summary>
                    <section class="subpanel">
                    <div class="form-grid">
                        <label>
                            Aktywna Przysięga
                            <select name="selectedPaladinOathId">
                """);
        html.append(renderOptions(buildPaladinOathOptions(selectedOathId)));
        html.append("""
                            </select>
                        </label>
                    </div>
                """);
        PaladinOathRegistry.findByRawId(selectedOathId)
                .ifPresentOrElse(
                        oath -> {
                            html.append(renderPaladinOathDescription(oath));
                            if ("JUGGERNAUT".equals(oath.getId().name())) {
                                html.append(renderAnimusFields(model.getFormData()));
                            }
                        },
                        () -> html.append("<p class=\"helper\">Brak wybranej Przysięgi Paladyna. Przysięgi są mechaniką klasową poza paskiem akcji i poza zwykłym drzewem umiejętności.</p>")
                );
        html.append("""
                    </section>
                </details>
                """);
        return html.toString();
    }

    private static List<CurrentBuildPageModel.SelectOption> buildPaladinOathOptions(String selectedOathId) {
        List<CurrentBuildPageModel.SelectOption> options = new ArrayList<>();
        options.add(new CurrentBuildPageModel.SelectOption("NONE", "Brak wybranej Przysięgi", selectedOathId == null || selectedOathId.isBlank() || "NONE".equals(selectedOathId)));
        for (PaladinOathDefinition oath : PaladinOathRegistry.all()) {
            String value = oath.getId().name();
            options.add(new CurrentBuildPageModel.SelectOption(value, oath.getDisplayName(), value.equals(selectedOathId)));
        }
        return options;
    }

    private static String renderPaladinOathDescription(PaladinOathDefinition oath) {
        StringBuilder html = new StringBuilder("<article class=\"summary-card paladin-oath-card\"><div class=\"summary-label\">")
                .append(escapeHtml(oath.getDisplayName()))
                .append("</div><div class=\"summary-value\">")
                .append(escapeHtml(oath.getSubtitle()))
                .append("</div><p class=\"helper\">")
                .append(escapeHtml(oathRuntimeSummary(oath)))
                .append("</p><ul class=\"message-list\">");
        for (String line : oath.getDescriptionLines()) {
            html.append("<li>").append(escapeHtml(line)).append("</li>");
        }
        html.append("</ul><ul class=\"message-list muted-list\">");
        for (String line : oath.getSecondaryDescriptionLines()) {
            html.append("<li>").append(escapeHtml(line)).append("</li>");
        }
        html.append("</ul><p class=\"helper\">Status runtime: ")
                .append(escapeHtml(oathRuntimeStatusLabel(oath.getRuntimeStatus())))
                .append("</p></article>");
        return html.toString();
    }

    private static String renderAnimusFields(CurrentBuildFormData formData) {
        return """
                <div class="form-grid animus-fields">
                    <label>
                        Początkowy Animusz
                        <input type="number" min="0" step="1" name="initialAnimus" value="{{INITIAL_ANIMUS}}">
                    </label>
                    <label>
                        Maksymalny Animusz
                        <input type="number" min="0" step="1" name="maxAnimus" value="{{MAX_ANIMUS}}">
                    </label>
                    <label>
                        Minimalny Animusz
                        <input type="number" value="1" disabled>
                    </label>
                </div>
                <p class="helper">Runtime Molocha aktywny częściowo: buff obrażeń +60%[x] dla umiejętności Molocha. Początkowy Animusz 8 jest ustawieniem testowym formularza.</p>
                """
                .replace("{{INITIAL_ANIMUS}}", escapeHtml(formData.getInitialAnimus()))
                .replace("{{MAX_ANIMUS}}", escapeHtml(formData.getMaxAnimus()));
    }

    private static String activePaladinOathSummary(CurrentBuildFormData formData) {
        return PaladinOathRegistry.findByRawId(formData.getSelectedPaladinOathId())
                .map(oath -> oath.getDisplayName() + " — " + oathRuntimeStatusLabel(oath.getRuntimeStatus()))
                .orElse("Brak");
    }

    private static String oathRuntimeStatusLabel(PaladinOathRuntimeStatus runtimeStatus) {
        return switch (runtimeStatus) {
            case NOT_RUNTIME_ENABLED -> "Opisowe / runtime DPS nieaktywny";
            case PARTIALLY_RUNTIME_ENABLED -> "Częściowo aktywne w runtime";
        };
    }

    private static String oathRuntimeSummary(PaladinOathDefinition oath) {
        if (oath.getRuntimeStatus() == PaladinOathRuntimeStatus.PARTIALLY_RUNTIME_ENABLED) {
            return "Runtime Molocha aktywny częściowo: koszt 8 Animuszu i buff obrażeń +60%[x] dla umiejętności Molocha.";
        }
        return "Efekty Przysięgi są opisowe — runtime DPS nieaktywny w tym etapie.";
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
        List<String> parts = new ArrayList<>();
        parts.add(item.getDisplayName());
        if (item.getWeaponDps() != null) {
            parts.add("DPS " + item.getWeaponDps());
        }
        findAffixValue(item, ImportedItemAffixType.MAXIMUM_LIFE)
                .ifPresent(value -> parts.add("+" + ItemLibraryPresentationSupport.formatWhole(value) + " zdrowia"));
        return String.join(" | ", parts);
    }

    private static java.util.Optional<Double> findAffixValue(SavedImportedItem item, ImportedItemAffixType type) {
        for (ImportedItemAffix affix : item.getAffixes()) {
            if (affix.getType() == type) {
                return java.util.Optional.of(affix.getValue());
            }
        }
        return java.util.Optional.empty();
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

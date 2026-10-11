package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.app.CurrentBuildCalculation;
import krys.app.CurrentBuildCalculationService;
import krys.item.HeroEquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.EffectiveCurrentBuildResolution;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.ItemLibraryPresentationSupport;
import krys.necromancer.app.NecromancerCurrentBuildCalculation;
import krys.necromancer.app.NecromancerCurrentBuildCalculationService;
import krys.skill.SkillId;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Kontroler HTTP dla pierwszego klikalnego GUI M8. */
public final class CurrentBuildController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final CurrentBuildCalculationService calculationService;
    private final CurrentBuildPageRenderer renderer;
    private final CurrentBuildFormMapper formMapper;
    private final CurrentBuildRuntimeInputResolver runtimeInputResolver;
    private final ItemLibraryService itemLibraryService;
    private final HeroService heroService;
    private final NecromancerCurrentBuildCalculationService necromancerService = new NecromancerCurrentBuildCalculationService();
    private final NecromancerCurrentBuildInputResolver necromancerInputResolver = new NecromancerCurrentBuildInputResolver();

    public CurrentBuildController(CurrentBuildCalculationService calculationService,
                                  CurrentBuildPageRenderer renderer,
                                  ItemLibraryService itemLibraryService,
                                  HeroService heroService) {
        this(calculationService, renderer, new CurrentBuildFormMapper(), itemLibraryService, heroService);
    }

    CurrentBuildController(CurrentBuildCalculationService calculationService,
                           CurrentBuildPageRenderer renderer,
                           CurrentBuildFormMapper formMapper,
                           ItemLibraryService itemLibraryService,
                           HeroService heroService) {
        this.calculationService = calculationService;
        this.renderer = renderer;
        this.formMapper = formMapper;
        this.runtimeInputResolver = new CurrentBuildRuntimeInputResolver();
        this.itemLibraryService = itemLibraryService;
        this.heroService = heroService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                HeroProfile activeHero = heroService.getActiveHero().orElse(null);
                if (activeHero == null) {
                    renderPage(exchange, buildEmptyPageModel());
                    return;
                }
                if (CurrentBuildRuntimeMode.forHero(activeHero) == CurrentBuildRuntimeMode.NECROMANCER_DIRECT_HIT) {
                    renderPage(exchange, buildNecromancerPageModel(activeHero, savedNecromancerForm(activeHero), List.of(), List.of(), null));
                    return;
                }
                Map<String, String> queryFields = UrlEncodedFormSupport.parseQuery(exchange.getRequestURI().getRawQuery());
                CurrentBuildFormData formData = queryFields.isEmpty()
                        ? activeHero.getCurrentBuildFormData()
                        : CurrentBuildFormData.fromFormFields(queryFields, activeHero.getCurrentBuildFormData());
                if (!queryFields.isEmpty()) {
                    HeroSkillLoadout updatedSkillLoadout = activeHero.getSkillLoadout().withAppliedFormData(formData);
                    CurrentBuildFormData normalizedFormData = updatedSkillLoadout.applyToFormData(formData);
                    heroService.saveActiveHeroState(normalizedFormData, updatedSkillLoadout);
                    activeHero = heroService.requireActiveHero();
                    formData = activeHero.getCurrentBuildFormData();
                }
                List<String> errors = new ArrayList<>();
                EffectiveCurrentBuildResolution resolution = buildEffectiveResolution(formData, errors);
                renderPage(exchange, buildPageModel(formData, List.of(), errors, null, resolution));
                return;
            }
            if ("POST".equals(method)) {
                renderPage(exchange, handlePost(exchange));
                return;
            }
            exchange.getResponseHeaders().set("Allow", "GET, POST");
            exchange.sendResponseHeaders(405, -1);
        } finally {
            exchange.close();
        }
    }

    private CurrentBuildPageModel handlePost(HttpExchange exchange) throws IOException {
        HeroProfile activeHero = heroService.getActiveHero().orElse(null);
        if (activeHero == null) {
            return buildEmptyPageModel();
        }
        Map<String, String> fields = UrlEncodedFormSupport.parseBody(exchange);
        List<String> errors = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        CurrentBuildFormAction formAction = CurrentBuildFormAction.from(fields.get("formAction"));

        if (CurrentBuildRuntimeMode.forHero(activeHero) == CurrentBuildRuntimeMode.NECROMANCER_DIRECT_HIT
                && !"setActiveHeroInline".equals(fields.get("heroAction"))) {
            return handleNecromancerPost(activeHero, fields, messages, errors);
        }

        if (handleHeroAction(fields, messages, errors)) {
            HeroProfile refreshedHero = heroService.getActiveHero().orElse(null);
            if (refreshedHero == null) {
                return buildEmptyPageModel();
            }
            if (CurrentBuildRuntimeMode.forHero(refreshedHero) == CurrentBuildRuntimeMode.NECROMANCER_DIRECT_HIT) {
                return buildNecromancerPageModel(refreshedHero, savedNecromancerForm(refreshedHero), messages, errors, null);
            }
            CurrentBuildFormData refreshedFormData = refreshedHero.getCurrentBuildFormData();
            return buildPageModel(
                    refreshedFormData,
                    messages,
                    errors,
                    null,
                    buildEffectiveResolution(refreshedFormData, errors)
            );
        }

        CurrentBuildFormData requestedFormData = CurrentBuildFormData.fromFormFields(fields, activeHero.getCurrentBuildFormData());
        HeroSkillLoadout updatedSkillLoadout = activeHero.getSkillLoadout().withAppliedFormData(requestedFormData);
        CurrentBuildFormData formData = requestedFormData;
        HeroSkillPointBudget skillPointBudget = HeroSkillPointBudget.from(requestedFormData, updatedSkillLoadout);
        errors.addAll(skillPointBudget.getValidationErrors());
        if (errors.isEmpty()) {
            handleEquipmentSelection(fields, formAction, errors, messages);
        }

        EffectiveCurrentBuildResolution resolution = buildEffectiveResolution(requestedFormData, errors);
        if (resolution.getEffectiveStats() != null) {
            CurrentBuildFormData effectiveFormData = CurrentBuildFormQuerySupport.withAppliedStats(
                    requestedFormData,
                    statsForPreSaveValidation(resolution.getEffectiveStats())
            );
            effectiveFormData = runtimeInputResolver.applyRuntimeResourceBonuses(effectiveFormData, resolution);
            CurrentBuildFormMapper.MappingResult preSaveMapping = formMapper.map(effectiveFormData);
            errors.addAll(preSaveMapping.getErrors());
        }
        if (errors.isEmpty()) {
            formData = updatedSkillLoadout.applyToFormData(requestedFormData);
            if (formAction == CurrentBuildFormAction.SAVE) {
                heroService.saveActiveHeroState(formData, updatedSkillLoadout);
                formData = heroService.requireActiveHero().getCurrentBuildFormData();
                resolution = buildEffectiveResolution(formData, errors);
                messages.add("Zapisano konfigurację.");
            }
        }
        CurrentBuildCalculation calculation = tryCalculate(formData, resolution, errors);
        if (errors.isEmpty() && formAction == CurrentBuildFormAction.CALCULATE) {
            messages.add("Obliczono aktualny build.");
        }
        return buildPageModel(formData, messages, errors, calculation, resolution);
    }

    private CurrentBuildCalculation tryCalculate(CurrentBuildFormData formData,
                                                 EffectiveCurrentBuildResolution resolution,
                                                 List<String> errors) {
        if (!errors.isEmpty() || resolution == null || resolution.getEffectiveStats() == null) {
            return null;
        }

        CurrentBuildFormData effectiveFormData = CurrentBuildFormQuerySupport.withAppliedStats(
                formData,
                resolution.getEffectiveStats()
        );
        effectiveFormData = runtimeInputResolver.applyRuntimeResourceBonuses(effectiveFormData, resolution);
        CurrentBuildFormMapper.MappingResult mappingResult = formMapper.map(
                effectiveFormData,
                hasActiveItemInSlot(resolution, HeroEquipmentSlot.MAIN_HAND),
                hasActiveItemInSlot(resolution, HeroEquipmentSlot.OFF_HAND)
        );
        errors.addAll(mappingResult.getErrors());
        if (!errors.isEmpty() || mappingResult.getRequest() == null) {
            return null;
        }

        try {
            return calculationService.calculate(mappingResult.getRequest()
                    .withCriticalChancePercent(resolution.getEffectiveStats().getCriticalChancePercent())
                    .withActiveAspectIds(activeAspectIds(resolution)));
        } catch (IllegalArgumentException exception) {
            errors.add(exception.getMessage());
            return null;
        }
    }

    private static List<String> activeAspectIds(EffectiveCurrentBuildResolution resolution) {
        List<String> aspectIds = new ArrayList<>();
        if (resolution == null) {
            return aspectIds;
        }
        for (var assignment : resolution.getActiveItems()) {
            String aspectId = assignment.getItem().getSelectedAspectId();
            if (aspectId != null && !aspectId.isBlank() && !aspectIds.contains(aspectId)) {
                aspectIds.add(aspectId);
            }
        }
        return aspectIds;
    }

    private CurrentBuildPageModel buildPageModel(CurrentBuildFormData formData,
                                                 List<String> messages,
                                                 List<String> errors,
                                                 CurrentBuildCalculation calculation,
                                                 EffectiveCurrentBuildResolution resolution) {
        HeroProfile activeHero = heroService.getActiveHero().orElse(null);
        return new CurrentBuildPageModel(
                formData,
                List.of(),
                List.of(),
                List.of(),
                messages,
                errors,
                calculation,
                resolution,
                activeHero,
                heroService.getHeroes(),
                itemLibraryService.getSavedItems(),
                "/biblioteka-itemow",
                "/importuj-item-ze-screena",
                "/bohaterowie",
                buildChoiceHelpText()
        );
    }

    private CurrentBuildPageModel buildEmptyPageModel() {
        return new CurrentBuildPageModel(
                CurrentBuildFormData.defaultValues(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                null,
                heroService.getHeroes(),
                itemLibraryService.getSavedItems(),
                "/biblioteka-itemow",
                "/importuj-item-ze-screena",
                "/bohaterowie",
                buildChoiceHelpText()
        );
    }

    private static NecromancerCurrentBuildFormData savedNecromancerForm(HeroProfile hero) {
        return NecromancerCurrentBuildFormQuerySupport.fromSerializedQuery(hero.getCurrentBuildQuery(), hero.getRuntimeSkillLoadout());
    }

    private CurrentBuildPageModel buildNecromancerPageModel(HeroProfile hero, NecromancerCurrentBuildFormData form,
                                                           List<String> messages, List<String> errors,
                                                           NecromancerCurrentBuildCalculation calculation) {
        errors = new ArrayList<>(errors);
        return CurrentBuildPageModel.forNecromancer(form, messages, errors, calculation,
                resolveEquipmentForPage(zeroStats(), hero, errors),
                hero, heroService.getHeroes(), itemLibraryService.getSavedItems());
    }

    private EffectiveCurrentBuildResolution resolveEquipmentForPage(CurrentBuildImportableStats base, HeroProfile hero, List<String> errors) {
        try { return itemLibraryService.resolveEffectiveCurrentBuild(base, hero.getItemSelection()); }
        catch (IllegalArgumentException exception) {
            if (!errors.contains(exception.getMessage())) { errors.add(exception.getMessage()); }
            // Niespójny zapis pozostaje widoczny do korekty; jego wkład i kalkulacja są odrzucone.
            return new EffectiveCurrentBuildResolution(base, List.of(), zeroStats(), krys.itemlibrary.CurrentHeroActiveItemStats.empty(), null);
        }
    }

    private CurrentBuildPageModel handleNecromancerPost(HeroProfile hero, Map<String, String> fields,
                                                       List<String> messages, List<String> errors) {
        if (!fields.getOrDefault("heroAction", "").isBlank()) {
            errors.add("Edycja legacy umiejętności Paladyna nie jest dostępna dla Nekromanty.");
            return buildNecromancerPageModel(hero, savedNecromancerForm(hero), messages, errors, null);
        }
        var form = NecromancerCurrentBuildFormData.fromFormFields(fields, savedNecromancerForm(hero),
                NecromancerCurrentBuildFormQuerySupport.baselineIntelligence(hero.getCurrentBuildQuery()));
        errors.addAll(form.validationErrors());
        errors.addAll(NecromancerCurrentBuildFormData.postValidationErrors(fields));
        NecromancerCurrentBuildCalculation calculation = null;
        if (errors.isEmpty()) {
            try {
                var loadout = NecromancerCurrentBuildLoadoutEditor.apply(hero.getRuntimeSkillLoadout(), form);
                if (!fields.getOrDefault("slotAction", "").isBlank()) {
                    handlePageAction(fields, errors, messages);
                    hero = heroService.requireActiveHero();
                } else if (CurrentBuildFormAction.from(fields.get("formAction")) == CurrentBuildFormAction.SAVE) {
                    if (!form.selectedSkillId().isEmpty()) {
                        var configuration = necromancerService.requireKrwotokOnActionBar(loadout);
                        if (configuration.rank() != 1 || !configuration.selections().isEmpty()) {
                            throw new UnsupportedOperationException(unsupportedNecromancerConfiguration(configuration.rank(), !configuration.selections().isEmpty()));
                        }
                    }
                    HeroItemSelection selection = validatedNecromancerItemSelection(hero.getItemSelection(), fields);
                    heroService.saveActiveHeroRuntimeState(
                            NecromancerCurrentBuildFormQuerySupport.withFormData(hero.getCurrentBuildQuery(), form), loadout, selection);
                    hero = heroService.requireActiveHero();
                    messages.add("Zapisano konfigurację Nekromanty.");
                } else {
                    var resolution = itemLibraryService.resolveEffectiveCurrentBuild(zeroStats(), hero.getItemSelection());
                    // Sprawdzamy obecność skilla przed wejściem liczbowym, bez naprawiania loadoutu.
                    var configuration = necromancerService.requireKrwotokOnActionBar(loadout);
                    try {
                        calculation = necromancerService.calculate(loadout, necromancerInputResolver.resolve(hero, form, resolution));
                    } catch (UnsupportedOperationException exception) {
                        throw new UnsupportedOperationException(unsupportedNecromancerConfiguration(configuration.rank(), !configuration.selections().isEmpty()), exception);
                    }
                    messages.add("Obliczono bazowy direct hit Krwotoku R1.");
                }
            } catch (IllegalArgumentException | UnsupportedOperationException exception) {
                errors.add(exception.getMessage());
            }
        }
        return buildNecromancerPageModel(hero, form, messages, errors, calculation);
    }

    private static String unsupportedNecromancerConfiguration(int rank, boolean hasSelections) {
        return hasSelections ? "Upgrade'y Krwotoku nie są jeszcze wykonywane w N11. Zachowano zapisaną konfigurację."
                : "N11 obsługuje wyłącznie Krwotok R1; obecna konfiguracja ma rangę " + rank + ".";
    }

    private HeroItemSelection validatedNecromancerItemSelection(HeroItemSelection saved, Map<String, String> fields) {
        HeroItemSelection selection = saved;
        for (HeroEquipmentSlot slot : HeroEquipmentSlot.values()) {
            String rawId = fields.get("selectedItemId_" + slot.name());
            if (rawId == null || rawId.isBlank()) { continue; }
            long itemId;
            try { itemId = Long.parseLong(rawId); }
            catch (NumberFormatException exception) { throw new IllegalArgumentException("Wybierz poprawny item biblioteki dla slotu " + ItemLibraryPresentationSupport.heroSlotDisplayName(slot) + "."); }
            itemLibraryService.requireCompatibleItem(slot, itemId);
            selection = selection.withSelectedItem(slot, itemId);
        }
        itemLibraryService.validateEquipmentSelection(selection);
        return selection;
    }

    private EffectiveCurrentBuildResolution buildEffectiveResolution(CurrentBuildFormData formData,
                                                                    List<String> errors) {
        ManualBaseStatsParseResult manualBaseParseResult = parseManualBaseStats(formData);
        errors.addAll(manualBaseParseResult.getErrors());
        if (!manualBaseParseResult.isValid()) {
            EffectiveCurrentBuildResolution zeroBaseResolution = resolveEquipmentForPage(zeroStats(), heroService.requireActiveHero(), errors);
            return new EffectiveCurrentBuildResolution(
                    zeroStats(),
                    zeroBaseResolution.getActiveItems(),
                    zeroBaseResolution.getActiveItemsContribution(),
                    zeroBaseResolution.getActiveHeroItemStats(),
                    null
            );
        }
        HeroProfile activeHero = heroService.requireActiveHero();
        EffectiveCurrentBuildResolution libraryResolution = resolveEquipmentForPage(manualBaseParseResult.getStats(), activeHero, errors);
        if (libraryResolution.getEffectiveStats() == null) { return libraryResolution; }
        CurrentBuildImportableStats runtimeInputStats = runtimeInputResolver.resolve(activeHero, formData, libraryResolution);
        return new EffectiveCurrentBuildResolution(
                libraryResolution.getManualBaseStats(),
                libraryResolution.getActiveItems(),
                libraryResolution.getActiveItemsContribution(),
                libraryResolution.getActiveHeroItemStats(),
                runtimeInputStats
        );
    }

    private static ManualBaseStatsParseResult parseManualBaseStats(CurrentBuildFormData formData) {
        List<String> errors = new ArrayList<>();

        Long weaponDamage = parseNonNegativeLongAllowingBlank(formData.getWeaponDamage(), "Obrażenia broni", errors);
        Double strength = parseNonNegativeDoubleAllowingBlank(formData.getStrength(), "Siła", errors);
        Double intelligence = parseNonNegativeDoubleAllowingBlank(formData.getIntelligence(), "Inteligencja", errors);
        Double thorns = parseNonNegativeDoubleAllowingBlank(formData.getThorns(), "Kolce", errors);
        Double blockChance = parseNonNegativeDoubleAllowingBlank(formData.getBlockChance(), "Szansa bloku", errors);
        Double retributionChance = parseNonNegativeDoubleAllowingBlank(formData.getRetributionChance(), "Szansa retribution", errors);

        if (!errors.isEmpty()) {
            return new ManualBaseStatsParseResult(null, errors);
        }

        return new ManualBaseStatsParseResult(
                new CurrentBuildImportableStats(
                        weaponDamage,
                        strength,
                        intelligence,
                        thorns,
                        blockChance,
                        retributionChance
                ),
                errors
        );
    }

    private static Long parseNonNegativeLongAllowingBlank(String rawValue, String label, List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0L;
        }
        try {
            long value = Long.parseLong(rawValue);
            if (value < 0L) {
                errors.add(label + " nie może być mniejszy niż 0.");
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            errors.add(label + " musi być liczbą całkowitą.");
            return null;
        }
    }

    private static Double parseNonNegativeDoubleAllowingBlank(String rawValue, String label, List<String> errors) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0.0d;
        }
        try {
            double value = Double.parseDouble(rawValue);
            if (value < 0.0d) {
                errors.add(label + " nie może być mniejszy niż 0.");
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            errors.add(label + " musi być liczbą.");
            return null;
        }
    }

    private static CurrentBuildImportableStats zeroStats() {
        return new CurrentBuildImportableStats(0L, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d);
    }

    private static CurrentBuildImportableStats statsForPreSaveValidation(CurrentBuildImportableStats runtimeStats) {
        return new CurrentBuildImportableStats(
                Math.max(1L, runtimeStats.getWeaponDamage()),
                runtimeStats.getStrength(),
                runtimeStats.getIntelligence(),
                runtimeStats.getThorns(),
                runtimeStats.getBlockChance(),
                runtimeStats.getRetributionChance()
        );
    }

    private static boolean hasActiveItemInSlot(EffectiveCurrentBuildResolution resolution, HeroEquipmentSlot slot) {
        for (var assignment : resolution.getActiveItems()) {
            if (assignment.getHeroSlot() == slot) {
                return true;
            }
        }
        return false;
    }

    private static String buildChoiceHelpText() {
        return "Ręczne nadpisanie statów pozostaje częścią kontekstu bohatera, ale nie buduje osobnego runtime. Aktywne itemy per slot są dodawane deterministycznie przed zbudowaniem finalnych efektywnych statów, CurrentBuildRequest i wejściem do tego samego runtime aktualnego buildu.";
    }

    private boolean handleHeroAction(Map<String, String> fields, List<String> messages, List<String> errors) {
        String heroAction = fields.getOrDefault("heroAction", "");
        if (heroAction.isBlank()) {
            return false;
        }
        switch (heroAction) {
            case "setActiveHeroInline" -> {
                long heroId = parsePositiveLong(fields.get("selectedHeroId"), "Wybierz bohatera do aktywacji.", errors);
                if (!errors.isEmpty()) {
                    return true;
                }
                heroService.setActiveHero(heroId);
                messages.add("Zmieniono aktywnego bohatera bez opuszczania ekranu buildu.");
                return true;
            }
            case "addAssignedSkill" -> {
                String rawSkillId = fields.getOrDefault("skillIdToAdd", "");
                SkillId skillId;
                try {
                    skillId = SkillId.valueOf(rawSkillId);
                } catch (IllegalArgumentException exception) {
                    errors.add("Wybierz poprawną umiejętność do przypisania bohaterowi.");
                    return true;
                }
                try {
                    heroService.addSkillToActiveHero(skillId);
                    messages.add("Dodano umiejętność " + HeroSkillCatalogAdapter.displayName(skillId) + " do bohatera.");
                } catch (IllegalArgumentException exception) {
                    errors.add(exception.getMessage());
                }
                return true;
            }
            default -> {
                if (heroAction.startsWith("removeAssignedSkill:")) {
                    String rawSkillId = heroAction.substring("removeAssignedSkill:".length());
                    try {
                        SkillId skillId = SkillId.valueOf(rawSkillId);
                        HeroProfile activeHero = heroService.requireActiveHero();
                        List<String> previousActionBar = activeHero.getCurrentBuildFormData().getActionBarSlots();
                        heroService.removeSkillFromActiveHero(skillId);
                        List<String> updatedActionBar = heroService.requireActiveHero().getCurrentBuildFormData().getActionBarSlots();
                        messages.add("Usunięto przypisaną umiejętność " + HeroSkillCatalogAdapter.displayName(skillId) + ".");
                        if (!previousActionBar.equals(updatedActionBar)) {
                            messages.add("Pasek akcji został oczyszczony po usunięciu nieobsługiwanej umiejętności.");
                        }
                    } catch (IllegalArgumentException exception) {
                        errors.add("Niepoprawna umiejętność do usunięcia.");
                    }
                    return true;
                }
                errors.add("Nieobsługiwana akcja kontekstu bohatera.");
                return true;
            }
        }
    }

    private void handlePageAction(Map<String, String> fields, List<String> errors, List<String> messages) {
        String slotAction = fields.getOrDefault("slotAction", "");
        if (slotAction.isBlank()) {
            return;
        }

        String[] actionParts = slotAction.split(":", 2);
        if (actionParts.length != 2) {
            errors.add("Nieobsługiwana akcja sekcji ekwipunku.");
            return;
        }

        HeroEquipmentSlot slot;
        try {
            slot = HeroEquipmentSlot.valueOf(actionParts[1]);
        } catch (IllegalArgumentException exception) {
            errors.add("Nie wybrano poprawnego slotu ekwipunku.");
            return;
        }

        switch (actionParts[0]) {
            case "setActiveSlotItem" -> handleSetActiveSlotItem(fields, slot, errors, messages);
            case "clearActiveSlotItem" -> {
                heroService.clearActiveHeroItem(slot);
                messages.add("Wyczyszczono aktywny item dla slotu " + ItemLibraryPresentationSupport.heroSlotDisplayName(slot) + ".");
            }
            default -> errors.add("Nieobsługiwana akcja sekcji ekwipunku.");
        }
    }

    private void handleEquipmentSelection(Map<String, String> fields,
                                          CurrentBuildFormAction formAction,
                                          List<String> errors,
                                          List<String> messages) {
        String slotAction = fields.getOrDefault("slotAction", "");
        if (!slotAction.isBlank()) {
            handlePageAction(fields, errors, messages);
            return;
        }
        if (formAction == CurrentBuildFormAction.SAVE) {
            applySlotSelectionsFromGlobalSave(fields, errors, messages);
        }
    }

    private void applySlotSelectionsFromGlobalSave(Map<String, String> fields, List<String> errors, List<String> messages) {
        HeroItemSelection selection = heroService.requireActiveHero().getItemSelection();
        List<String> changes = new ArrayList<>();
        for (HeroEquipmentSlot slot : HeroEquipmentSlot.values()) {
            String fieldName = "selectedItemId_" + slot.name();
            if (!fields.containsKey(fieldName)) {
                continue;
            }
            String rawItemId = fields.get(fieldName);
            if (rawItemId == null || rawItemId.isBlank()) {
                continue;
            }

            long itemId;
            try {
                itemId = Long.parseLong(rawItemId);
            } catch (NumberFormatException exception) {
                errors.add("Wybierz poprawny item biblioteki dla slotu " + ItemLibraryPresentationSupport.heroSlotDisplayName(slot) + ".");
                continue;
            }
            if (itemId <= 0L) {
                errors.add("Wybierz zapisany item dla slotu " + ItemLibraryPresentationSupport.heroSlotDisplayName(slot) + ".");
                continue;
            }

            Long currentItemId = heroService.requireActiveHero().getItemSelection().getSelectedItemId(slot);
            if (currentItemId != null && currentItemId == itemId) {
                continue;
            }
            try {
                itemLibraryService.requireCompatibleItem(slot, itemId);
                selection = selection.withSelectedItem(slot, itemId);
                changes.add("Zmieniono aktywny item dla slotu " + ItemLibraryPresentationSupport.heroSlotDisplayName(slot) + ".");
            } catch (IllegalArgumentException exception) {
                errors.add(exception.getMessage());
            }
        }
        if (errors.isEmpty()) {
            try {
                itemLibraryService.validateEquipmentSelection(selection);
                if (!changes.isEmpty()) { heroService.replaceActiveHeroItemSelection(selection); messages.addAll(changes); }
            } catch (IllegalArgumentException exception) { errors.add(exception.getMessage()); }
        }
    }

    private void handleSetActiveSlotItem(Map<String, String> fields,
                                         HeroEquipmentSlot slot,
                                         List<String> errors,
                                         List<String> messages) {
        String rawItemId = fields.get("selectedItemId_" + slot.name());
        if (rawItemId == null || rawItemId.isBlank()) {
            errors.add("Wybierz item dla slotu " + ItemLibraryPresentationSupport.heroSlotDisplayName(slot) + ".");
            return;
        }
        try {
            long itemId = Long.parseLong(rawItemId);
            if (itemId <= 0L) {
                errors.add("Wybierz zapisany item dla slotu " + ItemLibraryPresentationSupport.heroSlotDisplayName(slot) + ".");
                return;
            }
            itemLibraryService.requireCompatibleItem(slot, itemId);
            itemLibraryService.validateEquipmentSelection(heroService.requireActiveHero().getItemSelection().withSelectedItem(slot, itemId));
            heroService.setActiveHeroItem(slot, itemId);
            messages.add("Zmieniono aktywny item dla slotu " + ItemLibraryPresentationSupport.heroSlotDisplayName(slot) + ".");
        } catch (NumberFormatException exception) {
            errors.add("Wybierz poprawny item biblioteki dla slotu " + ItemLibraryPresentationSupport.heroSlotDisplayName(slot) + ".");
        } catch (IllegalArgumentException exception) {
            errors.add(exception.getMessage());
        }
    }

    private void renderPage(HttpExchange exchange, CurrentBuildPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }

    private static long parsePositiveLong(String rawValue, String errorMessage, List<String> errors) {
        try {
            long value = Long.parseLong(rawValue);
            if (value <= 0L) {
                errors.add(errorMessage);
                return 0L;
            }
            return value;
        } catch (NumberFormatException | NullPointerException exception) {
            errors.add(errorMessage);
            return 0L;
        }
    }

    /** Miękko parsuje ręczną bazę pod effective current build, ale nie ukrywa błędów nienumerycznych ani ujemnych wartości. */
    private static final class ManualBaseStatsParseResult {
        private final CurrentBuildImportableStats stats;
        private final List<String> errors;

        private ManualBaseStatsParseResult(CurrentBuildImportableStats stats, List<String> errors) {
            this.stats = stats;
            this.errors = List.copyOf(errors);
        }

        private boolean isValid() {
            return stats != null && errors.isEmpty();
        }

        private CurrentBuildImportableStats getStats() {
            return stats;
        }

        private List<String> getErrors() {
            return errors;
        }
    }

    private enum CurrentBuildFormAction {
        SAVE,
        CALCULATE;

        private static CurrentBuildFormAction from(String rawValue) {
            if ("calculate".equals(rawValue)) {
                return CALCULATE;
            }
            return SAVE;
        }
    }
}

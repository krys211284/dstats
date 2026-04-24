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
import krys.skill.PaladinSkillDefs;
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
    private final ItemLibraryService itemLibraryService;
    private final HeroService heroService;

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
                renderPage(exchange, buildPageModel(formData, List.of(), List.of(), null, buildEffectiveResolution(formData, new ArrayList<>())));
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

        if (handleHeroAction(fields, messages, errors)) {
            HeroProfile refreshedHero = heroService.getActiveHero().orElse(null);
            if (refreshedHero == null) {
                return buildEmptyPageModel();
            }
            CurrentBuildFormData refreshedFormData = refreshedHero.getCurrentBuildFormData();
            return buildPageModel(
                    refreshedFormData,
                    messages,
                    errors,
                    null,
                    buildEffectiveResolution(refreshedFormData, new ArrayList<>(errors))
            );
        }

        CurrentBuildFormData requestedFormData = CurrentBuildFormData.fromFormFields(fields, activeHero.getCurrentBuildFormData());
        HeroSkillLoadout updatedSkillLoadout = activeHero.getSkillLoadout().withAppliedFormData(requestedFormData);
        CurrentBuildFormData formData = updatedSkillLoadout.applyToFormData(requestedFormData);
        if (!requestedFormData.getActionBarSlots().equals(formData.getActionBarSlots())) {
            messages.add("Pasek akcji został oczyszczony do umiejętności przypisanych i nauczonych przez aktywnego bohatera.");
        }
        heroService.saveActiveHeroState(formData, updatedSkillLoadout);
        handlePageAction(fields, errors, messages);
        formData = heroService.requireActiveHero().getCurrentBuildFormData();
        EffectiveCurrentBuildResolution resolution = buildEffectiveResolution(formData, errors);
        CurrentBuildCalculation calculation = tryCalculate(formData, resolution, errors);
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
        CurrentBuildFormMapper.MappingResult mappingResult = formMapper.map(effectiveFormData);
        errors.addAll(mappingResult.getErrors());
        if (!errors.isEmpty() || mappingResult.getRequest() == null) {
            return null;
        }

        try {
            return calculationService.calculate(mappingResult.getRequest());
        } catch (IllegalArgumentException exception) {
            errors.add(exception.getMessage());
            return null;
        }
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

    private EffectiveCurrentBuildResolution buildEffectiveResolution(CurrentBuildFormData formData,
                                                                    List<String> errors) {
        ManualBaseStatsParseResult manualBaseParseResult = parseManualBaseStats(formData);
        errors.addAll(manualBaseParseResult.getErrors());
        if (!manualBaseParseResult.isValid()) {
            EffectiveCurrentBuildResolution zeroBaseResolution = itemLibraryService.resolveEffectiveCurrentBuild(
                    zeroStats(),
                    heroService.requireActiveHero().getItemSelection()
            );
            return new EffectiveCurrentBuildResolution(
                    zeroStats(),
                    zeroBaseResolution.getActiveItems(),
                    zeroBaseResolution.getActiveItemsContribution(),
                    null
            );
        }
        return itemLibraryService.resolveEffectiveCurrentBuild(manualBaseParseResult.getStats(), heroService.requireActiveHero().getItemSelection());
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
            case "updateHeroLevel" -> {
                Integer heroLevel = parsePositiveInt(fields.get("heroLevelEdit"), "Poziom bohatera musi być dodatni.", errors);
                if (!errors.isEmpty()) {
                    return true;
                }
                heroService.updateActiveHeroLevel(heroLevel);
                messages.add("Zapisano poziom aktywnego bohatera.");
                return true;
            }
            case "addAssignedSkill" -> {
                try {
                    SkillId skillId = SkillId.valueOf(fields.getOrDefault("skillIdToAdd", ""));
                    heroService.addSkillToActiveHero(skillId);
                    messages.add("Dodano umiejętność " + PaladinSkillDefs.get(skillId).getName() + " do bohatera.");
                } catch (IllegalArgumentException exception) {
                    errors.add("Wybierz poprawną umiejętność do przypisania bohaterowi.");
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
                        messages.add("Usunięto przypisaną umiejętność " + PaladinSkillDefs.get(skillId).getName() + ".");
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

    private static Integer parsePositiveInt(String rawValue, String errorMessage, List<String> errors) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value <= 0) {
                errors.add(errorMessage);
                return null;
            }
            return value;
        } catch (NumberFormatException | NullPointerException exception) {
            errors.add(errorMessage);
            return null;
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
}

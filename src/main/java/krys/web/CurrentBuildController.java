package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.app.CurrentBuildCalculation;
import krys.app.CurrentBuildCalculationService;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.EffectiveCurrentBuildResolution;
import krys.itemlibrary.ItemLibraryService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Kontroler HTTP dla pierwszego klikalnego GUI M8. */
public final class CurrentBuildController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final CurrentBuildCalculationService calculationService;
    private final CurrentBuildPageRenderer renderer;
    private final CurrentBuildFormMapper formMapper;
    private final ItemLibraryService itemLibraryService;

    public CurrentBuildController(CurrentBuildCalculationService calculationService,
                                  CurrentBuildPageRenderer renderer,
                                  ItemLibraryService itemLibraryService) {
        this(calculationService, renderer, new CurrentBuildFormMapper(), itemLibraryService);
    }

    CurrentBuildController(CurrentBuildCalculationService calculationService,
                           CurrentBuildPageRenderer renderer,
                           CurrentBuildFormMapper formMapper,
                           ItemLibraryService itemLibraryService) {
        this.calculationService = calculationService;
        this.renderer = renderer;
        this.formMapper = formMapper;
        this.itemLibraryService = itemLibraryService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(
                        UrlEncodedFormSupport.parseQuery(exchange.getRequestURI().getRawQuery())
                );
                renderPage(exchange, buildPageModel(formData, List.of(), null, buildEffectiveResolution(formData, new ArrayList<>())));
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
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(UrlEncodedFormSupport.parseBody(exchange));
        List<String> errors = new ArrayList<>();
        EffectiveCurrentBuildResolution resolution = buildEffectiveResolution(formData, errors);
        CurrentBuildCalculation calculation = tryCalculate(formData, resolution, errors);
        return buildPageModel(formData, errors, calculation, resolution);
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
                                                 List<String> errors,
                                                 CurrentBuildCalculation calculation,
                                                 EffectiveCurrentBuildResolution resolution) {
        return new CurrentBuildPageModel(
                formData,
                List.of(),
                List.of(),
                List.of(),
                errors,
                calculation,
                resolution,
                "/biblioteka-itemow?" + CurrentBuildFormQuerySupport.toQuery(formData),
                buildChoiceHelpText()
        );
    }

    private EffectiveCurrentBuildResolution buildEffectiveResolution(CurrentBuildFormData formData,
                                                                    List<String> errors) {
        ManualBaseStatsParseResult manualBaseParseResult = parseManualBaseStats(formData);
        errors.addAll(manualBaseParseResult.getErrors());
        if (!manualBaseParseResult.isValid()) {
            EffectiveCurrentBuildResolution zeroBaseResolution = itemLibraryService.resolveEffectiveCurrentBuild(zeroStats());
            return new EffectiveCurrentBuildResolution(
                    zeroStats(),
                    zeroBaseResolution.getActiveItems(),
                    zeroBaseResolution.getActiveItemsContribution(),
                    null
            );
        }
        return itemLibraryService.resolveEffectiveCurrentBuild(manualBaseParseResult.getStats());
    }

    private static ManualBaseStatsParseResult parseManualBaseStats(CurrentBuildFormData formData) {
        List<String> errors = new ArrayList<>();

        Long weaponDamage = parseNonNegativeLongAllowingBlank(formData.getWeaponDamage(), "Weapon damage", errors);
        Double strength = parseNonNegativeDoubleAllowingBlank(formData.getStrength(), "Strength", errors);
        Double intelligence = parseNonNegativeDoubleAllowingBlank(formData.getIntelligence(), "Intelligence", errors);
        Double thorns = parseNonNegativeDoubleAllowingBlank(formData.getThorns(), "Thorns", errors);
        Double blockChance = parseNonNegativeDoubleAllowingBlank(formData.getBlockChance(), "Block chance", errors);
        Double retributionChance = parseNonNegativeDoubleAllowingBlank(formData.getRetributionChance(), "Retribution chance", errors);

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
        return "Pola formularza oznaczają ręczną bazę current build poza biblioteką itemów. Ta baza może być częściowo pusta albo zerowa. Aktywne itemy z biblioteki są dodawane deterministycznie jeszcze przed zbudowaniem finalnych effective stats, CurrentBuildRequest i wejściem do tego samego runtime.";
    }

    private void renderPage(HttpExchange exchange, CurrentBuildPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
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

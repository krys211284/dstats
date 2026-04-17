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
                renderPage(exchange, buildPageModel(formData, List.of(), null, buildEffectiveResolutionOrFallback(formData)));
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
        EffectiveCurrentBuildResolution resolution = buildEffectiveResolutionOrFallback(formData);
        CurrentBuildCalculation calculation = tryCalculate(formData, resolution, errors);
        return buildPageModel(formData, errors, calculation, resolution);
    }

    private CurrentBuildCalculation tryCalculate(CurrentBuildFormData formData,
                                                 EffectiveCurrentBuildResolution resolution,
                                                 List<String> errors) {
        CurrentBuildFormData effectiveFormData = formData;
        if (resolution != null) {
            effectiveFormData = CurrentBuildFormQuerySupport.withAppliedStats(formData, resolution.getEffectiveStats());
        }

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

    private EffectiveCurrentBuildResolution buildEffectiveResolutionOrFallback(CurrentBuildFormData formData) {
        CurrentBuildImportableStats manualBaseStats = tryParseManualBaseStats(formData);
        if (manualBaseStats == null) {
            return new EffectiveCurrentBuildResolution(
                    new CurrentBuildImportableStats(0L, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d),
                    itemLibraryService.getActiveItems(),
                    itemLibraryService.resolveEffectiveCurrentBuild(
                            new CurrentBuildImportableStats(0L, 0.0d, 0.0d, 0.0d, 0.0d, 0.0d)
                    ).getActiveItemsContribution(),
                    null
            );
        }
        return itemLibraryService.resolveEffectiveCurrentBuild(manualBaseStats);
    }

    private static CurrentBuildImportableStats tryParseManualBaseStats(CurrentBuildFormData formData) {
        try {
            long weaponDamage = Long.parseLong(formData.getWeaponDamage());
            double strength = Double.parseDouble(formData.getStrength());
            double intelligence = Double.parseDouble(formData.getIntelligence());
            double thorns = Double.parseDouble(formData.getThorns());
            double blockChance = Double.parseDouble(formData.getBlockChance());
            double retributionChance = Double.parseDouble(formData.getRetributionChance());
            if (weaponDamage <= 0L || strength < 0.0d || intelligence < 0.0d || thorns < 0.0d
                    || blockChance < 0.0d || retributionChance < 0.0d) {
                return null;
            }
            return new CurrentBuildImportableStats(
                    weaponDamage,
                    strength,
                    intelligence,
                    thorns,
                    blockChance,
                    retributionChance
            );
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String buildChoiceHelpText() {
        return "Pola formularza oznaczają ręczną bazę current build poza biblioteką itemów. Aktywne itemy z biblioteki są dodawane deterministycznie do tej bazy jeszcze przed zbudowaniem CurrentBuildRequest i wejściem do tego samego runtime.";
    }

    private void renderPage(HttpExchange exchange, CurrentBuildPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

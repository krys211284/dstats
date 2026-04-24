package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.ItemLibraryService;
import krys.search.BuildSearchCalculationService;
import krys.search.BuildSearchResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Kontroler HTTP dla klikalnego GUI M12 nad istniejącym backendem searcha. */
public final class SearchBuildController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final BuildSearchCalculationService calculationService;
    private final SearchBuildPageRenderer renderer;
    private final SearchBuildFormMapper formMapper;
    private final ItemLibraryService itemLibraryService;
    private final HeroService heroService;

    public SearchBuildController(BuildSearchCalculationService calculationService,
                                 SearchBuildPageRenderer renderer,
                                 ItemLibraryService itemLibraryService,
                                 HeroService heroService) {
        this(calculationService, renderer, new SearchBuildFormMapper(), itemLibraryService, heroService);
    }

    SearchBuildController(BuildSearchCalculationService calculationService,
                          SearchBuildPageRenderer renderer,
                          SearchBuildFormMapper formMapper,
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
                renderPage(exchange, buildPageModel(
                        activeHero == null ? SearchBuildFormData.defaultValues() : SearchBuildFormData.fromHeroProfile(activeHero),
                        List.of(),
                        null,
                        activeHero
                ));
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

    private SearchBuildPageModel handlePost(HttpExchange exchange) throws IOException {
        HeroProfile activeHero = heroService.getActiveHero().orElse(null);
        if (activeHero == null) {
            return buildPageModel(SearchBuildFormData.defaultValues(), List.of("Brak aktywnego bohatera. Utwórz albo wybierz bohatera przed uruchomieniem searcha."), null, null);
        }
        SearchBuildFormData formData = SearchBuildFormData.fromFormFields(UrlEncodedFormSupport.parseBody(exchange));
        List<String> errors = new ArrayList<>();
        BuildSearchResult result = tryCalculate(formData, activeHero, errors);
        return buildPageModel(formData, errors, result, activeHero);
    }

    private BuildSearchResult tryCalculate(SearchBuildFormData formData, HeroProfile activeHero, List<String> errors) {
        CurrentBuildImportableStats activeHeroItemsContribution =
                itemLibraryService.resolveActiveItemsContribution(activeHero.getItemSelection());
        SearchBuildFormMapper.MappingResult mappingResult = formMapper.map(formData, activeHeroItemsContribution);
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

    private SearchBuildPageModel buildPageModel(SearchBuildFormData formData,
                                                List<String> errors,
                                                BuildSearchResult result,
                                                HeroProfile activeHero) {
        return new SearchBuildPageModel(
                formData,
                errors,
                result,
                activeHero,
                "GUI M12 jest cienką warstwą SSR nad backendowym search foundation. Opcjonalny tryb biblioteki itemów nadal składa efektywny aktualny build przez ten sam pipeline CurrentBuildRequest -> CurrentBuildSnapshotFactory -> runtime i tylko podmienia źródło kandydatów itemowych."
        );
    }

    private void renderPage(HttpExchange exchange, SearchBuildPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

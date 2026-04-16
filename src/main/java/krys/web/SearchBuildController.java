package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.search.BuildSearchCalculationService;
import krys.search.BuildSearchResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Kontroler HTTP dla klikalnego GUI M11 nad istniejącym backendem searcha. */
public final class SearchBuildController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final BuildSearchCalculationService calculationService;
    private final SearchBuildPageRenderer renderer;
    private final SearchBuildFormMapper formMapper;

    public SearchBuildController(BuildSearchCalculationService calculationService,
                                 SearchBuildPageRenderer renderer) {
        this(calculationService, renderer, new SearchBuildFormMapper());
    }

    SearchBuildController(BuildSearchCalculationService calculationService,
                          SearchBuildPageRenderer renderer,
                          SearchBuildFormMapper formMapper) {
        this.calculationService = calculationService;
        this.renderer = renderer;
        this.formMapper = formMapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                renderPage(exchange, buildPageModel(SearchBuildFormData.defaultValues(), List.of(), null));
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
        SearchBuildFormData formData = SearchBuildFormData.fromFormFields(UrlEncodedFormSupport.parseBody(exchange));
        List<String> errors = new ArrayList<>();
        BuildSearchResult result = tryCalculate(formData, errors);
        return buildPageModel(formData, errors, result);
    }

    private BuildSearchResult tryCalculate(SearchBuildFormData formData, List<String> errors) {
        SearchBuildFormMapper.MappingResult mappingResult = formMapper.map(formData);
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
                                                BuildSearchResult result) {
        return new SearchBuildPageModel(
                formData,
                errors,
                result,
                "GUI M11 jest cienką warstwą SSR nad backendowym search foundation. Formularz buduje BuildSearchRequest, uruchamia ten sam backend co CLI, pokazuje top wyniki po normalizacji i pozwala przejść do drill-downu wybranego reprezentanta."
        );
    }

    private void renderPage(HttpExchange exchange, SearchBuildPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

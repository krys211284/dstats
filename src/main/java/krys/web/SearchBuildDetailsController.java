package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.app.CurrentBuildCalculation;
import krys.app.CurrentBuildCalculationService;
import krys.search.BuildSearchCandidate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Kontroler SSR drill-downu M12 z listy wyników searcha do pełnej analizy reprezentanta. */
public final class SearchBuildDetailsController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final CurrentBuildCalculationService calculationService;
    private final SearchBuildDetailsPageRenderer renderer;
    private final CurrentBuildFormMapper formMapper;

    public SearchBuildDetailsController(CurrentBuildCalculationService calculationService,
                                        SearchBuildDetailsPageRenderer renderer) {
        this(calculationService, renderer, new CurrentBuildFormMapper());
    }

    SearchBuildDetailsController(CurrentBuildCalculationService calculationService,
                                 SearchBuildDetailsPageRenderer renderer,
                                 CurrentBuildFormMapper formMapper) {
        this.calculationService = calculationService;
        this.renderer = renderer;
        this.formMapper = formMapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("POST".equals(method)) {
                renderPage(exchange, handlePost(exchange));
                return;
            }
            exchange.getResponseHeaders().set("Allow", "POST");
            exchange.sendResponseHeaders(405, -1);
        } finally {
            exchange.close();
        }
    }

    private SearchBuildDetailsPageModel handlePost(HttpExchange exchange) throws IOException {
        Map<String, String> fields = UrlEncodedFormSupport.parseBody(exchange);
        int selectedRank = parseSelectedRank(fields);
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(fields);
        List<String> errors = new ArrayList<>();
        BuildSearchCandidate candidate = null;
        CurrentBuildCalculation calculation = null;

        CurrentBuildFormMapper.MappingResult mappingResult = formMapper.map(formData);
        errors.addAll(mappingResult.getErrors());
        if (mappingResult.getRequest() != null) {
            candidate = new BuildSearchCandidate(mappingResult.getRequest());
        }
        if (errors.isEmpty() && mappingResult.getRequest() != null) {
            try {
                calculation = calculationService.calculate(mappingResult.getRequest());
            } catch (IllegalArgumentException exception) {
                errors.add(exception.getMessage());
            }
        }

        return new SearchBuildDetailsPageModel(
                selectedRank,
                errors,
                candidate,
                calculation,
                "Drill-down M12 odtwarza reprezentanta znormalizowanego wyniku na tym samym runtime co flow „Policz aktualny build”."
        );
    }

    private static int parseSelectedRank(Map<String, String> fields) {
        String rawSelectedRank = fields.get("selectedRank");
        if (rawSelectedRank == null || rawSelectedRank.isBlank()) {
            return 0;
        }
        try {
            int selectedRank = Integer.parseInt(rawSelectedRank);
            return Math.max(selectedRank, 0);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private void renderPage(HttpExchange exchange, SearchBuildDetailsPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

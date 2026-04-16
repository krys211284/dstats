package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.app.CurrentBuildCalculation;
import krys.app.CurrentBuildCalculationService;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Kontroler HTTP dla pierwszego klikalnego GUI M8. */
public final class CurrentBuildController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final CurrentBuildCalculationService calculationService;
    private final CurrentBuildPageRenderer renderer;
    private final CurrentBuildFormMapper formMapper;

    public CurrentBuildController(CurrentBuildCalculationService calculationService,
                                  CurrentBuildPageRenderer renderer) {
        this(calculationService, renderer, new CurrentBuildFormMapper());
    }

    CurrentBuildController(CurrentBuildCalculationService calculationService,
                           CurrentBuildPageRenderer renderer,
                           CurrentBuildFormMapper formMapper) {
        this.calculationService = calculationService;
        this.renderer = renderer;
        this.formMapper = formMapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                renderPage(exchange, buildPageModel(CurrentBuildFormData.defaultValues(), List.of(), null));
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
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(parseUrlEncodedBody(exchange));
        List<String> errors = new ArrayList<>();
        CurrentBuildCalculation calculation = tryCalculate(formData, errors);
        return buildPageModel(formData, errors, calculation);
    }

    private CurrentBuildCalculation tryCalculate(CurrentBuildFormData formData, List<String> errors) {
        CurrentBuildFormMapper.MappingResult mappingResult = formMapper.map(formData);
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
                                                 CurrentBuildCalculation calculation) {
        return new CurrentBuildPageModel(
                formData,
                List.of(),
                List.of(),
                List.of(),
                errors,
                calculation,
                buildChoiceHelpText(formData)
        );
    }

    private static String buildChoiceHelpText(CurrentBuildFormData formData) {
        return "Formularz M8 buduje prawdziwy snapshot z levelu, statów użytkownika, konfiguracji wszystkich skilli foundation oraz action bara. Domyślne wartości odpowiadają referencyjnemu scenariuszowi pomocniczemu, ale możesz je ręcznie zmieniać.";
    }

    private static Map<String, String> parseUrlEncodedBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> fields = new LinkedHashMap<>();
        if (body.isBlank()) {
            return fields;
        }

        String[] pairs = body.split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }
            String[] keyValue = pair.split("=", 2);
            String key = decodeUrlPart(keyValue[0]);
            String value = keyValue.length > 1 ? decodeUrlPart(keyValue[1]) : "";
            fields.put(key, value);
        }
        return fields;
    }

    private static String decodeUrlPart(String rawValue) {
        return URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
    }

    private void renderPage(HttpExchange exchange, CurrentBuildPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

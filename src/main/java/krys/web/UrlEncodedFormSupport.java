package krys.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Wspólna obsługa formularzy `application/x-www-form-urlencoded` dla prostych kontrolerów SSR. */
final class UrlEncodedFormSupport {
    private UrlEncodedFormSupport() {
    }

    static Map<String, String> parseBody(HttpExchange exchange) throws IOException {
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
}

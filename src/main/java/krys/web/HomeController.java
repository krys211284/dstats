package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Kontroler SSR ekranu głównego aplikacji będącego hubem modułów i placeholderów. */
public final class HomeController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final HomePageRenderer renderer;

    public HomeController(HomePageRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if (!"GET".equals(method)) {
                exchange.getResponseHeaders().set("Allow", "GET");
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            HomePageModel pageModel = new HomePageModel(
                    AppModuleRegistry.groupedForHome(),
                    "Strona główna porządkuje dostępne moduły i przyszłe sekcje dodatku bez zgadywania mechanik. Istniejące flow aktualnego buildu, importu, biblioteki i searcha pozostają cienkimi warstwami nad tym samym runtime."
            );
            byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
        } finally {
            exchange.close();
        }
    }
}

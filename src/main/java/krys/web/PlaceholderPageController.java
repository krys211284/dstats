package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Kontroler SSR pojedynczego placeholdera przyszłego modułu. */
public final class PlaceholderPageController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final AppModule module;
    private final PlaceholderPageRenderer renderer;

    public PlaceholderPageController(AppModule module, PlaceholderPageRenderer renderer) {
        this.module = module;
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

            PlaceholderPageModel pageModel = new PlaceholderPageModel(
                    module,
                    AppShellRendererSupport.buildPlaceholderLead(module)
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

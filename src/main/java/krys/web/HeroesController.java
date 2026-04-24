package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.hero.HeroClass;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Kontroler SSR modułu zarządzania wieloma bohaterami i aktywnym bohaterem. */
public final class HeroesController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final HeroService heroService;
    private final HeroesPageRenderer renderer;

    public HeroesController(HeroService heroService, HeroesPageRenderer renderer) {
        this.heroService = heroService;
        this.renderer = renderer;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                renderPage(exchange, buildPageModel(List.of(), List.of()));
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

    private HeroesPageModel handlePost(HttpExchange exchange) throws IOException {
        Map<String, String> fields = UrlEncodedFormSupport.parseBody(exchange);
        String action = fields.getOrDefault("action", "");
        List<String> messages = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try {
            switch (action) {
                case "createHero" -> handleCreateHero(fields, messages);
                case "setActive" -> handleSetActive(fields, messages);
                case "deleteHero" -> handleDelete(fields, messages);
                default -> errors.add("Nieobsługiwana akcja modułu bohaterów.");
            }
        } catch (IllegalArgumentException exception) {
            errors.add(exception.getMessage());
        }
        return buildPageModel(messages, errors);
    }

    private void handleCreateHero(Map<String, String> fields, List<String> messages) {
        String heroName = fields.getOrDefault("heroName", "");
        HeroClass heroClass = HeroClass.valueOf(fields.getOrDefault("heroClass", ""));
        int level = Integer.parseInt(fields.getOrDefault("heroLevel", "0"));
        HeroProfile createdHero = heroService.createHero(heroName, heroClass, level);
        messages.add("Utworzono bohatera " + createdHero.getName() + ".");
        if (heroService.getActiveHero().map(HeroProfile::getHeroId).orElse(0L) == createdHero.getHeroId()) {
            messages.add("Nowy bohater został ustawiony jako aktywny.");
        }
    }

    private void handleSetActive(Map<String, String> fields, List<String> messages) {
        long heroId = Long.parseLong(fields.getOrDefault("heroId", "0"));
        heroService.setActiveHero(heroId);
        messages.add("Zmieniono aktywnego bohatera.");
    }

    private void handleDelete(Map<String, String> fields, List<String> messages) {
        long heroId = Long.parseLong(fields.getOrDefault("heroId", "0"));
        String heroName = heroService.getHeroes().stream()
                .filter(hero -> hero.getHeroId() == heroId)
                .map(HeroProfile::getName)
                .findFirst()
                .orElse("bohater");
        heroService.deleteHero(heroId);
        messages.add("Usunięto bohatera " + heroName + ".");
    }

    private HeroesPageModel buildPageModel(List<String> messages, List<String> errors) {
        return new HeroesPageModel(
                heroService.getHeroes(),
                heroService.getActiveHero().orElse(null),
                messages,
                errors
        );
    }

    private void renderPage(HttpExchange exchange, HeroesPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

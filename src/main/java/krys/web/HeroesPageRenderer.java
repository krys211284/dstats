package krys.web;

import krys.hero.HeroClassDefs;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Renderuje SSR modułu listy bohaterów i aktywnego bohatera. */
public final class HeroesPageRenderer {
    private final String template;

    public HeroesPageRenderer() {
        this.template = loadTemplate();
    }

    public String render(HeroesPageModel model) {
        return template
                .replace("{{GLOBAL_NAV}}", AppShellRendererSupport.renderGlobalNavigation("/bohaterowie"))
                .replace("{{MESSAGES}}", renderMessages(model.getMessages()))
                .replace("{{ERRORS}}", renderErrors(model.getErrors()))
                .replace("{{ACTIVE_HERO_SECTION}}", renderActiveHeroSection(model))
                .replace("{{HERO_LIST}}", renderHeroList(model));
    }

    private static String renderMessages(java.util.List<String> messages) {
        if (messages.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<section class=\"panel panel-success\"><ul class=\"message-list\">");
        for (String message : messages) {
            html.append("<li>").append(CurrentBuildCalculationSectionsRenderer.escapeHtml(message)).append("</li>");
        }
        html.append("</ul></section>");
        return html.toString();
    }

    private static String renderErrors(java.util.List<String> errors) {
        if (errors.isEmpty()) {
            return "";
        }
        StringBuilder html = new StringBuilder("<section class=\"panel panel-error\"><ul class=\"message-list\">");
        for (String error : errors) {
            html.append("<li>").append(CurrentBuildCalculationSectionsRenderer.escapeHtml(error)).append("</li>");
        }
        html.append("</ul></section>");
        return html.toString();
    }

    private static String renderActiveHeroSection(HeroesPageModel model) {
        if (model.getActiveHero() == null) {
            return """
                    <section class="panel">
                        <h2>Brak aktywnego bohatera</h2>
                        <p>Utwórz pierwszego bohatera, aby odblokować ekran aktualnego buildu, import itemu, bibliotekę aktywnych slotów i search w jego kontekście.</p>
                    </section>
                    """;
        }
        HeroProfile hero = model.getActiveHero();
        return """
                <section class="panel active-hero-panel">
                    <h2>Aktywny bohater</h2>
                    <div class="summary-grid">
                        %s
                        %s
                        %s
                    </div>
                </section>
                """.formatted(
                CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Nazwa bohatera", hero.getName()),
                CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Klasa postaci", HeroClassDefs.get(hero.getHeroClass()).getDisplayName()),
                CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Poziom bohatera", hero.getCurrentBuildFormData().getLevel())
        );
    }

    private static String renderHeroList(HeroesPageModel model) {
        if (!model.hasHeroes()) {
            return """
                    <div class="empty-state">
                        <h3>Nie masz jeszcze żadnego bohatera</h3>
                        <p>To jest pierwszy krok do pracy w modelu bohatera i jego buildu. Po utworzeniu bohatera current build, import, biblioteka i search zaczną działać w jego kontekście.</p>
                    </div>
                    """;
        }
        StringBuilder html = new StringBuilder("<div class=\"hero-list\">");
        for (HeroProfile hero : model.getHeroes()) {
            boolean active = model.getActiveHero() != null && model.getActiveHero().getHeroId() == hero.getHeroId();
            html.append("<article class=\"hero-card\"><div class=\"hero-card-top\">")
                    .append(active ? "<span class=\"status-badge status-active\">Aktywny</span>" : "<span class=\"status-badge status-idle\">Nieaktywny</span>")
                    .append("</div><h3>")
                    .append(CurrentBuildCalculationSectionsRenderer.escapeHtml(hero.getName()))
                    .append("</h3><p class=\"helper\">")
                    .append(CurrentBuildCalculationSectionsRenderer.escapeHtml(HeroClassDefs.get(hero.getHeroClass()).getDisplayName()))
                    .append(" | poziom ")
                    .append(CurrentBuildCalculationSectionsRenderer.escapeHtml(hero.getCurrentBuildFormData().getLevel()))
                    .append("</p><div class=\"hero-actions\">");
            if (!active) {
                html.append("<form method=\"post\" action=\"/bohaterowie\" class=\"inline-form\">")
                        .append("<input type=\"hidden\" name=\"action\" value=\"setActive\">")
                        .append("<input type=\"hidden\" name=\"heroId\" value=\"").append(hero.getHeroId()).append("\">")
                        .append("<button type=\"submit\">Ustaw jako aktywnego</button></form>");
            }
            html.append("<form method=\"post\" action=\"/bohaterowie\" class=\"inline-form\">")
                    .append("<input type=\"hidden\" name=\"action\" value=\"deleteHero\">")
                    .append("<input type=\"hidden\" name=\"heroId\" value=\"").append(hero.getHeroId()).append("\">")
                    .append("<button type=\"submit\" class=\"secondary-button\">Usuń</button></form>")
                    .append("</div></article>");
        }
        html.append("</div>");
        return html.toString();
    }

    private static String loadTemplate() {
        try (InputStream inputStream = HeroesPageRenderer.class.getResourceAsStream("/templates/heroes.html")) {
            if (inputStream == null) {
                throw new IllegalStateException("Brak szablonu /templates/heroes.html");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się wczytać szablonu strony bohaterów.", exception);
        }
    }
}

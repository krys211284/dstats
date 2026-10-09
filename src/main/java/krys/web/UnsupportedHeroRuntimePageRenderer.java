package krys.web;

/** Wspólny widok profilu, dla którego ekran obliczeń nie ma jeszcze runtime. */
final class UnsupportedHeroRuntimePageRenderer {
    private UnsupportedHeroRuntimePageRenderer() { }

    static String renderWarning(String message) {
        return "<section class=\"panel panel-warning\"><p>"
                + CurrentBuildCalculationSectionsRenderer.escapeHtml(message) + "</p></section>";
    }

    static String render(HeroProfile hero, String title, String navigationPath, String message) {
        return """
                <!DOCTYPE html><html lang="pl"><head><meta charset="UTF-8"><title>%s</title>
                <style>body { font-family: sans-serif; } .layout { max-width: 1100px; margin: auto; }
                .panel { padding: 20px; margin: 16px 0; border: 1px solid #ddd; border-radius: 12px; }
                %s</style></head><body><main class="layout">%s
                <section class="panel"><h1>%s</h1>%s%s%s</section>%s
                <a href="/bohaterowie">Zarządzaj bohaterami</a></main></body></html>
                """.formatted(CurrentBuildCalculationSectionsRenderer.escapeHtml(title),
                AppShellRendererSupport.renderSharedStyles(), AppShellRendererSupport.renderGlobalNavigation(navigationPath),
                CurrentBuildCalculationSectionsRenderer.escapeHtml(title),
                CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Nazwa bohatera", hero.getName()),
                CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Klasa postaci", hero.getHeroClass().getDisplayName()),
                CurrentBuildCalculationSectionsRenderer.renderSummaryCard("Poziom bohatera", hero.getCurrentBuildFormData().getLevel()),
                renderWarning(message));
    }
}

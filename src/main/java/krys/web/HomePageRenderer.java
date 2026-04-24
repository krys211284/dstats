package krys.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Renderuje ekran główny app shell z grupami modułów i placeholderów. */
public final class HomePageRenderer {
    private final String template;

    public HomePageRenderer() {
        this.template = loadTemplate();
    }

    public String render(HomePageModel model) {
        return template
                .replace("{{GLOBAL_NAV}}", AppShellRendererSupport.renderGlobalNavigation("/"))
                .replace("{{INTRO_TEXT}}", AppShellRendererSupport.escapeHtml(model.getIntroText()))
                .replace("{{MODULE_GROUPS}}", renderModuleGroups(model.getGroupedModules()));
    }

    private static String renderModuleGroups(Map<AppModuleGroup, List<AppModule>> groupedModules) {
        StringBuilder html = new StringBuilder();
        for (AppModuleGroup group : List.of(
                AppModuleGroup.BUILD_TOOLS,
                AppModuleGroup.ITEMS_AND_IMPORT,
                AppModuleGroup.EXPANSION_AND_SEASON
        )) {
            List<AppModule> modules = groupedModules.get(group);
            if (modules == null || modules.isEmpty()) {
                continue;
            }
            html.append("""
                    <section class="module-group">
                        <div class="group-heading">
                    """)
                    .append("<div><h2>")
                    .append(AppShellRendererSupport.escapeHtml(group.getDisplayName()))
                    .append("</h2><p>")
                    .append(AppShellRendererSupport.escapeHtml(group.getDescription()))
                    .append("</p></div></div><div class=\"module-grid\">");
            for (AppModule module : modules) {
                html.append(renderModuleCard(module));
            }
            html.append("</div></section>");
        }
        return html.toString();
    }

    private static String renderModuleCard(AppModule module) {
        return new StringBuilder("""
                <article class="module-card">
                    <div class="module-card-top">
                """)
                .append(AppShellRendererSupport.renderModuleStatusBadge(module.getStatus()))
                .append("""
                        <div class="module-meta">
                """)
                .append(AppShellRendererSupport.escapeHtml(AppShellRendererSupport.buildModuleMetaLabel(module)))
                .append("""
                        </div>
                    </div>
                    <h3>
                """)
                .append(AppShellRendererSupport.escapeHtml(module.getDisplayName()))
                .append("""
                    </h3>
                    <p class="module-description">
                """)
                .append(AppShellRendererSupport.escapeHtml(module.getDescription()))
                .append("""
                    </p>
                    <p class="module-hint">
                """)
                .append(AppShellRendererSupport.escapeHtml(AppShellRendererSupport.renderModuleHint(module)))
                .append("""
                    </p>
                    <a class="module-link" href="
                """)
                .append(AppShellRendererSupport.escapeHtml(module.getUrl()))
                .append("\">")
                .append(AppShellRendererSupport.escapeHtml(module.isActive() ? "Otwórz moduł" : "Otwórz placeholder"))
                .append("</a></article>")
                .toString();
    }

    private static String loadTemplate() {
        try (InputStream inputStream = HomePageRenderer.class.getResourceAsStream("/templates/home.html")) {
            if (inputStream == null) {
                throw new IllegalStateException("Brak szablonu /templates/home.html");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się wczytać szablonu strony głównej app shell.", exception);
        }
    }
}

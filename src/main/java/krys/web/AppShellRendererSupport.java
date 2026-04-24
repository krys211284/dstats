package krys.web;

import java.util.ArrayList;
import java.util.List;

/** Wspólne fragmenty SSR app shell używane przez ekran główny, placeholdery i globalną nawigację. */
public final class AppShellRendererSupport {
    private AppShellRendererSupport() {
    }

    public static String renderGlobalNavigation(String currentPath) {
        StringBuilder html = new StringBuilder("""
                <nav class="app-nav" aria-label="Główna nawigacja aplikacji">
                    <div class="app-nav-title">Diablo 4 DPS Engine / Build WebApp</div>
                    <div class="app-nav-links">
                """);
        for (AppModule module : AppModuleRegistry.navigationModules()) {
            boolean active = isActivePath(currentPath, module.getUrl());
            html.append("<a class=\"app-nav-link")
                    .append(active ? " app-nav-link-active" : "")
                    .append("\" href=\"")
                    .append(escapeHtml(module.getUrl()))
                    .append("\">")
                    .append(escapeHtml(module.getDisplayName()))
                    .append("</a>");
        }
        html.append("""
                    </div>
                </nav>
                """);
        return html.toString();
    }

    public static String renderModuleStatusBadge(AppModuleStatus status) {
        return "<span class=\"module-status " + escapeHtml(status.getCssClassName()) + "\">"
                + escapeHtml(status.getDisplayName())
                + "</span>";
    }

    public static String renderModuleHint(AppModule module) {
        if (module.isActive()) {
            return "Moduł jest już dostępny i działa na obecnym foundation repo.";
        }
        return "To jest placeholder produktowy. Szczegółowa logika zostanie doprecyzowana po stabilizacji zasad po premierze dodatku.";
    }

    public static String buildPlaceholderLead(AppModule module) {
        return module.getDisplayName()
                + " jest przygotowane jako placeholder produktowy. Ta sekcja świadomie nie implementuje jeszcze mechaniki i nie zgaduje zasad dodatku ani sezonu.";
    }

    public static String buildModuleMetaLabel(AppModule module) {
        List<String> labels = new ArrayList<>();
        labels.add(module.isActive() ? "Aktywny moduł" : "Placeholder");
        labels.add(module.getGroup().getDisplayName());
        labels.add(module.getStatus().getDisplayName());
        return String.join(" | ", labels);
    }

    private static boolean isActivePath(String currentPath, String modulePath) {
        if (currentPath == null || currentPath.isBlank()) {
            return "/".equals(modulePath);
        }
        if ("/".equals(modulePath)) {
            return "/".equals(currentPath);
        }
        return currentPath.equals(modulePath) || currentPath.startsWith(modulePath + "/");
    }

    public static String escapeHtml(String value) {
        return CurrentBuildCalculationSectionsRenderer.escapeHtml(value);
    }
}

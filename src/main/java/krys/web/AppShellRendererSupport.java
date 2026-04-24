package krys.web;

import java.util.ArrayList;
import java.util.List;

/** Wspólne fragmenty SSR app shell używane przez ekran główny, placeholdery i globalną nawigację. */
public final class AppShellRendererSupport {
    private AppShellRendererSupport() {
    }

    public static String renderSharedStyles() {
        return """
                :root {
                    --bg-app: #edf3f9;
                    --bg-app-secondary: #f8fbff;
                    --surface-panel: rgba(255, 255, 255, 0.96);
                    --surface-panel-strong: #ffffff;
                    --surface-soft: #f3f7fc;
                    --surface-emphasis: #e9f0ff;
                    --border-default: #c9d6e4;
                    --border-strong: #97abc0;
                    --text-primary: #132235;
                    --text-secondary: #55687f;
                    --accent: #2158d8;
                    --accent-strong: #173e99;
                    --accent-soft: #e7efff;
                    --accent-active-text: #f8fbff;
                    --button-secondary-bg: #e7edf5;
                    --button-secondary-text: #24415f;
                    --status-success: #1f7a49;
                    --status-success-bg: #e5f6eb;
                    --status-info: #1b63a8;
                    --status-info-bg: #e6f1fe;
                    --status-warning: #9a5f00;
                    --status-warning-bg: #fff2da;
                    --status-error: #a3263d;
                    --status-error-bg: #fde8ec;
                    --status-neutral: #6a7b90;
                    --status-neutral-bg: #edf2f7;
                    --shadow-soft: 0 14px 32px rgba(19, 34, 53, 0.08);
                    --shadow-focus: 0 0 0 3px rgba(33, 88, 216, 0.18);
                    --bg: var(--bg-app);
                    --panel: var(--surface-panel);
                    --line: var(--border-default);
                    --text: var(--text-primary);
                    --muted: var(--text-secondary);
                    --nav-bg: rgba(255, 255, 255, 0.92);
                    --success: var(--status-success);
                    --success-bg: var(--status-success-bg);
                    --success-text: var(--status-success);
                    --warning: var(--status-warning);
                    --warning-bg: var(--status-warning-bg);
                    --warning-text: var(--status-warning);
                    --error: var(--status-error);
                    --error-bg: var(--status-error-bg);
                    --available: var(--status-success);
                    --available-bg: var(--status-success-bg);
                    --prep: var(--status-warning);
                    --prep-bg: var(--status-warning-bg);
                    --expansion: var(--status-info);
                    --expansion-bg: var(--status-info-bg);
                    --requires: var(--status-error);
                    --requires-bg: var(--status-error-bg);
                    --seasonal: #6d4bd2;
                    --seasonal-bg: #efe8ff;
                }

                * {
                    box-sizing: border-box;
                }

                body {
                    margin: 0;
                    font-family: "Segoe UI", Tahoma, sans-serif;
                    color: var(--text-primary);
                    background:
                        radial-gradient(circle at top left, rgba(33, 88, 216, 0.16), transparent 28%),
                        radial-gradient(circle at top right, rgba(217, 119, 6, 0.12), transparent 24%),
                        linear-gradient(180deg, var(--bg-app-secondary) 0%, var(--bg-app) 100%);
                }

                .layout {
                    max-width: 1460px;
                    margin: 0 auto;
                    padding: 24px 16px 48px;
                }

                .app-nav {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 14px;
                    align-items: center;
                    justify-content: space-between;
                    margin-bottom: 22px;
                    padding: 14px 16px;
                    border: 1px solid var(--border-default);
                    border-radius: 18px;
                    background: rgba(255, 255, 255, 0.92);
                    box-shadow: var(--shadow-soft);
                    backdrop-filter: blur(10px);
                }

                .app-nav-title {
                    font-weight: 800;
                    color: var(--text-primary);
                    letter-spacing: 0.01em;
                }

                .app-nav-links {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 10px;
                }

                .app-nav-link {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    min-height: 42px;
                    padding: 10px 15px;
                    border-radius: 999px;
                    text-decoration: none;
                    color: var(--text-primary);
                    background: var(--surface-panel-strong);
                    border: 1px solid var(--border-default);
                    font-weight: 700;
                    transition: background-color 0.16s ease, border-color 0.16s ease, color 0.16s ease, transform 0.16s ease, box-shadow 0.16s ease;
                }

                .app-nav-link:hover,
                .app-nav-link:focus-visible {
                    border-color: var(--accent);
                    color: var(--accent-strong);
                    background: var(--accent-soft);
                    transform: translateY(-1px);
                    outline: none;
                    box-shadow: var(--shadow-focus);
                }

                .app-nav-link-active {
                    background: linear-gradient(135deg, var(--accent) 0%, var(--accent-strong) 100%);
                    border-color: transparent;
                    color: var(--accent-active-text);
                    box-shadow: 0 10px 24px rgba(33, 88, 216, 0.28);
                }

                .app-nav-link-active:hover,
                .app-nav-link-active:focus-visible {
                    color: var(--accent-active-text);
                    background: linear-gradient(135deg, #2d66ea 0%, var(--accent-strong) 100%);
                    border-color: transparent;
                }

                .hero,
                .panel,
                .module-group,
                .module-card,
                .hero-card,
                .input-card,
                .summary-box {
                    border: 1px solid var(--border-default);
                    border-radius: 18px;
                    background: var(--surface-panel);
                    box-shadow: var(--shadow-soft);
                }

                .hero,
                .module-group,
                .panel {
                    margin-bottom: 18px;
                    padding: 18px;
                }

                .panel-success {
                    border-color: rgba(31, 122, 73, 0.24);
                    background: var(--status-success-bg);
                }

                .panel-error {
                    border-color: rgba(163, 38, 61, 0.26);
                    background: var(--status-error-bg);
                }

                .panel-warning {
                    border-color: rgba(154, 95, 0, 0.24);
                    background: var(--status-warning-bg);
                }

                .panel-info {
                    border-color: rgba(27, 99, 168, 0.22);
                    background: var(--status-info-bg);
                }

                .eyebrow,
                .section-kicker {
                    display: inline-block;
                    margin-bottom: 10px;
                    padding: 6px 10px;
                    border-radius: 999px;
                    background: var(--accent-soft);
                    color: var(--accent-strong);
                    font-size: 0.82rem;
                    font-weight: 800;
                    letter-spacing: 0.05em;
                    text-transform: uppercase;
                }

                .section-kicker {
                    margin-bottom: 6px;
                    font-size: 0.76rem;
                }

                h1,
                h2,
                h3,
                h4 {
                    margin-top: 0;
                    color: var(--text-primary);
                }

                p {
                    line-height: 1.6;
                }

                .hero p,
                .helper,
                .module-meta,
                .module-hint,
                .module-description,
                .muted,
                .status-note,
                .summary-label {
                    color: var(--text-secondary);
                }

                .module-card,
                .hero-card,
                .input-card,
                .summary-box {
                    background: rgba(255, 255, 255, 0.96);
                }

                .module-card-top,
                .hero-card-top {
                    display: flex;
                    align-items: center;
                    justify-content: space-between;
                    gap: 10px;
                    margin-bottom: 12px;
                }

                label {
                    display: grid;
                    gap: 8px;
                    font-weight: 600;
                }

                input[type="text"],
                input[type="number"],
                input[type="file"],
                select,
                textarea {
                    width: 100%;
                    padding: 11px 12px;
                    border: 1px solid var(--border-default);
                    border-radius: 12px;
                    background: var(--surface-panel-strong);
                    color: var(--text-primary);
                    font: inherit;
                    transition: border-color 0.16s ease, box-shadow 0.16s ease, background-color 0.16s ease;
                }

                input[type="text"]:focus,
                input[type="number"]:focus,
                input[type="file"]:focus,
                select:focus,
                textarea:focus {
                    border-color: var(--accent);
                    outline: none;
                    box-shadow: var(--shadow-focus);
                    background: #fbfdff;
                }

                button,
                .nav-link,
                .link-button,
                .module-link {
                    display: inline-flex;
                    align-items: center;
                    justify-content: center;
                    min-height: 44px;
                    border: none;
                    border-radius: 999px;
                    padding: 11px 18px;
                    background: linear-gradient(135deg, var(--accent) 0%, var(--accent-strong) 100%);
                    color: var(--accent-active-text);
                    font: inherit;
                    font-weight: 700;
                    text-decoration: none;
                    cursor: pointer;
                    transition: transform 0.16s ease, box-shadow 0.16s ease, filter 0.16s ease;
                }

                button:hover,
                button:focus-visible,
                .nav-link:hover,
                .nav-link:focus-visible,
                .link-button:hover,
                .link-button:focus-visible,
                .module-link:hover,
                .module-link:focus-visible {
                    transform: translateY(-1px);
                    filter: brightness(1.02);
                    outline: none;
                    box-shadow: 0 12px 24px rgba(33, 88, 216, 0.22);
                }

                .secondary-button,
                .secondary-link {
                    background: var(--button-secondary-bg);
                    color: var(--button-secondary-text);
                    box-shadow: none;
                }

                .secondary-button:hover,
                .secondary-button:focus-visible,
                .secondary-link:hover,
                .secondary-link:focus-visible {
                    box-shadow: 0 10px 20px rgba(84, 104, 127, 0.16);
                }

                .summary-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(170px, 1fr));
                    gap: 12px;
                }

                .summary-card {
                    padding: 12px;
                    border: 1px solid var(--border-default);
                    border-radius: 14px;
                    background: var(--surface-panel-strong);
                }

                .summary-value {
                    font-size: 1.05rem;
                    font-weight: 800;
                    color: var(--text-primary);
                }

                .status-badge,
                .module-status {
                    display: inline-block;
                    padding: 5px 10px;
                    border-radius: 999px;
                    font-size: 0.82rem;
                    font-weight: 800;
                }

                .status-active,
                .status-available {
                    color: var(--status-success);
                    background: var(--status-success-bg);
                }

                .status-inactive,
                .status-empty,
                .status-idle {
                    color: var(--status-neutral);
                    background: var(--status-neutral-bg);
                }

                .status-in-preparation,
                .status-warning {
                    color: var(--status-warning);
                    background: var(--status-warning-bg);
                }

                .status-after-expansion,
                .status-seasonal,
                .status-info {
                    color: var(--status-info);
                    background: var(--status-info-bg);
                }

                .status-requires-expansion,
                .status-error {
                    color: var(--status-error);
                    background: var(--status-error-bg);
                }

                .empty-state {
                    padding: 22px;
                    border: 1px dashed var(--border-strong);
                    border-radius: 16px;
                    background: linear-gradient(180deg, rgba(243, 247, 252, 0.95) 0%, rgba(255, 255, 255, 0.86) 100%);
                }

                .library-mode-card {
                    display: grid;
                    gap: 12px;
                    padding: 16px;
                    border: 1px solid rgba(33, 88, 216, 0.18);
                    border-radius: 18px;
                    background: linear-gradient(180deg, rgba(231, 239, 255, 0.92) 0%, rgba(255, 255, 255, 0.9) 100%);
                    box-shadow: var(--shadow-soft);
                }

                .checkbox-row,
                .bar-state-item {
                    padding: 12px 14px;
                    border: 1px solid rgba(33, 88, 216, 0.16);
                    border-radius: 14px;
                    background: rgba(255, 255, 255, 0.88);
                }

                .info-strip {
                    margin-top: 14px;
                    padding: 12px 14px;
                    border-radius: 14px;
                    border: 1px solid rgba(27, 99, 168, 0.16);
                    background: var(--status-info-bg);
                    color: #23415f;
                }

                .hero-links,
                .submit-row,
                .nav-row,
                .hero-actions,
                .action-links,
                .slot-actions {
                    display: flex;
                    flex-wrap: wrap;
                    gap: 10px;
                    margin-top: 14px;
                }

                .action-stack {
                    display: grid;
                    gap: 8px;
                }

                .inline-form {
                    display: inline-block;
                }

                .form-grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
                    gap: 14px;
                    align-items: end;
                }

                .subpanel {
                    margin-top: 16px;
                    padding-top: 16px;
                    border-top: 1px solid var(--border-default);
                }

                .data-table {
                    width: 100%;
                    border-collapse: collapse;
                    font-size: 0.94rem;
                }

                .data-table th,
                .data-table td {
                    padding: 10px 8px;
                    border-bottom: 1px solid rgba(85, 104, 127, 0.16);
                    text-align: left;
                    vertical-align: top;
                }

                .data-table thead th {
                    color: var(--text-secondary);
                    font-weight: 700;
                }

                .error-list,
                .message-list {
                    margin: 0;
                    padding-left: 20px;
                }

                .error-list {
                    color: var(--status-error);
                }

                .active-hero-panel,
                .save-feedback-panel,
                .result-panel {
                    position: relative;
                    overflow: hidden;
                }

                .active-hero-panel::before,
                .save-feedback-panel::before,
                .result-panel::before {
                    content: "";
                    position: absolute;
                    inset: 0 auto 0 0;
                    width: 6px;
                    background: linear-gradient(180deg, var(--accent) 0%, var(--accent-strong) 100%);
                }

                @media (max-width: 900px) {
                    .app-nav {
                        align-items: flex-start;
                    }

                    .app-nav-links {
                        width: 100%;
                    }

                    .data-table,
                    .data-table thead,
                    .data-table tbody,
                    .data-table tr,
                    .data-table th,
                    .data-table td {
                        display: block;
                    }

                    .data-table thead {
                        display: none;
                    }

                    .data-table tr {
                        margin-bottom: 12px;
                        padding: 10px;
                        border: 1px solid rgba(85, 104, 127, 0.16);
                        border-radius: 14px;
                        background: var(--surface-panel-strong);
                    }

                    .data-table td {
                        border: none;
                        padding: 6px 0;
                    }
                }
                """;
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

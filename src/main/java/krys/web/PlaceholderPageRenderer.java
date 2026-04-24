package krys.web;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Renderuje lekką stronę placeholdera dla przyszłych sekcji dodatku i sezonu. */
public final class PlaceholderPageRenderer {
    private final String template;

    public PlaceholderPageRenderer() {
        this.template = loadTemplate();
    }

    public String render(PlaceholderPageModel model) {
        AppModule module = model.getModule();
        return template
                .replace("{{PAGE_TITLE}}", AppShellRendererSupport.escapeHtml(module.getDisplayName()))
                .replace("{{APP_SHELL_STYLES}}", AppShellRendererSupport.renderSharedStyles())
                .replace("{{GLOBAL_NAV}}", AppShellRendererSupport.renderGlobalNavigation(module.getUrl()))
                .replace("{{MODULE_NAME}}", AppShellRendererSupport.escapeHtml(module.getDisplayName()))
                .replace("{{GROUP_NAME}}", AppShellRendererSupport.escapeHtml(module.getGroup().getDisplayName()))
                .replace("{{MODULE_DESCRIPTION}}", AppShellRendererSupport.escapeHtml(module.getDescription()))
                .replace("{{STATUS_BADGE}}", AppShellRendererSupport.renderModuleStatusBadge(module.getStatus()))
                .replace("{{LEAD_TEXT}}", AppShellRendererSupport.escapeHtml(model.getLeadText()))
                .replace("{{STATUS_LABEL}}", AppShellRendererSupport.escapeHtml(module.getStatus().getDisplayName()));
    }

    private static String loadTemplate() {
        try (InputStream inputStream = PlaceholderPageRenderer.class.getResourceAsStream("/templates/placeholder-page.html")) {
            if (inputStream == null) {
                throw new IllegalStateException("Brak szablonu /templates/placeholder-page.html");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się wczytać szablonu strony placeholdera.", exception);
        }
    }
}

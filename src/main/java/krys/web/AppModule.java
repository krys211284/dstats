package krys.web;

/** Jeden moduł aplikacji używany przez app shell, hub SSR i globalną nawigację. */
public final class AppModule {
    private final String id;
    private final String displayName;
    private final String description;
    private final AppModuleGroup group;
    private final AppModuleStatus status;
    private final String url;
    private final boolean active;
    private final boolean placeholder;

    public AppModule(String id,
                     String displayName,
                     String description,
                     AppModuleGroup group,
                     AppModuleStatus status,
                     String url,
                     boolean active,
                     boolean placeholder) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Id modułu jest wymagane.");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("Nazwa modułu jest wymagana.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Opis modułu jest wymagany.");
        }
        if (group == null) {
            throw new IllegalArgumentException("Grupa modułu jest wymagana.");
        }
        if (status == null) {
            throw new IllegalArgumentException("Status modułu jest wymagany.");
        }
        if (url == null || url.isBlank() || !url.startsWith("/")) {
            throw new IllegalArgumentException("URL modułu musi zaczynać się od '/'.");
        }
        if (active && placeholder) {
            throw new IllegalArgumentException("Moduł nie może być równocześnie aktywny i placeholderem.");
        }

        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.group = group;
        this.status = status;
        this.url = url;
        this.active = active;
        this.placeholder = placeholder;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public AppModuleGroup getGroup() {
        return group;
    }

    public AppModuleStatus getStatus() {
        return status;
    }

    public String getUrl() {
        return url;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isPlaceholder() {
        return placeholder;
    }
}

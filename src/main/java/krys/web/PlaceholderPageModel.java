package krys.web;

/** Model widoku placeholdera przyszłego modułu aplikacji. */
public final class PlaceholderPageModel {
    private final AppModule module;
    private final String leadText;

    public PlaceholderPageModel(AppModule module, String leadText) {
        this.module = module;
        this.leadText = leadText;
    }

    public AppModule getModule() {
        return module;
    }

    public String getLeadText() {
        return leadText;
    }
}

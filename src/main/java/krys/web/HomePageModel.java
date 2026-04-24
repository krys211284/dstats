package krys.web;

import java.util.Map;

/** Model widoku ekranu głównego app shell z grupami modułów i statusem sekcji. */
public final class HomePageModel {
    private final Map<AppModuleGroup, java.util.List<AppModule>> groupedModules;
    private final String introText;

    public HomePageModel(Map<AppModuleGroup, java.util.List<AppModule>> groupedModules, String introText) {
        this.groupedModules = groupedModules;
        this.introText = introText;
    }

    public Map<AppModuleGroup, java.util.List<AppModule>> getGroupedModules() {
        return groupedModules;
    }

    public String getIntroText() {
        return introText;
    }
}

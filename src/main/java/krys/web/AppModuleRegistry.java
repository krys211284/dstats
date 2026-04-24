package krys.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Centralny rejestr modułów aplikacji używany przez routing, hub i globalną nawigację SSR. */
public final class AppModuleRegistry {
    private static final List<AppModule> MODULES = List.of(
            new AppModule(
                    "home",
                    "Strona główna",
                    "Hub aplikacji grupujący dostępne moduły, statusy i placeholdery przyszłych sekcji.",
                    AppModuleGroup.APP_SHELL,
                    AppModuleStatus.AVAILABLE,
                    "/",
                    true,
                    false
            ),
            new AppModule(
                    "current-build",
                    "Policz aktualny build",
                    "Manual simulation nad istniejącym runtime i efektywnymi statami current build.",
                    AppModuleGroup.BUILD_TOOLS,
                    AppModuleStatus.AVAILABLE,
                    "/policz-aktualny-build",
                    true,
                    false
            ),
            new AppModule(
                    "search-build",
                    "Znajdź najlepszy build",
                    "Backendowy search i drill-down nad tym samym runtime co current build.",
                    AppModuleGroup.BUILD_TOOLS,
                    AppModuleStatus.AVAILABLE,
                    "/znajdz-najlepszy-build",
                    true,
                    false
            ),
            new AppModule(
                    "item-import",
                    "Importuj item ze screena",
                    "Wspomagany SSR import pojedynczego itemu z ręcznym potwierdzeniem pól.",
                    AppModuleGroup.ITEMS_AND_IMPORT,
                    AppModuleStatus.AVAILABLE,
                    "/importuj-item-ze-screena",
                    true,
                    false
            ),
            new AppModule(
                    "item-library",
                    "Biblioteka itemów",
                    "Trwała biblioteka zapisanych itemów z aktywnym wyborem per slot.",
                    AppModuleGroup.ITEMS_AND_IMPORT,
                    AppModuleStatus.AVAILABLE,
                    "/biblioteka-itemow",
                    true,
                    false
            ),
            new AppModule(
                    "war-plans",
                    "Plany Wojenne",
                    "Placeholder pod przyszły moduł produktowy sezonowej progresji i planowania aktywności.",
                    AppModuleGroup.EXPANSION_AND_SEASON,
                    AppModuleStatus.SEASONAL,
                    "/plany-wojenne",
                    false,
                    true
            ),
            new AppModule(
                    "medallion",
                    "Medalion",
                    "Placeholder pod moduł systemu dodatku wymagający stabilizacji zasad po premierze.",
                    AppModuleGroup.EXPANSION_AND_SEASON,
                    AppModuleStatus.REQUIRES_EXPANSION,
                    "/medalion",
                    false,
                    true
            ),
            new AppModule(
                    "horadric-cube",
                    "Kostka Horadrimów",
                    "Placeholder pod przyszły moduł konfiguracji systemu dodatku bez zgadywania mechaniki.",
                    AppModuleGroup.EXPANSION_AND_SEASON,
                    AppModuleStatus.REQUIRES_EXPANSION,
                    "/kostka-horadrimow",
                    false,
                    true
            ),
            new AppModule(
                    "loot-filter",
                    "Filtr łupów",
                    "Placeholder pod produktowy moduł filtrowania dropu po doprecyzowaniu zakresu funkcji.",
                    AppModuleGroup.EXPANSION_AND_SEASON,
                    AppModuleStatus.IN_PREPARATION,
                    "/filtr-lupow",
                    false,
                    true
            ),
            new AppModule(
                    "skill-tree-3",
                    "Drzewka umiejętności 3.0",
                    "Placeholder pod przyszłe ekrany nowych drzewek po ustabilizowaniu finalnych zasad wersji 3.0.",
                    AppModuleGroup.EXPANSION_AND_SEASON,
                    AppModuleStatus.AFTER_EXPANSION_LAUNCH,
                    "/drzewka-umiejetnosci-3-0",
                    false,
                    true
            ),
            new AppModule(
                    "item-system-3",
                    "System przedmiotów 3.0",
                    "Placeholder pod nową architekturę itemów po ustabilizowaniu kontraktów dodatku.",
                    AppModuleGroup.EXPANSION_AND_SEASON,
                    AppModuleStatus.AFTER_EXPANSION_LAUNCH,
                    "/system-przedmiotow-3-0",
                    false,
                    true
            ),
            new AppModule(
                    "tower-rankings",
                    "Wieża / rankingi",
                    "Placeholder pod przyszły moduł rankingów i sekcji endgame bez implementacji mechaniki.",
                    AppModuleGroup.EXPANSION_AND_SEASON,
                    AppModuleStatus.IN_PREPARATION,
                    "/wieza-i-rankingi",
                    false,
                    true
            ),
            new AppModule(
                    "resonating-hatred",
                    "Rezonująca Nienawiść",
                    "Placeholder pod sezonową sekcję wydarzeń i aktywności po premierze dodatku.",
                    AppModuleGroup.EXPANSION_AND_SEASON,
                    AppModuleStatus.SEASONAL,
                    "/rezonujaca-nienawisc",
                    false,
                    true
            ),
            new AppModule(
                    "fishing",
                    "Wędkarstwo",
                    "Placeholder pod przyszły lżejszy moduł poboczny, celowo bez implementacji zasad na tym etapie.",
                    AppModuleGroup.EXPANSION_AND_SEASON,
                    AppModuleStatus.AFTER_EXPANSION_LAUNCH,
                    "/wedkarstwo",
                    false,
                    true
            )
    );

    private AppModuleRegistry() {
    }

    public static List<AppModule> all() {
        return MODULES;
    }

    public static AppModule homeModule() {
        return findById("home").orElseThrow();
    }

    public static List<AppModule> navigationModules() {
        List<AppModule> modules = new ArrayList<>();
        modules.add(homeModule());
        for (AppModule module : MODULES) {
            if ("home".equals(module.getId())) {
                continue;
            }
            if (module.isActive()) {
                modules.add(module);
            }
        }
        return Collections.unmodifiableList(modules);
    }

    public static Map<AppModuleGroup, List<AppModule>> groupedForHome() {
        EnumMap<AppModuleGroup, List<AppModule>> grouped = new EnumMap<>(AppModuleGroup.class);
        for (AppModuleGroup group : AppModuleGroup.values()) {
            if (group == AppModuleGroup.APP_SHELL) {
                continue;
            }
            grouped.put(group, new ArrayList<>());
        }
        for (AppModule module : MODULES) {
            if (module.getGroup() == AppModuleGroup.APP_SHELL) {
                continue;
            }
            grouped.get(module.getGroup()).add(module);
        }
        EnumMap<AppModuleGroup, List<AppModule>> view = new EnumMap<>(AppModuleGroup.class);
        for (Map.Entry<AppModuleGroup, List<AppModule>> entry : grouped.entrySet()) {
            view.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(view);
    }

    public static List<AppModule> placeholderModules() {
        List<AppModule> modules = new ArrayList<>();
        for (AppModule module : MODULES) {
            if (module.isPlaceholder()) {
                modules.add(module);
            }
        }
        return Collections.unmodifiableList(modules);
    }

    public static Optional<AppModule> findById(String id) {
        return MODULES.stream()
                .filter(module -> module.getId().equals(id))
                .findFirst();
    }

    public static Optional<AppModule> findByUrl(String url) {
        return MODULES.stream()
                .filter(module -> module.getUrl().equals(url))
                .findFirst();
    }
}

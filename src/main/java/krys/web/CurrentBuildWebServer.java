package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import krys.app.CurrentBuildCalculationService;
import krys.combat.DamageEngine;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryDataDirectoryResolver;
import krys.itemlibrary.ItemLibraryService;
import krys.itemimport.ItemImageImportService;
import krys.itemknowledge.FileItemKnowledgeRepository;
import krys.itemknowledge.ItemKnowledgeService;
import krys.ranking.DamageRankingService;
import krys.ranking.SkillTreeRegistryProvider;
import krys.search.BuildSearchCalculationService;
import krys.search.BuildSearchEvaluationService;
import krys.simulation.ManualSimulationService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Najprostszy lokalny serwer HTTP dla M8 uruchamiający SSR nad istniejącym runtime manual simulation. */
public final class CurrentBuildWebServer implements AutoCloseable {
    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 8080;
    private final HttpServer server;
    private final String bindHost;

    public CurrentBuildWebServer(int port) throws IOException {
        this(DEFAULT_HOST, port, new ItemLibraryDataDirectoryResolver().resolveDataDirectory());
    }

    public CurrentBuildWebServer(int port, Path itemLibraryDataDirectory) throws IOException {
        this(DEFAULT_HOST, port, itemLibraryDataDirectory);
    }

    public CurrentBuildWebServer(String host, int port) throws IOException {
        this(host, port, new ItemLibraryDataDirectoryResolver().resolveDataDirectory());
    }

    public CurrentBuildWebServer(String host, int port, Path itemLibraryDataDirectory) throws IOException {
        this.bindHost = requireHost(host);
        this.server = HttpServer.create(new InetSocketAddress(this.bindHost, port), 0);

        ItemLibraryService itemLibraryService = new ItemLibraryService(
                new FileItemLibraryRepository(itemLibraryDataDirectory)
        );
        ItemKnowledgeService itemKnowledgeService = new ItemKnowledgeService(
                new FileItemKnowledgeRepository(itemLibraryDataDirectory)
        );
        HeroService heroService = new HeroService(
                new FileHeroProfileRepository(itemLibraryDataDirectory)
        );
        CurrentBuildCalculationService calculationService = new CurrentBuildCalculationService(
                new ManualSimulationService(new DamageEngine())
        );
        CurrentBuildController controller = new CurrentBuildController(
                calculationService,
                new CurrentBuildPageRenderer(),
                itemLibraryService,
                heroService
        );
        HomeController homeController = new HomeController(new HomePageRenderer());
        HeroesController heroesController = new HeroesController(
                heroService,
                new HeroesPageRenderer()
        );
        SearchBuildDetailsController searchBuildDetailsController = new SearchBuildDetailsController(
                calculationService,
                new SearchBuildDetailsPageRenderer(),
                heroService
        );
        BuildSearchCalculationService searchCalculationService = new BuildSearchCalculationService(
                new BuildSearchEvaluationService(new ManualSimulationService(new DamageEngine())),
                itemLibraryService
        );
        SearchBuildController searchController = new SearchBuildController(
                searchCalculationService,
                new SearchBuildPageRenderer(),
                itemLibraryService,
                heroService
        );
        SkillTreeRegistryProvider skillTreeRegistryProvider = SkillTreeRegistryProvider.paladinOnly();
        DamageRankingController damageRankingController = new DamageRankingController(
                new DamageRankingService(new DamageEngine(), skillTreeRegistryProvider),
                new DamageRankingPageRenderer(),
                skillTreeRegistryProvider
        );
        ItemImportController itemImportController = new ItemImportController(
                new ItemImageImportService(),
                new ItemImportPageRenderer(),
                itemLibraryService,
                itemKnowledgeService,
                heroService
        );
        ItemKnowledgeController itemKnowledgeController = new ItemKnowledgeController(
                itemKnowledgeService,
                new ItemKnowledgePageRenderer()
        );
        ItemLibraryController itemLibraryController = new ItemLibraryController(
                itemLibraryService,
                new ItemLibraryPageRenderer(),
                heroService
        );
        ItemEditController itemEditController = new ItemEditController(itemLibraryService);
        PlaceholderPageRenderer placeholderPageRenderer = new PlaceholderPageRenderer();

        server.createContext("/bohaterowie", heroesController);
        server.createContext("/policz-aktualny-build", controller);
        server.createContext("/znajdz-najlepszy-build", searchController);
        server.createContext("/znajdz-najlepszy-build/szczegoly", searchBuildDetailsController);
        server.createContext("/ranking-obrazen", damageRankingController);
        server.createContext("/ranking-obrazen-paladyna", damageRankingController);
        server.createContext("/importuj-item-ze-screena", itemImportController);
        server.createContext("/biblioteka-itemow/edytuj", itemEditController);
        server.createContext("/biblioteka-itemow", itemLibraryController);
        server.createContext("/baza-wiedzy-itemow", itemKnowledgeController);
        for (AppModule module : AppModuleRegistry.placeholderModules()) {
            server.createContext(module.getUrl(), new PlaceholderPageController(module, placeholderPageRenderer));
        }
        server.createContext("/", new RootHandler(homeController));
    }

    public void start() {
        server.start();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    public String getBindHost() {
        return bindHost;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    public static void main(String[] args) throws Exception {
        ServerArguments serverArguments = parseArguments(args);
        CurrentBuildWebServer webServer = new CurrentBuildWebServer(serverArguments.host(), serverArguments.port());
        webServer.start();
        printStartupMessages(webServer);
        System.out.println("Drill-down searcha jest dostępny z poziomu listy wyników GUI searcha.");

        synchronized (CurrentBuildWebServer.class) {
            CurrentBuildWebServer.class.wait();
        }
    }

    static ServerArguments parseArguments(String[] args) {
        String host = DEFAULT_HOST;
        int port = DEFAULT_PORT;
        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            switch (argument) {
                case "--port" -> {
                    if (index + 1 >= args.length) {
                        throw new IllegalArgumentException("Brak wartości po argumencie --port.");
                    }
                    port = parsePort(args[++index]);
                }
                case "--host", "--address" -> {
                    if (index + 1 >= args.length) {
                        throw new IllegalArgumentException("Brak wartości po argumencie " + argument + ".");
                    }
                    host = requireHost(args[++index]);
                }
                default -> throw new IllegalArgumentException("Nieznany argument: " + argument + ". Obsługiwane argumenty: --host, --address, --port.");
            }
        }
        return new ServerArguments(host, port);
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65535) {
                throw new IllegalArgumentException("Port musi być z zakresu 1..65535: " + value);
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Port musi być liczbą całkowitą z zakresu 1..65535: " + value, exception);
        }
    }

    private static String requireHost(String host) {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("Host nie może być pusty.");
        }
        return host;
    }

    private static void printStartupMessages(CurrentBuildWebServer webServer) {
        String host = webServer.getBindHost();
        int port = webServer.getPort();
        String baseUrl = "http://" + host + ":" + port;

        System.out.println("Server started at " + baseUrl + "/");
        System.out.println("Bind host: " + host);
        System.out.println("Port: " + port);
        if ("0.0.0.0".equals(host)) {
            System.out.println("LAN access enabled. Open http://<computer-ip>:" + port + "/ from another device on the same network, if firewall allows inbound TCP " + port + ".");
        }
        System.out.println("GUI manual simulation dostępne pod adresem: " + baseUrl + "/policz-aktualny-build");
        System.out.println("GUI search dostępne pod adresem: " + baseUrl + "/znajdz-najlepszy-build");
        System.out.println("GUI rankingu obrażeń dostępne pod adresem: " + baseUrl + "/ranking-obrazen");
        System.out.println("Legacy alias rankingu Paladyna: " + baseUrl + "/ranking-obrazen-paladyna");
        System.out.println("GUI importu itemu dostępne pod adresem: " + baseUrl + "/importuj-item-ze-screena");
        System.out.println("GUI biblioteki itemów dostępne pod adresem: " + baseUrl + "/biblioteka-itemow");
        System.out.println("GUI bazy wiedzy itemów dostępne pod adresem: " + baseUrl + "/baza-wiedzy-itemow");
    }

    record ServerArguments(String host, int port) {
    }

    /** Obsługuje ekran wejściowy pod rootem i odrzuca pozostałe ścieżki. */
    private static final class RootHandler implements HttpHandler {
        private final HomeController homeController;

        private RootHandler(HomeController homeController) {
            this.homeController = homeController;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("/".equals(exchange.getRequestURI().getPath())) {
                homeController.handle(exchange);
                return;
            }

            byte[] response = "404".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }
    }
}

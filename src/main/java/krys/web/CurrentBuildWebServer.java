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
import krys.search.BuildSearchCalculationService;
import krys.search.BuildSearchEvaluationService;
import krys.simulation.ManualSimulationService;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/** Najprostszy lokalny serwer HTTP dla M8 uruchamiający SSR nad istniejącym runtime manual simulation. */
public final class CurrentBuildWebServer implements AutoCloseable {
    private final HttpServer server;

    public CurrentBuildWebServer(int port) throws IOException {
        this(port, new ItemLibraryDataDirectoryResolver().resolveDataDirectory());
    }

    public CurrentBuildWebServer(int port, Path itemLibraryDataDirectory) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);

        ItemLibraryService itemLibraryService = new ItemLibraryService(
                new FileItemLibraryRepository(itemLibraryDataDirectory)
        );
        CurrentBuildCalculationService calculationService = new CurrentBuildCalculationService(
                new ManualSimulationService(new DamageEngine())
        );
        CurrentBuildController controller = new CurrentBuildController(
                calculationService,
                new CurrentBuildPageRenderer(),
                itemLibraryService
        );
        SearchBuildDetailsController searchBuildDetailsController = new SearchBuildDetailsController(
                calculationService,
                new SearchBuildDetailsPageRenderer()
        );
        BuildSearchCalculationService searchCalculationService = new BuildSearchCalculationService(
                new BuildSearchEvaluationService(new ManualSimulationService(new DamageEngine()))
        );
        SearchBuildController searchController = new SearchBuildController(
                searchCalculationService,
                new SearchBuildPageRenderer()
        );
        ItemImportController itemImportController = new ItemImportController(
                new ItemImageImportService(),
                new ItemImportPageRenderer()
        );
        ItemLibraryController itemLibraryController = new ItemLibraryController(
                itemLibraryService,
                new ItemLibraryPageRenderer()
        );

        server.createContext("/policz-aktualny-build", controller);
        server.createContext("/znajdz-najlepszy-build", searchController);
        server.createContext("/znajdz-najlepszy-build/szczegoly", searchBuildDetailsController);
        server.createContext("/importuj-item-ze-screena", itemImportController);
        server.createContext("/biblioteka-itemow", itemLibraryController);
        server.createContext("/", new RootHandler(controller));
    }

    public void start() {
        server.start();
    }

    public int getPort() {
        return server.getAddress().getPort();
    }

    @Override
    public void close() {
        server.stop(0);
    }

    public static void main(String[] args) throws Exception {
        int port = parsePort(args);
        CurrentBuildWebServer webServer = new CurrentBuildWebServer(port);
        webServer.start();
        System.out.println("GUI manual simulation dostępne pod adresem: http://127.0.0.1:" + webServer.getPort() + "/policz-aktualny-build");
        System.out.println("GUI search dostępne pod adresem: http://127.0.0.1:" + webServer.getPort() + "/znajdz-najlepszy-build");
        System.out.println("GUI importu itemu dostępne pod adresem: http://127.0.0.1:" + webServer.getPort() + "/importuj-item-ze-screena");
        System.out.println("GUI biblioteki itemów dostępne pod adresem: http://127.0.0.1:" + webServer.getPort() + "/biblioteka-itemow");
        System.out.println("Drill-down searcha jest dostępny z poziomu listy wyników GUI searcha.");

        synchronized (CurrentBuildWebServer.class) {
            CurrentBuildWebServer.class.wait();
        }
    }

    private static int parsePort(String[] args) {
        int port = 8080;
        for (int index = 0; index < args.length; index++) {
            if ("--port".equals(args[index]) && index + 1 < args.length) {
                port = Integer.parseInt(args[++index]);
            }
        }
        return port;
    }

    /** Obsługuje ekran wejściowy pod rootem i odrzuca pozostałe ścieżki. */
    private static final class RootHandler implements HttpHandler {
        private final CurrentBuildController controller;

        private RootHandler(CurrentBuildController controller) {
            this.controller = controller;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("/".equals(exchange.getRequestURI().getPath())) {
                controller.handle(exchange);
                return;
            }

            byte[] response = "404".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        }
    }
}

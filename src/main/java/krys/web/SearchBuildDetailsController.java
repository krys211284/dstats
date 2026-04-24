package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.app.CurrentBuildCalculation;
import krys.app.CurrentBuildCalculationService;
import krys.item.EquipmentSlot;
import krys.itemlibrary.ItemLibrarySearchCombination;
import krys.itemlibrary.SavedImportedItem;
import krys.search.BuildSearchCandidate;
import krys.itemimport.CurrentBuildImportableStats;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Kontroler SSR drill-downu M12 z listy wyników searcha do pełnej analizy reprezentanta. */
public final class SearchBuildDetailsController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final CurrentBuildCalculationService calculationService;
    private final SearchBuildDetailsPageRenderer renderer;
    private final CurrentBuildFormMapper formMapper;

    public SearchBuildDetailsController(CurrentBuildCalculationService calculationService,
                                        SearchBuildDetailsPageRenderer renderer) {
        this(calculationService, renderer, new CurrentBuildFormMapper());
    }

    SearchBuildDetailsController(CurrentBuildCalculationService calculationService,
                                 SearchBuildDetailsPageRenderer renderer,
                                 CurrentBuildFormMapper formMapper) {
        this.calculationService = calculationService;
        this.renderer = renderer;
        this.formMapper = formMapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("POST".equals(method)) {
                renderPage(exchange, handlePost(exchange));
                return;
            }
            exchange.getResponseHeaders().set("Allow", "POST");
            exchange.sendResponseHeaders(405, -1);
        } finally {
            exchange.close();
        }
    }

    private SearchBuildDetailsPageModel handlePost(HttpExchange exchange) throws IOException {
        Map<String, String> fields = UrlEncodedFormSupport.parseBody(exchange);
        int selectedRank = parseSelectedRank(fields);
        CurrentBuildFormData formData = CurrentBuildFormData.fromFormFields(fields);
        List<String> errors = new ArrayList<>();
        BuildSearchCandidate candidate = null;
        CurrentBuildCalculation calculation = null;
        boolean useItemLibrary = Boolean.parseBoolean(fields.getOrDefault("useItemLibrary", "false"));
        ItemLibrarySearchCombination itemLibraryCombination = parseItemLibraryCombination(fields, errors);

        CurrentBuildFormMapper.MappingResult mappingResult = formMapper.map(formData);
        errors.addAll(mappingResult.getErrors());
        if (mappingResult.getRequest() != null) {
            candidate = new BuildSearchCandidate(mappingResult.getRequest(), useItemLibrary, itemLibraryCombination);
        }
        if (errors.isEmpty() && mappingResult.getRequest() != null) {
            try {
                calculation = calculationService.calculate(mappingResult.getRequest());
            } catch (IllegalArgumentException exception) {
                errors.add(exception.getMessage());
            }
        }

        return new SearchBuildDetailsPageModel(
                selectedRank,
                errors,
                candidate,
                calculation,
                "Drill-down M12 odtwarza reprezentanta znormalizowanego wyniku na tym samym runtime co flow „Policz aktualny build”."
        );
    }

    private static int parseSelectedRank(Map<String, String> fields) {
        String rawSelectedRank = fields.get("selectedRank");
        if (rawSelectedRank == null || rawSelectedRank.isBlank()) {
            return 0;
        }
        try {
            int selectedRank = Integer.parseInt(rawSelectedRank);
            return Math.max(selectedRank, 0);
        } catch (NumberFormatException exception) {
            return 0;
        }
    }

    private static ItemLibrarySearchCombination parseItemLibraryCombination(Map<String, String> fields, List<String> errors) {
        String rawCount = fields.get("itemLibrarySelectedCount");
        if (rawCount == null || rawCount.isBlank()) {
            return ItemLibrarySearchCombination.empty();
        }

        int selectedCount;
        try {
            selectedCount = Math.max(0, Integer.parseInt(rawCount));
        } catch (NumberFormatException exception) {
            errors.add("Drill-down searcha otrzymał niepoprawną liczbę itemów biblioteki.");
            return ItemLibrarySearchCombination.empty();
        }

        List<SavedImportedItem> selectedItems = new ArrayList<>();
        for (int index = 0; index < selectedCount; index++) {
            SavedImportedItem item = parseLibraryItem(fields, index, errors);
            if (item != null) {
                selectedItems.add(item);
            }
        }
        if (!errors.isEmpty()) {
            return ItemLibrarySearchCombination.empty();
        }

        return new ItemLibrarySearchCombination(selectedItems, aggregateContribution(selectedItems));
    }

    private static SavedImportedItem parseLibraryItem(Map<String, String> fields, int index, List<String> errors) {
        try {
            return new SavedImportedItem(
                    parseLong(fields.get("itemLibraryItemId_" + index), "id itemu biblioteki", errors),
                    requireField(fields, "itemLibraryDisplayName_" + index, "display name itemu biblioteki", errors),
                    requireField(fields, "itemLibrarySourceImageName_" + index, "nazwę pliku źródłowego itemu biblioteki", errors),
                    EquipmentSlot.valueOf(requireField(fields, "itemLibrarySlot_" + index, "slot itemu biblioteki", errors)),
                    parseLong(fields.get("itemLibraryWeaponDamage_" + index), "weapon damage itemu biblioteki", errors),
                    parseDouble(fields.get("itemLibraryStrength_" + index), "strength itemu biblioteki", errors),
                    parseDouble(fields.get("itemLibraryIntelligence_" + index), "intelligence itemu biblioteki", errors),
                    parseDouble(fields.get("itemLibraryThorns_" + index), "thorns itemu biblioteki", errors),
                    parseDouble(fields.get("itemLibraryBlockChance_" + index), "block chance itemu biblioteki", errors),
                    parseDouble(fields.get("itemLibraryRetributionChance_" + index), "retribution chance itemu biblioteki", errors)
            );
        } catch (IllegalArgumentException exception) {
            errors.add("Drill-down searcha otrzymał niepoprawny item biblioteki.");
            return null;
        }
    }

    private static CurrentBuildImportableStats aggregateContribution(List<SavedImportedItem> selectedItems) {
        long weaponDamage = 0L;
        double strength = 0.0d;
        double intelligence = 0.0d;
        double thorns = 0.0d;
        double blockChance = 0.0d;
        double retributionChance = 0.0d;
        for (SavedImportedItem item : selectedItems) {
            weaponDamage += item.getWeaponDamage();
            strength += item.getStrength();
            intelligence += item.getIntelligence();
            thorns += item.getThorns();
            blockChance += item.getBlockChance();
            retributionChance += item.getRetributionChance();
        }
        return new CurrentBuildImportableStats(weaponDamage, strength, intelligence, thorns, blockChance, retributionChance);
    }

    private static String requireField(Map<String, String> fields, String fieldName, String label, List<String> errors) {
        String value = fields.get(fieldName);
        if (value == null || value.isBlank()) {
            errors.add("Drill-down searcha utracił " + label + ".");
            return "";
        }
        return value;
    }

    private static long parseLong(String rawValue, String label, List<String> errors) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException | NullPointerException exception) {
            errors.add("Drill-down searcha utracił " + label + ".");
            return 0L;
        }
    }

    private static double parseDouble(String rawValue, String label, List<String> errors) {
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException | NullPointerException exception) {
            errors.add("Drill-down searcha utracił " + label + ".");
            return 0.0d;
        }
    }

    private void renderPage(HttpExchange exchange, SearchBuildDetailsPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

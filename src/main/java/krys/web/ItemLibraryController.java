package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.item.EquipmentSlot;
import krys.itemlibrary.ItemLibraryPresentationSupport;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportFormMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Kontroler SSR minimalnej biblioteki zapisanych itemów z aktywnym wyborem per slot. */
public final class ItemLibraryController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final ItemLibraryService itemLibraryService;
    private final ItemLibraryPageRenderer renderer;
    private final ItemImportFormMapper itemImportFormMapper;

    public ItemLibraryController(ItemLibraryService itemLibraryService,
                                 ItemLibraryPageRenderer renderer) {
        this(itemLibraryService, renderer, new ItemImportFormMapper());
    }

    ItemLibraryController(ItemLibraryService itemLibraryService,
                          ItemLibraryPageRenderer renderer,
                          ItemImportFormMapper itemImportFormMapper) {
        this.itemLibraryService = itemLibraryService;
        this.renderer = renderer;
        this.itemImportFormMapper = itemImportFormMapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                String currentBuildQuery = CurrentBuildFormQuerySupport.toQuery(
                        CurrentBuildFormQuerySupport.resolveImportContext(
                                UrlEncodedFormSupport.parseQuery(exchange.getRequestURI().getRawQuery())
                        )
                );
                renderPage(exchange, buildPageModel(List.of(), List.of(), currentBuildQuery, null));
                return;
            }
            if ("POST".equals(method)) {
                renderPage(exchange, handlePost(exchange));
                return;
            }
            exchange.getResponseHeaders().set("Allow", "GET, POST");
            exchange.sendResponseHeaders(405, -1);
        } finally {
            exchange.close();
        }
    }

    private ItemLibraryPageModel handlePost(HttpExchange exchange) throws IOException {
        Map<String, String> fields = UrlEncodedFormSupport.parseBody(exchange);
        String currentBuildQuery = fields.getOrDefault("currentBuildQuery", "");
        String action = fields.getOrDefault("action", "");
        try {
            return switch (action) {
                case "saveImportedItem" -> handleSaveImportedItem(fields, currentBuildQuery);
                case "activateItem" -> handleActivateItem(fields, currentBuildQuery);
                case "deleteItem" -> handleDeleteItem(fields, currentBuildQuery);
                default -> buildPageModel(List.of("Nieobsługiwana akcja biblioteki itemów."), List.of(), currentBuildQuery, null);
            };
        } catch (IllegalArgumentException exception) {
            return buildPageModel(List.of(exception.getMessage()), List.of(), currentBuildQuery, null);
        }
    }

    private ItemLibraryPageModel handleSaveImportedItem(Map<String, String> fields, String currentBuildQuery) {
        ItemImportEditableForm form = new ItemImportEditableForm(
                fields.getOrDefault("sourceImageName", ""),
                fields.getOrDefault("slot", ""),
                fields.getOrDefault("weaponDamage", ""),
                fields.getOrDefault("strength", ""),
                fields.getOrDefault("intelligence", ""),
                fields.getOrDefault("thorns", ""),
                fields.getOrDefault("blockChance", ""),
                fields.getOrDefault("retributionChance", "")
        );
        ItemImportFormMapper.MappingResult mappingResult = itemImportFormMapper.map(form);
        if (!mappingResult.getErrors().isEmpty() || mappingResult.getItem() == null) {
            return buildPageModel(mappingResult.getErrors(), List.of(), currentBuildQuery, null);
        }

        SavedImportedItem savedItem = itemLibraryService.saveImportedItem(mappingResult.getItem());
        return buildPageModel(
                List.of(),
                List.of("Zapisano item w bibliotece: " + savedItem.getDisplayName() + "."),
                currentBuildQuery,
                savedItem
        );
    }

    private ItemLibraryPageModel handleActivateItem(Map<String, String> fields, String currentBuildQuery) {
        long itemId = parseItemId(fields.getOrDefault("itemId", ""));
        EquipmentSlot slot = EquipmentSlot.valueOf(fields.getOrDefault("slot", ""));
        itemLibraryService.setActiveItem(slot, itemId);
        return buildPageModel(
                List.of(),
                List.of("Aktywny item dla slotu " + ItemLibraryPresentationSupport.slotDisplayName(slot) + " został zmieniony. Nowy wybór zastępuje poprzedni aktywny item w tym samym slocie."),
                currentBuildQuery,
                null
        );
    }

    private ItemLibraryPageModel handleDeleteItem(Map<String, String> fields, String currentBuildQuery) {
        long itemId = parseItemId(fields.getOrDefault("itemId", ""));
        itemLibraryService.deleteItem(itemId);
        return buildPageModel(List.of(), List.of("Usunięto item z biblioteki."), currentBuildQuery, null);
    }

    private ItemLibraryPageModel buildPageModel(List<String> errors,
                                                List<String> messages,
                                                String currentBuildQuery,
                                                SavedImportedItem savedItemFeedback) {
        return new ItemLibraryPageModel(
                itemLibraryService.getSavedItems(),
                itemLibraryService.getSelection(),
                errors,
                messages,
                currentBuildQuery,
                savedItemFeedback
        );
    }

    private static long parseItemId(String rawValue) {
        try {
            long itemId = Long.parseLong(rawValue);
            if (itemId <= 0L) {
                throw new IllegalArgumentException("Id itemu musi być dodatnie.");
            }
            return itemId;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Niepoprawne id itemu biblioteki.");
        }
    }

    private void renderPage(HttpExchange exchange, ItemLibraryPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadAffixUpdater;
import krys.itemimport.FullItemReadFormCodec;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportFieldConfidence;
import krys.itemimport.ItemImportFormMapper;
import krys.itemimport.ValidatedImportedItem;
import krys.itemlibrary.ItemLibraryFilter;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Kontroler SSR edycji zapisanego itemu z biblioteki. */
final class ItemEditController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final ItemLibraryService itemLibraryService;
    private final ItemEditPageRenderer renderer;
    private final ItemImportFormMapper formMapper;
    private final FullItemReadAffixUpdater fullItemReadAffixUpdater;

    ItemEditController(ItemLibraryService itemLibraryService) {
        this(itemLibraryService, new ItemEditPageRenderer(), new ItemImportFormMapper(), new FullItemReadAffixUpdater());
    }

    ItemEditController(ItemLibraryService itemLibraryService,
                       ItemEditPageRenderer renderer,
                       ItemImportFormMapper formMapper,
                       FullItemReadAffixUpdater fullItemReadAffixUpdater) {
        this.itemLibraryService = itemLibraryService;
        this.renderer = renderer;
        this.formMapper = formMapper;
        this.fullItemReadAffixUpdater = fullItemReadAffixUpdater;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                renderPage(exchange, handleGet(exchange));
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

    private ItemEditPageModel handleGet(HttpExchange exchange) {
        Map<String, String> fields = UrlEncodedFormSupport.parseQuery(exchange.getRequestURI().getRawQuery());
        ItemLibraryFilter filter = ItemLibraryFilter.fromFields(fields);
        try {
            SavedImportedItem item = itemLibraryService.requireItem(parseItemId(fields.getOrDefault("itemId", "")));
            return new ItemEditPageModel(item, toForm(item), List.of(), List.of(), filter);
        } catch (IllegalArgumentException exception) {
            return new ItemEditPageModel(null, null, List.of(exception.getMessage()), List.of(), filter);
        }
    }

    private ItemEditPageModel handlePost(HttpExchange exchange) throws IOException {
        Map<String, String> fields = UrlEncodedFormSupport.parseBody(exchange);
        ItemLibraryFilter filter = ItemLibraryFilter.fromFields(fields);
        long itemId = parseItemId(fields.getOrDefault("itemId", ""));
        SavedImportedItem existingItem = itemLibraryService.requireItem(itemId);
        FullItemRead decodedFullItemRead = FullItemReadFormCodec.decode(fields.getOrDefault("fullItemRead", ""));
        AffixParseResult affixParseResult = parseExistingAffixes(fields);
        ArrayList<ImportedItemAffix> affixes = new ArrayList<>(affixParseResult.affixes());
        parseNewAffix(fields, affixParseResult.errors()).ifPresent(affixes::add);
        ItemImportEditableForm form = buildEditableForm(fields, existingItem, decodedFullItemRead, affixes);

        ItemImportFormMapper.MappingResult mappingResult = formMapper.map(form);
        ArrayList<String> errors = new ArrayList<>(affixParseResult.errors());
        errors.addAll(mappingResult.getErrors());
        if (!errors.isEmpty() || mappingResult.getItem() == null) {
            return new ItemEditPageModel(existingItem, form, errors, List.of(), filter);
        }

        ValidatedImportedItem importedItem = mappingResult.getItem();
        FullItemRead fullItemRead = fullItemReadAffixUpdater.withEditedAffixes(form.getFullItemRead(), form.getAffixes());
        SavedImportedItem updatedItem = itemLibraryService.updateImportedItem(itemId, importedItem, fullItemRead);
        return new ItemEditPageModel(updatedItem, toForm(updatedItem), List.of(), List.of("Zapisano zmiany itemu."), filter);
    }

    private static ItemImportEditableForm toForm(SavedImportedItem item) {
        return new ItemImportEditableForm(
                item.getSourceImageName(),
                item.getSlot().name(),
                Long.toString(item.getWeaponDamage()),
                formatNumber(item.getStrength()),
                formatNumber(item.getIntelligence()),
                formatNumber(item.getThorns()),
                formatNumber(item.getBlockChance()),
                formatNumber(item.getRetributionChance()),
                item.getFullItemRead(),
                item.getAffixes(),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                item.getSelectedAspectId()
        );
    }

    private static ItemImportEditableForm buildEditableForm(Map<String, String> fields,
                                                            SavedImportedItem existingItem,
                                                            FullItemRead decodedFullItemRead,
                                                            List<ImportedItemAffix> affixes) {
        return new ItemImportEditableForm(
                existingItem.getSourceImageName(),
                fields.getOrDefault("slot", ""),
                fields.getOrDefault("weaponDamage", ""),
                fields.getOrDefault("strength", ""),
                fields.getOrDefault("intelligence", ""),
                fields.getOrDefault("thorns", ""),
                fields.getOrDefault("blockChance", ""),
                fields.getOrDefault("retributionChance", ""),
                decodedFullItemRead,
                affixes,
                "",
                ItemImportFieldConfidence.UNKNOWN,
                fields.getOrDefault("selectedAspectId", "")
        );
    }

    private static AffixParseResult parseExistingAffixes(Map<String, String> fields) {
        List<ImportedItemAffix> affixes = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int affixCount = parseAffixCount(fields.get("affixCount"));
        for (int index = 0; index < affixCount; index++) {
            parseAffixRow(fields, index, errors).ifPresent(affixes::add);
        }
        return new AffixParseResult(affixes, errors);
    }

    private static java.util.Optional<ImportedItemAffix> parseAffixRow(Map<String, String> fields, int index, List<String> errors) {
        return parseAffix(
                fields.getOrDefault("affixType_" + index, ""),
                fields.getOrDefault("affixValue_" + index, ""),
                "true".equals(fields.get("affixGreater_" + index)),
                index,
                ImportedItemAffixSource.CORRECTED,
                errors
        );
    }

    private static java.util.Optional<ImportedItemAffix> parseNewAffix(Map<String, String> fields, List<String> errors) {
        return parseAffix(
                fields.getOrDefault("newAffixType", ""),
                fields.getOrDefault("newAffixValue", ""),
                "true".equals(fields.get("newAffixGreater")),
                parseAffixCount(fields.get("affixCount")),
                ImportedItemAffixSource.MANUAL,
                errors
        );
    }

    private static java.util.Optional<ImportedItemAffix> parseAffix(String rawType,
                                                                    String rawValue,
                                                                    boolean greaterAffix,
                                                                    int displayOrder,
                                                                    ImportedItemAffixSource source,
                                                                    List<String> errors) {
        boolean missingType = rawType == null || rawType.isBlank();
        boolean missingValue = rawValue == null || rawValue.isBlank();
        if (missingType && missingValue) {
            return java.util.Optional.empty();
        }
        if (missingType) {
            errors.add("Affix #" + (displayOrder + 1) + ": typ affixu jest wymagany, jeśli podano wartość.");
            return java.util.Optional.empty();
        }
        if (missingValue) {
            errors.add("Affix #" + (displayOrder + 1) + ": wartość affixu jest wymagana, jeśli podano typ.");
            return java.util.Optional.empty();
        }
        try {
            ImportedItemAffixType type = ImportedItemAffixType.valueOf(rawType);
            double value = Double.parseDouble(rawValue.replace(',', '.'));
            if (value < 0.0d) {
                errors.add("Affix #" + (displayOrder + 1) + ": wartość affixu nie może być ujemna.");
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new ImportedItemAffix(type, value, defaultUnit(type), greaterAffix, displayOrder, "", source));
        } catch (IllegalArgumentException exception) {
            errors.add("Affix #" + (displayOrder + 1) + ": affix ma niepoprawny typ albo wartość.");
            return java.util.Optional.empty();
        }
    }

    private static String defaultUnit(ImportedItemAffixType type) {
        return switch (type) {
            case BLOCK_CHANCE, RETRIBUTION_CHANCE, LUCKY_HIT_CHANCE, COOLDOWN_REDUCTION,
                 MOVEMENT_SPEED, DODGE_CHANCE -> "%";
            case STRENGTH, INTELLIGENCE, THORNS -> "";
        };
    }

    private static int parseAffixCount(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(rawValue));
        } catch (NumberFormatException exception) {
            return 0;
        }
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

    private static String formatNumber(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.2f", value);
    }

    private void renderPage(HttpExchange exchange, ItemEditPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }

    private record AffixParseResult(List<ImportedItemAffix> affixes, List<String> errors) {
    }
}

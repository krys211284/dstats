package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadAffixUpdater;
import krys.itemimport.FullItemReadFormCodec;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ItemImportFieldConfidence;
import krys.itemimport.ItemImportFormMapper;
import krys.itemimport.ValidatedImportedItem;
import krys.itemlibrary.ItemLibraryFilter;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.masterworking.MasterworkedAffixSource;
import krys.socketing.ItemSocketing;

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
        TemperingFormSupport.ParseResult temperingParseResult = TemperingFormSupport.parse(fields);
        ItemMasterworking masterworking = parseMasterworking(fields);
        krys.transfiguration.ItemTransfiguration transfiguration = TransfigurationFormSupport.parse(fields);
        ItemSocketing socketing = SocketingFormSupport.parse(fields);
        ArrayList<ImportedItemAffix> affixes = new ArrayList<>(affixParseResult.affixes());
        parseNewAffix(fields, affixParseResult.errors()).ifPresent(affixes::add);
        ItemImportEditableForm form = buildEditableForm(fields, existingItem, decodedFullItemRead, affixes, temperingParseResult.affixes(), masterworking, transfiguration, socketing);

        ItemImportFormMapper.MappingResult mappingResult = formMapper.map(form);
        ArrayList<String> errors = new ArrayList<>(affixParseResult.errors());
        errors.addAll(temperingParseResult.errors());
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
                item.getSelectedAspectId(),
                item.getDetails(),
                item.getTemperingAffixes(),
                item.getMasterworking(),
                item.getTransfiguration(),
                item.getSocketing()
        );
    }

    private static ItemImportEditableForm buildEditableForm(Map<String, String> fields,
                                                            SavedImportedItem existingItem,
                                                            FullItemRead decodedFullItemRead,
                                                            List<ImportedItemAffix> affixes) {
        return buildEditableForm(fields, existingItem, decodedFullItemRead, affixes, List.of());
    }

    private static ItemImportEditableForm buildEditableForm(Map<String, String> fields,
                                                            SavedImportedItem existingItem,
                                                            FullItemRead decodedFullItemRead,
                                                            List<ImportedItemAffix> affixes,
                                                            List<krys.tempering.ItemTemperingAffix> temperingAffixes) {
        return buildEditableForm(fields, existingItem, decodedFullItemRead, affixes, temperingAffixes, parseMasterworking(fields));
    }

    private static ItemImportEditableForm buildEditableForm(Map<String, String> fields,
                                                            SavedImportedItem existingItem,
                                                            FullItemRead decodedFullItemRead,
                                                            List<ImportedItemAffix> affixes,
                                                            List<krys.tempering.ItemTemperingAffix> temperingAffixes,
                                                            ItemMasterworking masterworking) {
        return buildEditableForm(fields, existingItem, decodedFullItemRead, affixes, temperingAffixes, masterworking,
                TransfigurationFormSupport.parse(fields), SocketingFormSupport.parse(fields));
    }

    private static ItemImportEditableForm buildEditableForm(Map<String, String> fields,
                                                            SavedImportedItem existingItem,
                                                            FullItemRead decodedFullItemRead,
                                                            List<ImportedItemAffix> affixes,
                                                            List<krys.tempering.ItemTemperingAffix> temperingAffixes,
                                                            ItemMasterworking masterworking,
                                                            krys.transfiguration.ItemTransfiguration transfiguration) {
        return buildEditableForm(fields, existingItem, decodedFullItemRead, affixes, temperingAffixes, masterworking,
                transfiguration, SocketingFormSupport.parse(fields));
    }

    private static ItemImportEditableForm buildEditableForm(Map<String, String> fields,
                                                            SavedImportedItem existingItem,
                                                            FullItemRead decodedFullItemRead,
                                                            List<ImportedItemAffix> affixes,
                                                            List<krys.tempering.ItemTemperingAffix> temperingAffixes,
                                                            ItemMasterworking masterworking,
                                                            krys.transfiguration.ItemTransfiguration transfiguration,
                                                            ItemSocketing socketing) {
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
                fields.getOrDefault("selectedAspectId", ""),
                parseItemDetails(fields, existingItem),
                temperingAffixes,
                masterworking,
                transfiguration,
                socketing
        );
    }

    private static ItemMasterworking parseMasterworking(Map<String, String> fields) {
        return new ItemMasterworking(
                parseIntOrDefault(fields.get("masterworkingQualityCurrent"), ItemMasterworking.DEFAULT_QUALITY_CURRENT),
                parseIntOrDefault(fields.get("masterworkingQualityMax"), ItemMasterworking.DEFAULT_QUALITY_MAX),
                parsePerfectedAffix(fields.getOrDefault("masterworkingPerfectedAffix", ""))
        );
    }

    private static MasterworkedAffixSelection parsePerfectedAffix(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String[] tokens = rawValue.split(":", 2);
        if (tokens.length != 2 || tokens[0].isBlank() || tokens[1].isBlank()) {
            return MasterworkedAffixSelection.unknown(tokens.length == 0 ? "" : tokens[0], tokens.length < 2 ? "" : tokens[1]);
        }
        try {
            return new MasterworkedAffixSelection(MasterworkedAffixSource.valueOf(tokens[0]), tokens[1]);
        } catch (IllegalArgumentException exception) {
            return MasterworkedAffixSelection.unknown(tokens[0], tokens[1]);
        }
    }

    private static int parseIntOrDefault(String rawValue, int fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(rawValue.replace(" ", ""));
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private static ItemImportDetails parseItemDetails(Map<String, String> fields, SavedImportedItem existingItem) {
        ItemImportDetails fallback = existingItem.getDetails();
        EquipmentSlot equipmentSlot = parseEquipmentSlot(valueOrFallback(fields, "slot",
                fallback.getEquipmentSlot() == null ? existingItem.getSlot().name() : fallback.getEquipmentSlot().name()));
        return new ItemImportDetails(
                valueOrFallback(fields, "itemName", firstNonBlank(fallback.getItemName(), existingItem.getDisplayName())),
                valueOrFallback(fields, "itemType", fallback.getItemType()),
                valueOrFallback(fields, "itemRarity", fallback.getItemRarity()),
                fields.containsKey("isAncientSubmitted") ? "true".equals(fields.get("isAncient")) : fallback.isAncient(),
                equipmentSlot,
                parseLongOrFallback(fields.get("itemPower"), fallback.getItemPower()),
                parseLongOrFallback(fields.get("weaponDps"), fallback.getWeaponDps()),
                parseLongOrFallback(fields.get("weaponDamageMin"), fallback.getWeaponDamageMin()),
                parseLongOrFallback(fields.get("weaponDamageMax"), fallback.getWeaponDamageMax()),
                parseLongOrFallback(fields.get("averageWeaponDamage"), fallback.getAverageWeaponDamage()),
                parseDoubleOrFallback(fields.get("attacksPerSecond"), fallback.getAttacksPerSecond()),
                parseLongOrFallback(fields.get("itemArmor"), fallback.getItemArmor()),
                valueOrFallback(fields, "uniqueEffectText", fallback.getUniqueEffectText())
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
                parseAffixSource(fields.get("affixSource_" + index), ImportedItemAffixSource.CORRECTED),
                fields.getOrDefault("affixSourceText_" + index, ""),
                fields.getOrDefault("affixOriginalType_" + index, fields.getOrDefault("affixType_" + index, "")),
                fields.getOrDefault("affixOriginalValue_" + index, fields.getOrDefault("affixValue_" + index, "")),
                fields.getOrDefault("affixDefinitionId_" + index, ""),
                parseNullableDouble(fields.get("affixRangeMin_" + index)),
                parseNullableDouble(fields.get("affixRangeMax_" + index)),
                fields.getOrDefault("affixDisplayValue_" + index, ""),
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
                "",
                fields.getOrDefault("newAffixType", ""),
                fields.getOrDefault("newAffixValue", ""),
                "",
                null,
                null,
                "",
                errors
        );
    }

    private static java.util.Optional<ImportedItemAffix> parseAffix(String rawType,
                                                                    String rawValue,
                                                                    boolean greaterAffix,
                                                                    int displayOrder,
                                                                    ImportedItemAffixSource source,
                                                                    String sourceText,
                                                                    String originalType,
                                                                    String originalValue,
                                                                    String affixDefinitionId,
                                                                    Double rollRangeMin,
                                                                    Double rollRangeMax,
                                                                    String displayValue,
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
            if (!rawType.equals(originalType) || !normalizeNumber(rawValue).equals(normalizeNumber(originalValue))) {
                sourceText = "";
                affixDefinitionId = "";
                rollRangeMin = null;
                rollRangeMax = null;
                displayValue = "";
                source = ImportedItemAffixSource.CORRECTED;
            }
            return java.util.Optional.of(new ImportedItemAffix(type, value, defaultUnit(type), greaterAffix,
                    displayOrder, sourceText, source, affixDefinitionId, rollRangeMin, rollRangeMax, displayValue));
        } catch (IllegalArgumentException exception) {
            errors.add("Affix #" + (displayOrder + 1) + ": affix ma niepoprawny typ albo wartość.");
            return java.util.Optional.empty();
        }
    }

    private static String defaultUnit(ImportedItemAffixType type) {
        return switch (type) {
            case BLOCK_CHANCE, RETRIBUTION_CHANCE, LUCKY_HIT_CHANCE, COOLDOWN_REDUCTION,
                 MOVEMENT_SPEED, DODGE_CHANCE, DAMAGE_REDUCTION -> "%";
            case STRENGTH, INTELLIGENCE, THORNS, WEAPON_DAMAGE_FLAT, MAXIMUM_LIFE, LIFE_ON_HIT,
                 LUCKY_HIT_PRIMARY_RESOURCE, ALL_RESISTANCE, FIRE_RESISTANCE -> "";
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

    private static EquipmentSlot parseEquipmentSlot(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return EquipmentSlot.valueOf(rawValue);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Long parseLongOrFallback(String rawValue, Long fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            return Long.parseLong(rawValue.replace(" ", ""));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static Double parseDoubleOrFallback(String rawValue, Double fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(rawValue.replace(',', '.'));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static Double parseNullableDouble(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(rawValue.replace(',', '.'));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static ImportedItemAffixSource parseAffixSource(String rawValue, ImportedItemAffixSource fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            return ImportedItemAffixSource.valueOf(rawValue);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static String valueOrFallback(Map<String, String> fields, String key, String fallback) {
        String value = fields.get(key);
        return value == null || value.isBlank() ? (fallback == null ? "" : fallback) : value;
    }

    private static String firstNonBlank(String preferred, String fallback) {
        return preferred == null || preferred.isBlank() ? (fallback == null ? "" : fallback) : preferred;
    }

    private static String normalizeNumber(String value) {
        return value == null ? "" : value.replace(" ", "").replace(',', '.').trim();
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

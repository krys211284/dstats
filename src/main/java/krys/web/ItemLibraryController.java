package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.item.HeroEquipmentSlot;
import krys.itemlibrary.ItemLibraryFilter;
import krys.itemlibrary.ItemLibraryPresentationSupport;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportFormMapper;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.masterworking.MasterworkedAffixSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/** Kontroler SSR minimalnej biblioteki zapisanych itemów z aktywnym wyborem per slot. */
public final class ItemLibraryController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final ItemLibraryService itemLibraryService;
    private final ItemLibraryPageRenderer renderer;
    private final ItemImportFormMapper itemImportFormMapper;
    private final HeroService heroService;

    public ItemLibraryController(ItemLibraryService itemLibraryService,
                                 ItemLibraryPageRenderer renderer,
                                 HeroService heroService) {
        this(itemLibraryService, renderer, new ItemImportFormMapper(), heroService);
    }

    ItemLibraryController(ItemLibraryService itemLibraryService,
                          ItemLibraryPageRenderer renderer,
                          ItemImportFormMapper itemImportFormMapper,
                          HeroService heroService) {
        this.itemLibraryService = itemLibraryService;
        this.renderer = renderer;
        this.itemImportFormMapper = itemImportFormMapper;
        this.heroService = heroService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                String currentBuildQuery = heroService.getActiveHero()
                        .map(HeroProfile::getCurrentBuildQuery)
                        .orElse("");
                ItemLibraryFilter filter = ItemLibraryFilter.fromFields(UrlEncodedFormSupport.parseQuery(exchange.getRequestURI().getRawQuery()));
                renderPage(exchange, buildPageModel(List.of(), List.of(), currentBuildQuery, null, filter));
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
                default -> buildPageModel(List.of("Nieobsługiwana akcja biblioteki itemów."), List.of(), currentBuildQuery, null, filterFromFields(fields));
            };
        } catch (IllegalArgumentException exception) {
            return buildPageModel(List.of(exception.getMessage()), List.of(), currentBuildQuery, null, filterFromFields(fields));
        }
    }

    private ItemLibraryPageModel handleSaveImportedItem(Map<String, String> fields, String currentBuildQuery) {
        TemperingFormSupport.ParseResult temperingParseResult = TemperingFormSupport.parse(fields);
        ItemImportEditableForm form = new ItemImportEditableForm(
                fields.getOrDefault("sourceImageName", ""),
                fields.getOrDefault("slot", ""),
                fields.getOrDefault("weaponDamage", ""),
                fields.getOrDefault("strength", ""),
                fields.getOrDefault("intelligence", ""),
                fields.getOrDefault("thorns", ""),
                fields.getOrDefault("blockChance", ""),
                fields.getOrDefault("retributionChance", ""),
                krys.itemimport.FullItemRead.empty(),
                legacyAffixes(fields),
                "",
                krys.itemimport.ItemImportFieldConfidence.UNKNOWN,
                "",
                krys.itemimport.ItemImportDetails.empty(),
                temperingParseResult.affixes(),
                parseMasterworking(fields),
                TransfigurationFormSupport.parse(fields),
                SocketingFormSupport.parse(fields)
        );
        ItemImportFormMapper.MappingResult mappingResult = itemImportFormMapper.map(form);
        List<String> errors = new ArrayList<>(temperingParseResult.errors());
        errors.addAll(mappingResult.getErrors());
        if (!errors.isEmpty() || mappingResult.getItem() == null) {
            return buildPageModel(errors, List.of(), currentBuildQuery, null);
        }

        SavedImportedItem savedItem = itemLibraryService.saveImportedItem(mappingResult.getItem());
        return buildPageModel(
                List.of(),
                List.of("Zapisano item w bibliotece: " + savedItem.getDisplayName() + "."),
                currentBuildQuery,
                savedItem,
                filterFromFields(fields)
        );
    }

    private ItemLibraryPageModel handleActivateItem(Map<String, String> fields, String currentBuildQuery) {
        HeroProfile activeHero = heroService.requireActiveHero();
        long itemId = parseItemId(fields.getOrDefault("itemId", ""));
        HeroEquipmentSlot heroSlot = HeroEquipmentSlot.valueOf(fields.getOrDefault("heroSlot", ""));
        SavedImportedItem item = itemLibraryService.requireCompatibleItem(heroSlot, itemId);
        Long previousItemId = activeHero.getItemSelection().getSelectedItemId(heroSlot);
        heroService.setActiveHeroItem(heroSlot, itemId);
        String slotName = ItemLibraryPresentationSupport.heroSlotDisplayName(heroSlot);
        String message = previousItemId == null
                ? "Założono item " + item.getDisplayName() + " w slocie " + slotName + " bohatera " + activeHero.getName() + "."
                : "Zmieniono item w slocie " + slotName + " bohatera " + activeHero.getName() + " na " + item.getDisplayName() + ".";
        return buildPageModel(
                List.of(),
                List.of(message),
                currentBuildQuery,
                null,
                filterFromFields(fields)
        );
    }

    private ItemLibraryPageModel handleDeleteItem(Map<String, String> fields, String currentBuildQuery) {
        long itemId = parseItemId(fields.getOrDefault("itemId", ""));
        itemLibraryService.deleteItem(itemId);
        heroService.clearItemFromAllHeroes(itemId);
        return buildPageModel(List.of(), List.of("Usunięto item z biblioteki."), currentBuildQuery, null, filterFromFields(fields));
    }

    private ItemLibraryPageModel buildPageModel(List<String> errors,
                                                List<String> messages,
                                                String currentBuildQuery,
                                                SavedImportedItem savedItemFeedback,
                                                ItemLibraryFilter filter) {
        List<SavedImportedItem> allItems = itemLibraryService.getSavedItems();
        HeroItemSelection activeSelection = heroService.getActiveHero().map(HeroProfile::getItemSelection).orElse(HeroItemSelection.empty());
        List<SavedImportedItem> filteredItems = allItems.stream()
                .filter(item -> filter.matches(item, isActive(activeSelection, item)))
                .toList();
        return new ItemLibraryPageModel(
                filteredItems,
                heroService.getActiveHero().orElse(null),
                activeSelection,
                errors,
                messages,
                currentBuildQuery,
                savedItemFeedback,
                filter,
                availableItemTypes(allItems),
                allItems.size()
        );
    }

    private ItemLibraryPageModel buildPageModel(List<String> errors,
                                                List<String> messages,
                                                String currentBuildQuery,
                                                SavedImportedItem savedItemFeedback) {
        return buildPageModel(errors, messages, currentBuildQuery, savedItemFeedback, ItemLibraryFilter.empty());
    }

    private static ItemLibraryFilter filterFromFields(Map<String, String> fields) {
        return ItemLibraryFilter.fromFields(fields);
    }

    private static boolean isActive(HeroItemSelection activeSelection, SavedImportedItem item) {
        if (activeSelection == null || item == null) {
            return false;
        }
        return activeSelection.getSelectedItemIdsBySlot().values().stream()
                .anyMatch(itemId -> itemId != null && itemId == item.getItemId());
    }

    private static List<String> availableItemTypes(List<SavedImportedItem> items) {
        TreeSet<String> itemTypes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (SavedImportedItem item : items) {
            itemTypes.add(ItemLibraryFilter.resolvedItemType(item));
        }
        return new ArrayList<>(itemTypes);
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

    private static List<ImportedItemAffix> legacyAffixes(Map<String, String> fields) {
        java.util.ArrayList<ImportedItemAffix> affixes = new java.util.ArrayList<>();
        addLegacyAffix(affixes, ImportedItemAffixType.STRENGTH, fields.get("strength"));
        addLegacyAffix(affixes, ImportedItemAffixType.INTELLIGENCE, fields.get("intelligence"));
        addLegacyAffix(affixes, ImportedItemAffixType.THORNS, fields.get("thorns"));
        addLegacyAffix(affixes, ImportedItemAffixType.BLOCK_CHANCE, fields.get("blockChance"));
        addLegacyAffix(affixes, ImportedItemAffixType.RETRIBUTION_CHANCE, fields.get("retributionChance"));
        return affixes;
    }

    private static void addLegacyAffix(List<ImportedItemAffix> affixes, ImportedItemAffixType type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return;
        }
        double value = Double.parseDouble(rawValue.replace(',', '.'));
        if (value <= 0.0d) {
            return;
        }
        String unit = switch (type) {
            case BLOCK_CHANCE, RETRIBUTION_CHANCE, CRITICAL_STRIKE_CHANCE, LUCKY_HIT_CHANCE, COOLDOWN_REDUCTION,
                 MOVEMENT_SPEED, DODGE_CHANCE, DAMAGE_REDUCTION, ALL_DAMAGE_MULTIPLIER, DAMAGE_OVER_TIME_MULTIPLIER -> "%";
            case STRENGTH, INTELLIGENCE, THORNS, WEAPON_DAMAGE_FLAT, MAXIMUM_LIFE, LIFE_ON_HIT, LIFE_ON_KILL,
                 LUCKY_HIT_PRIMARY_RESOURCE, ALL_RESISTANCE, FIRE_RESISTANCE, CORE_SKILL_RANKS -> "";
        };
        affixes.add(new ImportedItemAffix(type, value, unit, false, affixes.size(), "", ImportedItemAffixSource.MANUAL));
    }

    private void renderPage(HttpExchange exchange, ItemLibraryPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

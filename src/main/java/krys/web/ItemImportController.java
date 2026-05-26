package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.item.Item;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadAffixUpdater;
import krys.itemimport.FullItemReadFormCodec;
import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ImportedItemCurrentBuildContributionMapper;
import krys.itemimport.ItemImageImportCandidateParseResult;
import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.ItemImageImportService;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ItemImportEditableFormFactory;
import krys.itemimport.ItemImportFieldConfidence;
import krys.itemimport.ItemImportFormMapper;
import krys.itemimport.ValidatedImportedItem;
import krys.itemimport.ValidatedImportedItemToItemMapper;
import krys.itemknowledge.ItemKnowledgeService;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.masterworking.MasterworkedAffixSource;
import krys.socketing.ItemSocketing;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Kontroler SSR dla pierwszego foundation importu pojedynczego itemu ze screena. */
public final class ItemImportController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";

    private final ItemImageImportService imageImportService;
    private final ItemImportPageRenderer renderer;
    private final ItemImportEditableFormFactory editableFormFactory;
    private final ItemImportFormMapper formMapper;
    private final ValidatedImportedItemToItemMapper itemMapper;
    private final ImportedItemCurrentBuildContributionMapper contributionMapper;
    private final FullItemReadAffixUpdater fullItemReadAffixUpdater;
    private final ItemLibraryService itemLibraryService;
    private final ItemKnowledgeService itemKnowledgeService;
    private final HeroService heroService;

    public ItemImportController(ItemImageImportService imageImportService,
                                ItemImportPageRenderer renderer,
                                ItemLibraryService itemLibraryService,
                                HeroService heroService) {
        this(imageImportService, renderer, itemLibraryService, null, heroService);
    }

    public ItemImportController(ItemImageImportService imageImportService,
                                ItemImportPageRenderer renderer,
                                ItemLibraryService itemLibraryService,
                                ItemKnowledgeService itemKnowledgeService,
                                HeroService heroService) {
        this(
                imageImportService,
                renderer,
                new ItemImportEditableFormFactory(),
                new ItemImportFormMapper(),
                new ValidatedImportedItemToItemMapper(),
                new ImportedItemCurrentBuildContributionMapper(),
                new FullItemReadAffixUpdater(),
                itemLibraryService,
                itemKnowledgeService,
                heroService
        );
    }

    ItemImportController(ItemImageImportService imageImportService,
                         ItemImportPageRenderer renderer,
                         ItemImportEditableFormFactory editableFormFactory,
                         ItemImportFormMapper formMapper,
                         ValidatedImportedItemToItemMapper itemMapper,
                         ImportedItemCurrentBuildContributionMapper contributionMapper,
                         FullItemReadAffixUpdater fullItemReadAffixUpdater,
                         ItemLibraryService itemLibraryService,
                         ItemKnowledgeService itemKnowledgeService,
                         HeroService heroService) {
        this.imageImportService = imageImportService;
        this.renderer = renderer;
        this.editableFormFactory = editableFormFactory;
        this.formMapper = formMapper;
        this.itemMapper = itemMapper;
        this.contributionMapper = contributionMapper;
        this.fullItemReadAffixUpdater = fullItemReadAffixUpdater;
        this.itemLibraryService = itemLibraryService;
        this.itemKnowledgeService = itemKnowledgeService;
        this.heroService = heroService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                HeroProfile activeHero = heroService.getActiveHero().orElse(null);
                String currentBuildQuery = activeHero == null ? "" : activeHero.getCurrentBuildQuery();
                renderPage(exchange, emptyPageModel(currentBuildQuery, activeHero));
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

    private ItemImportPageModel handlePost(HttpExchange exchange) throws IOException {
        HeroProfile activeHero = heroService.getActiveHero().orElse(null);
        if (activeHero == null) {
            return buildErrorPageModel(null, null, List.of("Brak aktywnego bohatera. Utwórz albo wybierz bohatera przed importem itemu."), "", null);
        }
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        String currentBuildQuery = activeHero.getCurrentBuildQuery();
        if (contentType == null) {
            return buildErrorPageModel(null, null, List.of("Brak nagłówka `Content-Type`."), currentBuildQuery, activeHero);
        }
        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        if (normalizedContentType.startsWith("multipart/form-data")) {
            return handleImageUpload(exchange);
        }
        if (normalizedContentType.startsWith("application/x-www-form-urlencoded")) {
            return handleConfirmation(exchange);
        }
        return buildErrorPageModel(null, null, List.of("Nieobsługiwany typ danych formularza dla importu itemu."), currentBuildQuery, activeHero);
    }

    private ItemImportPageModel handleImageUpload(HttpExchange exchange) throws IOException {
        try {
            HeroProfile activeHero = heroService.requireActiveHero();
            MultipartFormSupport.MultipartFormData multipartFormData = MultipartFormSupport.parse(exchange);
            List<MultipartFormSupport.MultipartFilePart> fileParts = multipartFormData.requireFiles("itemImage");
            ItemImageImportCandidateParseResult parseResult = imageImportService.analyze(fileParts.stream()
                    .map(filePart -> new ItemImageImportRequest(
                            filePart.getOriginalFilename(),
                            filePart.getContentType(),
                            filePart.getContent()
                    ))
                    .toList());
            return new ItemImportPageModel(
                    editableFormFactory.create(parseResult),
                    parseResult,
                    List.of(),
                    null,
                    activeHero,
                    buildHelpText(),
                    activeHero.getCurrentBuildQuery()
            );
        } catch (IllegalArgumentException exception) {
            HeroProfile activeHero = heroService.getActiveHero().orElse(null);
            return buildErrorPageModel(null, null, List.of(exception.getMessage()), activeHero == null ? "" : activeHero.getCurrentBuildQuery(), activeHero);
        }
    }

    private ItemImportPageModel handleConfirmation(HttpExchange exchange) throws IOException {
        Map<String, String> fields = UrlEncodedFormSupport.parseBody(exchange);
        String currentBuildQuery = fields.getOrDefault("currentBuildQuery", "");
        FullItemRead decodedFullItemRead = FullItemReadFormCodec.decode(fields.getOrDefault("fullItemRead", ""));
        String formAction = fields.getOrDefault("formAction", "confirmItem");
        AffixParseResult affixParseResult = parseExistingAffixes(fields);
        TemperingFormSupport.ParseResult temperingParseResult = TemperingFormSupport.parse(fields);
        ItemMasterworking masterworking = parseMasterworking(fields);
        krys.transfiguration.ItemTransfiguration transfiguration = TransfigurationFormSupport.parse(fields);
        ItemSocketing socketing = SocketingFormSupport.parse(fields);
        List<ImportedItemAffix> affixes = affixParseResult.affixes();
        if ("addAffix".equals(formAction)) {
            java.util.ArrayList<ImportedItemAffix> updatedAffixes = new java.util.ArrayList<>(affixes);
            parseNewAffix(fields, affixParseResult.errors()).ifPresent(updatedAffixes::add);
            ItemImportEditableForm form = buildEditableForm(fields, decodedFullItemRead, updatedAffixes, temperingParseResult.affixes(), masterworking, transfiguration, socketing);
            return new ItemImportPageModel(
                    form,
                    null,
                    List.of(),
                    null,
                    heroService.requireActiveHero(),
                    buildHelpText(),
                    currentBuildQuery
            );
        }

        ItemImportEditableForm form = buildEditableForm(fields, decodedFullItemRead, affixes, temperingParseResult.affixes(), masterworking, transfiguration, socketing);

        ItemImportFormMapper.MappingResult mappingResult = formMapper.map(form);
        java.util.ArrayList<String> allErrors = new java.util.ArrayList<>(affixParseResult.errors());
        allErrors.addAll(temperingParseResult.errors());
        allErrors.addAll(mappingResult.getErrors());
        if (!allErrors.isEmpty() || mappingResult.getItem() == null) {
            return buildErrorPageModel(
                    form,
                    null,
                    allErrors,
                    currentBuildQuery,
                    heroService.requireActiveHero()
            );
        }

        HeroProfile activeHero = heroService.requireActiveHero();
        ValidatedImportedItem importedItem = mappingResult.getItem();
        FullItemRead editedFullItemRead = fullItemReadAffixUpdater.withEditedAffixes(form.getFullItemRead(), form.getAffixes());
        FullItemRead fullItemRead = new FullItemRead(
                editedFullItemRead.getItemName(),
                editedFullItemRead.getItemTypeLine(),
                editedFullItemRead.getRarity(),
                editedFullItemRead.getItemPower(),
                editedFullItemRead.getBaseItemValue(),
                editedFullItemRead.getLines(),
                importedItem.getDetails()
        );
        SavedImportedItem savedItem = itemLibraryService.saveImportedItem(importedItem, fullItemRead);
        if (itemKnowledgeService != null) {
            itemKnowledgeService.learnFromConfirmedItem(importedItem, fullItemRead);
        }
        Item mappedItem = itemMapper.map(importedItem);
        ImportedItemCurrentBuildContribution contribution = contributionMapper.map(importedItem);
        return new ItemImportPageModel(
                form,
                null,
                List.of(),
                new ItemImportPageModel.ConfirmedImportView(
                        importedItem,
                        savedItem,
                        mappedItem,
                        contribution
                ),
                activeHero,
                buildHelpText(),
                currentBuildQuery
        );
    }

    private static ItemImportEditableForm buildEditableForm(Map<String, String> fields,
                                                            FullItemRead decodedFullItemRead,
                                                            List<ImportedItemAffix> affixes) {
        return buildEditableForm(fields, decodedFullItemRead, affixes, List.of());
    }

    private static ItemImportEditableForm buildEditableForm(Map<String, String> fields,
                                                            FullItemRead decodedFullItemRead,
                                                            List<ImportedItemAffix> affixes,
                                                            List<krys.tempering.ItemTemperingAffix> temperingAffixes) {
        return buildEditableForm(fields, decodedFullItemRead, affixes, temperingAffixes, parseMasterworking(fields));
    }

    private static ItemImportEditableForm buildEditableForm(Map<String, String> fields,
                                                            FullItemRead decodedFullItemRead,
                                                            List<ImportedItemAffix> affixes,
                                                            List<krys.tempering.ItemTemperingAffix> temperingAffixes,
                                                            ItemMasterworking masterworking) {
        return buildEditableForm(fields, decodedFullItemRead, affixes, temperingAffixes, masterworking,
                TransfigurationFormSupport.parse(fields), SocketingFormSupport.parse(fields));
    }

    private static ItemImportEditableForm buildEditableForm(Map<String, String> fields,
                                                            FullItemRead decodedFullItemRead,
                                                            List<ImportedItemAffix> affixes,
                                                            List<krys.tempering.ItemTemperingAffix> temperingAffixes,
                                                            ItemMasterworking masterworking,
                                                            krys.transfiguration.ItemTransfiguration transfiguration) {
        return buildEditableForm(fields, decodedFullItemRead, affixes, temperingAffixes, masterworking,
                transfiguration, SocketingFormSupport.parse(fields));
    }

    private static ItemImportEditableForm buildEditableForm(Map<String, String> fields,
                                                            FullItemRead decodedFullItemRead,
                                                            List<ImportedItemAffix> affixes,
                                                            List<krys.tempering.ItemTemperingAffix> temperingAffixes,
                                                            ItemMasterworking masterworking,
                                                            krys.transfiguration.ItemTransfiguration transfiguration,
                                                            ItemSocketing socketing) {
        return new ItemImportEditableForm(
                fields.getOrDefault("sourceImageName", "nieznany-item"),
                fields.getOrDefault("slot", ""),
                fields.getOrDefault("weaponDamage", ""),
                fields.getOrDefault("strength", ""),
                fields.getOrDefault("intelligence", ""),
                fields.getOrDefault("thorns", ""),
                fields.getOrDefault("blockChance", ""),
                fields.getOrDefault("retributionChance", ""),
                decodedFullItemRead,
                affixes,
                fields.getOrDefault("ocrSuggestedAspectId", ""),
                parseConfidence(fields.getOrDefault("ocrAspectConfidence", "")),
                fields.getOrDefault("selectedAspectId", ""),
                parseItemDetails(fields),
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

    private static ItemImportDetails parseItemDetails(Map<String, String> fields) {
        krys.item.EquipmentSlot equipmentSlot = parseEquipmentSlot(fields.getOrDefault("slot", ""));
        return new ItemImportDetails(
                fields.getOrDefault("itemName", ""),
                fields.getOrDefault("itemType", ""),
                fields.getOrDefault("itemRarity", ""),
                "true".equals(fields.get("isAncient")),
                equipmentSlot,
                parseNullableLong(fields.get("itemPower")),
                parseNullableLong(fields.get("weaponDps")),
                parseNullableLong(fields.get("weaponDamageMin")),
                parseNullableLong(fields.get("weaponDamageMax")),
                parseNullableLong(fields.get("averageWeaponDamage")),
                parseNullableDouble(fields.get("attacksPerSecond")),
                parseNullableLong(fields.get("itemArmor")),
                fields.getOrDefault("uniqueEffectText", "")
        );
    }

    private static AffixParseResult parseExistingAffixes(Map<String, String> fields) {
        List<ImportedItemAffix> affixes = new java.util.ArrayList<>();
        List<String> errors = new java.util.ArrayList<>();
        int affixCount = parseAffixCount(fields.get("affixCount"));
        for (int index = 0; index < affixCount; index++) {
            if ("true".equals(fields.get("affixRemoved_" + index))) {
                continue;
            }
            parseAffixRow(fields, index, errors).ifPresent(affixes::add);
        }
        return new AffixParseResult(affixes, errors);
    }

    private static java.util.Optional<ImportedItemAffix> parseAffixRow(Map<String, String> fields, int index, List<String> errors) {
        String typeValue = fields.getOrDefault("affixType_" + index, "");
        String value = fields.getOrDefault("affixValue_" + index, "");
        boolean greaterAffix = "true".equals(fields.get("affixGreater_" + index));
        String sourceText = fields.getOrDefault("affixSourceText_" + index, "");
        String originalType = fields.getOrDefault("affixOriginalType_" + index, typeValue);
        String originalValue = fields.getOrDefault("affixOriginalValue_" + index, value);
        String affixDefinitionId = fields.getOrDefault("affixDefinitionId_" + index, "");
        Double rollRangeMin = parseNullableDouble(fields.get("affixRangeMin_" + index));
        Double rollRangeMax = parseNullableDouble(fields.get("affixRangeMax_" + index));
        String displayValue = fields.getOrDefault("affixDisplayValue_" + index, "");
        if (!typeValue.equals(originalType) || !normalizeNumber(value).equals(normalizeNumber(originalValue))) {
            sourceText = "";
            affixDefinitionId = "";
            rollRangeMin = null;
            rollRangeMax = null;
            displayValue = "";
        }
        return parseAffix(typeValue, value, greaterAffix, sourceText, index, ImportedItemAffixSource.CORRECTED,
                affixDefinitionId, rollRangeMin, rollRangeMax, displayValue, errors);
    }

    private static java.util.Optional<ImportedItemAffix> parseNewAffix(Map<String, String> fields, List<String> errors) {
        return parseAffix(
                fields.getOrDefault("newAffixType", ""),
                fields.getOrDefault("newAffixValue", ""),
                "true".equals(fields.get("newAffixGreater")),
                "",
                parseAffixCount(fields.get("affixCount")),
                ImportedItemAffixSource.MANUAL,
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
                                                                     String sourceText,
                                                                     int displayOrder,
                                                                     ImportedItemAffixSource source,
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
            return java.util.Optional.of(new ImportedItemAffix(type, value, defaultUnit(type), greaterAffix,
                    displayOrder, sourceText, source, affixDefinitionId, rollRangeMin, rollRangeMax, displayValue));
        } catch (IllegalArgumentException exception) {
            errors.add("Affix #" + (displayOrder + 1) + ": affix ma niepoprawny typ albo wartość.");
            return java.util.Optional.empty();
        }
    }

    private static ItemImportFieldConfidence parseConfidence(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return ItemImportFieldConfidence.UNKNOWN;
        }
        try {
            return ItemImportFieldConfidence.valueOf(rawValue);
        } catch (IllegalArgumentException exception) {
            return ItemImportFieldConfidence.UNKNOWN;
        }
    }

    private static String defaultUnit(ImportedItemAffixType type) {
        return switch (type) {
            case BLOCK_CHANCE, RETRIBUTION_CHANCE, CRITICAL_STRIKE_CHANCE, LUCKY_HIT_CHANCE, COOLDOWN_REDUCTION,
                 MOVEMENT_SPEED, DODGE_CHANCE, DAMAGE_REDUCTION, DAMAGE_OVER_TIME_MULTIPLIER -> "%";
            case STRENGTH, INTELLIGENCE, THORNS, WEAPON_DAMAGE_FLAT, MAXIMUM_LIFE, LIFE_ON_HIT, LIFE_ON_KILL,
                 LUCKY_HIT_PRIMARY_RESOURCE, ALL_RESISTANCE, FIRE_RESISTANCE -> "";
        };
    }

    private static Long parseNullableLong(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(rawValue.replace(" ", ""));
        } catch (NumberFormatException exception) {
            return null;
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

    private static krys.item.EquipmentSlot parseEquipmentSlot(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return krys.item.EquipmentSlot.valueOf(rawValue);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static String normalizeNumber(String value) {
        return value == null ? "" : value.replace(" ", "").replace(',', '.').trim();
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

    private ItemImportPageModel emptyPageModel(String currentBuildQuery, HeroProfile activeHero) {
        return new ItemImportPageModel(null, null, List.of(), null, activeHero, buildHelpText(), currentBuildQuery);
    }

    private ItemImportPageModel buildErrorPageModel(ItemImportEditableForm form,
                                                    ItemImageImportCandidateParseResult parseResult,
                                                    List<String> errors,
                                                    String currentBuildQuery,
                                                    HeroProfile activeHero) {
        return new ItemImportPageModel(form, parseResult, errors, null, activeHero, buildHelpText(), currentBuildQuery);
    }

    private static String buildHelpText() {
        return "Możesz dodać jeden lub kilka screenów tego samego itemu. Jeśli tooltip jest przewijany, dodaj screeny w kolejności od góry do dołu. Limit: 1..5 screenów jednego itemu. Foundation sprawdza obraz, pokazuje niepewność pól i wymaga ręcznego zatwierdzenia użytkownika.";
    }

    private void renderPage(HttpExchange exchange, ItemImportPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }

    private record AffixParseResult(List<ImportedItemAffix> affixes, List<String> errors) {
    }
}

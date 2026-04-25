package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.item.Item;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadAffixUpdater;
import krys.itemimport.FullItemReadFormCodec;
import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixExtractor;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ImportedItemCurrentBuildContributionMapper;
import krys.itemimport.ItemImageImportCandidateParseResult;
import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.ItemImageImportService;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportEditableFormFactory;
import krys.itemimport.ItemImportFormMapper;
import krys.itemimport.ValidatedImportedItem;
import krys.itemimport.ValidatedImportedItemToItemMapper;
import krys.itemknowledge.ItemKnowledgeService;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;

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
            MultipartFormSupport.MultipartFilePart filePart = multipartFormData.requireFile("itemImage");
            ItemImageImportCandidateParseResult parseResult = imageImportService.analyze(new ItemImageImportRequest(
                    filePart.getOriginalFilename(),
                    filePart.getContentType(),
                    filePart.getContent()
            ));
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
        List<ImportedItemAffix> affixes = parseAffixes(fields);
        if (affixes.isEmpty()) {
            affixes = new ImportedItemAffixExtractor().extractEditableAffixes(decodedFullItemRead);
        }
        ItemImportEditableForm form = new ItemImportEditableForm(
                fields.getOrDefault("sourceImageName", "nieznany-item"),
                fields.getOrDefault("slot", ""),
                fields.getOrDefault("weaponDamage", ""),
                fields.getOrDefault("strength", ""),
                fields.getOrDefault("intelligence", ""),
                fields.getOrDefault("thorns", ""),
                fields.getOrDefault("blockChance", ""),
                fields.getOrDefault("retributionChance", ""),
                decodedFullItemRead,
                affixes
        );

        ItemImportFormMapper.MappingResult mappingResult = formMapper.map(form);
        if (!mappingResult.getErrors().isEmpty() || mappingResult.getItem() == null) {
            return buildErrorPageModel(
                    form,
                    null,
                    mappingResult.getErrors(),
                    currentBuildQuery,
                    heroService.requireActiveHero()
            );
        }

        HeroProfile activeHero = heroService.requireActiveHero();
        ValidatedImportedItem importedItem = mappingResult.getItem();
        FullItemRead fullItemRead = fullItemReadAffixUpdater.withEditedAffixes(form.getFullItemRead(), form.getAffixes());
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

    private static List<ImportedItemAffix> parseAffixes(Map<String, String> fields) {
        List<ImportedItemAffix> affixes = new java.util.ArrayList<>();
        int affixCount = parseAffixCount(fields.get("affixCount"));
        for (int index = 0; index < affixCount; index++) {
            if ("true".equals(fields.get("affixRemoved_" + index))) {
                continue;
            }
            parseAffixRow(fields, index).ifPresent(affixes::add);
        }
        parseNewAffix(fields).ifPresent(affixes::add);
        return affixes;
    }

    private static java.util.Optional<ImportedItemAffix> parseAffixRow(Map<String, String> fields, int index) {
        String typeValue = fields.getOrDefault("affixType_" + index, "");
        String value = fields.getOrDefault("affixValue_" + index, "");
        String originalType = fields.getOrDefault("affixOriginalType_" + index, "");
        String originalValue = fields.getOrDefault("affixOriginalValue_" + index, "");
        String sourceText = fields.getOrDefault("affixSourceText_" + index, "");
        return parseAffix(typeValue, value, typeValue.equals(originalType) && value.equals(originalValue) ? sourceText : "");
    }

    private static java.util.Optional<ImportedItemAffix> parseNewAffix(Map<String, String> fields) {
        return parseAffix(fields.getOrDefault("newAffixType", ""), fields.getOrDefault("newAffixValue", ""), "");
    }

    private static java.util.Optional<ImportedItemAffix> parseAffix(String rawType, String rawValue, String sourceText) {
        if (rawType == null || rawType.isBlank() || rawValue == null || rawValue.isBlank()) {
            return java.util.Optional.empty();
        }
        try {
            ImportedItemAffixType type = ImportedItemAffixType.valueOf(rawType);
            double value = Double.parseDouble(rawValue.replace(',', '.'));
            if (value < 0.0d) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(new ImportedItemAffix(type, value, sourceText));
        } catch (IllegalArgumentException exception) {
            return java.util.Optional.empty();
        }
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
        return "To jest import wspomagany pojedynczego itemu ze screena. Foundation sprawdza obraz, pokazuje niepewność pól i wymaga ręcznego zatwierdzenia użytkownika. Nie jest to jeszcze pełny automatyczny import całej postaci.";
    }

    private void renderPage(HttpExchange exchange, ItemImportPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

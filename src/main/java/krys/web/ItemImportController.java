package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.item.Item;
import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ImportedItemCurrentBuildContributionMapper;
import krys.itemimport.ItemImageImportCandidateParseResult;
import krys.itemimport.ItemImageImportRequest;
import krys.itemimport.ItemImageImportService;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportEditableFormFactory;
import krys.itemimport.ItemImportFormMapper;
import krys.itemimport.ValidatedImportedItem;
import krys.itemimport.ValidatedImportedItemToItemMapper;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Kontroler SSR dla pierwszego foundation importu pojedynczego itemu ze screena. */
public final class ItemImportController implements HttpHandler {
    private static final String HTML_CONTENT_TYPE = "text/html; charset=UTF-8";
    private static final long CURRENT_BUILD_DEFAULT_WEAPON_DAMAGE =
            Long.parseLong(CurrentBuildFormData.defaultValues().getWeaponDamage());

    private final ItemImageImportService imageImportService;
    private final ItemImportPageRenderer renderer;
    private final ItemImportEditableFormFactory editableFormFactory;
    private final ItemImportFormMapper formMapper;
    private final ValidatedImportedItemToItemMapper itemMapper;
    private final ImportedItemCurrentBuildContributionMapper contributionMapper;

    public ItemImportController(ItemImageImportService imageImportService,
                                ItemImportPageRenderer renderer) {
        this(
                imageImportService,
                renderer,
                new ItemImportEditableFormFactory(),
                new ItemImportFormMapper(),
                new ValidatedImportedItemToItemMapper(),
                new ImportedItemCurrentBuildContributionMapper()
        );
    }

    ItemImportController(ItemImageImportService imageImportService,
                         ItemImportPageRenderer renderer,
                         ItemImportEditableFormFactory editableFormFactory,
                         ItemImportFormMapper formMapper,
                         ValidatedImportedItemToItemMapper itemMapper,
                         ImportedItemCurrentBuildContributionMapper contributionMapper) {
        this.imageImportService = imageImportService;
        this.renderer = renderer;
        this.editableFormFactory = editableFormFactory;
        this.formMapper = formMapper;
        this.itemMapper = itemMapper;
        this.contributionMapper = contributionMapper;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                renderPage(exchange, emptyPageModel());
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
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null) {
            return buildErrorPageModel(null, null, List.of("Brak nagłówka `Content-Type`."));
        }
        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        if (normalizedContentType.startsWith("multipart/form-data")) {
            return handleImageUpload(exchange);
        }
        if (normalizedContentType.startsWith("application/x-www-form-urlencoded")) {
            return handleConfirmation(exchange);
        }
        return buildErrorPageModel(null, null, List.of("Nieobsługiwany typ danych formularza dla importu itemu."));
    }

    private ItemImportPageModel handleImageUpload(HttpExchange exchange) throws IOException {
        try {
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
                    buildHelpText()
            );
        } catch (IllegalArgumentException exception) {
            return buildErrorPageModel(null, null, List.of(exception.getMessage()));
        }
    }

    private ItemImportPageModel handleConfirmation(HttpExchange exchange) throws IOException {
        Map<String, String> fields = UrlEncodedFormSupport.parseBody(exchange);
        ItemImportEditableForm form = new ItemImportEditableForm(
                fields.getOrDefault("sourceImageName", "nieznany-item"),
                fields.getOrDefault("slot", ""),
                fields.getOrDefault("weaponDamage", ""),
                fields.getOrDefault("strength", ""),
                fields.getOrDefault("intelligence", ""),
                fields.getOrDefault("thorns", ""),
                fields.getOrDefault("blockChance", ""),
                fields.getOrDefault("retributionChance", "")
        );

        ItemImportFormMapper.MappingResult mappingResult = formMapper.map(form);
        if (!mappingResult.getErrors().isEmpty() || mappingResult.getItem() == null) {
            return buildErrorPageModel(form, null, mappingResult.getErrors());
        }

        ValidatedImportedItem importedItem = mappingResult.getItem();
        Item mappedItem = itemMapper.map(importedItem);
        ImportedItemCurrentBuildContribution contribution = contributionMapper.map(importedItem);
        return new ItemImportPageModel(
                form,
                null,
                List.of(),
                new ItemImportPageModel.ConfirmedImportView(
                        importedItem,
                        mappedItem,
                        contribution,
                        buildCurrentBuildPrefillUrl(contribution)
                ),
                buildHelpText()
        );
    }

    private ItemImportPageModel emptyPageModel() {
        return new ItemImportPageModel(null, null, List.of(), null, buildHelpText());
    }

    private ItemImportPageModel buildErrorPageModel(ItemImportEditableForm form,
                                                    ItemImageImportCandidateParseResult parseResult,
                                                    List<String> errors) {
        return new ItemImportPageModel(form, parseResult, errors, null, buildHelpText());
    }

    private static String buildHelpText() {
        return "To jest import wspomagany pojedynczego itemu ze screena. Foundation sprawdza obraz, pokazuje niepewność pól i wymaga ręcznego zatwierdzenia użytkownika. Nie jest to jeszcze pełny automatyczny import całej postaci.";
    }

    private static String buildCurrentBuildPrefillUrl(ImportedItemCurrentBuildContribution contribution) {
        List<String> pairs = new ArrayList<>();
        long weaponDamage = contribution.getWeaponDamage() > 0L ? contribution.getWeaponDamage() : CURRENT_BUILD_DEFAULT_WEAPON_DAMAGE;
        pairs.add(pair("weaponDamage", Long.toString(weaponDamage)));
        pairs.add(pair("strength", formatWhole(contribution.getStrength())));
        pairs.add(pair("intelligence", formatWhole(contribution.getIntelligence())));
        pairs.add(pair("thorns", formatWhole(contribution.getThorns())));
        pairs.add(pair("blockChance", formatWhole(contribution.getBlockChance())));
        pairs.add(pair("retributionChance", formatWhole(contribution.getRetributionChance())));
        return "/policz-aktualny-build?" + String.join("&", pairs);
    }

    private static String pair(String key, String value) {
        return URLEncoder.encode(key, StandardCharsets.UTF_8) + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String formatWhole(double value) {
        return String.format(Locale.US, "%.0f", value);
    }

    private void renderPage(HttpExchange exchange, ItemImportPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

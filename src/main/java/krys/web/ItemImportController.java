package krys.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import krys.item.Item;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemimport.CurrentBuildItemApplicationMode;
import krys.itemimport.ImportedItemCurrentBuildContribution;
import krys.itemimport.ImportedItemCurrentBuildApplicationService;
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
import java.nio.charset.StandardCharsets;
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
    private final ImportedItemCurrentBuildApplicationService applicationService;

    public ItemImportController(ItemImageImportService imageImportService,
                                ItemImportPageRenderer renderer) {
        this(
                imageImportService,
                renderer,
                new ItemImportEditableFormFactory(),
                new ItemImportFormMapper(),
                new ValidatedImportedItemToItemMapper(),
                new ImportedItemCurrentBuildContributionMapper(),
                new ImportedItemCurrentBuildApplicationService()
        );
    }

    ItemImportController(ItemImageImportService imageImportService,
                         ItemImportPageRenderer renderer,
                         ItemImportEditableFormFactory editableFormFactory,
                         ItemImportFormMapper formMapper,
                         ValidatedImportedItemToItemMapper itemMapper,
                         ImportedItemCurrentBuildContributionMapper contributionMapper,
                         ImportedItemCurrentBuildApplicationService applicationService) {
        this.imageImportService = imageImportService;
        this.renderer = renderer;
        this.editableFormFactory = editableFormFactory;
        this.formMapper = formMapper;
        this.itemMapper = itemMapper;
        this.contributionMapper = contributionMapper;
        this.applicationService = applicationService;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase(Locale.ROOT);
            if ("GET".equals(method)) {
                CurrentBuildFormData contextFormData = CurrentBuildFormQuerySupport.resolveImportContext(
                        UrlEncodedFormSupport.parseQuery(exchange.getRequestURI().getRawQuery())
                );
                renderPage(exchange, emptyPageModel(CurrentBuildFormQuerySupport.toQuery(contextFormData)));
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
        String currentBuildQuery = CurrentBuildFormQuerySupport.toQuery(
                CurrentBuildFormQuerySupport.resolveImportContext(UrlEncodedFormSupport.parseQuery(exchange.getRequestURI().getRawQuery()))
        );
        if (contentType == null) {
            return buildErrorPageModel(null, null, List.of("Brak nagłówka `Content-Type`."), currentBuildQuery);
        }
        String normalizedContentType = contentType.toLowerCase(Locale.ROOT);
        if (normalizedContentType.startsWith("multipart/form-data")) {
            return handleImageUpload(exchange);
        }
        if (normalizedContentType.startsWith("application/x-www-form-urlencoded")) {
            return handleConfirmation(exchange);
        }
        return buildErrorPageModel(null, null, List.of("Nieobsługiwany typ danych formularza dla importu itemu."), currentBuildQuery);
    }

    private ItemImportPageModel handleImageUpload(HttpExchange exchange) throws IOException {
        try {
            CurrentBuildFormData contextFormData = CurrentBuildFormQuerySupport.resolveImportContext(
                    UrlEncodedFormSupport.parseQuery(exchange.getRequestURI().getRawQuery())
            );
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
                    buildHelpText(),
                    CurrentBuildFormQuerySupport.toQuery(contextFormData)
            );
        } catch (IllegalArgumentException exception) {
            CurrentBuildFormData contextFormData = CurrentBuildFormQuerySupport.resolveImportContext(
                    UrlEncodedFormSupport.parseQuery(exchange.getRequestURI().getRawQuery())
            );
            return buildErrorPageModel(null, null, List.of(exception.getMessage()), CurrentBuildFormQuerySupport.toQuery(contextFormData));
        }
    }

    private ItemImportPageModel handleConfirmation(HttpExchange exchange) throws IOException {
        Map<String, String> fields = UrlEncodedFormSupport.parseBody(exchange);
        String currentBuildQuery = fields.getOrDefault("currentBuildQuery", "");
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
            return buildErrorPageModel(form, null, mappingResult.getErrors(), currentBuildQuery);
        }

        CurrentBuildFormData contextFormData = CurrentBuildFormQuerySupport.fromSerializedQuery(currentBuildQuery);
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
                        buildCurrentBuildPrefillUrl(contextFormData, contribution, CurrentBuildItemApplicationMode.OVERWRITE),
                        buildCurrentBuildPrefillUrl(contextFormData, contribution, CurrentBuildItemApplicationMode.ADD_CONTRIBUTION)
                ),
                buildHelpText(),
                currentBuildQuery
        );
    }

    private ItemImportPageModel emptyPageModel(String currentBuildQuery) {
        return new ItemImportPageModel(null, null, List.of(), null, buildHelpText(), currentBuildQuery);
    }

    private ItemImportPageModel buildErrorPageModel(ItemImportEditableForm form,
                                                    ItemImageImportCandidateParseResult parseResult,
                                                    List<String> errors,
                                                    String currentBuildQuery) {
        return new ItemImportPageModel(form, parseResult, errors, null, buildHelpText(), currentBuildQuery);
    }

    private static String buildHelpText() {
        return "To jest import wspomagany pojedynczego itemu ze screena. Foundation sprawdza obraz, pokazuje niepewność pól i wymaga ręcznego zatwierdzenia użytkownika. Nie jest to jeszcze pełny automatyczny import całej postaci.";
    }

    private String buildCurrentBuildPrefillUrl(CurrentBuildFormData contextFormData,
                                               ImportedItemCurrentBuildContribution contribution,
                                               CurrentBuildItemApplicationMode mode) {
        CurrentBuildImportableStats contextStats = CurrentBuildFormQuerySupport.importableStats(contextFormData);
        CurrentBuildImportableStats appliedStats = applicationService.apply(contextStats, contribution, mode);
        CurrentBuildImportableStats normalizedStats = new CurrentBuildImportableStats(
                appliedStats.getWeaponDamage() > 0L ? appliedStats.getWeaponDamage() : CURRENT_BUILD_DEFAULT_WEAPON_DAMAGE,
                appliedStats.getStrength(),
                appliedStats.getIntelligence(),
                appliedStats.getThorns(),
                appliedStats.getBlockChance(),
                appliedStats.getRetributionChance()
        );
        CurrentBuildFormData updatedFormData = CurrentBuildFormQuerySupport.withAppliedStats(contextFormData, normalizedStats);
        return "/policz-aktualny-build?" + CurrentBuildFormQuerySupport.toQuery(updatedFormData);
    }

    private void renderPage(HttpExchange exchange, ItemImportPageModel pageModel) throws IOException {
        byte[] responseBytes = renderer.render(pageModel).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML_CONTENT_TYPE);
        exchange.sendResponseHeaders(200, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }
}

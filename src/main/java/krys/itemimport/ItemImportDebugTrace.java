package krys.itemimport;

import krys.itemlibrary.HeroSlotItemAssignment;
import krys.itemlibrary.SavedImportedItem;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkingResolvedItemValueResolver;
import krys.tempering.ItemTemperingAffix;
import krys.transfiguration.ItemTransfiguration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Centralny, domyślnie wyłączony trace techniczny importu itemów. */
public final class ItemImportDebugTrace {
    public static final String JVM_PROPERTY = "dstats.itemImport.debug";
    public static final String CONFIG_PROPERTY = "dstats.item-import.debug";
    public static final String FILE_PROPERTY = "dstats.itemImport.debug.file";
    public static final String LOGGER_NAME = "krys.itemimport.debug";

    private static final Logger LOGGER = Logger.getLogger(LOGGER_NAME);
    private static final DateTimeFormatter ID_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern NUMERIC_TOKEN_PATTERN = Pattern.compile(
            "\\+?\\s*[0-9]+(?:\\s[0-9]{3})*(?:[,.][0-9]+)?%?|\\[[^\\]]*]|\\([^)]*[0-9][^)]*\\)"
    );
    private static final ThreadLocal<TraceContext> CURRENT = new ThreadLocal<>();
    private static final Map<ItemImageMetadata, String> METADATA_TRACE_IDS =
            Collections.synchronizedMap(new WeakHashMap<>());

    static {
        LOGGER.setLevel(Level.INFO);
    }

    private ItemImportDebugTrace() {
    }

    public static boolean isEnabled() {
        return Boolean.parseBoolean(System.getProperty(JVM_PROPERTY, "false"))
                || Boolean.parseBoolean(System.getProperty(CONFIG_PROPERTY, "false"));
    }

    public static Scope startImport() {
        if (!isEnabled()) {
            return Scope.noop();
        }
        return push(new TraceContext(
                "ITEM-IMPORT-" + ID_TIMESTAMP.format(LocalDateTime.now()) + "-" + UUID.randomUUID(),
                null,
                null,
                ""
        ));
    }

    public static Scope startOperation(String prefix) {
        if (!isEnabled()) {
            return Scope.noop();
        }
        TraceContext current = CURRENT.get();
        if (current != null && current.id() != null && !current.id().isBlank()) {
            return Scope.noop();
        }
        String safePrefix = prefix == null || prefix.isBlank() ? "ITEM-IMPORT" : prefix;
        return push(new TraceContext(
                safePrefix + "-" + ID_TIMESTAMP.format(LocalDateTime.now()) + "-" + UUID.randomUUID(),
                null,
                null,
                ""
        ));
    }

    public static Scope withOcrVariant(int screenIndex, int variantIndex, String variantId) {
        if (!isEnabled()) {
            return Scope.noop();
        }
        TraceContext current = CURRENT.get();
        if (current == null) {
            current = new TraceContext("ITEM-IMPORT-" + ID_TIMESTAMP.format(LocalDateTime.now()) + "-" + UUID.randomUUID(),
                    null, null, "");
        }
        return push(new TraceContext(current.id(), screenIndex, variantIndex, safe(variantId)));
    }

    public static void bindMetadata(ItemImageMetadata metadata) {
        if (!isEnabled() || metadata == null) {
            return;
        }
        String id = currentId();
        if (!id.isBlank()) {
            METADATA_TRACE_IDS.put(metadata, id);
        }
    }

    public static Scope withMetadata(ItemImageMetadata metadata) {
        if (!isEnabled() || metadata == null) {
            return Scope.noop();
        }
        TraceContext current = CURRENT.get();
        if (current != null && current.id() != null && !current.id().isBlank()) {
            return Scope.noop();
        }
        String id = METADATA_TRACE_IDS.get(metadata);
        if (id == null || id.isBlank()) {
            return Scope.noop();
        }
        return push(new TraceContext(id, null, null, ""));
    }

    public static String currentId() {
        TraceContext current = CURRENT.get();
        return current == null ? "" : current.id();
    }

    public static void log(String section, Supplier<String> messageSupplier) {
        if (!isEnabled()) {
            return;
        }
        String sectionName = section == null ? "TRACE" : section;
        String payload = messageSupplier == null ? "" : messageSupplier.get();
        TraceContext context = CURRENT.get();
        String id = context == null || context.id() == null || context.id().isBlank()
                ? "NO-CORRELATION-ID"
                : context.id();
        String contextPayload = contextPayload(context);
        String line = "[ITEM_IMPORT_DEBUG][id=" + id + "] "
                + sectionName
                + (contextPayload.isBlank() ? "" : " " + contextPayload)
                + (payload == null || payload.isBlank() ? "" : " " + payload);
        LOGGER.info(line);
        appendToDebugFile(line);
    }

    public static String compactText(String value) {
        return quote(safe(value).replace("\r\n", "\n").replace('\r', '\n').replace("\n", "\\n"));
    }

    public static String quote(String value) {
        return "\"" + safe(value).replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    public static boolean hasNumericOrBracketTokens(String line) {
        return line != null && NUMERIC_TOKEN_PATTERN.matcher(line).find();
    }

    public static String numericTokens(String line) {
        Matcher matcher = NUMERIC_TOKEN_PATTERN.matcher(safe(line));
        StringBuilder builder = new StringBuilder("[");
        while (matcher.find()) {
            if (builder.length() > 1) {
                builder.append(", ");
            }
            builder.append(quote(matcher.group().trim()));
        }
        return builder.append(']').toString();
    }

    public static String formatDetails(ItemImportDetails details) {
        ItemImportDetails safeDetails = details == null ? ItemImportDetails.empty() : details;
        return "name=" + quote(safeDetails.getItemName())
                + " itemType=" + quote(safeDetails.getItemType())
                + " slot=" + value(safeDetails.getEquipmentSlot())
                + " rarity=" + quote(safeDetails.getItemRarity())
                + " ancient=" + safeDetails.isAncient()
                + " mythicUnique=" + safeDetails.isMythicUnique()
                + " power=" + value(safeDetails.getItemPower())
                + " armor=" + value(safeDetails.getItemArmor())
                + " weaponDps=" + value(safeDetails.getWeaponDps())
                + " weaponDamage=" + value(safeDetails.getWeaponDamageMin()) + "-" + value(safeDetails.getWeaponDamageMax())
                + " averageWeaponDamage=" + value(safeDetails.getAverageWeaponDamage())
                + " attacksPerSecond=" + value(safeDetails.getAttacksPerSecond())
                + " effectText=" + compactText(safeDetails.getUniqueEffectText());
    }

    public static String formatAffix(ImportedItemAffix affix) {
        if (affix == null) {
            return "affix=null";
        }
        return "type=" + affix.getType()
                + " value=" + value(affix.getValue())
                + " unit=" + quote(affix.getUnit())
                + " referenceValue=" + value(affix.getReferenceValue())
                + " rollRangeMin=" + value(affix.getRollRangeMin())
                + " rollRangeMax=" + value(affix.getRollRangeMax())
                + " greaterAffix=" + affix.isGreaterAffix()
                + " displayOrder=" + affix.getDisplayOrder()
                + " source=" + affix.getSource()
                + " dedupKey=" + quote(affix.getAffixDefinitionId())
                + " displayValue=" + quote(affix.getDisplayValue())
                + " sourceLine=" + compactText(affix.getSourceText());
    }

    public static String formatForm(ItemImportEditableForm form) {
        if (form == null) {
            return "form=null";
        }
        return "source=" + quote(form.getSourceImageName())
                + " slot=" + quote(form.getSlot())
                + " weaponDamage=" + quote(form.getWeaponDamage())
                + " strength=" + quote(form.getStrength())
                + " intelligence=" + quote(form.getIntelligence())
                + " thorns=" + quote(form.getThorns())
                + " blockChance=" + quote(form.getBlockChance())
                + " retributionChance=" + quote(form.getRetributionChance())
                + " mythicUnique=" + form.isMythicUnique()
                + " selectedAspectId=" + quote(form.getSelectedAspectId())
                + " ordinaryAffixes=" + form.getAffixes().size()
                + " tempering=" + form.getTemperingAffixes().size()
                + " transfiguration=" + formatTransfiguration(form.getTransfiguration());
    }

    public static String formatSavedItem(SavedImportedItem item) {
        if (item == null) {
            return "item=null";
        }
        return "itemId=" + item.getItemId()
                + " name=" + quote(item.getDisplayName())
                + " source=" + quote(item.getSourceImageName())
                + " slot=" + item.getSlot()
                + " mythicUnique=" + item.getDetails().isMythicUnique()
                + " affixes=" + item.getAffixes().size()
                + " tempering=" + item.getTemperingAffixes().size()
                + " transfiguration=" + formatTransfiguration(item.getTransfiguration());
    }

    public static String formatRuntimeAssignment(HeroSlotItemAssignment assignment,
                                                 ImportedItemAffix affix,
                                                 ItemMasterworking masterworking,
                                                 double resolvedValue) {
        SavedImportedItem item = assignment.getItem();
        return "slot=" + assignment.getHeroSlot()
                + " itemId=" + item.getItemId()
                + " item=" + quote(item.getDisplayName())
                + " mythicUnique=" + item.getDetails().isMythicUnique()
                + " type=" + affix.getType()
                + " stored=" + value(affix.getValue())
                + " referenceValue=" + value(affix.getReferenceValue())
                + " rollRange=" + value(affix.getRollRangeMin()) + "-" + value(affix.getRollRangeMax())
                + " greaterAffix=" + affix.isGreaterAffix()
                + " resolved=" + value(resolvedValue)
                + " masterworkingQuality=" + (masterworking == null ? "null" : masterworking.getQualityCurrent())
                + " reason=" + quote(runtimeReason(affix, masterworking, resolvedValue));
    }

    public static double resolveRuntimeAffixValue(ImportedItemAffix affix, ItemMasterworking masterworking) {
        return new MasterworkingResolvedItemValueResolver().resolveAffixValue(affix, masterworking);
    }

    public static double resolveRuntimeAffixValue(ImportedItemAffix affix,
                                                  ItemMasterworking masterworking,
                                                  boolean displayedValueAlreadyCurrent) {
        return new MasterworkingResolvedItemValueResolver().resolveAffixValue(affix, masterworking, displayedValueAlreadyCurrent);
    }

    public static String formatTempering(ItemTemperingAffix affix) {
        if (affix == null) {
            return "tempering=null";
        }
        return "definitionId=" + quote(affix.getDefinitionId())
                + " category=" + affix.getCategory()
                + " value=" + value(affix.getValue())
                + " runtimeStatus=" + affix.getRuntimeStatus()
                + " greaterAffix=" + affix.isGreaterAffix()
                + " displayText=" + compactText(affix.getDisplayText());
    }

    public static void logAffixList(String section, List<ImportedItemAffix> affixes) {
        if (!isEnabled()) {
            return;
        }
        List<ImportedItemAffix> safeAffixes = affixes == null ? List.of() : affixes;
        for (int index = 0; index < safeAffixes.size(); index++) {
            int finalIndex = index;
            ImportedItemAffix affix = safeAffixes.get(index);
            log(section, () -> "index=" + finalIndex + " sourceCategory=ordinary " + formatAffix(affix));
        }
    }

    static String safeFileName(String originalFilename) {
        String value = safe(originalFilename);
        int slash = Math.max(value.lastIndexOf('/'), value.lastIndexOf('\\'));
        return slash >= 0 ? value.substring(slash + 1) : value;
    }

    private static Scope push(TraceContext next) {
        TraceContext previous = CURRENT.get();
        CURRENT.set(next);
        return new Scope(previous, true);
    }

    private static String contextPayload(TraceContext context) {
        if (context == null || context.screenIndex() == null) {
            return "";
        }
        return "screen=" + context.screenIndex()
                + " variant=" + (context.variantIndex() == null ? "null" : context.variantIndex())
                + (context.variantId().isBlank() ? "" : " variantId=" + quote(context.variantId()));
    }

    private static void appendToDebugFile(String line) {
        String fileName = System.getProperty(FILE_PROPERTY, "logs/item-import-debug.log");
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(fileName);
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(
                    path,
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException exception) {
            LOGGER.log(Level.FINE, "Nie udało się dopisać technicznego trace importu itemu do pliku.", exception);
        }
    }

    private static String formatTransfiguration(ItemTransfiguration transfiguration) {
        if (transfiguration == null) {
            return "null";
        }
        return "{transfigured=" + transfiguration.isTransfigured()
                + ", outcome=" + transfiguration.getOutcome()
                + ", locked=" + transfiguration.isLockedAfterTransfiguration()
                + "}";
    }

    private static String runtimeReason(ImportedItemAffix affix, ItemMasterworking masterworking, double resolvedValue) {
        if (affix == null) {
            return "no affix";
        }
        if (masterworking == null || masterworking.getQualityCurrent() <= 0 || Math.abs(affix.getValue() - resolvedValue) < 0.0000001d) {
            return "stored value used by runtime";
        }
        return "masterworking resolver adjusted stored value";
    }

    private static String value(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Double doubleValue) {
            return String.format(Locale.US, "%.4f", doubleValue);
        }
        if (value instanceof Float floatValue) {
            return String.format(Locale.US, "%.4f", floatValue);
        }
        return value.toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private record TraceContext(String id, Integer screenIndex, Integer variantIndex, String variantId) {
    }

    public static final class Scope implements AutoCloseable {
        private static final Scope NOOP = new Scope(null, false);
        private final TraceContext previous;
        private final boolean active;

        private Scope(TraceContext previous, boolean active) {
            this.previous = previous;
            this.active = active;
        }

        private static Scope noop() {
            return NOOP;
        }

        @Override
        public void close() {
            if (!active) {
                return;
            }
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }
}

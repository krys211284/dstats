package krys.itemlibrary;

import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportDetails;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.masterworking.MasterworkedAffixSource;
import krys.socketing.ItemSocket;
import krys.socketing.ItemSocketing;
import krys.socketing.SocketContentType;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingRuntimeStatus;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.HoradricTuningPrism;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationAffixRoll;
import krys.transfiguration.TransfigurationValueProvenance;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Trwałe repozytorium biblioteki itemów oparte o proste pliki tekstowe w wyznaczonym katalogu danych użytkownika. */
public final class FileItemLibraryRepository implements ItemLibraryRepository {
    private static final String ITEM_PREFIX = "ITEM";
    private static final String SELECTION_PREFIX = "SEL";

    private final Path itemsFilePath;
    private final Path selectionFilePath;

    public FileItemLibraryRepository(Path dataDirectory) {
        this.itemsFilePath = dataDirectory.resolve("saved-items.db");
        this.selectionFilePath = dataDirectory.resolve("active-selection.db");
    }

    @Override
    public synchronized SavedImportedItem save(SavedImportedItem item) {
        List<SavedImportedItem> items = loadItems();
        SavedImportedItem persistedItem = item;
        if (item.getItemId() <= 0L) {
            long nextId = items.stream()
                    .mapToLong(SavedImportedItem::getItemId)
                    .max()
                    .orElse(0L) + 1L;
            persistedItem = new SavedImportedItem(
                    nextId,
                    item.getDisplayName(),
                    item.getSourceImageName(),
                    item.getSlot(),
                    item.getWeaponDamage(),
                    item.getStrength(),
                    item.getIntelligence(),
                    item.getThorns(),
                    item.getBlockChance(),
                    item.getRetributionChance(),
                    item.getFullItemRead(),
                    item.getAffixes(),
                    item.getSelectedAspectId(),
                    item.getDetails(),
                    item.getTemperingAffixes(),
                    item.getMasterworking(),
                    item.getTransfiguration(),
                    item.getSocketing()
            );
        }

        SavedImportedItem finalPersistedItem = persistedItem;
        items.removeIf(existingItem -> existingItem.getItemId() == finalPersistedItem.getItemId());
        items.add(finalPersistedItem);
        items.sort(Comparator.comparingLong(SavedImportedItem::getItemId));
        writeItems(items);
        return finalPersistedItem;
    }

    @Override
    public synchronized List<SavedImportedItem> findAll() {
        return List.copyOf(loadItems());
    }

    @Override
    public synchronized Optional<SavedImportedItem> findById(long itemId) {
        return loadItems().stream()
                .filter(item -> item.getItemId() == itemId)
                .findFirst();
    }

    @Override
    public synchronized void delete(long itemId) {
        List<SavedImportedItem> items = loadItems();
        items.removeIf(item -> item.getItemId() == itemId);
        writeItems(items);
    }

    @Override
    public synchronized ActiveItemSelection loadSelection() {
        if (!Files.exists(selectionFilePath)) {
            return ActiveItemSelection.empty();
        }

        EnumMap<EquipmentSlot, Long> selection = new EnumMap<>(EquipmentSlot.class);
        try {
            for (String line : Files.readAllLines(selectionFilePath, StandardCharsets.UTF_8)) {
                String trimmedLine = line.trim();
                if (trimmedLine.isBlank()) {
                    continue;
                }
                String[] tokens = trimmedLine.split("\\|", -1);
                if (tokens.length != 3 || !SELECTION_PREFIX.equals(tokens[0])) {
                    continue;
                }
                selection.put(
                        EquipmentSlot.valueOf(tokens[1]),
                        Long.parseLong(tokens[2])
                );
            }
            return new ActiveItemSelection(selection);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się odczytać pliku selekcji biblioteki itemów.", exception);
        }
    }

    @Override
    public synchronized void saveSelection(ActiveItemSelection selection) {
        ensureDirectoryExists();
        List<String> lines = new ArrayList<>();
        for (Map.Entry<EquipmentSlot, Long> entry : selection.getSelectedItemIdsBySlot().entrySet()) {
            lines.add(SELECTION_PREFIX + "|" + entry.getKey().name() + "|" + entry.getValue());
        }
        try {
            Files.write(selectionFilePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się zapisać selekcji biblioteki itemów.", exception);
        }
    }

    private List<SavedImportedItem> loadItems() {
        if (!Files.exists(itemsFilePath)) {
            return new ArrayList<>();
        }

        try {
            List<SavedImportedItem> items = new ArrayList<>();
            for (String line : Files.readAllLines(itemsFilePath, StandardCharsets.UTF_8)) {
                String trimmedLine = line.trim();
                if (trimmedLine.isBlank()) {
                    continue;
                }
                items.add(parseItem(trimmedLine));
            }
            items.sort(Comparator.comparingLong(SavedImportedItem::getItemId));
            return items;
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się odczytać pliku biblioteki itemów.", exception);
        }
    }

    private SavedImportedItem parseItem(String line) {
        String[] tokens = line.split("\\|", -1);
        if (tokens.length < 11 || !ITEM_PREFIX.equals(tokens[0])) {
            throw new IllegalStateException("Plik biblioteki itemów ma niepoprawny format.");
        }
        return new SavedImportedItem(
                Long.parseLong(tokens[1]),
                decode(tokens[2]),
                decode(tokens[3]),
                EquipmentSlot.valueOf(tokens[4]),
                Long.parseLong(tokens[5]),
                Double.parseDouble(tokens[6]),
                Double.parseDouble(tokens[7]),
                Double.parseDouble(tokens[8]),
                Double.parseDouble(tokens[9]),
                Double.parseDouble(tokens[10]),
                tokens.length >= 12 ? decodeFullItemRead(tokens[11]) : FullItemRead.empty(),
                tokens.length >= 13 ? decodeAffixes(tokens[12]) : List.of(),
                tokens.length >= 14 ? decode(tokens[13]) : "",
                tokens.length >= 15 ? decodeDetails(tokens[14]) : ItemImportDetails.empty(),
                tokens.length >= 16 ? decodeTemperingAffixes(tokens[15]) : List.of(),
                tokens.length >= 17 ? decodeMasterworking(tokens[16]) : ItemMasterworking.defaultState(),
                tokens.length >= 18 ? decodeTransfiguration(tokens[17]) : ItemTransfiguration.none(),
                tokens.length >= 19 ? decodeSocketing(tokens[18]) : ItemSocketing.empty()
        );
    }

    private void writeItems(List<SavedImportedItem> items) {
        ensureDirectoryExists();
        List<String> lines = new ArrayList<>();
        for (SavedImportedItem item : items) {
            lines.add(String.join("|",
                    ITEM_PREFIX,
                    Long.toString(item.getItemId()),
                    encode(item.getDisplayName()),
                    encode(item.getSourceImageName()),
                    item.getSlot().name(),
                    Long.toString(item.getWeaponDamage()),
                    formatDouble(item.getStrength()),
                    formatDouble(item.getIntelligence()),
                    formatDouble(item.getThorns()),
                    formatDouble(item.getBlockChance()),
                    formatDouble(item.getRetributionChance()),
                    encodeFullItemRead(item.getFullItemRead()),
                    encodeAffixes(item.getAffixes()),
                    encode(item.getSelectedAspectId()),
                    encodeDetails(item.getDetails()),
                    encodeTemperingAffixes(item.getTemperingAffixes()),
                    encodeMasterworking(item.getMasterworking()),
                    encodeTransfiguration(item.getTransfiguration()),
                    encodeSocketing(item.getSocketing())
            ));
        }
        try {
            Files.write(itemsFilePath, lines, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się zapisać biblioteki itemów.", exception);
        }
    }

    private void ensureDirectoryExists() {
        try {
            Files.createDirectories(itemsFilePath.getParent());
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się przygotować katalogu biblioteki itemów.", exception);
        }
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private static String formatDouble(double value) {
        return String.format(Locale.US, "%.4f", value);
    }

    private static String encodeFullItemRead(FullItemRead fullItemRead) {
        FullItemRead safeRead = fullItemRead == null ? FullItemRead.empty() : fullItemRead;
        List<String> payloadLines = new ArrayList<>();
        payloadLines.add("NAME|" + encode(safeRead.getItemName()));
        payloadLines.add("TYPE|" + encode(safeRead.getItemTypeLine()));
        payloadLines.add("RARITY|" + encode(safeRead.getRarity()));
        payloadLines.add("POWER|" + encode(safeRead.getItemPower()));
        payloadLines.add("BASE|" + encode(safeRead.getBaseItemValue()));
        for (FullItemReadLine line : safeRead.getLines()) {
            payloadLines.add("LINE|" + line.getType().name() + "|" + encode(line.getText()));
        }
        return encode(String.join("\n", payloadLines));
    }

    private static FullItemRead decodeFullItemRead(String encodedPayload) {
        String payload = decode(encodedPayload);
        String itemName = "";
        String itemTypeLine = "";
        String rarity = "";
        String itemPower = "";
        String baseItemValue = "";
        List<FullItemReadLine> lines = new ArrayList<>();
        for (String line : payload.split("\\R")) {
            String[] tokens = line.split("\\|", -1);
            if (tokens.length < 2) {
                continue;
            }
            switch (tokens[0]) {
                case "NAME" -> itemName = decode(tokens[1]);
                case "TYPE" -> itemTypeLine = decode(tokens[1]);
                case "RARITY" -> rarity = decode(tokens[1]);
                case "POWER" -> itemPower = decode(tokens[1]);
                case "BASE" -> baseItemValue = decode(tokens[1]);
                case "LINE" -> {
                    if (tokens.length >= 3) {
                        lines.add(new FullItemReadLine(FullItemReadLineType.valueOf(tokens[1]), decode(tokens[2])));
                    }
                }
                default -> {
                }
            }
        }
        return new FullItemRead(itemName, itemTypeLine, rarity, itemPower, baseItemValue, lines);
    }

    private static String encodeAffixes(List<ImportedItemAffix> affixes) {
        List<String> payloadLines = new ArrayList<>();
        for (ImportedItemAffix affix : affixes) {
            payloadLines.add(String.join("|",
                    affix.getType().name(),
                    formatDouble(affix.getValue()),
                    encode(affix.getUnit()),
                    Boolean.toString(affix.isGreaterAffix()),
                    Integer.toString(affix.getDisplayOrder()),
                    encode(affix.getRawOcrLine()),
                    affix.getSource().name(),
                    encode(affix.getAffixDefinitionId()),
                    encodeDouble(affix.getRollRangeMin()),
                    encodeDouble(affix.getRollRangeMax()),
                    encodeDouble(affix.getReferenceValue()),
                    encode(affix.getDisplayValue())
            ));
        }
        return encode(String.join("\n", payloadLines));
    }

    private static String encodeTemperingAffixes(List<ItemTemperingAffix> affixes) {
        List<String> payloadLines = new ArrayList<>();
        for (ItemTemperingAffix affix : affixes) {
            payloadLines.add(String.join("|",
                    encode(affix.getDefinitionId()),
                    affix.getCategory().name(),
                    formatDouble(affix.getValue()),
                    encode(affix.getDisplayText()),
                    affix.getRuntimeStatus().name(),
                    Boolean.toString(affix.isGreaterAffix())
            ));
        }
        return encode(String.join("\n", payloadLines));
    }

    private static String encodeMasterworking(ItemMasterworking masterworking) {
        ItemMasterworking safe = masterworking == null ? ItemMasterworking.defaultState() : masterworking;
        MasterworkedAffixSelection perfectedAffix = safe.getPerfectedAffix();
        return encode(String.join("|",
                Integer.toString(safe.getQualityCurrent()),
                Integer.toString(safe.getQualityMax()),
                perfectedAffix == null || perfectedAffix.getSource() == null ? "" : perfectedAffix.getSource().name(),
                perfectedAffix == null ? "" : encode(perfectedAffix.getKey())
        ));
    }

    private static String encodeTransfiguration(ItemTransfiguration transfiguration) {
        ItemTransfiguration safe = transfiguration == null ? ItemTransfiguration.none() : transfiguration;
        TransfigurationAffixRoll added = safe.getAddedTransfigurationAffix();
        TransfigurationAffixRoll replacement = safe.getReplacementTransfigurationAffix();
        return encode(String.join("|",
                Boolean.toString(safe.isTransfigured()),
                Boolean.toString(safe.isLockedAfterTransfiguration()),
                safe.getTuningPrism().name(),
                safe.getOutcome().name(),
                encode(safe.getUpgradedAffixRef()),
                encodeRollDefinition(added),
                encodeRollValue(added),
                encodeRollProvenance(added),
                encodeRollElement(added),
                encode(safe.getReplacedAffixRef()),
                encodeRollDefinition(replacement),
                encodeRollValue(replacement),
                encodeRollProvenance(replacement),
                encodeRollElement(replacement),
                safe.getBonusQuality() == null ? "" : safe.getBonusQuality().toString(),
                Boolean.toString(safe.isIndestructible()),
                encode(safe.getNotes())
        ));
    }

    private static String encodeSocketing(ItemSocketing socketing) {
        ItemSocketing safe = socketing == null ? ItemSocketing.empty() : socketing;
        List<String> payloadLines = new ArrayList<>();
        payloadLines.add("COUNT|" + safe.getSocketCount());
        for (ItemSocket socket : safe.getSockets()) {
            payloadLines.add(String.join("|",
                    "SOCKET",
                    Integer.toString(socket.getIndex()),
                    socket.getContentType().name(),
                    encode(socket.getGemId())
            ));
        }
        return encode(String.join("\n", payloadLines));
    }

    private static String encodeDetails(ItemImportDetails details) {
        ItemImportDetails safeDetails = details == null ? ItemImportDetails.empty() : details;
        List<String> payloadLines = new ArrayList<>();
        payloadLines.add("ITEM_NAME|" + encode(safeDetails.getItemName()));
        payloadLines.add("ITEM_TYPE|" + encode(safeDetails.getItemType()));
        payloadLines.add("ITEM_RARITY|" + encode(safeDetails.getItemRarity()));
        payloadLines.add("ANCIENT|" + safeDetails.isAncient());
        payloadLines.add("EQUIPMENT_SLOT|" + encode(safeDetails.getEquipmentSlot() == null ? "" : safeDetails.getEquipmentSlot().name()));
        payloadLines.add("ITEM_POWER|" + encodeLong(safeDetails.getItemPower()));
        payloadLines.add("WEAPON_DPS|" + encodeLong(safeDetails.getWeaponDps()));
        payloadLines.add("WEAPON_DAMAGE_MIN|" + encodeLong(safeDetails.getWeaponDamageMin()));
        payloadLines.add("WEAPON_DAMAGE_MAX|" + encodeLong(safeDetails.getWeaponDamageMax()));
        payloadLines.add("AVERAGE_WEAPON_DAMAGE|" + encodeLong(safeDetails.getAverageWeaponDamage()));
        payloadLines.add("ATTACKS_PER_SECOND|" + encodeDouble(safeDetails.getAttacksPerSecond()));
        payloadLines.add("ITEM_ARMOR|" + encodeLong(safeDetails.getItemArmor()));
        payloadLines.add("UNIQUE_EFFECT_TEXT|" + encode(safeDetails.getUniqueEffectText()));
        payloadLines.add("MYTHIC_UNIQUE|" + safeDetails.isMythicUnique());
        return encode(String.join("\n", payloadLines));
    }

    private static ItemImportDetails decodeDetails(String encodedPayload) {
        String payload = decode(encodedPayload);
        String itemName = "";
        String itemType = "";
        String itemRarity = "";
        boolean ancient = false;
        EquipmentSlot equipmentSlot = null;
        Long itemPower = null;
        Long weaponDps = null;
        Long weaponDamageMin = null;
        Long weaponDamageMax = null;
        Long averageWeaponDamage = null;
        Double attacksPerSecond = null;
        Long itemArmor = null;
        String uniqueEffectText = "";
        boolean mythicUnique = false;
        for (String line : payload.split("\\R")) {
            String[] tokens = line.split("\\|", -1);
            if (tokens.length < 2) {
                continue;
            }
            switch (tokens[0]) {
                case "ITEM_NAME" -> itemName = decode(tokens[1]);
                case "ITEM_TYPE" -> itemType = decode(tokens[1]);
                case "ITEM_RARITY" -> itemRarity = decode(tokens[1]);
                case "ANCIENT" -> ancient = Boolean.parseBoolean(tokens[1]);
                case "EQUIPMENT_SLOT" -> {
                    String rawSlot = decode(tokens[1]);
                    if (!rawSlot.isBlank()) {
                        equipmentSlot = EquipmentSlot.valueOf(rawSlot);
                    }
                }
                case "ITEM_POWER" -> itemPower = decodeLong(tokens[1]);
                case "WEAPON_DPS" -> weaponDps = decodeLong(tokens[1]);
                case "WEAPON_DAMAGE_MIN" -> weaponDamageMin = decodeLong(tokens[1]);
                case "WEAPON_DAMAGE_MAX" -> weaponDamageMax = decodeLong(tokens[1]);
                case "AVERAGE_WEAPON_DAMAGE" -> averageWeaponDamage = decodeLong(tokens[1]);
                case "ATTACKS_PER_SECOND" -> attacksPerSecond = decodeDouble(tokens[1]);
                case "ITEM_ARMOR" -> itemArmor = decodeLong(tokens[1]);
                case "UNIQUE_EFFECT_TEXT" -> uniqueEffectText = decode(tokens[1]);
                case "MYTHIC_UNIQUE" -> mythicUnique = Boolean.parseBoolean(tokens[1]);
                default -> {
                }
            }
        }
        return new ItemImportDetails(itemName, itemType, itemRarity, ancient, equipmentSlot, itemPower,
                weaponDps, weaponDamageMin, weaponDamageMax, averageWeaponDamage, attacksPerSecond, itemArmor,
                uniqueEffectText, mythicUnique);
    }

    private static String encodeLong(Long value) {
        return value == null ? "" : Long.toString(value);
    }

    private static Long decodeLong(String value) {
        return value == null || value.isBlank() ? null : Long.parseLong(value);
    }

    private static String encodeDouble(Double value) {
        return value == null ? "" : formatDouble(value);
    }

    private static Double decodeDouble(String value) {
        return value == null || value.isBlank() ? null : Double.parseDouble(value);
    }

    private static List<ImportedItemAffix> decodeAffixes(String encodedPayload) {
        String payload = decode(encodedPayload);
        List<ImportedItemAffix> affixes = new ArrayList<>();
        for (String line : payload.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String[] tokens = line.split("\\|", -1);
            if (tokens.length < 2) {
                continue;
            }
            ImportedItemAffixType type = ImportedItemAffixType.valueOf(tokens[0]);
            double value = Double.parseDouble(tokens[1]);
            if (tokens.length >= 7) {
                affixes.add(new ImportedItemAffix(
                        type,
                        value,
                        decode(tokens[2]),
                        Boolean.parseBoolean(tokens[3]),
                        Integer.parseInt(tokens[4]),
                        decode(tokens[5]),
                        ImportedItemAffixSource.valueOf(tokens[6]),
                        tokens.length >= 8 ? decode(tokens[7]) : "",
                        tokens.length >= 9 ? decodeDouble(tokens[8]) : null,
                        tokens.length >= 10 ? decodeDouble(tokens[9]) : null,
                        tokens.length >= 12 ? decodeDouble(tokens[10]) : null,
                        tokens.length >= 12 ? decode(tokens[11]) : tokens.length >= 11 ? decode(tokens[10]) : ""
                ));
            } else {
                affixes.add(new ImportedItemAffix(type, value, tokens.length >= 3 ? decode(tokens[2]) : ""));
            }
        }
        return affixes;
    }

    private static List<ItemTemperingAffix> decodeTemperingAffixes(String encodedPayload) {
        String payload = decode(encodedPayload);
        List<ItemTemperingAffix> affixes = new ArrayList<>();
        for (String line : payload.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String[] tokens = line.split("\\|", -1);
            if (tokens.length < 5) {
                continue;
            }
            affixes.add(new ItemTemperingAffix(
                    decode(tokens[0]),
                    TemperingCategory.valueOf(tokens[1]),
                    Double.parseDouble(tokens[2]),
                    decode(tokens[3]),
                    TemperingRuntimeStatus.valueOf(tokens[4]),
                    tokens.length >= 6 && Boolean.parseBoolean(tokens[5])
            ));
        }
        return affixes;
    }

    private static ItemMasterworking decodeMasterworking(String encodedPayload) {
        String payload = decode(encodedPayload);
        String[] tokens = payload.split("\\|", -1);
        if (tokens.length >= 3 && ("true".equalsIgnoreCase(tokens[0]) || "false".equalsIgnoreCase(tokens[0]))) {
            return ItemMasterworking.fromLegacy(
                    Boolean.parseBoolean(tokens[0]),
                    Integer.parseInt(tokens[1]),
                    Integer.parseInt(tokens[2])
            );
        }
        if (tokens.length < 3) {
            return ItemMasterworking.defaultState();
        }
        MasterworkedAffixSelection perfectedAffix = null;
        if (tokens.length >= 4 && !tokens[2].isBlank()) {
            try {
                perfectedAffix = new MasterworkedAffixSelection(
                        MasterworkedAffixSource.valueOf(tokens[2]),
                        decode(tokens[3])
                );
            } catch (IllegalArgumentException exception) {
                perfectedAffix = MasterworkedAffixSelection.unknown(tokens[2], tokens.length >= 4 ? decode(tokens[3]) : "");
            }
        }
        return ItemMasterworking.fromPersisted(Integer.parseInt(tokens[0]), Integer.parseInt(tokens[1]), perfectedAffix);
    }

    private static ItemTransfiguration decodeTransfiguration(String encodedPayload) {
        if (encodedPayload == null || encodedPayload.isBlank()) {
            return ItemTransfiguration.none();
        }
        String payload = decode(encodedPayload);
        if (payload.isBlank()) {
            return ItemTransfiguration.none();
        }
        String[] tokens = payload.split("\\|", -1);
        if (tokens.length < 4) {
            return ItemTransfiguration.none();
        }
        boolean transfigured = Boolean.parseBoolean(tokens[0]);
        if (!transfigured) {
            return ItemTransfiguration.none();
        }
        if (tokens.length >= 17) {
            return new ItemTransfiguration(
                    true,
                    Boolean.parseBoolean(tokens[1]),
                    HoradricTuningPrism.fromNullable(tokens[2]),
                    HoradricTransfigurationOutcome.fromNullable(tokens[3]),
                    tokens.length >= 5 ? decode(tokens[4]) : "",
                    decodeRoll(tokens[5], tokens[6], tokens[7], tokens[8]),
                    tokens.length >= 10 ? decode(tokens[9]) : "",
                    decodeRoll(tokens[10], tokens[11], tokens[12], tokens[13]),
                    tokens.length >= 15 && !tokens[14].isBlank() ? Integer.parseInt(tokens[14]) : null,
                    tokens.length >= 16 && Boolean.parseBoolean(tokens[15]),
                    tokens.length >= 17 ? decode(tokens[16]) : ""
            );
        }
        return new ItemTransfiguration(
                true,
                Boolean.parseBoolean(tokens[1]),
                HoradricTuningPrism.fromNullable(tokens[2]),
                HoradricTransfigurationOutcome.fromNullable(tokens[3]),
                tokens.length >= 5 ? decode(tokens[4]) : "",
                tokens.length >= 8 ? decodeLegacyRoll(tokens[5], tokens[6], tokens[7]) : null,
                tokens.length >= 9 ? decode(tokens[8]) : "",
                tokens.length >= 12 ? decodeLegacyRoll(tokens[9], tokens[10], tokens[11]) : null,
                tokens.length >= 13 && !tokens[12].isBlank() ? Integer.parseInt(tokens[12]) : null,
                tokens.length >= 14 && Boolean.parseBoolean(tokens[13]),
                tokens.length >= 15 ? decode(tokens[14]) : ""
        );
    }

    private static ItemSocketing decodeSocketing(String encodedPayload) {
        if (encodedPayload == null || encodedPayload.isBlank()) {
            return ItemSocketing.empty();
        }
        String payload = decode(encodedPayload);
        if (payload.isBlank()) {
            return ItemSocketing.empty();
        }
        int socketCount = 0;
        List<ItemSocket> sockets = new ArrayList<>();
        for (String line : payload.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            String[] tokens = line.split("\\|", -1);
            if (tokens.length < 2) {
                continue;
            }
            if ("COUNT".equals(tokens[0])) {
                socketCount = Integer.parseInt(tokens[1]);
                continue;
            }
            if ("SOCKET".equals(tokens[0]) && tokens.length >= 4) {
                sockets.add(new ItemSocket(
                        Integer.parseInt(tokens[1]),
                        SocketContentType.valueOf(tokens[2]),
                        decode(tokens[3])
                ));
            }
        }
        return new ItemSocketing(socketCount, sockets);
    }

    private static String encodeRollDefinition(TransfigurationAffixRoll roll) {
        return roll == null ? "" : encode(roll.getDefinitionId());
    }

    private static String encodeRollValue(TransfigurationAffixRoll roll) {
        return roll == null ? "" : formatDouble(roll.getDisplayedValue());
    }

    private static String encodeRollProvenance(TransfigurationAffixRoll roll) {
        return roll == null ? "" : roll.getValueProvenance().name();
    }

    private static String encodeRollElement(TransfigurationAffixRoll roll) {
        return roll == null ? "" : encode(roll.getElement());
    }

    private static TransfigurationAffixRoll decodeRoll(String definition, String value, String provenance, String element) {
        if (definition == null || definition.isBlank()) {
            return null;
        }
        return new TransfigurationAffixRoll(
                decode(definition),
                Double.parseDouble(value),
                TransfigurationValueProvenance.fromNullable(provenance),
                decode(element)
        );
    }

    private static TransfigurationAffixRoll decodeLegacyRoll(String definition, String value, String element) {
        if (definition == null || definition.isBlank()) {
            return null;
        }
        return new TransfigurationAffixRoll(
                decode(definition),
                Double.parseDouble(value),
                TransfigurationValueProvenance.UNKNOWN,
                decode(element)
        );
    }
}

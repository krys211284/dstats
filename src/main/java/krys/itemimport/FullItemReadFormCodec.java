package krys.itemimport;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Koduje pełny odczyt itemu do ukrytego pola formularza importu i odtwarza go po zatwierdzeniu. */
public final class FullItemReadFormCodec {
    private FullItemReadFormCodec() {
    }

    public static String encode(FullItemRead fullItemRead) {
        FullItemRead safeRead = fullItemRead == null ? FullItemRead.empty() : fullItemRead;
        List<String> payloadLines = new ArrayList<>();
        payloadLines.add("NAME|" + encodeText(safeRead.getItemName()));
        payloadLines.add("TYPE|" + encodeText(safeRead.getItemTypeLine()));
        payloadLines.add("RARITY|" + encodeText(safeRead.getRarity()));
        payloadLines.add("POWER|" + encodeText(safeRead.getItemPower()));
        payloadLines.add("BASE|" + encodeText(safeRead.getBaseItemValue()));
        payloadLines.add("DETAIL_ITEM_NAME|" + encodeText(safeRead.getDetails().getItemName()));
        payloadLines.add("DETAIL_ITEM_TYPE|" + encodeText(safeRead.getDetails().getItemType()));
        payloadLines.add("DETAIL_ITEM_RARITY|" + encodeText(safeRead.getDetails().getItemRarity()));
        payloadLines.add("DETAIL_ANCIENT|" + safeRead.getDetails().isAncient());
        payloadLines.add("DETAIL_EQUIPMENT_SLOT|" + encodeText(safeRead.getDetails().getEquipmentSlot() == null ? "" : safeRead.getDetails().getEquipmentSlot().name()));
        payloadLines.add("DETAIL_ITEM_POWER|" + encodeNullableLong(safeRead.getDetails().getItemPower()));
        payloadLines.add("DETAIL_WEAPON_DPS|" + encodeNullableLong(safeRead.getDetails().getWeaponDps()));
        payloadLines.add("DETAIL_WEAPON_DAMAGE_MIN|" + encodeNullableLong(safeRead.getDetails().getWeaponDamageMin()));
        payloadLines.add("DETAIL_WEAPON_DAMAGE_MAX|" + encodeNullableLong(safeRead.getDetails().getWeaponDamageMax()));
        payloadLines.add("DETAIL_AVERAGE_WEAPON_DAMAGE|" + encodeNullableLong(safeRead.getDetails().getAverageWeaponDamage()));
        payloadLines.add("DETAIL_ATTACKS_PER_SECOND|" + encodeNullableDouble(safeRead.getDetails().getAttacksPerSecond()));
        payloadLines.add("DETAIL_ITEM_ARMOR|" + encodeNullableLong(safeRead.getDetails().getItemArmor()));
        payloadLines.add("DETAIL_UNIQUE_EFFECT_TEXT|" + encodeText(safeRead.getDetails().getUniqueEffectText()));
        payloadLines.add("DETAIL_MYTHIC_UNIQUE|" + safeRead.getDetails().isMythicUnique());
        for (FullItemReadLine line : safeRead.getLines()) {
            payloadLines.add("LINE|" + line.getType().name() + "|" + encodeText(line.getText()));
        }
        return encodeText(String.join("\n", payloadLines));
    }

    public static FullItemRead decode(String encodedPayload) {
        if (encodedPayload == null || encodedPayload.isBlank()) {
            return FullItemRead.empty();
        }
        String payload = decodeText(encodedPayload);
        String itemName = "";
        String itemTypeLine = "";
        String rarity = "";
        String itemPower = "";
        String baseItemValue = "";
        String detailItemName = "";
        String detailItemType = "";
        String detailItemRarity = "";
        boolean detailAncient = false;
        krys.item.EquipmentSlot detailEquipmentSlot = null;
        Long detailItemPower = null;
        Long detailWeaponDps = null;
        Long detailWeaponDamageMin = null;
        Long detailWeaponDamageMax = null;
        Long detailAverageWeaponDamage = null;
        Double detailAttacksPerSecond = null;
        Long detailItemArmor = null;
        String detailUniqueEffectText = "";
        boolean detailMythicUnique = false;
        List<FullItemReadLine> lines = new ArrayList<>();
        for (String line : payload.split("\\R")) {
            String[] tokens = line.split("\\|", -1);
            if (tokens.length < 2) {
                continue;
            }
            switch (tokens[0]) {
                case "NAME" -> itemName = decodeText(tokens[1]);
                case "TYPE" -> itemTypeLine = decodeText(tokens[1]);
                case "RARITY" -> rarity = decodeText(tokens[1]);
                case "POWER" -> itemPower = decodeText(tokens[1]);
                case "BASE" -> baseItemValue = decodeText(tokens[1]);
                case "DETAIL_ITEM_NAME" -> detailItemName = decodeText(tokens[1]);
                case "DETAIL_ITEM_TYPE" -> detailItemType = decodeText(tokens[1]);
                case "DETAIL_ITEM_RARITY" -> detailItemRarity = decodeText(tokens[1]);
                case "DETAIL_ANCIENT" -> detailAncient = Boolean.parseBoolean(tokens[1]);
                case "DETAIL_EQUIPMENT_SLOT" -> {
                    String rawSlot = decodeText(tokens[1]);
                    if (!rawSlot.isBlank()) {
                        detailEquipmentSlot = krys.item.EquipmentSlot.valueOf(rawSlot);
                    }
                }
                case "DETAIL_ITEM_POWER" -> detailItemPower = decodeNullableLong(tokens[1]);
                case "DETAIL_WEAPON_DPS" -> detailWeaponDps = decodeNullableLong(tokens[1]);
                case "DETAIL_WEAPON_DAMAGE_MIN" -> detailWeaponDamageMin = decodeNullableLong(tokens[1]);
                case "DETAIL_WEAPON_DAMAGE_MAX" -> detailWeaponDamageMax = decodeNullableLong(tokens[1]);
                case "DETAIL_AVERAGE_WEAPON_DAMAGE" -> detailAverageWeaponDamage = decodeNullableLong(tokens[1]);
                case "DETAIL_ATTACKS_PER_SECOND" -> detailAttacksPerSecond = decodeNullableDouble(tokens[1]);
                case "DETAIL_ITEM_ARMOR" -> detailItemArmor = decodeNullableLong(tokens[1]);
                case "DETAIL_UNIQUE_EFFECT_TEXT" -> detailUniqueEffectText = decodeText(tokens[1]);
                case "DETAIL_MYTHIC_UNIQUE" -> detailMythicUnique = Boolean.parseBoolean(tokens[1]);
                case "LINE" -> {
                    if (tokens.length >= 3) {
                        lines.add(new FullItemReadLine(FullItemReadLineType.valueOf(tokens[1]), decodeText(tokens[2])));
                    }
                }
                default -> {
                }
            }
        }
        ItemImportDetails details = new ItemImportDetails(detailItemName, detailItemType, detailItemRarity, detailAncient,
                detailEquipmentSlot, detailItemPower, detailWeaponDps, detailWeaponDamageMin, detailWeaponDamageMax,
                detailAverageWeaponDamage, detailAttacksPerSecond, detailItemArmor, detailUniqueEffectText,
                detailMythicUnique);
        return new FullItemRead(itemName, itemTypeLine, rarity, itemPower, baseItemValue, lines, details);
    }

    private static String encodeNullableLong(Long value) {
        return value == null ? "" : Long.toString(value);
    }

    private static Long decodeNullableLong(String value) {
        return value == null || value.isBlank() ? null : Long.parseLong(value);
    }

    private static String encodeNullableDouble(Double value) {
        return value == null ? "" : Double.toString(value);
    }

    private static Double decodeNullableDouble(String value) {
        return value == null || value.isBlank() ? null : Double.parseDouble(value);
    }

    private static String encodeText(String value) {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }
}

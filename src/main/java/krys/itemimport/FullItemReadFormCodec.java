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
                case "LINE" -> {
                    if (tokens.length >= 3) {
                        lines.add(new FullItemReadLine(FullItemReadLineType.valueOf(tokens[1]), decodeText(tokens[2])));
                    }
                }
                default -> {
                }
            }
        }
        return new FullItemRead(itemName, itemTypeLine, rarity, itemPower, baseItemValue, lines);
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

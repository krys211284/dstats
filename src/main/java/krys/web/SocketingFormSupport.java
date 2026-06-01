package krys.web;

import krys.socketing.ItemSocket;
import krys.socketing.ItemSocketing;
import krys.socketing.SocketContentType;
import krys.socketing.SocketGemRuneStat;
import krys.itemimport.ImportedItemAffixType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Parser pól formularza gniazd itemu. */
final class SocketingFormSupport {
    private SocketingFormSupport() {
    }

    static ItemSocketing parse(Map<String, String> fields) {
        int socketCount = parseIntOrDefault(fields.get("socketCount"), 0);
        List<ItemSocket> sockets = new ArrayList<>();
        for (int index = 0; index < Math.min(Math.max(socketCount, 0), ItemSocketing.MAX_SOCKET_COUNT); index++) {
            SocketContentType contentType = parseContentType(fields.getOrDefault("socketContent_" + index, "EMPTY"));
            String gemId = fields.getOrDefault("socketGemId_" + index, "").trim();
            SocketGemRuneStat detectedStat = null;
            if (contentType == SocketContentType.DETECTED_STAT) {
                detectedStat = parseDetectedStat(fields, index);
            }
            sockets.add(new ItemSocket(index, contentType, gemId, detectedStat));
        }
        return new ItemSocketing(socketCount, sockets);
    }

    private static SocketGemRuneStat parseDetectedStat(Map<String, String> fields, int index) {
        String displayText = fields.getOrDefault("socketDetectedDisplayText_" + index, "").trim();
        String normalizedText = fields.getOrDefault("socketDetectedNormalizedText_" + index, "").trim();
        Double value = parseDoubleOrNull(fields.get("socketDetectedValue_" + index));
        ImportedItemAffixType matchedType = parseAffixType(fields.get("socketDetectedMatchedAffixType_" + index));
        String sourceLine = fields.getOrDefault("socketDetectedSourceLine_" + index, "").trim();
        return new SocketGemRuneStat(
                displayText,
                normalizedText,
                value,
                matchedType,
                sourceLine,
                "SOCKET_GEM_RUNE_REGION",
                SocketGemRuneStat.RUNTIME_STATUS,
                false
        );
    }

    private static ImportedItemAffixType parseAffixType(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return ImportedItemAffixType.valueOf(rawValue);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static Double parseDoubleOrNull(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(rawValue.replace(',', '.'));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static SocketContentType parseContentType(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return SocketContentType.EMPTY;
        }
        try {
            return SocketContentType.valueOf(rawValue);
        } catch (IllegalArgumentException exception) {
            return SocketContentType.EMPTY;
        }
    }

    private static int parseIntOrDefault(String rawValue, int fallback) {
        if (rawValue == null || rawValue.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}

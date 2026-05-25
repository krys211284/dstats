package krys.web;

import krys.socketing.ItemSocket;
import krys.socketing.ItemSocketing;
import krys.socketing.SocketContentType;

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
            sockets.add(new ItemSocket(index, contentType, gemId));
        }
        return new ItemSocketing(socketCount, sockets);
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

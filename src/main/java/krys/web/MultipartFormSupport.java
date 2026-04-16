package krys.web;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Minimalny parser `multipart/form-data` dla uploadu pojedynczego screena itemu w prostym SSR. */
final class MultipartFormSupport {
    private MultipartFormSupport() {
    }

    static MultipartFormData parse(HttpExchange exchange) throws IOException {
        String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("multipart/form-data")) {
            throw new IllegalArgumentException("Endpoint importu itemu oczekuje `multipart/form-data`.");
        }

        String boundary = extractBoundary(contentType);
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
        String delimiter = "--" + boundary;
        String[] rawParts = body.split(java.util.regex.Pattern.quote(delimiter));

        Map<String, MultipartFilePart> fileParts = new LinkedHashMap<>();
        for (String rawPart : rawParts) {
            String normalizedPart = normalizePart(rawPart);
            if (normalizedPart.isEmpty() || "--".equals(normalizedPart)) {
                continue;
            }

            int separatorIndex = normalizedPart.indexOf("\r\n\r\n");
            if (separatorIndex < 0) {
                continue;
            }

            String headerBlock = normalizedPart.substring(0, separatorIndex);
            String bodyBlock = normalizedPart.substring(separatorIndex + 4);
            Map<String, String> headers = parseHeaders(headerBlock);
            String disposition = headers.getOrDefault("content-disposition", "");
            String fieldName = extractDispositionValue(disposition, "name");
            String fileName = extractDispositionValue(disposition, "filename");
            if (fieldName == null || fileName == null) {
                continue;
            }

            if (bodyBlock.endsWith("\r\n")) {
                bodyBlock = bodyBlock.substring(0, bodyBlock.length() - 2);
            }

            fileParts.put(fieldName, new MultipartFilePart(
                    fileName,
                    headers.getOrDefault("content-type", "application/octet-stream"),
                    bodyBlock.getBytes(StandardCharsets.ISO_8859_1)
            ));
        }

        return new MultipartFormData(fileParts);
    }

    private static String extractBoundary(String contentType) {
        String[] segments = contentType.split(";");
        for (String segment : segments) {
            String trimmedSegment = segment.trim();
            if (trimmedSegment.startsWith("boundary=")) {
                return trimmedSegment.substring("boundary=".length());
            }
        }
        throw new IllegalArgumentException("Brak boundary w `multipart/form-data`.");
    }

    private static String normalizePart(String rawPart) {
        String normalizedPart = rawPart;
        if (normalizedPart.startsWith("\r\n")) {
            normalizedPart = normalizedPart.substring(2);
        }
        if (normalizedPart.endsWith("\r\n")) {
            normalizedPart = normalizedPart.substring(0, normalizedPart.length() - 2);
        }
        return normalizedPart;
    }

    private static Map<String, String> parseHeaders(String headerBlock) {
        Map<String, String> headers = new LinkedHashMap<>();
        String[] lines = headerBlock.split("\r\n");
        for (String line : lines) {
            int separatorIndex = line.indexOf(':');
            if (separatorIndex <= 0) {
                continue;
            }
            headers.put(
                    line.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT),
                    line.substring(separatorIndex + 1).trim()
            );
        }
        return headers;
    }

    private static String extractDispositionValue(String disposition, String key) {
        String token = key + "=\"";
        int startIndex = disposition.indexOf(token);
        if (startIndex < 0) {
            return null;
        }
        int valueStart = startIndex + token.length();
        int valueEnd = disposition.indexOf('"', valueStart);
        if (valueEnd < 0) {
            return null;
        }
        return disposition.substring(valueStart, valueEnd);
    }

    static final class MultipartFormData {
        private final Map<String, MultipartFilePart> fileParts;

        private MultipartFormData(Map<String, MultipartFilePart> fileParts) {
            this.fileParts = Map.copyOf(fileParts);
        }

        MultipartFilePart requireFile(String fieldName) {
            MultipartFilePart filePart = fileParts.get(fieldName);
            if (filePart == null || filePart.getContent().length == 0) {
                throw new IllegalArgumentException("Wgraj screenshot pojedynczego itemu.");
            }
            return filePart;
        }
    }

    static final class MultipartFilePart {
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        private MultipartFilePart(String originalFilename, String contentType, byte[] content) {
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        String getOriginalFilename() {
            return originalFilename;
        }

        String getContentType() {
            return contentType;
        }

        byte[] getContent() {
            return content.clone();
        }
    }
}

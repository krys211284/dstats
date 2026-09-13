package krys.itemimport;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Supplier;

/** Buduje bezpieczne nazwy plików debug importu itemów. */
final class ItemImportLogFileNameFactory {
    static final String UNKNOWN_ITEM_SLUG = "unknown-item";
    private static final int MAX_SLUG_LENGTH = 80;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS");

    private final Clock clock;
    private final Supplier<UUID> uuidSupplier;

    ItemImportLogFileNameFactory(Clock clock, Supplier<UUID> uuidSupplier) {
        this.clock = clock == null ? Clock.systemDefaultZone() : clock;
        this.uuidSupplier = uuidSupplier == null ? UUID::randomUUID : uuidSupplier;
    }

    static ItemImportLogFileNameFactory system() {
        return new ItemImportLogFileNameFactory(Clock.systemDefaultZone(), UUID::randomUUID);
    }

    PendingLogName pending() {
        String timestamp = TIMESTAMP_FORMAT.format(LocalDateTime.now(clock));
        String suffix = shortId(uuidSupplier.get());
        return new PendingLogName(timestamp, suffix, fileName(timestamp, "pending", suffix));
    }

    String finalFileName(String timestamp, String itemName, String suffix) {
        return fileName(timestamp, sanitizeItemSlug(itemName), suffix);
    }

    static String sanitizeItemSlug(String itemName) {
        String normalized = Normalizer.normalize(itemName == null ? "" : itemName.trim(), Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[<>:\"/\\\\|?*]", " ")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("-{2,}", "-")
                .replaceAll("^-|-$", "");
        if (normalized.isBlank()) {
            return UNKNOWN_ITEM_SLUG;
        }
        if (normalized.length() <= MAX_SLUG_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_SLUG_LENGTH)
                .replaceAll("-+$", "");
    }

    private static String fileName(String timestamp, String itemSlug, String suffix) {
        return "item-import-" + timestamp + "-" + itemSlug + "-" + suffix + ".log";
    }

    private static String shortId(UUID uuid) {
        String raw = uuid == null ? UUID.randomUUID().toString() : uuid.toString();
        return raw.replace("-", "").substring(0, Math.min(8, raw.replace("-", "").length()));
    }

    record PendingLogName(String timestamp, String suffix, String fileName) {
    }
}

package krys.itemimport;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemImportLogFileNameFactoryTest {
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-06-12T07:29:23.378Z"),
            ZoneId.of("Europe/Warsaw")
    );

    @Test
    void slugSanityzujeNazweItemu() {
        assertEquals("odlamek-verathiela", ItemImportLogFileNameFactory.sanitizeItemSlug("Odłamek Verathiela"));
        assertEquals("unknown-item", ItemImportLogFileNameFactory.sanitizeItemSlug("   "));
        assertEquals("helm-zly-name-test", ItemImportLogFileNameFactory.sanitizeItemSlug("Hełm <zły>:\"name\"/test?*"));
    }

    @Test
    void slugPrzycinaBardzoDlugaNazwe() {
        String slug = ItemImportLogFileNameFactory.sanitizeItemSlug(
                "Bardzo Długa Nazwa Przedmiotu Która Ma Zdecydowanie Więcej Niż Osiemdziesiąt Znaków I Nie Powinna Rozsadzić Nazwy Pliku"
        );

        assertTrue(slug.length() <= 80);
        assertTrue(slug.startsWith("bardzo-dluga-nazwa-przedmiotu"));
    }

    @Test
    void nazwaPlikuZawieraKontrolowanyTimestampSlugISuffix() {
        ItemImportLogFileNameFactory factory = new ItemImportLogFileNameFactory(
                FIXED_CLOCK,
                () -> UUID.fromString("ea980a72-0000-0000-0000-000000000000")
        );

        ItemImportLogFileNameFactory.PendingLogName pending = factory.pending();
        String finalName = factory.finalFileName(pending.timestamp(), "Odłamek Verathiela", pending.suffix());

        assertEquals("item-import-2026-06-12-09-29-23-378-pending-ea980a72.log", pending.fileName());
        assertEquals("item-import-2026-06-12-09-29-23-378-odlamek-verathiela-ea980a72.log", finalName);
    }

    @Test
    void dwaImportyWTymSamymCzasieDostajaRozneSuffixy() {
        Queue<UUID> uuids = new ArrayDeque<>();
        uuids.add(UUID.fromString("11111111-0000-0000-0000-000000000000"));
        uuids.add(UUID.fromString("22222222-0000-0000-0000-000000000000"));
        ItemImportLogFileNameFactory factory = new ItemImportLogFileNameFactory(FIXED_CLOCK, uuids::remove);

        String first = factory.pending().fileName();
        String second = factory.pending().fileName();

        assertNotEquals(first, second);
        assertTrue(first.contains("11111111"));
        assertTrue(second.contains("22222222"));
    }
}

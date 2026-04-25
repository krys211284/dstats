package krys.itemknowledge;

import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ValidatedImportedItem;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Testuje uczenie bazy wiedzy wyłącznie z zatwierdzonych itemów. */
class ItemKnowledgeServiceTest {
    @Test
    void shouldLearnAffixesAndAspectsFromConfirmedItemsAndResetEpoch() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-knowledge-service");
        ItemKnowledgeService service = new ItemKnowledgeService(new FileItemKnowledgeRepository(tempDirectory));

        service.learnFromConfirmedItem(shieldItem(), shieldRead());
        service.learnFromConfirmedItem(shieldItem(), shieldRead());

        ItemKnowledgeSnapshot snapshot = service.getSnapshot();

        assertEquals("Epoka wiedzy 1", snapshot.getActiveEpoch().label());
        assertEquals(1, snapshot.getEntryCount());
        assertEquals(2, snapshot.getItemObservationCount());
        ItemKnowledgeEntry entry = snapshot.getEntries().getFirst();
        assertEquals(EquipmentSlot.OFF_HAND, entry.getKey().slot());
        assertEquals("Tarcza", entry.getKey().itemType());
        assertEquals(2, entry.getAffixTypeCounts().get(ImportedItemAffixType.STRENGTH));
        assertEquals(2, entry.getAffixTypeCounts().get(ImportedItemAffixType.THORNS));
        assertEquals(2, entry.getAffixTypeCounts().get(ImportedItemAffixType.LUCKY_HIT_CHANCE));
        assertEquals(2, entry.getAspectCounts().get("Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%"));

        ItemKnowledgeSnapshot resetSnapshot = service.resetKnowledge("Sezon testowy");

        assertEquals(2, resetSnapshot.getActiveEpoch().sequence());
        assertEquals("Sezon testowy", resetSnapshot.getActiveEpoch().label());
        assertEquals(0, resetSnapshot.getEntryCount());
        assertEquals(0, resetSnapshot.getItemObservationCount());
    }

    private static ValidatedImportedItem shieldItem() {
        return new ValidatedImportedItem(
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                114.0d,
                0.0d,
                494.0d,
                20.0d,
                0.0d,
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 114.0d, "+114 siły [107 - 121]"),
                        new ImportedItemAffix(ImportedItemAffixType.THORNS, 494.0d, "+494 cierni [473 - 506]"),
                        new ImportedItemAffix(ImportedItemAffixType.LUCKY_HIT_CHANCE, 7.0d, "+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%")
                )
        );
    }

    private static FullItemRead shieldRead() {
        return new FullItemRead(
                "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU",
                "Starożytna legendarna tarcza",
                "Starożytna legendarna",
                "Moc przedmiotu: 800",
                "Pancerz: 1 131 pkt.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+114 siły [107 - 121]"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+494 cierni [473 - 506]"),
                        new FullItemReadLine(FullItemReadLineType.ASPECT, "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%")
                )
        );
    }
}

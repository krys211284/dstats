package krys.itemknowledge;

import krys.item.EquipmentSlot;
import krys.itemimport.ImportedItemAffixType;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Testuje trwałość osobnego pliku bazy wiedzy itemów. */
class FileItemKnowledgeRepositoryTest {
    @Test
    void shouldPersistKnowledgeSnapshotSeparatelyFromItemLibrary() throws Exception {
        Path tempDirectory = Files.createTempDirectory("item-knowledge-repo");
        FileItemKnowledgeRepository repository = new FileItemKnowledgeRepository(tempDirectory);
        ItemKnowledgeEntry entry = new ItemKnowledgeEntry(
                new ItemKnowledgeKey(EquipmentSlot.OFF_HAND, "Tarcza"),
                3,
                Map.of(ImportedItemAffixType.STRENGTH, 2, ImportedItemAffixType.THORNS, 3),
                Map.of("Aspekt testowy", 2)
        );

        repository.save(new ItemKnowledgeSnapshot(new ItemKnowledgeEpoch(4, "Sezon repo"), List.of(entry)));

        ItemKnowledgeSnapshot reloaded = new FileItemKnowledgeRepository(tempDirectory).load();

        assertEquals(4, reloaded.getActiveEpoch().sequence());
        assertEquals("Sezon repo", reloaded.getActiveEpoch().label());
        assertEquals(1, reloaded.getEntryCount());
        assertEquals("Tarcza", reloaded.getEntries().getFirst().getKey().itemType());
        assertEquals(2, reloaded.getEntries().getFirst().getAffixTypeCounts().get(ImportedItemAffixType.STRENGTH));
        assertEquals(3, reloaded.getEntries().getFirst().getAffixTypeCounts().get(ImportedItemAffixType.THORNS));
        assertEquals(2, reloaded.getEntries().getFirst().getAspectCounts().get("Aspekt testowy"));
    }
}

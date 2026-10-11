package krys.itemlibrary;

import krys.item.*;
import krys.itemimport.*;
import krys.web.HeroItemSelection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ItemTypePersistenceAndEquipmentTest {
    @TempDir Path directory;

    @Test
    void newTechnicalTypeAndUnknownLegacyTextShouldRoundTrip() {
        var library = library();
        var scythe = save(library, "SCYTHE_TWO_HANDED", EquipmentSlot.MAIN_HAND);
        var unknown = save(library, "Nieznany dawny typ", EquipmentSlot.OFF_HAND);
        assertEquals(ItemType.SCYTHE_TWO_HANDED, library().requireItem(scythe.getItemId()).getDetails().getResolvedItemType());
        assertEquals("Kosa dwuręczna", library().requireItem(scythe.getItemId()).getItemType());
        assertEquals(ItemType.UNKNOWN, library().requireItem(unknown.getItemId()).getDetails().getResolvedItemType());
        assertEquals("Nieznany dawny typ", library().requireItem(unknown.getItemId()).getItemType());
    }

    @Test
    void oldPayloadWithoutTechnicalIdShouldMigrateKnownNamesWithoutRewritingOnRead() throws Exception {
        for (ItemType type : List.of(ItemType.SHIELD, ItemType.HELMET, ItemType.BOOTS, ItemType.SWORD)) { save(library(), type.getDisplayName(), type.getEquipmentSlot()); }
        Path file = directory.resolve("saved-items.db");
        var lines = new ArrayList<String>();
        for (String line : Files.readAllLines(file)) {
            String[] fields = line.split("\\|", -1);
            String payload = new String(Base64.getUrlDecoder().decode(fields[14]), StandardCharsets.UTF_8);
            payload = payload.replaceAll("(?m)^ITEM_TYPE_ID\\|[^\\r\\n]*\\R?", "");
            fields[14] = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
            lines.add(String.join("|", fields));
        }
        Files.write(file, lines);
        String before = Files.readString(file);
        assertEquals(Set.of(ItemType.SHIELD, ItemType.HELMET, ItemType.BOOTS, ItemType.SWORD),
                new HashSet<>(library().getSavedItems().stream().map(item -> item.getDetails().getResolvedItemType()).toList()));
        assertEquals(before, Files.readString(file));
    }

    @Test
    void twoHandedAndOffHandShouldBeRejectedByEveryContributionEntryPoint() {
        var library = library();
        var scythe = save(library, "SCYTHE_TWO_HANDED", EquipmentSlot.MAIN_HAND);
        var shield = save(library, "SHIELD", EquipmentSlot.OFF_HAND);
        var invalid = new HeroItemSelection(Map.of(HeroEquipmentSlot.MAIN_HAND, scythe.getItemId(), HeroEquipmentSlot.OFF_HAND, shield.getItemId()));
        assertThrows(IllegalArgumentException.class, () -> library.validateEquipmentSelection(invalid));
        assertThrows(IllegalArgumentException.class, () -> library.resolveActiveItemsContribution(invalid));
        assertThrows(IllegalArgumentException.class, () -> library.resolveActiveHeroItemStats(invalid));
        assertEquals(2, library.getSavedItems().size());
        assertTrue(library.generateSearchCombinations().stream().noneMatch(combo -> combo.getSelectedItems().size() == 2));
    }

    @Test
    void oneHandedWeaponAndShieldShouldRemainLegal() {
        var library = library();
        var sword = save(library, "Miecz", EquipmentSlot.MAIN_HAND);
        var shield = save(library, "Tarcza", EquipmentSlot.OFF_HAND);
        assertDoesNotThrow(() -> library.validateEquipmentSelection(new HeroItemSelection(Map.of(HeroEquipmentSlot.MAIN_HAND, sword.getItemId(), HeroEquipmentSlot.OFF_HAND, shield.getItemId()))));
    }

    private ItemLibraryService library() { return new ItemLibraryService(new FileItemLibraryRepository(directory)); }
    public static SavedImportedItem save(ItemLibraryService library, String type, EquipmentSlot slot) {
        return library.saveImportedItem(new ValidatedImportedItem("fixture.png", slot, 0, 0, 0, 0, 0, 0, List.of(), "",
                new ItemImportDetails("fixture", type, "UNIQUE", true, slot, 900L, null, null, null, null, null, "")));
    }
}

package krys.itemimport;

import krys.item.*;
import krys.itemimport.image.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class ItemTypeImportTest {
    @Test
    void firstRealScytheClassificationShouldKeepAllConfirmedIdentityFields() {
        var result = new Part1ReadParser().parse(List.of(
                new Part1RoiOcrResult(Part1RoiType.NAME_AND_STARS, "OLŚNIENIE", 1L, "fixture", List.of()),
                new Part1RoiOcrResult(Part1RoiType.CLASSIFICATION, "Starożytna unikatowa kosa dwuręczna", 1L, "fixture", List.of()),
                new Part1RoiOcrResult(Part1RoiType.ITEM_POWER, "Moc przedmiotu: 900", 1L, "fixture", List.of())));
        assertEquals(ItemType.SCYTHE_TWO_HANDED, result.getResolvedItemType());
        assertEquals("Kosa dwuręczna", result.getItemType());
        assertEquals(EquipmentSlot.MAIN_HAND, result.getEquipmentSlot().orElseThrow());
        assertEquals(ItemHandedness.TWO_HANDED, result.getResolvedItemType().getHandedness());
        assertEquals("UNIQUE", result.getItemRarity());
        assertTrue(result.getAncient().orElseThrow());
        assertEquals(900, result.getItemPower().orElseThrow());
        assertTrue(result.getParseFailures().isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"SCYTHE_TWO_HANDED,HELMET", "SCYTHE_TWO_HANDED,OFF_HAND", "SHIELD,MAIN_HAND", "HELMET,MAIN_HAND"})
    void incompatibleTypeAndSlotShouldBeRejectedWithoutRepair(String type, String slot) {
        var form = form(type, slot);
        var result = new ItemImportFormMapper().map(form);
        assertNull(result.getItem());
        assertTrue(result.getErrors().stream().anyMatch(error -> error.contains("wymaga slotu")));
        assertEquals(slot, form.getSlot());
        assertEquals(ItemType.valueOf(type), form.getDetails().getResolvedItemType());
    }

    @Test
    void unknownTypeAndSlotShouldRequireManualConfirmation() {
        var parsed = new Part1ReadParser().parse(List.of(new Part1RoiOcrResult(Part1RoiType.CLASSIFICATION,
                "Starożytny unikatowy nieznany przedmiot", 1L, "fixture", List.of())));
        assertEquals(ItemType.UNKNOWN, parsed.getResolvedItemType());
        assertTrue(parsed.getEquipmentSlot().isEmpty());
        var result = new ItemImportFormMapper().map(form("Nieznany typ", ""));
        assertNull(result.getItem());
        assertTrue(result.getErrors().contains("Slot itemu jest wymagany."));
        assertTrue(result.getErrors().contains("Wybierz rozpoznany typ itemu z katalogu."));
        assertNull(new ItemImportFormMapper().map(form("Nieznany typ", "HELMET")).getItem());
    }

    @Test
    void confirmedLegacyTypesShouldStillSave() {
        for (ItemType type : ItemTypeRegistry.selectableTypes()) {
            var result = new ItemImportFormMapper().map(form(type.getDisplayName(), type.getEquipmentSlot().name()));
            assertTrue(result.getErrors().isEmpty(), result.getErrors().toString());
            assertEquals(type, result.getItem().getDetails().getResolvedItemType());
        }
    }

    public static ItemImportEditableForm form(String type, String slot) {
        EquipmentSlot equipmentSlot = slot.isBlank() ? null : EquipmentSlot.valueOf(slot);
        var details = new ItemImportDetails("OLŚNIENIE", type, "UNIQUE", true, equipmentSlot, 900L,
                null, null, null, null, null, "");
        return new ItemImportEditableForm("fixture.png", slot, "0", "0", "0", "0", "0", "0", FullItemRead.empty(), List.of(), "", details);
    }
}

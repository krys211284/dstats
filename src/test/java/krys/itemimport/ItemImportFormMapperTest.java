package krys.itemimport;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testy walidacji ręcznie poprawionego itemu przed zatwierdzeniem do modelu aplikacji. */
class ItemImportFormMapperTest {
    @Test
    void shouldValidateManuallyCorrectedMainHandItem() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "topor.png",
                "MAIN_HAND",
                "444",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 70.0d, "+70 siły"))
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(EquipmentSlot.MAIN_HAND, result.getItem().getSlot());
        assertEquals(444L, result.getItem().getWeaponDamage());
        assertEquals(70.0d, result.getItem().getStrength());
    }

    @Test
    void shouldRejectWeaponDamageOutsideMainHandSlot() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "150",
                "12",
                "0",
                "30",
                "18",
                "25"
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertNull(result.getItem());
        assertEquals(1, result.getErrors().size());
        assertEquals("Weapon damage można ustawić wyłącznie dla slotu MAIN_HAND.", result.getErrors().getFirst());
    }

    @Test
    void shouldProjectRuntimeStatsFromEditableAffixList() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "20.0",
                "0",
                new FullItemRead(
                        "Tarcza",
                        "Tarcza",
                        "Legendarny",
                        "Moc przedmiotu: 800",
                        "Pancerz: 1 131 pkt.",
                        List.of(new FullItemReadLine(FullItemReadLineType.AFFIX, "20,0% szansy na blok [20,01]%"))
                ),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 114.0d, "+114 siły [107 - 121]"),
                        new ImportedItemAffix(ImportedItemAffixType.THORNS, 494.0d, "+494 cierni [473 - 506]"),
                        new ImportedItemAffix(ImportedItemAffixType.LUCKY_HIT_CHANCE, 7.0d, "+7,0% szansy na szczęśliwy traf [7,0 - 8,0]%"),
                        new ImportedItemAffix(ImportedItemAffixType.COOLDOWN_REDUCTION, 13.2d, "13,2% redukcji czasu odnowienia")
                )
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertTrue(result.getErrors().isEmpty());
        assertEquals(EquipmentSlot.OFF_HAND, result.getItem().getSlot());
        assertEquals(114.0d, result.getItem().getStrength());
        assertEquals(494.0d, result.getItem().getThorns());
        assertEquals(20.0d, result.getItem().getBlockChance());
        assertEquals(4, result.getItem().getAffixes().size());
    }

    @Test
    void shouldAcceptAspectAllowedForImportedItemSlot() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(),
                "inner-calm",
                ItemImportFieldConfidence.HIGH,
                "inner-calm"
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertTrue(result.getErrors().isEmpty(), () -> String.join(", ", result.getErrors()));
        assertEquals("inner-calm", result.getItem().getSelectedAspectId());
    }

    @Test
    void shouldRejectAspectOutsideImportedItemSlot() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "buty.png",
                "BOOTS",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(),
                "inner-calm",
                ItemImportFieldConfidence.HIGH,
                "inner-calm"
        );

        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);

        assertNull(result.getItem());
        assertTrue(result.getErrors().contains("Wybrany aspekt nie pasuje do slotu itemu."));
    }
}

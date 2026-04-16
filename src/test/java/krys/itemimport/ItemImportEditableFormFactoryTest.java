package krys.itemimport;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Test mapowania wstępnie rozpoznanych pól do edytowalnego formularza potwierdzenia. */
class ItemImportEditableFormFactoryTest {
    @Test
    void shouldMapSuggestedFieldsToEditableForm() {
        ItemImageImportCandidateParseResult parseResult = new ItemImageImportCandidateParseResult(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 1200, 800),
                new ItemImportFieldCandidate<>("MAIN_HAND", EquipmentSlot.MAIN_HAND, ItemImportFieldConfidence.MEDIUM, "slot"),
                new ItemImportFieldCandidate<>("321", 321L, ItemImportFieldConfidence.HIGH, "weapon"),
                new ItemImportFieldCandidate<>("+55 Strength", 55.0d, ItemImportFieldConfidence.HIGH, "str"),
                new ItemImportFieldCandidate<>("+12 Intelligence", 12.0d, ItemImportFieldConfidence.MEDIUM, "int"),
                new ItemImportFieldCandidate<>("+90 Thorns", 90.0d, ItemImportFieldConfidence.HIGH, "thorns"),
                new ItemImportFieldCandidate<>("+18% Block Chance", 18.0d, ItemImportFieldConfidence.LOW, "block"),
                new ItemImportFieldCandidate<>("+25% Retribution Chance", 25.0d, ItemImportFieldConfidence.MEDIUM, "retribution"),
                "Import wspomagany"
        );

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(parseResult);

        assertEquals("miecz.png", form.getSourceImageName());
        assertEquals("MAIN_HAND", form.getSlot());
        assertEquals("321", form.getWeaponDamage());
        assertEquals("55", form.getStrength());
        assertEquals("12", form.getIntelligence());
        assertEquals("90", form.getThorns());
        assertEquals("18", form.getBlockChance());
        assertEquals("25", form.getRetributionChance());
    }
}

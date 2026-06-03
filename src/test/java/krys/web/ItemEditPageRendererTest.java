package krys.web;

import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportFieldConfidence;
import krys.itemlibrary.ItemLibraryFilter;
import krys.itemlibrary.SavedImportedItem;
import krys.masterworking.ItemMasterworking;
import krys.socketing.ItemSocket;
import krys.socketing.ItemSocketing;
import krys.socketing.SocketGemRuneStat;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje formularz edycji itemu w bibliotece. */
class ItemEditPageRendererTest {
    @Test
    void shouldRenderNewOffensiveAspectsInEditSelect() {
        SavedImportedItem item = new SavedImportedItem(
                1L,
                "Tarcza",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        );
        ItemImportEditableForm form = new ItemImportEditableForm(
                item.getSourceImageName(),
                item.getSlot().name(),
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "sanctified_punishment_aspect"
        );

        String html = new ItemEditPageRenderer().render(new ItemEditPageModel(
                item,
                form,
                List.of(),
                List.of(),
                ItemLibraryFilter.empty()
        ));

        assertTrue(html.contains("Aspekt Uświęconej Kary"));
        assertTrue(html.contains("selected"));
        assertTrue(html.contains("Obrażenia Świętości i Ognia są zwiększone o 60,0%[x] [40,0 - 60,0]%."));
    }

    @Test
    void shouldRenderSingleMasterworkingQualityFieldInEditForm() {
        SavedImportedItem item = new SavedImportedItem(
                1L,
                "Tarcza",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        );
        ItemImportEditableForm form = new ItemImportEditableForm(
                item.getSourceImageName(),
                item.getSlot().name(),
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                new ItemImportDetails("Tarcza", "Tarcza", "LEGENDARY", true, EquipmentSlot.OFF_HAND,
                        900L, null, null, null, null, null, 1202L, ""),
                List.of(),
                ItemMasterworking.quality(25)
        );

        String html = new ItemEditPageRenderer().render(new ItemEditPageModel(
                item,
                form,
                List.of(),
                List.of(),
                ItemLibraryFilter.empty()
        ));

        assertTrue(html.contains("name=\"masterworkingQualityCurrent\""));
        assertTrue(html.contains("<option value=\"25\" selected>25/25</option>"));
        assertFalse(html.contains("Jakość maksymalna"));
        assertFalse(html.contains("name=\"masterworkingQualityMax\""));
    }

    @Test
    void shouldKeepWeaponFieldsEditableForSavedWeaponItem() {
        SavedImportedItem item = new SavedImportedItem(
                1L,
                "Odłamek Verathiela",
                "verathiel.png",
                EquipmentSlot.MAIN_HAND,
                1704L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        );
        ItemImportEditableForm form = new ItemImportEditableForm(
                item.getSourceImageName(),
                item.getSlot().name(),
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "verathiel_shard",
                new ItemImportDetails("Odłamek Verathiela", "Miecz", "UNIQUE", true, EquipmentSlot.MAIN_HAND,
                        900L, 1874L, 1390L, 2018L, 1704L, 1.10d,
                        "Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100]%, ale dodatkowo zużywają 25 pkt. podstawowego zasobu.")
        );

        String html = new ItemEditPageRenderer().render(new ItemEditPageModel(
                item,
                form,
                List.of(),
                List.of(),
                ItemLibraryFilter.empty()
        ));

        String weaponFields = fieldSetByLegend(html, "Dane broni");
        assertTrue(weaponFields.contains("name=\"weaponDps\" value=\"1874\""));
        assertTrue(weaponFields.contains("name=\"weaponDamageMin\" value=\"1390\""));
        assertTrue(weaponFields.contains("name=\"weaponDamageMax\" value=\"2018\""));
        assertTrue(weaponFields.contains("name=\"averageWeaponDamage\" value=\"1704\""));
        assertTrue(weaponFields.contains("name=\"attacksPerSecond\" value=\"1.10\""));
    }

    @Test
    void shouldRenderDetectedSocketGemRuneStatsInEditForm() {
        SavedImportedItem item = new SavedImportedItem(
                1L,
                "Dziedzic Zatracenia",
                "helm.png",
                EquipmentSlot.HELMET,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                FullItemRead.empty(),
                List.of(),
                "",
                new ItemImportDetails("Dziedzic Zatracenia", "Hełm", "UNIQUE", true, EquipmentSlot.HELMET,
                        900L, null, null, null, null, null, 2004L, "", true),
                List.of(),
                ItemMasterworking.defaultState(),
                krys.transfiguration.ItemTransfiguration.none(),
                new ItemSocketing(2, List.of(
                        ItemSocket.detectedStat(0, SocketGemRuneStat.fromDetectedLine("+150 siły")),
                        ItemSocket.detectedStat(1, SocketGemRuneStat.fromDetectedLine("+120 siły"))
                ))
        );
        ItemImportEditableForm form = new ItemImportEditableForm(
                item.getSourceImageName(),
                item.getSlot().name(),
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                item.getDetails(),
                List.of(),
                ItemMasterworking.defaultState(),
                krys.transfiguration.ItemTransfiguration.none(),
                item.getSocketing()
        );

        String html = new ItemEditPageRenderer().render(new ItemEditPageModel(
                item,
                form,
                List.of(),
                List.of(),
                ItemLibraryFilter.empty()
        ));

        assertTrue(html.contains("Liczba gniazd: 2"));
        assertTrue(html.contains("+150 siły"));
        assertTrue(html.contains("+120 siły"));
        assertTrue(html.contains("Runtime nieaktywny"));
        assertTrue(html.contains("socketing-detected-row"));
        assertTrue(html.contains("socketing-detected-card"));
        assertTrue(html.contains("Wykryty stat gema/runy: +150 siły"));
        assertTrue(html.contains("Wykryty stat gema/runy: +120 siły"));
    }

    @Test
    void shouldRenderDetectedSocketGemRuneStatsAsReadableCardsInEditForm() {
        String html = renderDetectedSocketEditPage();
        String socketSection = fieldSetByLegend(html, "Gniazda");

        assertTrue(socketSection.contains("Liczba gniazd: 2"));
        assertTrue(socketSection.contains("Gniazdo 1"));
        assertTrue(socketSection.contains("Gniazdo 2"));
        assertTrue(socketSection.contains("socketing-detected-row"));
        assertTrue(socketSection.contains("socketing-detected-card"));
        assertTrue(socketSection.contains("Wykryty stat gema/runy: +150 siły"));
        assertTrue(socketSection.contains("Wykryty stat gema/runy: +120 siły"));
        assertTrue(socketSection.contains("Runtime nieaktywny"));
    }

    @Test
    void shouldHideGemSelectColumnForDetectedSocketStatsInEditForm() {
        String html = renderDetectedSocketEditPage();
        String firstDetectedRow = detectedSocketRow(html, 0);

        assertTrue(firstDetectedRow.contains("name=\"socketContent_0\""));
        assertTrue(firstDetectedRow.contains("value=\"DETECTED_STAT\" selected"));
        assertTrue(firstDetectedRow.contains("name=\"socketDetectedDisplayText_0\" value=\"+150 siły\""));
        assertTrue(firstDetectedRow.contains("name=\"socketDetectedNormalizedText_0\""));
        assertTrue(firstDetectedRow.contains("name=\"socketDetectedValue_0\" value=\"150\""));
        assertTrue(firstDetectedRow.contains("class=\"socketing-gem-field\" hidden data-socket-gem-field"));
        assertTrue(firstDetectedRow.contains("name=\"socketGemId_0\" data-socket-gem disabled"));
        assertTrue(firstDetectedRow.contains("Wykryty stat gema/runy: +150 siły"));
    }

    @Test
    void shouldRenderEditFormActionsOutsideSocketingFieldset() {
        String html = renderDetectedSocketEditPage();
        String socketSection = fieldSetByLegend(html, "Gniazda");
        int socketLegend = html.indexOf("<legend>Gniazda</legend>");
        int socketEnd = html.indexOf("</fieldset>", socketLegend) + "</fieldset>".length();
        int saveButton = html.indexOf("Zapisz zmiany");

        assertTrue(socketEnd > "</fieldset>".length());
        assertTrue(socketEnd < saveButton);
        assertFalse(socketSection.contains("Zapisz zmiany"));
        assertFalse(socketSection.contains("item-edit-actions"));
        assertTrue(html.contains("<div class=\"form-actions item-edit-actions\">"));
    }

    @Test
    void shouldRenderSaveButtonWithNormalActionMarkup() {
        String html = renderDetectedSocketEditPage();
        String saveButton = openingTagBeforeText(html, "Zapisz zmiany", "<button");

        assertTrue(saveButton.contains("class=\"item-edit-save-button\""));
        assertFalse(saveButton.contains("style="));
        assertFalse(html.contains("button, .link-button, .secondary-link"));
        assertFalse(html.contains("button { display: block"));
    }

    @Test
    void shouldRenderMythicEditAffixWithoutReferenceWithoutRemasteredPreview() {
        SavedImportedItem item = mythicHelmetItem();
        ItemImportEditableForm form = mythicHelmetForm(item, List.of(
                new ImportedItemAffix(ImportedItemAffixType.LUCKY_HIT_CHANCE, 25.0d, "%", false, 0,
                        "+25,0% szansy na szczęśliwy traf", ImportedItemAffixSource.OCR)
        ));

        String html = renderEditPage(item, form);
        String row = affixRowByOriginalType(html, "LUCKY_HIT_CHANCE");

        assertTrue(row.contains("name=\"affixValue_0\" value=\"25\""));
        assertTrue(row.contains("Brak zakresu"));
        assertFalse(row.contains("Wartość bazowa"));
        assertFalse(html.contains("31,3"));
        assertFalse(html.contains("31.3"));
    }

    @Test
    void shouldRenderMythicEditAffixWithReferenceWithoutRemasteredPreview() {
        SavedImportedItem item = mythicHelmetItem();
        ItemImportEditableForm form = mythicHelmetForm(item, List.of(
                new ImportedItemAffix(ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 15.0d, "%", false, 0,
                        "+15,0% szansy na trafienie krytyczne [12,0]%", ImportedItemAffixSource.OCR,
                        "critical_strike_chance", null, null, 12.0d, "")
        ));

        String html = renderEditPage(item, form);
        String row = affixRowByOriginalType(html, "CRITICAL_STRIKE_CHANCE");

        assertTrue(row.contains("name=\"affixValue_0\" value=\"15\""));
        assertTrue(row.contains("Wartość bazowa: 12"));
        assertFalse(html.contains("18,8"));
        assertFalse(html.contains("18.8"));
    }

    private static SavedImportedItem mythicHelmetItem() {
        return new SavedImportedItem(
                1L,
                "Generyczny Hełm Mityczny",
                "helm.png",
                EquipmentSlot.HELMET,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d
        );
    }

    private static ItemImportEditableForm mythicHelmetForm(SavedImportedItem item, List<ImportedItemAffix> affixes) {
        return new ItemImportEditableForm(
                item.getSourceImageName(),
                item.getSlot().name(),
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                affixes,
                "heir_of_perdition",
                ItemImportFieldConfidence.UNKNOWN,
                "heir_of_perdition",
                new ItemImportDetails("Generyczny Hełm Mityczny", "Hełm", "UNIQUE", true, EquipmentSlot.HELMET,
                        900L, null, null, null, null, null, 2004L, "Opisowy efekt 80%[x].", true),
                List.of(),
                ItemMasterworking.quality(25)
        );
    }

    private static String renderEditPage(SavedImportedItem item, ItemImportEditableForm form) {
        return new ItemEditPageRenderer().render(new ItemEditPageModel(
                item,
                form,
                List.of(),
                List.of(),
                ItemLibraryFilter.empty()
        ));
    }

    private static String renderDetectedSocketEditPage() {
        SavedImportedItem item = new SavedImportedItem(
                1L,
                "Dziedzic Zatracenia",
                "helm.png",
                EquipmentSlot.HELMET,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                FullItemRead.empty(),
                List.of(),
                "",
                new ItemImportDetails("Dziedzic Zatracenia", "Hełm", "UNIQUE", true, EquipmentSlot.HELMET,
                        900L, null, null, null, null, null, 2004L, "", true),
                List.of(),
                ItemMasterworking.defaultState(),
                krys.transfiguration.ItemTransfiguration.none(),
                new ItemSocketing(2, List.of(
                        ItemSocket.detectedStat(0, SocketGemRuneStat.fromDetectedLine("+150 siły")),
                        ItemSocket.detectedStat(1, SocketGemRuneStat.fromDetectedLine("+120 siły"))
                ))
        );
        ItemImportEditableForm form = new ItemImportEditableForm(
                item.getSourceImageName(),
                item.getSlot().name(),
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                FullItemRead.empty(),
                List.of(),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                item.getDetails(),
                List.of(),
                ItemMasterworking.defaultState(),
                krys.transfiguration.ItemTransfiguration.none(),
                item.getSocketing()
        );
        return renderEditPage(item, form);
    }

    private static String fieldSetByLegend(String html, String legend) {
        int legendIndex = html.indexOf("<legend>" + legend + "</legend>");
        if (legendIndex < 0) {
            throw new AssertionError("Brak fieldsetu: " + legend);
        }
        int start = html.lastIndexOf("<fieldset", legendIndex);
        int end = html.indexOf("</fieldset>", legendIndex);
        if (start < 0 || end < 0) {
            throw new AssertionError("Nie udało się wyciąć fieldsetu: " + legend);
        }
        return html.substring(start, end + "</fieldset>".length());
    }

    private static String affixRowByOriginalType(String html, String originalType) {
        String marker = "name=\"affixOriginalType_";
        int sourceIndex = html.indexOf(marker);
        while (sourceIndex >= 0) {
            int inputEnd = html.indexOf(">", sourceIndex);
            if (inputEnd >= 0 && html.substring(sourceIndex, inputEnd).contains("value=\"" + originalType + "\"")) {
                int rowStart = html.lastIndexOf("<tr>", sourceIndex);
                int rowEnd = html.indexOf("</tr>", sourceIndex);
                if (rowStart >= 0 && rowEnd >= 0) {
                    return html.substring(rowStart, rowEnd + "</tr>".length());
                }
            }
            sourceIndex = html.indexOf(marker, sourceIndex + marker.length());
        }
        throw new AssertionError("Brak wiersza affixu: " + originalType);
    }

    private static String detectedSocketRow(String html, int index) {
        String marker = "data-socket-index=\"" + index + "\"";
        int markerIndex = html.indexOf(marker);
        if (markerIndex < 0) {
            throw new AssertionError("Brak wiersza gniazda: " + index);
        }
        int start = html.lastIndexOf("<div class=\"socketing-detected-row\"", markerIndex);
        int nextRow = html.indexOf("<div class=\"socketing-detected-row\"", start + 1);
        int fieldsetEnd = html.indexOf("</fieldset>", start);
        if (start < 0 || fieldsetEnd < 0) {
            throw new AssertionError("Nie udało się wyciąć wiersza gniazda: " + index);
        }
        int end = nextRow >= 0 && nextRow < fieldsetEnd ? nextRow : fieldsetEnd;
        return html.substring(start, end);
    }

    private static String openingTagBeforeText(String html, String text, String tagStart) {
        int textIndex = html.indexOf(text);
        if (textIndex < 0) {
            throw new AssertionError("Brak tekstu: " + text);
        }
        int start = html.lastIndexOf(tagStart, textIndex);
        int end = html.indexOf(">", start);
        if (start < 0 || end < 0) {
            throw new AssertionError("Nie udało się wyciąć tagu dla: " + text);
        }
        return html.substring(start, end + 1);
    }
}

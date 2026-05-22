package krys.web;

import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.hero.HeroClass;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.itemlibrary.SavedImportedItem;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingRuntimeStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje semantyczny render zapisanych itemów w bibliotece. */
class ItemLibraryPageRendererTest {
    @Test
    void shouldRenderShieldItemInSemanticSectionsWithoutFlatOcrDump() {
        SavedImportedItem shield = new SavedImportedItem(
                1L,
                "Ręka dodatkowa / tarcza.png",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                114.0d,
                0.0d,
                494.0d,
                20.0d,
                0.0d,
                new FullItemRead(
                        "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU",
                        "Starożytna legendarna tarcza",
                        "Starożytna legendarna",
                        "Moc przedmiotu: 800",
                        "1 131 pkt. pancerza",
                        List.of(
                                new FullItemReadLine(FullItemReadLineType.ITEM_NAME, "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU"),
                                new FullItemReadLine(FullItemReadLineType.TYPE_OR_SLOT, "Starożytna legendarna tarcza"),
                                new FullItemReadLine(FullItemReadLineType.ITEM_POWER, "Moc przedmiotu: 800"),
                                new FullItemReadLine(FullItemReadLineType.BASE_STAT, "1 131 pkt. pancerza"),
                                new FullItemReadLine(FullItemReadLineType.BASE_STAT, "800 1 131 pkt. pancerza"),
                                new FullItemReadLine(FullItemReadLineType.IMPLICIT, "45% redukcji blokowanych obrażeń [45]%"),
                                new FullItemReadLine(FullItemReadLineType.IMPLICIT, "20,0% szansy na blok [20,0]%"),
                                new FullItemReadLine(FullItemReadLineType.IMPLICIT, "+100% obrażeń od broni w głównej ręce [100]%"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "+494 cierni [473 - 506]"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "+7,0% szansy na szczęśliwy traf [7,0"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "13,2% redukcji czasu odnowienia"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "+114 siły [107 - 121]"),
                                new FullItemReadLine(FullItemReadLineType.ASPECT, "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%"),
                                new FullItemReadLine(FullItemReadLineType.ASPECT, "Zadajesz obrażenia zwiększone o [5,0 - Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek."),
                                new FullItemReadLine(FullItemReadLineType.ASPECT, "Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek."),
                                new FullItemReadLine(FullItemReadLineType.SOCKET, "Puste gniazdo")
                        )
                ),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.THORNS, 494.0d, "", false, 0, "+494 cierni [473 - 506]", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.LUCKY_HIT_CHANCE, 7.0d, "%", false, 1, "+7,0% szansy na szczęśliwy traf [7,0", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.COOLDOWN_REDUCTION, 13.2d, "%", true, 2, "13,2% redukcji czasu odnowienia", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 114.0d, "", false, 3, "+114 siły [107 - 121]", ImportedItemAffixSource.OCR)
                ),
                "inner-calm"
        );

        String html = render(List.of(shield));

        assertTrue(html.contains("<table class=\"data-table item-index-table\">"));
        assertTrue(html.contains("<th>Item</th>"));
        assertTrue(html.contains("<th>Slot / typ</th>"));
        assertTrue(html.contains("<th>Aspekt</th>"));
        assertTrue(html.contains("<th>Affixy</th>"));
        assertTrue(html.contains("<th>Akcje</th>"));
        assertFalse(html.contains("<th>GA</th>"));
        assertFalse(html.contains("<th>Status</th>"));
        assertFalse(html.contains("<th>Źródło</th>"));
        assertEquals(1, countOccurrences(html, "class=\"item-index-row\""));
        assertFalse(html.contains("class=\"item-card-grid\""));
        assertTrue(html.contains("<a class=\"item-name item-details-link\" href=\"#item-details-1\""));
        assertFalse(html.contains("<summary>Szczegóły</summary>"));
        assertTrue(html.contains("<section id=\"item-details-1\" class=\"item-details-modal\" role=\"dialog\""));
        assertTrue(html.contains("<a class=\"modal-close\" href=\"#biblioteka-lista\" aria-label=\"Zamknij szczegóły itemu\">×</a>"));
        assertTrue(html.contains("Dane podstawowe"));
        assertTrue(html.contains("Base stats"));
        assertTrue(html.contains("Linie bazowe"));
        assertFalse(html.contains("Implicit / linie bazowe"));
        assertTrue(html.contains("Affixy"));
        assertTrue(html.contains("Aspekt / efekt"));
        assertFalse(html.contains("Aspekt / efekt legendarny"));
        assertTrue(html.contains("Socket / gniazdo"));
        assertTrue(html.contains("1 131 pkt. pancerza"));
        assertTrue(html.contains("45% redukcji blokowanych obrażeń [45]%"));
        assertTrue(html.contains("20,0% szansy na blok [20,0]%"));
        assertTrue(html.contains("+100% obrażeń od broni w głównej ręce [100]%"));
        assertTrue(html.contains("+494 cierni"));
        assertTrue(html.contains("+7% szansy na szczęśliwy traf"));
        assertFalse(html.contains("+7,0% szansy na szczęśliwy traf [7,0"));
        assertFalse(html.contains("* 13,2% redukcji czasu odnowienia"));
        assertTrue(html.contains("★ 13,2% redukcji czasu odnowienia"));
        assertTrue(html.contains("<ul class=\"affix-summary\"><li>+494 cierni</li><li>+7% szansy na szczęśliwy traf</li><li>★ 13,2% redukcji czasu odnowienia</li><li>+114 siły</li></ul>"));
        assertTrue(html.contains("+114 siły"));
        assertTrue(html.contains("Aspekt Wewnętrznego Spokoju"));
        assertTrue(html.contains("<span class=\"aspect-summary\" title=\"Zwiększa zadawane obrażenia podczas stania w bezruchu"));
        assertFalse(html.contains("Wybrany aspekt: Aspekt Wewnętrznego Spokoju"));
        assertFalse(html.contains("Opis aspektu: Zwiększa zadawane obrażenia podczas stania w bezruchu"));
        assertFalse(html.contains("Status runtime:"));
        assertFalse(html.contains("Odczyt OCR efektu:"));
        assertFalse(html.contains("Zadajesz obrażenia zwiększone o 11,0%[x]"));
        assertFalse(html.contains("Odczyt efektu OCR niepełny / wymaga ręcznej weryfikacji."));
        assertFalse(html.contains("Affix: 45% redukcji blokowanych obrażeń"));
        assertFalse(html.contains("Affix: 20,0% szansy na blok"));
        assertFalse(html.contains("Affix: +100% obrażeń od broni w głównej ręce"));
        assertFalse(html.contains("Brak zapisanych linii w tej sekcji."));
        assertFalse(html.contains("800 1 131 pkt. pancerza"));
        assertFalse(html.contains("* +114 siły"));
        assertFalse(html.contains("Zadajesz obrażenia zwiększone o [5,0 - Ta premia"));
        assertEquals(0, countOccurrences(html, "Ta premia jest trzy razy większa"));
        assertFalse(html.contains("Projekcja do aktualnego runtime"));
        assertFalse(html.contains("Łączne obrażenia"));
        assertFalse(html.contains("<h2>DPS</h2>"));
        assertEquals(1, countOccurrences(html, ">Moc przedmiotu<"));
        assertFalse(html.contains("Pokaż slot w current build"));
        assertFalse(html.contains(">Załóż bohaterowi:"));
        assertFalse(html.contains(">Zmień w slocie:"));
        assertTrue(html.contains("class=\"icon-action edit-action\""));
        assertTrue(html.contains("aria-label=\"Edytuj item Ręka dodatkowa / tarcza.png\""));
        assertTrue(html.contains("class=\"icon-action delete-action\""));
        assertTrue(html.contains("aria-label=\"Usuń item Ręka dodatkowa / tarcza.png\""));
    }

    @Test
    void shouldRenderIncompleteAspectMessageWhenOnlyEffectTailIsAvailable() {
        SavedImportedItem shield = new SavedImportedItem(
                1L,
                "Ręka dodatkowa / tarcza.png",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                new FullItemRead(
                        "Tarcza testowa",
                        "Tarcza",
                        "Legendarny",
                        "Moc przedmiotu: 800",
                        "1 131 pkt. pancerza",
                        List.of(new FullItemReadLine(FullItemReadLineType.ASPECT,
                                "Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek."))
                ),
                List.of(),
                "inner-calm"
        );

        String html = render(List.of(shield));

        assertTrue(html.contains("Aspekt Wewnętrznego Spokoju"));
        assertTrue(html.contains("Zwiększa zadawane obrażenia podczas stania w bezruchu"));
        assertFalse(html.contains("Wybrany aspekt: Aspekt Wewnętrznego Spokoju"));
        assertFalse(html.contains("Opis aspektu: Zwiększa zadawane obrażenia podczas stania w bezruchu"));
        assertFalse(html.contains("Odczyt efektu OCR niepełny / wymaga ręcznej weryfikacji."));
        assertFalse(html.contains("<li>Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek.</li>"));
    }

    @Test
    void shouldHideMasterworkingDetailsForQualityZero() {
        SavedImportedItem shield = new SavedImportedItem(
                1L,
                "Ręka dodatkowa / tarcza.png",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                new FullItemRead("Tarcza testowa", "Tarcza", "Legendarny", "Moc przedmiotu: 900", "1 202 pkt. pancerza", List.of()),
                List.of(),
                "",
                krys.itemimport.ItemImportDetails.empty(),
                List.of(),
                ItemMasterworking.quality(0)
        );

        String html = render(List.of(shield));

        assertFalse(html.contains("Jakość 0/25"));
        assertFalse(html.contains("Dane itemu / runtime nieaktywny"));
    }

    @Test
    void shouldRenderMasterworkingDetailsFromQualityCurrent() {
        SavedImportedItem shield = new SavedImportedItem(
                1L,
                "Ręka dodatkowa / tarcza.png",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                new FullItemRead("Tarcza testowa", "Tarcza", "Legendarny", "Moc przedmiotu: 900", "1 202 pkt. pancerza", List.of()),
                List.of(),
                "",
                krys.itemimport.ItemImportDetails.empty(),
                List.of(),
                ItemMasterworking.quality(3)
        );

        String html = render(List.of(shield));

        assertTrue(html.contains("Doskonalenie · Jakość 3/25 · Runtime aktywny dla potwierdzonych wartości"));
    }

    @Test
    void shouldRenderPerfectedAffixInMasterworkingDetails() {
        SavedImportedItem shield = new SavedImportedItem(
                1L,
                "Ręka dodatkowa / tarcza.png",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                new FullItemRead("Tarcza testowa", "Tarcza", "Legendarny", "Moc przedmiotu: 900", "1 202 pkt. pancerza", List.of()),
                List.of(new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 270.0d)),
                "",
                krys.itemimport.ItemImportDetails.empty(),
                List.of(new ItemTemperingAffix(
                        "defense_max_animus",
                        TemperingCategory.DEFENSE,
                        7.0d,
                        "+7 do maksymalnej liczby kumulacji Animuszu",
                        TemperingRuntimeStatus.DATA_ONLY,
                        true
                )),
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.ordinaryAffix("STRENGTH"))
        );

        String html = render(List.of(shield));

        assertTrue(html.contains("Doskonalenie · Jakość 25/25 · Doskonalony afiks: Siła · Runtime aktywny dla potwierdzonych wartości"));
    }

    @Test
    void shouldRenderTemperingPerfectedAffixInMasterworkingDetails() {
        SavedImportedItem shield = new SavedImportedItem(
                1L,
                "Ręka dodatkowa / tarcza.png",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                new FullItemRead("Tarcza testowa", "Tarcza", "Legendarny", "Moc przedmiotu: 900", "1 202 pkt. pancerza", List.of()),
                List.of(),
                "",
                krys.itemimport.ItemImportDetails.empty(),
                List.of(new ItemTemperingAffix(
                        "defense_max_animus",
                        TemperingCategory.DEFENSE,
                        7.0d,
                        "+7 do maksymalnej liczby kumulacji Animuszu",
                        TemperingRuntimeStatus.DATA_ONLY,
                        true
                )),
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.temperingAffix("defense_max_animus"))
        );

        String html = render(List.of(shield));

        assertTrue(html.contains("Doskonalenie · Jakość 25/25 · Doskonalony afiks: maksymalna liczba kumulacji Animuszu · Runtime aktywny dla potwierdzonych wartości"));
    }

    @Test
    void shouldRenderMasterworkingPresentationValuesForReferenceShield() {
        SavedImportedItem shield = referenceShield(
                new ItemMasterworking(25, 25),
                List.of(referenceMaxAnimusTempering())
        );

        String html = render(List.of(shield));

        assertFalse(html.contains("Podgląd wartości po Doskonaleniu"));
        assertFalse(html.contains("<h5>Wartości po Doskonaleniu</h5>"));
        assertFalse(html.contains("225 → 270"));
        assertFalse(html.contains("787 → 945"));
        assertFalse(html.contains("+5 → +7"));
        assertTrue(html.contains("<span class=\"masterworking-value masterworking-value--upgraded\">1502</span>"));
        assertTrue(html.contains("Siła <span class=\"masterworking-value masterworking-value--upgraded\">270</span>"));
        assertTrue(html.contains("Odporność na Ogień <span class=\"masterworking-value masterworking-value--upgraded\">945</span>"));
        assertTrue(html.contains("Redukcja obrażeń <span class=\"masterworking-value masterworking-value--upgraded\">14,3%</span>"));
        assertTrue(html.contains("★ <span class=\"masterworking-value masterworking-value--upgraded\">+7</span> do maksymalnej liczby kumulacji Animuszu"));
    }

    @Test
    void shouldRenderPerfectedTemperingPresentationValuesForReferenceShield() {
        SavedImportedItem shield = referenceShield(
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.temperingAffix("defense_max_animus")),
                List.of(referenceMaxAnimusTempering())
        );

        String html = render(List.of(shield));

        assertFalse(html.contains("+5 → +12"));
        assertTrue(html.contains("★ <span class=\"masterworking-value masterworking-value--perfected\">+12</span> do maksymalnej liczby kumulacji Animuszu"));
        assertTrue(html.contains("Doskonalony afiks"));
        assertFalse(html.contains("Runtime nadal używa zapisanej wartości +5"));
    }

    @Test
    void shouldRenderFinalPresentationValuesInItemLibrarySummaryRow() {
        SavedImportedItem shield = referenceShield(
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.temperingAffix("defense_max_animus")),
                List.of(referenceMaxAnimusTempering())
        );

        String html = render(List.of(shield));

        String row = firstItemIndexRow(html);
        assertTrue(row.contains("Siła <span class=\"masterworking-value masterworking-value--upgraded\">270</span>"));
        assertTrue(row.contains("Odporność na Ogień <span class=\"masterworking-value masterworking-value--upgraded\">945</span>"));
        assertTrue(row.contains("Redukcja obrażeń <span class=\"masterworking-value masterworking-value--upgraded\">14,3%</span>"));
        assertTrue(row.contains("Odporność na wszystkie żywioły <span class=\"masterworking-value masterworking-value--upgraded\">588</span>"));
        assertTrue(row.contains("★ <span class=\"masterworking-value masterworking-value--perfected\">+12</span> do maksymalnej liczby kumulacji Animuszu"));
        assertFalse(row.contains("+225 siły"));
        assertFalse(row.contains("+787 odporności"));
        assertFalse(row.contains("+490 odporności"));
        assertFalse(row.contains("+5 max Animusz"));
    }

    @Test
    void shouldRenderPerfectedOrdinaryPresentationInlineForReferenceShield() {
        SavedImportedItem strengthShield = referenceShield(
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.ordinaryAffix("STRENGTH")),
                List.of(referenceMaxAnimusTempering())
        );
        SavedImportedItem fireShield = referenceShield(
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.ordinaryAffix("FIRE_RESISTANCE")),
                List.of(referenceMaxAnimusTempering())
        );

        String strengthHtml = render(List.of(strengthShield));
        String fireHtml = render(List.of(fireShield));

        assertFalse(strengthHtml.contains("225 → 360"));
        assertTrue(strengthHtml.contains("Siła <span class=\"masterworking-value masterworking-value--perfected\">360</span>"));
        assertTrue(strengthHtml.contains("Doskonalony afiks"));
        assertFalse(fireHtml.contains("787 → 1260"));
        assertTrue(fireHtml.contains("Odporność na Ogień <span class=\"masterworking-value masterworking-value--perfected\">1260</span>"));
        assertTrue(fireHtml.contains("Doskonalony afiks"));
    }

    @Test
    void shouldRenderBootItemInTheSameSemanticSections() {
        SavedImportedItem boots = new SavedImportedItem(
                1L,
                "Buty / buty.png",
                "buty.png",
                EquipmentSlot.BOOTS,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                new FullItemRead(
                        "Marsz Pokutnika",
                        "Buty",
                        "Rzadki przedmiot",
                        "800 mocy przedmiotu",
                        "354 pkt. pancerza",
                        List.of(
                                new FullItemReadLine(FullItemReadLineType.ITEM_NAME, "Marsz Pokutnika"),
                                new FullItemReadLine(FullItemReadLineType.TYPE_OR_SLOT, "Buty"),
                                new FullItemReadLine(FullItemReadLineType.ITEM_POWER, "800 mocy przedmiotu"),
                                new FullItemReadLine(FullItemReadLineType.BASE_STAT, "354 pkt. pancerza"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "+12,5% szybkości ruchu"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "+7,0% uniku"),
                                new FullItemReadLine(FullItemReadLineType.SOCKET, "2 gniazda")
                        )
                ),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.MOVEMENT_SPEED, 12.5d, "%", true, 0, "+12,5% szybkości ruchu", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.DODGE_CHANCE, 7.0d, "%", true, 1, "+7,0% uniku", ImportedItemAffixSource.OCR)
                ),
                ""
        );

        String html = render(List.of(boots));

        assertTrue(html.contains("Base stats"));
        assertTrue(html.contains("354 pkt. pancerza"));
        assertTrue(html.contains("Affixy"));
        assertFalse(html.contains("* +12,5% szybkości ruchu"));
        assertFalse(html.contains("* +7,0% uniku"));
        assertTrue(html.contains("★ +12,5% szybkości ruchu"));
        assertTrue(html.contains("★ +7% uniku"));
        assertTrue(html.contains("<ul class=\"affix-summary\"><li>★ +12,5% szybkości ruchu</li><li>★ +7% uniku</li></ul>"));
        assertTrue(html.contains("Brak wybranego aspektu."));
        assertTrue(html.contains("Socket / gniazdo"));
        assertTrue(html.contains("2 gniazda"));
        assertFalse(html.contains("Diagnostyka OCR"));
        assertFalse(html.contains("Affix: 354 pkt. pancerza"));
        assertFalse(html.contains("Projekcja do aktualnego runtime"));
    }

    @Test
    void shouldShowNoSelectedAspectButKeepRawOcrEffectAsHelperRead() {
        SavedImportedItem item = new SavedImportedItem(
                1L,
                "Ręka dodatkowa / unknown.png",
                "unknown.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                new FullItemRead(
                        "Tarcza testowa",
                        "Tarcza",
                        "Legendarny",
                        "Moc przedmiotu: 800",
                        "1 131 pkt. pancerza",
                        List.of(new FullItemReadLine(FullItemReadLineType.ASPECT, "Aspekt zupełnie nieznany z OCR"))
                ),
                List.of(),
                ""
        );

        String html = render(List.of(item));

        assertTrue(html.contains("Brak wybranego aspektu."));
        assertFalse(html.contains("Odczyt OCR efektu: Aspekt zupełnie nieznany z OCR"));
        assertFalse(html.contains("Wybrany aspekt: Aspekt zupełnie nieznany z OCR"));
    }

    @Test
    void shouldShowActiveBadgeNearItemNameWithoutStatusColumn() {
        SavedImportedItem shield = new SavedImportedItem(
                1L,
                "Ręka dodatkowa / tarcza.png",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                0.0d,
                new FullItemRead("Tarcza aktywna", "Tarcza", "Legendarny", "Moc przedmiotu: 800", "1 131 pkt. pancerza", List.of()),
                List.of(),
                ""
        );
        HeroItemSelection selection = new HeroItemSelection(Map.of(HeroEquipmentSlot.OFF_HAND, 1L));
        HeroProfile hero = new HeroProfile(1L, "Tester", HeroClass.PALADIN, "level=13", selection);

        String html = new ItemLibraryPageRenderer().render(new ItemLibraryPageModel(
                List.of(shield),
                hero,
                selection,
                List.of(),
                List.of(),
                "",
                null
        ));

        assertTrue(html.contains("Tarcza aktywna"));
        assertTrue(html.contains("<span class=\"status-badge status-active\">Założony</span>"));
        assertTrue(html.contains("<div class=\"item-actions\"><div class=\"assign-actions\"><span class=\"icon-action assign-action assign-action-selected\""));
        assertTrue(html.contains("aria-label=\"Item Ręka dodatkowa / tarcza.png jest już założony w slocie Tarcza\""));
        assertTrue(html.contains("class=\"icon-action edit-action\""));
        assertTrue(html.contains("class=\"icon-action delete-action\""));
        assertFalse(html.contains("<th>Status</th>"));
    }

    private static String render(List<SavedImportedItem> items) {
        return new ItemLibraryPageRenderer().render(new ItemLibraryPageModel(
                items,
                null,
                HeroItemSelection.empty(),
                List.of(),
                List.of(),
                "",
                null
        ));
    }

    private static SavedImportedItem referenceShield(ItemMasterworking masterworking, List<ItemTemperingAffix> temperingAffixes) {
        return new SavedImportedItem(
                1L,
                "Ręka dodatkowa / tarcza.png",
                "tarcza.png",
                EquipmentSlot.OFF_HAND,
                0L,
                0.0d,
                0.0d,
                0.0d,
                20.0d,
                0.0d,
                new FullItemRead("Miażdżąca Tarcza Kościanych Łusek", "Tarcza", "Legendarny",
                        "Moc przedmiotu: 900", "1202 pkt. pancerza", List.of()),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 225.0d, "", true, 0, "+225 siły", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.ALL_RESISTANCE, 490.0d, "", true, 1, "+490 do odporności na wszystkie żywioły", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.FIRE_RESISTANCE, 787.0d, "", true, 2, "+787 do odporności na: Ogień", ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, "%", false, 3, "11,4% redukcji obrażeń", ImportedItemAffixSource.OCR)
                ),
                "",
                new krys.itemimport.ItemImportDetails("Miażdżąca Tarcza Kościanych Łusek", "Tarcza", "LEGENDARY", true,
                        EquipmentSlot.OFF_HAND, 900L, null, null, null, null, null, 1202L, ""),
                temperingAffixes,
                masterworking
        );
    }

    private static ItemTemperingAffix referenceMaxAnimusTempering() {
        return new ItemTemperingAffix(
                "defense_max_animus",
                TemperingCategory.DEFENSE,
                5.0d,
                "+5 do maksymalnej liczby kumulacji Animuszu",
                TemperingRuntimeStatus.DATA_ONLY,
                true
        );
    }

    private static int countOccurrences(String value, String needle) {
        int count = 0;
        int index = value.indexOf(needle);
        while (index >= 0) {
            count++;
            index = value.indexOf(needle, index + needle.length());
        }
        return count;
    }

    private static String firstItemIndexRow(String html) {
        int start = html.indexOf("<tr class=\"item-index-row");
        if (start < 0) {
            throw new AssertionError("Brak wiersza biblioteki itemów.");
        }
        int end = html.indexOf("</tr>", start);
        if (end < 0) {
            throw new AssertionError("Nie udało się wyciąć wiersza biblioteki itemów.");
        }
        return html.substring(start, end + "</tr>".length());
    }
}

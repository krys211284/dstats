package krys.web;

import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ItemImageImportCandidateParseResult;
import krys.itemimport.ItemImageMetadata;
import krys.itemimport.ItemImportFieldConfidence;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportEditableFormFactory;
import krys.itemimport.ItemImportFieldCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje strukturę SSR formularza ręcznej walidacji affixów itemu. */
class ItemImportPageRendererTest {
    @Test
    void shouldRenderEditableAffixListAsMainManualReviewModel() {
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
                        "NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU",
                        "Starożytna legendarna tarcza",
                        "Starożytna legendarna",
                        "Moc przedmiotu: 800",
                        "Pancerz: 1 131 pkt.",
                        List.of(new FullItemReadLine(FullItemReadLineType.AFFIX, "+114 siły [107 - 121]"))
                ),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 114.0d, "+114 siły [107 - 121]"),
                        new ImportedItemAffix(ImportedItemAffixType.THORNS, 494.0d, "+494 cierni [473 - 506]")
                ),
                "inner-calm",
                ItemImportFieldConfidence.HIGH,
                "inner-calm"
        );
        HeroProfile activeHero = new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty());

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                null,
                List.of(),
                null,
                activeHero,
                "Import testowy",
                ""
        ));

        assertTrue(html.contains("Ręczna weryfikacja affixów"));
        assertTrue(html.contains("Typ itemu"));
        assertTrue(html.contains("Slot ekwipunku"));
        assertTrue(html.contains("Aspekt"));
        assertTrue(html.contains("name=\"affixType_0\""));
        assertTrue(html.contains("name=\"affixValue_0\" value=\"114\""));
        assertTrue(html.contains("class=\"secondary-button remove-affix-button\""));
        assertFalse(html.contains("name=\"affixRemoved_0\""));
        assertTrue(html.contains("+114 siły [107 - 121]"));
        assertTrue(html.contains("name=\"affixType_1\""));
        assertTrue(html.contains("name=\"affixValue_1\" value=\"494\""));
        assertTrue(html.contains("Dodaj affix"));
        assertTrue(html.contains("type=\"button\" id=\"addAffixButton\""));
        assertTrue(html.contains("<noscript>"));
        assertTrue(html.contains("name=\"formAction\" value=\"addAffix\""));
        assertTrue(html.contains("name=\"newAffixType\""));
        assertTrue(html.contains("name=\"newAffixValue\""));
        assertTrue(html.contains("Aspekt Wewnętrznego Spokoju"));
        assertFalse(html.contains(">Aspekt Wewnętrznego Spokoju (wysoka)<"));
        assertTrue(html.contains("Wybrany aspekt: Aspekt Wewnętrznego Spokoju"));
        assertTrue(html.contains("Opis aspektu: Zwiększa zadawane obrażenia podczas stania w bezruchu"));
        assertTrue(html.contains("Sugestia OCR: Aspekt Wewnętrznego Spokoju"));
        assertTrue(html.contains("Pewność OCR sugestii: wysoka"));
        assertTrue(html.contains("Dopasowano w katalogu aspektów."));
        assertTrue(html.contains("wysoka"));
        assertFalse(html.contains("Projekcja do aktualnego runtime"));
        assertFalse(html.contains("Mapowanie do aktualnego modelu buildu"));
    }

    @Test
    void shouldCheckGreaterAffixCheckboxWhenExtractorDetectsMissingRollRange() {
        ItemImageImportCandidateParseResult parseResult = new ItemImageImportCandidateParseResult(
                new ItemImageMetadata("tarcza.png", "image/png", "PNG", 1200, 800),
                new FullItemRead(
                        "Tarcza testowa",
                        "Tarcza",
                        "Legendarny",
                        "800 mocy przedmiotu",
                        "1 131 pkt. pancerza",
                        List.of(new FullItemReadLine(FullItemReadLineType.AFFIX, "13,2% redukcji czasu odnowienia"))
                ),
                new ItemImportFieldCandidate<>("OFF_HAND", EquipmentSlot.OFF_HAND, ItemImportFieldConfidence.HIGH, "slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                ItemImportFieldCandidate.unknown("str"),
                ItemImportFieldCandidate.unknown("int"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution"),
                "Import wspomagany"
        );
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(parseResult);
        HeroProfile activeHero = new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty());

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                null,
                List.of(),
                null,
                activeHero,
                "Import testowy",
                ""
        ));

        assertTrue(html.contains("name=\"affixGreater_0\" value=\"true\" checked"));
        assertTrue(html.contains("* 13,2% redukcji czasu odnowienia"));
    }

    @Test
    void shouldRenderIncompleteAspectEffectMessageInsteadOfOrphanTail() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
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
                "inner-calm",
                ItemImportFieldConfidence.HIGH,
                "inner-calm"
        );
        HeroProfile activeHero = new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty());

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                null,
                List.of(),
                null,
                activeHero,
                "Import testowy",
                ""
        ));

        assertTrue(html.contains("Odczyt efektu OCR niepełny / wymaga ręcznej weryfikacji."));
        assertFalse(html.contains("<li>Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek.</li>"));
    }

    @Test
    void shouldRenderFullAspectEffectOnceWhenHeadAndTailAreAvailable() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                new FullItemRead(
                        "Tarcza testowa",
                        "Tarcza",
                        "Legendarny",
                        "Moc przedmiotu: 800",
                        "1 131 pkt. pancerza",
                        List.of(
                                new FullItemReadLine(FullItemReadLineType.ASPECT, "Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%"),
                                new FullItemReadLine(FullItemReadLineType.ASPECT, "Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek."),
                                new FullItemReadLine(FullItemReadLineType.ASPECT, "Zadajesz obrażenia zwiększone o [5,0 - Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek.")
                        )
                ),
                List.of(),
                "inner-calm",
                ItemImportFieldConfidence.HIGH,
                "inner-calm"
        );
        HeroProfile activeHero = new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty());

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                null,
                List.of(),
                null,
                activeHero,
                "Import testowy",
                ""
        ));

        assertTrue(html.contains("Opis aspektu: Zwiększa zadawane obrażenia podczas stania w bezruchu"));
        assertTrue(html.contains("Odczyt OCR efektu: Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]% Ta premia jest trzy razy większa"));
        assertFalse(html.contains("Zadajesz obrażenia zwiększone o [5,0 - Ta premia"));
        assertEquals(2, countOccurrences(html, "Ta premia jest trzy razy większa"));
        assertFalse(html.contains("Opis aspektu: Zadajesz obrażenia zwiększone o 11,0%"));
    }

    @Test
    void shouldRenderUnknownAspectRegistryMessageWhenOcrTextHasNoMatch() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "unknown.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                new FullItemRead(
                        "Tarcza testowa",
                        "Tarcza",
                        "Legendarny",
                        "Moc przedmiotu: 800",
                        "1 131 pkt. pancerza",
                        List.of(new FullItemReadLine(FullItemReadLineType.ASPECT, "Aspekt zupełnie nieznany z OCR"))
                ),
                List.of(),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                ""
        );
        HeroProfile activeHero = new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty());

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                null,
                List.of(),
                null,
                activeHero,
                "Import testowy",
                ""
        ));

        assertTrue(html.contains("OCR wykrył tekst aspektu, ale nie znaleziono dopasowania w katalogu aspektów. Wybierz ręcznie albo zostaw brak."));
        assertTrue(html.contains("Brak wybranego aspektu."));
        assertTrue(html.contains("Odczyt OCR efektu: Aspekt zupełnie nieznany z OCR"));
        assertTrue(html.contains("<option value=\"\" selected"));
    }

    @Test
    void shouldRenderVerathielWeaponFieldsForManualConfirmationWithoutMixingDpsWithAverage() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "miecz.png",
                "MAIN_HAND",
                "0",
                "0",
                "0",
                "0",
                "0",
                "0",
                new FullItemRead(
                        "Odłamek Verathiela",
                        "Starożytny unikatowy miecz",
                        "UNIQUE",
                        "Moc przedmiotu: 900",
                        "1 830 pkt. obrażeń na sek.",
                        List.of(
                                new FullItemReadLine(FullItemReadLineType.BASE_STAT, "1 830 pkt. obrażeń na sek."),
                                new FullItemReadLine(FullItemReadLineType.BASE_STAT, "[1 350 - 1 978] pkt. obrażeń za trafienie"),
                                new FullItemReadLine(FullItemReadLineType.BASE_STAT, "1,10 ataku na sekundę"),
                                new FullItemReadLine(FullItemReadLineType.AFFIX, "+94 obrażeń od broni [94 - 157]"),
                                new FullItemReadLine(FullItemReadLineType.ASPECT, "Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100], ale dodatkowo zużywają 25 pkt. podstawowego zasobu.")
                        ),
                        new ItemImportDetails(
                                "Odłamek Verathiela",
                                "Miecz",
                                "UNIQUE",
                                true,
                                EquipmentSlot.MAIN_HAND,
                                900L,
                                1830L,
                                1350L,
                                1978L,
                                1664L,
                                1.10d,
                                "Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100], ale dodatkowo zużywają 25 pkt. podstawowego zasobu."
                        )
                ),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.WEAPON_DAMAGE_FLAT, 94.0d, "+94 obrażeń od broni [94 - 157]"),
                        new ImportedItemAffix(ImportedItemAffixType.MAXIMUM_LIFE, 2141.0d, "+2 141 maksymalnego zdrowia [1 831 - 2 200]"),
                        new ImportedItemAffix(ImportedItemAffixType.LIFE_ON_HIT, 545.0d, "+545 pkt. zdrowia przy trafieniu [526 - 632]"),
                        new ImportedItemAffix(ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE, 15.0d, "Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]")
                ),
                "verathiel_shard",
                ItemImportFieldConfidence.HIGH,
                "verathiel_shard",
                new ItemImportDetails(
                        "Odłamek Verathiela",
                        "Miecz",
                        "UNIQUE",
                        true,
                        EquipmentSlot.MAIN_HAND,
                        900L,
                        1830L,
                        1350L,
                        1978L,
                        1664L,
                        1.10d,
                        "Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100], ale dodatkowo zużywają 25 pkt. podstawowego zasobu."
                )
        );
        HeroProfile activeHero = new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty());

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                null,
                List.of(),
                null,
                activeHero,
                "Import testowy",
                ""
        ));

        assertTrue(html.contains("Odłamek Verathiela"));
        assertTrue(html.contains("name=\"weaponDps\" value=\"1830\""));
        assertTrue(html.contains("name=\"weaponDamageMin\" value=\"1350\""));
        assertTrue(html.contains("name=\"weaponDamageMax\" value=\"1978\""));
        assertTrue(html.contains("name=\"averageWeaponDamage\" value=\"1664\""));
        assertTrue(html.contains("name=\"attacksPerSecond\" value=\"1.10\""));
        assertTrue(html.contains("name=\"itemPower\" value=\"900\""));
        assertTrue(html.contains("DPS broni"));
        assertTrue(html.contains("Średnie obrażenia trafienia"));
        assertFalse(html.contains("name=\"itemPower\" value=\"1\""));
        assertFalse(html.contains("<div class=\"summary-label\">Moc przedmiotu</div>\n                    <div class=\"summary-value\">1</div>"));
        assertFalse(html.contains("<div class=\"summary-label\">Bazowe obrażenia</div>\n                    <div class=\"summary-value\">1</div>"));
        assertFalse(html.contains("name=\"averageWeaponDamage\" value=\"1830\""));
        assertTrue(html.contains("Efekt unikatowy zapisany opisowo"));
        assertTrue(html.contains("Aspekt unikatowy: Odłamek Verathiela"));
        assertTrue(html.contains("Status runtime: Nieaktywny w runtime DPS"));
        assertFalse(html.contains("Brak wybranego aspektu."));
        assertFalse(html.contains("nie znaleziono dopasowania w katalogu aspektów"));
        assertTrue(html.contains("100%[x]"));
        assertTrue(html.contains("[70 - 100]"));
        assertTrue(html.contains("25 pkt. podstawowego zasobu"));
        assertTrue(html.contains("value=\"94\""));
        assertTrue(html.contains("[94 - 157]"));
        assertTrue(html.contains("value=\"2141\""));
        assertTrue(html.contains("[1 831 - 2 200]"));
        assertTrue(html.contains("value=\"545\""));
        assertTrue(html.contains("[526 - 632]"));
        assertTrue(html.contains("15%"));
        assertTrue(html.contains("+3 podstawowego zasobu [3 - 4]"));
        assertEquals(4, countOccurrences(html, "Źródło: OCR"));
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
}

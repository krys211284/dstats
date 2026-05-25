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
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingCategory;
import krys.tempering.TemperingRuntimeStatus;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.HoradricTuningPrism;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationAffixRoll;
import krys.transfiguration.TransfigurationValueProvenance;
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
        assertFalse(html.contains("Wybrany aspekt: Aspekt Wewnętrznego Spokoju"));
        assertFalse(html.contains("Typ: Legendarny"));
        assertTrue(html.contains("Treść efektu"));
        assertTrue(html.contains("Zwiększa zadawane obrażenia podczas stania w bezruchu"));
        assertFalse(html.contains("Sugestia OCR: Aspekt Wewnętrznego Spokoju"));
        assertFalse(html.contains("Pewność OCR sugestii: wysoka"));
        assertFalse(html.contains("Dopasowano w katalogu aspektów."));
        assertFalse(html.contains("Ta lista jest głównym modelem korekty itemu. Finalny zapis użyje tylko aktywnych wierszy widocznych w tej tabeli."));
        assertTrue(html.contains("affix-table-wrap"));
        assertTrue(html.contains("data-table affix-table"));
        assertTrue(html.contains("<main class=\"layout wide-item-page\">"));
        assertTrue(html.contains(".layout.wide-item-page"));
        assertFalse(html.contains("Projekcja do aktualnego runtime"));
        assertFalse(html.contains("Mapowanie do aktualnego modelu buildu"));
    }

    @Test
    void shouldRenderHoradricTransfigurationEditorFields() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "20",
                "0",
                FullItemRead.empty(),
                List.of(new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 225.0d)),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                ItemImportDetails.empty(),
                List.of(),
                ItemMasterworking.defaultState(),
                new ItemTransfiguration(
                        true,
                        true,
                        HoradricTuningPrism.AGGRESSIVE,
                        HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                        "",
                        new TransfigurationAffixRoll("PRIMARY_STAT", 180.0d),
                        "",
                        null,
                        null,
                        false,
                        "")
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

        assertTrue(html.contains("Przeistoczenie / Kostka Horadrimów"));
        assertTrue(html.contains("Wynik przeistoczenia"));
        assertTrue(html.contains("Pryzmat dostrojenia"));
        assertTrue(html.contains("Niemodyfikowalny po przeistoczeniu"));
        assertTrue(html.contains("name=\"transfigurationState\""));
        assertTrue(html.contains("value=\"TRANSFIGURED\" selected"));
        assertTrue(html.contains("name=\"transfigurationOutcome\""));
        assertTrue(html.contains("value=\"BONUS_TRANSFIGURATION_AFFIX\" selected"));
        assertTrue(html.contains("name=\"transfigurationTuningPrism\""));
        assertTrue(html.contains("value=\"AGGRESSIVE\" selected"));
        assertTrue(html.contains("name=\"transfigurationAddedAffixId\""));
        assertTrue(html.contains("value=\"PRIMARY_STAT\" selected"));
        assertTrue(html.contains("name=\"transfigurationAddedDisplayedValue\" step=\"0.1\" value=\"180\""));
        assertTrue(html.contains("Wartość widoczna na itemie"));
        assertTrue(html.contains("name=\"transfigurationAddedValueProvenance\""));
        assertTrue(html.contains("data-transfiguration-section"));
        assertTrue(html.contains("data-transfiguration-outcome=\"BONUS_TRANSFIGURATION_AFFIX\""));
        assertTrue(html.contains("data-transfiguration-field"));
        assertTrue(html.contains("Wartość widoczna w grze"));
    }

    @Test
    void shouldRenderMinimalTransfigurationEditorForNotTransfiguredItem() {
        String section = sectionByHeading(renderFullPage(formWithTransfiguration(ItemTransfiguration.none())), "Przeistoczenie / Kostka Horadrimów");

        assertTrue(section.contains("Nieprzeistoczony"));
        assertTrue(section.contains("Przeistoczenie nie jest ustawione dla tego itemu."));
        assertTrue(tagByDataAttribute(section, "data-transfiguration-active-fields").contains(" hidden"));
        assertTrue(tagByDataAttribute(section, "data-transfiguration-outcome-fields").contains(" hidden"));
        assertTrue(section.contains("Wynik przeistoczenia"));
        assertTrue(section.contains("Pryzmat dostrojenia"));
        assertTrue(section.contains("Niemodyfikowalny po przeistoczeniu"));
        assertTrue(section.contains("Bonusowy affix Przeistoczenia"));
        assertTrue(section.contains("Wartość widoczna na itemie"));
    }

    @Test
    void shouldRenderOnlyOutcomeSpecificTransfigurationFields() {
        assertOutcomeFields(
                HoradricTransfigurationOutcome.UPGRADE_TO_GREATER_AFFIX,
                List.of("Affix ulepszony do Greater Affix"),
                List.of("name=\"transfigurationAddedAffixId\"", "Wartość widoczna na itemie", "Affix do zastąpienia", "Affix zastępujący", "name=\"transfigurationBonusQuality\""));
        assertOutcomeFields(
                HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                List.of("Bonusowy affix Przeistoczenia", "Wartość widoczna na itemie"),
                List.of("Affix ulepszony do Greater Affix", "Affix do zastąpienia", "Affix zastępujący", "name=\"transfigurationBonusQuality\""));
        assertOutcomeFields(
                HoradricTransfigurationOutcome.REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX,
                List.of("Affix do zastąpienia", "Affix zastępujący", "Wartość widoczna na itemie"),
                List.of("Affix ulepszony do Greater Affix", "name=\"transfigurationAddedAffixId\"", "name=\"transfigurationBonusQuality\""));
        assertOutcomeFields(
                HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY,
                List.of("Bonusowa jakość itemu"),
                List.of("Affix ulepszony do Greater Affix", "name=\"transfigurationAddedAffixId\"", "Wartość widoczna na itemie", "Affix do zastąpienia", "Affix zastępujący"));
        assertOutcomeFields(
                HoradricTransfigurationOutcome.INDESTRUCTIBLE,
                List.of(),
                List.of("Affix ulepszony do Greater Affix", "name=\"transfigurationAddedAffixId\"", "Wartość widoczna na itemie", "Affix do zastąpienia", "Affix zastępujący", "name=\"transfigurationBonusQuality\""));
    }

    @Test
    void shouldRenderBonusTransfigurationAffixForRealShieldValue() {
        String section = sectionByHeading(renderFullPage(formWithTransfiguration(new ItemTransfiguration(
                true,
                true,
                HoradricTuningPrism.NONE,
                HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX,
                "",
                new TransfigurationAffixRoll("ALL_STATS", 96.0d, TransfigurationValueProvenance.GAME_DISPLAYED_VALUE, ""),
                "",
                null,
                null,
                false,
                ""))), "Przeistoczenie / Kostka Horadrimów");
        String activeGroup = outcomeGroup(section, HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX);

        assertTrue(section.contains("value=\"TRANSFIGURED\" selected"));
        assertTrue(activeGroup.contains("Bonusowy affix Przeistoczenia"));
        assertTrue(activeGroup.contains("do wszystkich współczynników [75-100]"));
        assertTrue(activeGroup.contains("name=\"transfigurationAddedDisplayedValue\" step=\"0.1\" value=\"96\""));
        assertTrue(activeGroup.contains("Wartość widoczna w grze"));
        assertTrue(section.contains("Niemodyfikowalny po przeistoczeniu"));
        assertTrue(section.contains("<option value=\"true\" selected>Tak</option>"));
        assertTrue(outcomeGroup(section, HoradricTransfigurationOutcome.REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX).contains(" hidden"));
        assertTrue(outcomeGroup(section, HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY).contains(" hidden"));
    }

    @Test
    void shouldRenderPolishTuningPrismLabels() {
        String section = sectionByHeading(renderFullPage(formWithTransfiguration(ItemTransfiguration.transfigured(
                HoradricTransfigurationOutcome.UNKNOWN))), "Przeistoczenie / Kostka Horadrimów");

        assertTrue(section.contains("Entropiczny"));
        assertTrue(section.contains("Kulleana"));
        assertTrue(section.contains("Agresywny"));
        assertTrue(section.contains("Pragmatyczny"));
        assertTrue(section.contains("Protektora"));
        assertTrue(section.contains("Zasobny"));
        assertTrue(section.contains("Adeptowski"));
        assertTrue(section.contains("Chromatyczny"));
        assertFalse(section.contains(">Aggressive<"));
    }

    @Test
    void shouldKeepGreaterAffixUncheckedWhenOnlyRollRangeIsMissing() {
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

        assertFalse(html.contains("name=\"affixGreater_0\" value=\"true\" checked"));
        assertFalse(html.contains("* 13,2% redukcji czasu odnowienia"));
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

        assertTrue(html.contains("Treść efektu"));
        assertFalse(html.contains("Odczyt efektu OCR niepełny / wymaga ręcznej weryfikacji."));
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

        assertTrue(html.contains("Treść efektu"));
        assertTrue(html.contains("Zwiększa zadawane obrażenia podczas stania w bezruchu"));
        assertFalse(html.contains("Odczyt OCR efektu:"));
        assertFalse(html.contains("Zadajesz obrażenia zwiększone o [5,0 - Ta premia"));
        assertFalse(html.contains("<li>Ta premia jest trzy razy większa"));
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
        assertFalse(html.contains("Odczyt OCR efektu: Aspekt zupełnie nieznany z OCR"));
        assertTrue(html.contains("Treść efektu"));
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
                        new ImportedItemAffix(ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE, 3.0d, "Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]")
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
        assertFalse(html.contains("Aspekt unikatowy: Odłamek Verathiela"));
        assertFalse(html.contains("Typ: Unikatowy"));
        assertFalse(html.contains("Status runtime:"));
        assertFalse(html.contains("Nieaktywny w runtime DPS"));
        assertTrue(html.contains("<option value=\"verathiel_shard\""));
        assertTrue(html.contains(">Odłamek Verathiela</option>"));
        assertTrue(html.contains("aspect-effect-fieldset"));
        assertTrue(html.contains("<textarea name=\"uniqueEffectText\""));
        assertEquals(1, countOccurrences(html, "Treść efektu"));
        assertFalse(html.contains("Unikatowy efekt / aspekt"));
        assertFalse(html.contains("Aspekt / efekt legendarny"));
        assertFalse(html.contains("Brak wybranego aspektu."));
        assertFalse(html.contains("nie znaleziono dopasowania w katalogu aspektów"));
        assertFalse(html.contains("Sugestia OCR"));
        assertFalse(html.contains("Pewność OCR sugestii"));
        assertFalse(html.contains("Odczyt OCR efektu"));
        assertTrue(html.contains("100%[x]"));
        assertTrue(html.contains("[70 - 100]"));
        assertTrue(html.contains("25 pkt. podstawowego zasobu"));
        assertTrue(html.contains("value=\"94\""));
        assertTrue(html.contains("[94 - 157]"));
        assertTrue(html.contains("value=\"2141\""));
        assertTrue(html.contains("[1 831 - 2 200]"));
        assertTrue(html.contains("value=\"545\""));
        assertTrue(html.contains("[526 - 632]"));
        assertTrue(html.contains("Szczęśliwy traf: zasób podstawowy"));
        assertTrue(html.contains("title=\"Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +X podstawowego zasobu\""));
        assertTrue(html.contains("name=\"affixValue_3\" value=\"3\""));
        assertFalse(html.contains("<span class=\"summary-value\">+3</span>"));
        assertFalse(html.contains("15% / +3"));
        assertTrue(html.contains("3 - 4"));
        assertTrue(html.contains("name=\"affixValue_0\" value=\"94\""));
        assertTrue(html.contains("name=\"affixValue_1\" value=\"2141\""));
        assertTrue(html.contains("name=\"affixValue_2\" value=\"545\""));
        assertEquals(1, countOccurrences(html, "name=\"affixValue_3\""));
        assertFalse(html.contains("Odczyt OCR / źródło"));
        assertFalse(html.contains("Źródło: OCR"));
        assertFalse(html.contains("Ta lista jest głównym modelem korekty itemu. Finalny zapis użyje tylko aktywnych wierszy widocznych w tej tabeli."));
        String affixSection = html.substring(html.indexOf("<table class=\"data-table affix-table\""), html.indexOf("<h3>Hartowanie</h3>"));
        assertTrue(affixSection.contains("data-table affix-table"));
        assertTrue(affixSection.contains("Limit affixów dla tego przedmiotu został wykorzystany."));
        assertFalse(affixSection.contains("item-affix-add-grid"));
        assertFalse(affixSection.contains("item-affix-add-actions"));
        assertFalse(affixSection.contains("<h4>Dodaj affix</h4>"));
        assertFalse(affixSection.contains("name=\"newAffixType\""));
        assertFalse(affixSection.contains("name=\"newAffixValue\""));
        assertFalse(affixSection.contains("id=\"newAffixGreater\""));
        assertFalse(affixSection.contains("id=\"addAffixButton\""));
        assertTrue(html.contains("name=\"affixType_0\""));
        assertTrue(html.contains("name=\"affixType_1\""));
        assertTrue(html.contains("name=\"affixType_2\""));
        assertTrue(html.contains("name=\"affixType_3\""));
        assertFalse(html.contains("<h5>Affixy</h5>"));
    }

    @Test
    void shouldRenderAddAffixFormWhenOrdinaryAffixLimitIsNotUsed() {
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
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 225.0d),
                        new ImportedItemAffix(ImportedItemAffixType.FIRE_RESISTANCE, 787.0d),
                        new ImportedItemAffix(ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, "%")
                ),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                new ItemImportDetails("Tarcza testowa", "Tarcza", "LEGENDARY", true, EquipmentSlot.OFF_HAND,
                        900L, null, null, null, null, null, 1202L, ""),
                List.of(),
                ItemMasterworking.defaultState()
        );

        String html = renderFullPage(form);

        assertTrue(html.contains("<h4>Dodaj affix</h4>"));
        assertTrue(html.contains("name=\"newAffixType\""));
        assertTrue(html.contains("name=\"newAffixValue\""));
        assertTrue(html.contains("id=\"newAffixGreater\""));
        assertTrue(html.contains("id=\"addAffixButton\""));
        assertFalse(html.contains("Limit affixów dla tego przedmiotu został wykorzystany."));
    }

    @Test
    void shouldRenderOnlyWeaponAndOffenseTemperingCategoriesForSword() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "miecz.png",
                "MAIN_HAND",
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
                new ItemImportDetails("Miecz testowy", "Miecz", "LEGENDARY", true, EquipmentSlot.MAIN_HAND,
                        900L, 1830L, 1350L, 1978L, 1664L, 1.10d, "")
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
        String tempering = sectionByHeading(html, "Hartowanie");

        assertTrue(tempering.contains(">Broń</option>"));
        assertTrue(tempering.contains(">Ofensywa</option>"));
        assertFalse(tempering.contains("Defensywa"));
        assertFalse(tempering.contains("Funkcjonalność"));
        assertFalse(tempering.contains("Mobilność"));
        assertFalse(tempering.contains("Zasoby"));
        assertTrue(tempering.contains("Katalog affixów tej kategorii nie został jeszcze uzupełniony."));
    }

    @Test
    void shouldRenderShieldTemperingCategoriesAndDefenseCatalog() {
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
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                new ItemImportDetails("Tarcza testowa", "Tarcza", "LEGENDARY", true, EquipmentSlot.OFF_HAND,
                        900L, null, null, null, null, null, 1202L, "")
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
        String tempering = sectionByHeading(html, "Hartowanie");

        assertTrue(tempering.contains(">Broń</option>"));
        assertTrue(tempering.contains(">Ofensywa</option>"));
        assertTrue(tempering.contains(">Defensywa</option>"));
        assertTrue(tempering.contains(">Funkcjonalność</option>"));
        assertFalse(tempering.contains("Mobilność"));
        assertFalse(tempering.contains("Zasoby"));
        String newAffixSelect = selectByName(tempering, "newTemperingDefinitionId");
        assertTrue(newAffixSelect.contains("disabled"));
        assertFalse(newAffixSelect.contains("value=\"defense_maximum_life\""));
        assertTrue(tempering.contains("Katalog affixów tej kategorii nie został jeszcze uzupełniony."));
        assertTrue(tempering.contains("\"DEFENSE\""));
        assertTrue(tempering.contains("\"UTILITY\":[]"));
        assertTrue(tempering.contains("\"OFFENSE\":[]"));
        assertTrue(tempering.contains("\"id\":\"defense_maximum_life\""));
        assertTrue(tempering.contains("\"rangeMin\":\"1000\""));
        assertTrue(tempering.contains("\"rangeMax\":\"1500\""));
        assertTrue(tempering.contains("\"greaterValue\":\"1875\""));
        assertTrue(tempering.contains("\"greaterLabel\":\"1875\""));
        assertTrue(tempering.contains("\"id\":\"defense_max_animus\""));
        assertTrue(tempering.contains("\"greaterValue\":\"5\""));
        assertTrue(tempering.contains("\"greaterLabel\":\"5\""));
        assertTrue(tempering.contains("Greater Affix / Gwiazdka"));
        assertEquals(12, countOccurrences(tempering, "\"id\":\"defense_"));
        assertTrue(tempering.contains("data-tempering-affix-select"));
        assertTrue(tempering.contains("data-limit=\"1\""));
        assertTrue(tempering.contains("id=\"temperingAddControls\""));
        assertTrue(tempering.contains("id=\"temperingLimitMessage\" hidden"));
        assertTrue(tempering.contains("id=\"newTemperingCatalogMessage\""));
        assertTrue(tempering.contains("tempering-add-card"));
        assertTrue(tempering.contains("tempering-add-grid"));
        assertTrue(tempering.contains("tempering-add-field-category"));
        assertTrue(tempering.contains("tempering-add-field-affix"));
        assertTrue(tempering.contains("tempering-add-field-value"));
        assertTrue(tempering.contains("tempering-add-field-greater"));
        assertTrue(tempering.contains("tempering-add-field-action"));
        assertTrue(tempering.contains("tempering-validation-message"));
    }

    @Test
    void shouldRenderMasterworkingEditorForImportedItem() {
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
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                new ItemImportDetails("Tarcza testowa", "Tarcza", "LEGENDARY", true, EquipmentSlot.OFF_HAND,
                        900L, null, null, null, null, null, 1202L, ""),
                List.of(),
                ItemMasterworking.quality(0)
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
        String masterworking = sectionByHeading(html, "Doskonalenie");

        assertFalse(masterworking.contains("Doskonalenie aktywne"));
        assertFalse(masterworking.contains("Item doskonalony"));
        assertFalse(masterworking.contains("name=\"masterworkingEnabled\""));
        assertFalse(masterworking.contains("type=\"checkbox\""));
        assertTrue(masterworking.contains("Jakość aktualna"));
        String qualitySelect = selectByName(masterworking, "masterworkingQualityCurrent");
        assertFalse(masterworking.contains("<input type=\"number\" min=\"0\" max=\"25\" step=\"1\" name=\"masterworkingQualityCurrent\""));
        assertTrue(qualitySelect.contains("<option value=\"0\" selected>0/25</option>"));
        assertTrue(qualitySelect.contains("<option value=\"3\">3/25</option>"));
        assertTrue(qualitySelect.contains("<option value=\"6\">6/25</option>"));
        assertTrue(qualitySelect.contains("<option value=\"9\">9/25</option>"));
        assertTrue(qualitySelect.contains("<option value=\"12\">12/25</option>"));
        assertTrue(qualitySelect.contains("<option value=\"15\">15/25</option>"));
        assertTrue(qualitySelect.contains("<option value=\"17\">17/25</option>"));
        assertTrue(qualitySelect.contains("<option value=\"20\">20/25</option>"));
        assertTrue(qualitySelect.contains("<option value=\"21\">21/25</option>"));
        assertTrue(qualitySelect.contains("<option value=\"25\">25/25</option>"));
        assertTrue(masterworking.contains("Jakość maksymalna"));
        assertTrue(masterworking.contains("name=\"masterworkingQualityMax\" value=\"25\" readonly"));
        assertTrue(masterworking.contains("Runtime aktywny dla potwierdzonych wartości"));
        assertFalse(masterworking.contains("Aktualny doskonalony afiks"));
    }

    @Test
    void shouldRenderPerfectedAffixSelectorOnlyForQualityTwentyFive() {
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
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 270.0d),
                        new ImportedItemAffix(ImportedItemAffixType.FIRE_RESISTANCE, 945.0d)
                ),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                new ItemImportDetails("Tarcza testowa", "Tarcza", "LEGENDARY", true, EquipmentSlot.OFF_HAND,
                        900L, null, null, null, null, null, 1502L, ""),
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

        String masterworking = sectionByHeading(renderFullPage(form), "Doskonalenie");
        String selector = selectByName(masterworking, "masterworkingPerfectedAffix");

        assertTrue(masterworking.contains("Aktualny doskonalony afiks"));
        assertTrue(selector.contains("<option value=\"\""));
        assertTrue(selector.contains("<option value=\"ORDINARY_AFFIX:STRENGTH\" selected>Siła</option>"));
        assertTrue(selector.contains("<option value=\"ORDINARY_AFFIX:FIRE_RESISTANCE\">Odporność na Ogień</option>"));
        assertTrue(selector.contains("<option value=\"TEMPERING_AFFIX:defense_max_animus\">Hartowanie: maksymalna liczba kumulacji Animuszu</option>"));
    }

    @Test
    void shouldRenderMasterworkingValuesInlineWithoutReplacingSourceInputs() {
        ItemImportEditableForm form = new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "20",
                "0",
                FullItemRead.empty(),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 225.0d, "", true, 0, "+225 siły", krys.itemimport.ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.FIRE_RESISTANCE, 787.0d, "", true, 1, "+787 do odporności na: Ogień", krys.itemimport.ImportedItemAffixSource.OCR),
                        new ImportedItemAffix(ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, "%", false, 2, "11,4% redukcji obrażeń", krys.itemimport.ImportedItemAffixSource.OCR)
                ),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                new ItemImportDetails("Tarcza testowa", "Tarcza", "LEGENDARY", true, EquipmentSlot.OFF_HAND,
                        900L, null, null, null, null, null, 1202L, ""),
                List.of(new ItemTemperingAffix(
                        "defense_max_animus",
                        TemperingCategory.DEFENSE,
                        5.0d,
                        "+5 do maksymalnej liczby kumulacji Animuszu",
                        TemperingRuntimeStatus.DATA_ONLY,
                        true
                )),
                new ItemMasterworking(25, 25, MasterworkedAffixSelection.ordinaryAffix("STRENGTH"))
        );

        String html = renderFullPage(form);

        assertFalse(html.contains("Podgląd wartości po Doskonaleniu"));
        assertFalse(html.contains("<h5>Wartości po Doskonaleniu</h5>"));
        assertFalse(html.contains("225 → 270"));
        assertFalse(html.contains("787 → 945"));
        assertFalse(html.contains("+5 → +7"));
        assertFalse(html.contains("po Doskonaleniu:"));
        assertFalse(html.contains("Aktualna wartość itemu:"));
        assertFalse(html.contains("Runtime nadal używa zapisanej wartości +5"));
        assertTrue(html.contains("name=\"itemArmor\" value=\"1202\""));
        assertTrue(html.contains("<div class=\"masterworking-current-value\"><span class=\"masterworking-value masterworking-value--upgraded\">1502</span>"));
        assertTrue(html.contains("name=\"affixValue_0\" value=\"225\""));
        assertTrue(html.contains("<div class=\"masterworking-current-value\"><span class=\"masterworking-value masterworking-value--perfected\">360</span>"));
        assertTrue(html.contains("Doskonalony afiks"));
        assertTrue(html.contains("name=\"affixValue_1\" value=\"787\""));
        assertTrue(html.contains("<div class=\"masterworking-current-value\"><span class=\"masterworking-value masterworking-value--upgraded\">945</span>"));
        assertTrue(html.contains("name=\"affixValue_2\" value=\"11.4\""));
        assertTrue(html.contains("<div class=\"masterworking-current-value\"><span class=\"masterworking-value masterworking-value--upgraded\">14,3%</span>"));
        assertTrue(html.contains("name=\"temperingValue_0\" value=\"5\""));
        assertTrue(html.contains("★ <span class=\"masterworking-value masterworking-value--upgraded\">+7</span> do maksymalnej liczby kumulacji Animuszu"));
    }

    @Test
    void shouldRenderOnlyDefenseAffixesForSelectedDefenseTemperingCategory() {
        ItemImportEditableForm form = shieldFormWithTempering(new ItemTemperingAffix(
                "defense_maximum_life",
                TemperingCategory.DEFENSE,
                1500.0d,
                "+1500 maksymalnego zdrowia",
                TemperingRuntimeStatus.DATA_ONLY
        ));

        String tempering = renderTemperingSection(form);

        assertTrue(tempering.contains("name=\"temperingDefinitionId_0\" value=\"defense_maximum_life\""));
        assertTrue(tempering.contains("Defensywa: maksymalnego zdrowia [1000 - 1500]"));
        assertTrue(tempering.contains("Defensywa: do maksymalnej liczby kumulacji Animuszu [2 - 3]"));
        assertEquals(12, countOccurrences(tempering, "\"id\":\"defense_"));
        assertTrue(tempering.contains("Limit hartowania dla tego przedmiotu został wykorzystany."));
        assertFalse(tempering.contains("<h4>Dodaj hartowanie</h4>"));
        assertFalse(tempering.contains("id=\"temperingAddControls\""));
        assertFalse(tempering.contains("name=\"newTemperingCategory\""));
        assertFalse(tempering.contains("name=\"newTemperingDefinitionId\""));
        assertFalse(tempering.contains("name=\"newTemperingValue\""));
        assertFalse(tempering.contains("name=\"newTemperingGreaterAffix\""));
        assertFalse(tempering.contains("id=\"addTemperingButton\""));
        assertFalse(tempering.contains("<select name=\"temperingDefinitionId_0\""));
        assertTrue(tempering.contains("tempering-existing-card"));
        assertTrue(tempering.contains("Usuń"));
        assertFalse(tempering.contains("<input type=\"number\" name=\"temperingValue_0\""));
        String card = existingTemperingCard(tempering);
        assertTrue(card.contains("+1500 maksymalnego zdrowia"));
        assertTrue(card.contains("Runtime nieaktywny"));
        assertFalse(card.contains("Wartość rolla"));
        assertFalse(card.contains("Greater Affix"));
        assertFalse(card.contains("Zakres / GA"));
        assertFalse(card.contains("Wartość GA:"));
        assertFalse(card.contains("Zakres:"));
        assertFalse(card.contains("[1000 - 1500]"));
    }

    @Test
    void shouldNotRenderDefenseAffixOptionsForSelectedUtilityTemperingCategory() {
        ItemImportEditableForm form = shieldFormWithTempering(new ItemTemperingAffix(
                "defense_maximum_life",
                TemperingCategory.UTILITY,
                1500.0d,
                "",
                TemperingRuntimeStatus.DATA_ONLY
        ));

        String tempering = renderTemperingSection(form);

        assertTrue(tempering.contains("Katalog affixów tej kategorii nie został jeszcze uzupełniony."));
        assertFalse(tempering.contains("<option value=\"defense_maximum_life\""));
        assertFalse(tempering.contains("<td>Defensywa: maksymalnego zdrowia"));
        assertFalse(tempering.contains("<td>Defensywa: pancerza"));
        assertFalse(tempering.contains("<td>Defensywa: do maksymalnej liczby kumulacji Animuszu"));
    }

    @Test
    void shouldRenderCompactSavedGreaterAffixTempering() {
        ItemImportEditableForm form = shieldFormWithTempering(new ItemTemperingAffix(
                "defense_max_animus",
                TemperingCategory.DEFENSE,
                5.0d,
                "+5 do maksymalnej liczby kumulacji Animuszu",
                TemperingRuntimeStatus.DATA_ONLY,
                true
        ));

        String tempering = renderTemperingSection(form);
        String card = existingTemperingCard(tempering);

        assertFalse(tempering.contains("undefined"));
        assertTrue(card.contains("Defensywa"));
        assertTrue(card.contains("★ +5 do maksymalnej liczby kumulacji Animuszu"));
        assertTrue(card.contains("Greater Affix"));
        assertTrue(card.contains("Runtime nieaktywny"));
        assertTrue(card.contains("Usuń"));
        assertFalse(card.contains("[2 - 3]"));
        assertFalse(card.contains("Zakres: 2 - 3"));
        assertFalse(card.contains("Wartość GA: 5"));
        assertFalse(card.contains("Wartość rolla"));
        assertFalse(card.contains("Zakres / GA"));
    }

    @Test
    void shouldRenderCompactSavedNormalTemperingWithoutGreaterAffixBadge() {
        ItemImportEditableForm form = shieldFormWithTempering(new ItemTemperingAffix(
                "defense_max_animus",
                TemperingCategory.DEFENSE,
                3.0d,
                "+3 do maksymalnej liczby kumulacji Animuszu",
                TemperingRuntimeStatus.DATA_ONLY
        ));

        String card = existingTemperingCard(renderTemperingSection(form));

        assertTrue(card.contains("+3 do maksymalnej liczby kumulacji Animuszu"));
        assertFalse(card.contains("Greater Affix"));
        assertFalse(card.contains("Zakres / GA"));
    }

    @Test
    void shouldKeepRangeAndGreaterAffixHelpInAddTemperingForm() {
        ItemImportEditableForm form = shieldForm(900L, List.of());

        String html = renderFullPage(form);

        assertTrue(html.contains("id=\"temperingAddControls\""));
        assertTrue(html.contains("Zakres: ${rangeLabel}"));
        assertTrue(html.contains("Wartość GA: ${greaterLabel}"));
        assertTrue(html.contains("\"id\":\"defense_max_animus\""));
        assertTrue(html.contains("\"rangeLabel\":\"2 - 3\""));
        assertTrue(html.contains("\"greaterLabel\":\"5\""));
    }

    @Test
    void shouldDisableTemperingGreaterAffixForItemPowerBelow900() {
        ItemImportEditableForm form = shieldForm(899L, List.of());

        String tempering = renderTemperingSection(form);

        assertTrue(tempering.contains("Greater Affix przy hartowaniu jest dostępny tylko dla przedmiotów o mocy 900."));
        assertTrue(tempering.contains("name=\"newTemperingGreaterAffix\" value=\"true\" disabled"));
        assertTrue(tempering.contains("data-greater-available=\"false\""));
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

    private static ItemImportEditableForm shieldFormWithTempering(ItemTemperingAffix temperingAffix) {
        return shieldForm(900L, List.of(temperingAffix));
    }

    private static ItemImportEditableForm shieldForm(Long itemPower, List<ItemTemperingAffix> temperingAffixes) {
        return new ItemImportEditableForm(
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
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                new ItemImportDetails("Tarcza testowa", "Tarcza", "LEGENDARY", true, EquipmentSlot.OFF_HAND,
                        itemPower, null, null, null, null, null, 1202L, ""),
                temperingAffixes
        );
    }

    private static String renderTemperingSection(ItemImportEditableForm form) {
        return sectionByHeading(renderFullPage(form), "Hartowanie");
    }

    private static void assertOutcomeFields(HoradricTransfigurationOutcome outcome,
                                            List<String> expected,
                                            List<String> rejected) {
        String section = sectionByHeading(renderFullPage(formWithTransfiguration(new ItemTransfiguration(
                true,
                true,
                HoradricTuningPrism.NONE,
                outcome,
                "DAMAGE_REDUCTION",
                new TransfigurationAffixRoll("ALL_STATS", 96.0d),
                "DAMAGE_REDUCTION",
                new TransfigurationAffixRoll("TOTAL_ARMOR_PERCENT", 10.0d),
                15,
                outcome == HoradricTransfigurationOutcome.INDESTRUCTIBLE,
                ""))), "Przeistoczenie / Kostka Horadrimów");
        String visibleFields = switch (outcome) {
            case UPGRADE_TO_GREATER_AFFIX, BONUS_TRANSFIGURATION_AFFIX,
                    REPLACE_EXISTING_AFFIX_WITH_TRANSFIGURATION_AFFIX, BONUS_ITEM_QUALITY -> outcomeGroup(section, outcome);
            case INDESTRUCTIBLE, UNKNOWN, NONE -> tagByDataAttribute(section, "data-transfiguration-outcome-fields");
        };
        for (String value : expected) {
            assertTrue(visibleFields.contains(value), value + "\n" + visibleFields);
        }
        for (String value : rejected) {
            assertFalse(visibleFields.contains(value), value + "\n" + visibleFields);
        }
    }

    private static String outcomeGroup(String section, HoradricTransfigurationOutcome outcome) {
        String marker = "data-transfiguration-outcome=\"" + outcome.name() + "\"";
        int markerIndex = section.indexOf(marker);
        if (markerIndex < 0) {
            throw new AssertionError("Brak grupy outcome: " + outcome);
        }
        int start = section.lastIndexOf("<div", markerIndex);
        int next = section.indexOf("<div class=\"transfiguration-grid transfiguration-dynamic-grid\"", markerIndex + marker.length());
        int end = next >= 0 ? next : section.indexOf("</div>\n</div>", markerIndex);
        if (start < 0 || end < 0) {
            throw new AssertionError("Nie udało się wyciąć grupy outcome: " + outcome);
        }
        return section.substring(start, end);
    }

    private static String tagByDataAttribute(String section, String attribute) {
        int markerIndex = section.indexOf(attribute);
        if (markerIndex < 0) {
            throw new AssertionError("Brak atrybutu: " + attribute);
        }
        int start = section.lastIndexOf("<", markerIndex);
        int end = section.indexOf(">", markerIndex);
        if (start < 0 || end < 0) {
            throw new AssertionError("Nie udało się wyciąć tagu: " + attribute);
        }
        return section.substring(start, end + 1);
    }

    private static ItemImportEditableForm formWithTransfiguration(ItemTransfiguration transfiguration) {
        return new ItemImportEditableForm(
                "tarcza.png",
                "OFF_HAND",
                "0",
                "0",
                "0",
                "0",
                "20",
                "0",
                FullItemRead.empty(),
                List.of(
                        new ImportedItemAffix(ImportedItemAffixType.STRENGTH, 225.0d),
                        new ImportedItemAffix(ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d)
                ),
                "",
                ItemImportFieldConfidence.UNKNOWN,
                "",
                ItemImportDetails.empty(),
                List.of(),
                ItemMasterworking.defaultState(),
                transfiguration
        );
    }

    private static String renderFullPage(ItemImportEditableForm form) {
        HeroProfile activeHero = new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty());
        return new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                null,
                List.of(),
                null,
                activeHero,
                "Import testowy",
                ""
        ));
    }

    private static String selectByName(String html, String name) {
        String marker = "name=\"" + name + "\"";
        int nameIndex = html.indexOf(marker);
        if (nameIndex < 0) {
            throw new AssertionError("Brak selecta: " + name);
        }
        int start = html.lastIndexOf("<select", nameIndex);
        int end = html.indexOf("</select>", nameIndex);
        if (start < 0 || end < 0) {
            throw new AssertionError("Nie udało się wyciąć selecta: " + name);
        }
        return html.substring(start, end + "</select>".length());
    }

    private static String sectionByHeading(String html, String heading) {
        int headingIndex = html.indexOf("<h3>" + heading + "</h3>");
        if (headingIndex < 0) {
            throw new AssertionError("Brak sekcji: " + heading);
        }
        int start = html.lastIndexOf("<section", headingIndex);
        int end = html.indexOf("</section>", headingIndex);
        if (start < 0 || end < 0) {
            throw new AssertionError("Nie udało się wyciąć sekcji: " + heading);
        }
        return html.substring(start, end + "</section>".length());
    }

    private static String existingTemperingCard(String html) {
        String marker = "<article class=\"tempering-existing-card\"";
        int start = html.indexOf(marker);
        if (start < 0) {
            throw new AssertionError("Brak kompaktowej karty hartowania");
        }
        int end = html.indexOf("</article>", start);
        if (end < 0) {
            throw new AssertionError("Nie udało się wyciąć karty hartowania");
        }
        return html.substring(start, end + "</article>".length());
    }
}

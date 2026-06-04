package krys.itemimport;

import krys.item.EquipmentSlot;
import krys.socketing.SocketContentType;
import krys.transfiguration.HoradricTransfigurationOutcome;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje uzupełnianie formularza importu na podstawie pełnego odczytu OCR. */
class ItemImportEditableFormFactoryTest {
    private final ItemImportEditableFormFactory factory = new ItemImportEditableFormFactory();

    @Test
    void shouldRecognizeTransfigurationByTextWithoutRejectingValueOutsideCatalogRange() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Hełm",
                "Starożytny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 004 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+115 pkt. do wszystkich współczynników [75 - 100]")
                ),
                details("Generyczny Hełm", "")
        )));

        assertEquals(HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX, form.getTransfiguration().getOutcome());
        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(115.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue(), 0.0001d);
        assertEquals(75.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMin(), 0.0001d);
        assertEquals(100.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMax(), 0.0001d);
    }

    @Test
    void shouldUseTransfigurationValueLocalToAnchorInJoinedOcrLine() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Hełm",
                "Starożytny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 004 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX,
                                "+3 do umiejętności: Główne [3] +115 pkt. do wszystkich współczynników [75 - 100]")
                ),
                details("Generyczny Hełm", "")
        )));

        assertEquals(3.0d, affix(form, ImportedItemAffixType.CORE_SKILL_RANKS).getValue(), 0.0001d);
        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(115.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue(), 0.0001d);
        assertEquals(75.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMin(), 0.0001d);
        assertEquals(100.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMax(), 0.0001d);
    }

    @Test
    void shouldRecoverGluedFlatTransfigurationValueFromLocalOcrToken() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Hełm",
                "Starożytny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900 25 (+25) jakości",
                "2 004 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX,
                                "+3 do umiejętności: Główne [31 4115 pkt. do wszystkich współczynników +175 - 1001")
                ),
                details("Generyczny Hełm", "")
        )));

        assertEquals(3.0d, affix(form, ImportedItemAffixType.CORE_SKILL_RANKS).getValue(), 0.0001d);
        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(115.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue(), 0.0001d);
        assertEquals(75.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMin(), 0.0001d);
        assertEquals(100.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMax(), 0.0001d);
    }

    @Test
    void shouldNotUsePreviousAffixValueWhenTransfigurationLocalValueIsUnsafe() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Hełm",
                "Starożytny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 004 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX,
                                "+3 do umiejętności: Główne [3] do wszystkich współczynników [75 - 100]")
                ),
                details("Generyczny Hełm", "")
        )));

        assertEquals(3.0d, affix(form, ImportedItemAffixType.CORE_SKILL_RANKS).getValue(), 0.0001d);
        assertEquals(HoradricTransfigurationOutcome.UNKNOWN, form.getTransfiguration().getOutcome());
        assertNull(form.getTransfiguration().getAddedTransfigurationAffix());
    }

    @Test
    void shouldKeepArbitraryAllStatsTransfigurationValueOutsideCatalogRange() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Hełm",
                "Starożytny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 004 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+999 pkt. do wszystkich współczynników [75 - 100]")
                ),
                details("Generyczny Hełm", "")
        )));

        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(999.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue(), 0.0001d);
        assertEquals(75.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMin(), 0.0001d);
        assertEquals(100.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMax(), 0.0001d);
    }

    @Test
    void shouldImportDifferentTransfigurationValueOutsideCatalogRangeWhenTextMatchesDefinition() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczne Buty",
                "Starożytne unikatowe buty",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "1 000 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+47% szybkość ruchu [20 - 30]")
                ),
                details("Generyczne Buty", "")
        )));

        assertEquals("MOVEMENT_SPEED", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(47.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue(), 0.0001d);
        assertEquals(20.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMin(), 0.0001d);
        assertEquals(30.0d, form.getTransfiguration().getAddedTransfigurationAffix().getSourceRangeMax(), 0.0001d);
    }

    @Test
    void shouldUseCanonicalAspectTextFromRegistryForMatchedUniqueEffect() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Dziedzic Zatracenia",
                "Starożytny mityczny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 004 pkt. pancerza",
                List.of(new FullItemReadLine(
                        FullItemReadLineType.ASPECT,
                        "Poddaj się nienawiści i doświadcz Łaski Matki, która zwiększy zadawane przez ciebie obrażenia o"
                )),
                details("Dziedzic Zatracenia", "Poddaj się nienawiści i doświadcz Łaski Matki")
        )));

        assertEquals("heir_of_perdition", form.getSelectedAspectId());
        assertTrue(form.getUniqueEffectText().contains("80%[x]"), form.getUniqueEffectText());
        assertEquals(AspectRuntimeStatus.DESCRIPTIVE_ONLY,
                ApplicationAspectRegistry.get().findById(form.getSelectedAspectId()).orElseThrow().getRuntimeStatus());
    }

    @Test
    void shouldPreserveSocketGemRuneStatsAsOccupiedSocketData() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Hełm",
                "Starożytny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 004 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.SOCKET, "+150 siły"),
                        new FullItemReadLine(FullItemReadLineType.SOCKET, "+120 siły")
                ),
                details("Generyczny Hełm", "")
        )));

        assertTrue(form.getAffixes().stream().noneMatch(affix -> affix.getType() == ImportedItemAffixType.STRENGTH));
        assertEquals(2, form.getSocketing().getSocketCount());
        assertEquals(2, form.getSocketing().getOccupiedSocketCount());
        assertEquals(0, form.getSocketing().getEmptySocketCount());
        assertEquals(SocketContentType.DETECTED_STAT, form.getSocketing().socketAt(0).getContentType());
        assertEquals("+150 siły", form.getSocketing().socketAt(0).getDetectedStat().getDisplayText());
        assertEquals(ImportedItemAffixType.STRENGTH, form.getSocketing().socketAt(0).getDetectedStat().getMatchedAffixType());
        assertEquals("DATA_ONLY", form.getSocketing().socketAt(0).getDetectedStat().getRuntimeStatus());
        assertEquals("+120 siły", form.getSocketing().socketAt(1).getDetectedStat().getDisplayText());
    }

    @Test
    void shouldSumEmptyAndOccupiedSockets() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Hełm",
                "Starożytny unikatowy hełm",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 004 pkt. pancerza",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.SOCKET, "Puste gniazdo"),
                        new FullItemReadLine(FullItemReadLineType.SOCKET, "+120 inteligencji")
                ),
                details("Generyczny Hełm", "")
        )));

        assertEquals(2, form.getSocketing().getSocketCount());
        assertEquals(1, form.getSocketing().getOccupiedSocketCount());
        assertEquals(1, form.getSocketing().getEmptySocketCount());
        assertEquals("+120 inteligencji", form.getSocketing().socketAt(0).getDetectedStat().getDisplayText());
    }

    @Test
    void shouldPreserveNonStrengthSocketGemRuneStats() {
        for (String text : List.of("+120 inteligencji", "+120 zręczności", "+120 siły woli", "+500 pkt. pancerza")) {
            ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                    "Generyczny Hełm",
                    "Starożytny unikatowy hełm",
                    "UNIQUE",
                    "Moc przedmiotu: 900",
                    "2 004 pkt. pancerza",
                    List.of(new FullItemReadLine(FullItemReadLineType.SOCKET, text)),
                    details("Generyczny Hełm", "")
            )));

            assertTrue(form.getAffixes().isEmpty(), text);
            assertEquals(1, form.getSocketing().getOccupiedSocketCount(), text);
            assertEquals(text, form.getSocketing().socketAt(0).getDetectedStat().getDisplayText(), text);
        }
    }

    @Test
    void shouldMoveCatalogTemperingLineAfterOrdinaryAffixCapOutOfOrdinaryAffixes() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Miecz",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 417 pkt. obrażeń na sek.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+314 obrażeń od broni"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "★ +270 siły"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "★ +948 pkt. zdrowia przy trafieniu"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Mnożnik x15% wszystkich obrażeń"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+7,5% szansy na trafienie krytyczne")
                ),
                details("Generyczny Miecz", "")
        )));

        assertEquals(4, form.getAffixes().size());
        assertEquals(ImportedItemAffixType.WEAPON_DAMAGE_FLAT, form.getAffixes().get(0).getType());
        assertEquals(ImportedItemAffixType.STRENGTH, form.getAffixes().get(1).getType());
        assertEquals(ImportedItemAffixType.LIFE_ON_HIT, form.getAffixes().get(2).getType());
        assertEquals(ImportedItemAffixType.ALL_DAMAGE_MULTIPLIER, form.getAffixes().get(3).getType());
        assertTrue(form.getAffixes().get(1).isGreaterAffix());
        assertTrue(form.getAffixes().get(2).isGreaterAffix());
        assertFalse(form.getAffixes().stream().anyMatch(affix -> affix.getType() == ImportedItemAffixType.CRITICAL_STRIKE_CHANCE));
        assertEquals(1, form.getTemperingAffixes().size());
        assertEquals("offense_critical_strike_chance", form.getTemperingAffixes().getFirst().getDefinitionId());
        assertEquals(7.5d, form.getTemperingAffixes().getFirst().getValue(), 0.0001d);
    }

    @Test
    void shouldUseVisualAnchorOrderInsteadOfSelectedTextCandidateOrder() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Miecz",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 417 pkt. obrażeń na sek.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "★ +314 obrażeń od broni uszkodzony wariant 999 888"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+270 siły"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+948 pkt. zdrowia przy trafieniu"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Mnożnik x15% wszystkich obrażeń"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+314 obrażeń od broni")
                ),
                details("Generyczny Miecz", "")
        )));

        assertEquals(4, form.getAffixes().size());
        assertEquals(ImportedItemAffixType.WEAPON_DAMAGE_FLAT, form.getAffixes().get(0).getType());
        assertEquals(ImportedItemAffixType.STRENGTH, form.getAffixes().get(1).getType());
        assertEquals(ImportedItemAffixType.LIFE_ON_HIT, form.getAffixes().get(2).getType());
        assertEquals(ImportedItemAffixType.ALL_DAMAGE_MULTIPLIER, form.getAffixes().get(3).getType());
        ImportedItemAffix weaponDamage = form.getAffixes().getFirst();
        assertEquals("+314 obrażeń od broni", weaponDamage.getSourceText());
        assertTrue(weaponDamage.getVisualSourceText().contains("uszkodzony wariant"));
        assertTrue(weaponDamage.getVisualDisplayOrder() < weaponDamage.getDisplayOrder());
        assertTrue(weaponDamage.isGreaterAffix());
    }

    @Test
    void shouldPropagateGreaterAffixMarkerFromVisualAnchorToSelectedCandidate() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Miecz",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 417 pkt. obrażeń na sek.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "★ +314 obrażeń od broni uszkodzony wariant 999 888"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+314 obrażeń od broni")
                ),
                details("Generyczny Miecz", "")
        )));

        ImportedItemAffix weaponDamage = affix(form, ImportedItemAffixType.WEAPON_DAMAGE_FLAT);
        assertEquals("+314 obrażeń od broni", weaponDamage.getSourceText());
        assertTrue(weaponDamage.getVisualSourceText().contains("★ +314"));
        assertTrue(weaponDamage.isGreaterAffix());
        assertFalse(weaponDamage.isGreaterAffixConfirmationRequired());
    }

    @Test
    void shouldPropagateLocalGreaterAffixMarkerFromDirectLine() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Miecz",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 417 pkt. obrażeń na sek.",
                List.of(new FullItemReadLine(FullItemReadLineType.AFFIX, "★ +270 siły")),
                details("Generyczny Miecz", "")
        )));

        assertEquals(1, form.getAffixes().size());
        assertEquals(ImportedItemAffixType.STRENGTH, form.getAffixes().getFirst().getType());
        assertTrue(form.getAffixes().getFirst().isGreaterAffix());
        assertFalse(form.getAffixes().getFirst().isGreaterAffixConfirmationRequired());
    }

    @Test
    void shouldKeepIndestructibleAsTransfigurationAndPhysicalMultiplierAsSocketGemRuneData() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Miecz",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 417 pkt. obrażeń na sek.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.OTHER, "Niezniszczalność"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Mnożnik x32% obrażeń (Fizyczne)")
                ),
                details("Generyczny Miecz", "")
        )));

        assertTrue(form.getTransfiguration().isTransfigured());
        assertEquals(HoradricTransfigurationOutcome.INDESTRUCTIBLE, form.getTransfiguration().getOutcome());
        assertNull(form.getTransfiguration().getAddedTransfigurationAffix());
        assertEquals(1, form.getSocketing().getOccupiedSocketCount());
        assertEquals("Mnożnik x32% obrażeń (Fizyczne)", form.getSocketing().socketAt(0).getDetectedStat().getDisplayText());
        assertEquals(32.0d, form.getSocketing().socketAt(0).getDetectedStat().getValue(), 0.0001d);
        assertEquals("PHYSICAL", form.getSocketing().socketAt(0).getDetectedStat().getDamageType());
        assertTrue(form.getAffixes().isEmpty());
    }

    @Test
    void shouldDeduplicatePhysicalSocketGemRuneStatsBySemanticKeyAndPreferCleanSourceLine() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Miecz",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 417 pkt. obrażeń na sek.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.OTHER, "Niezniszczalność"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Mnożnik x32% obrażeń (Fizyczne)"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "MNOZNIK X32% OBRAZEN (FIZYCZNE) TO OSTRZE W REKACH ANIOLA")
                ),
                details("Generyczny Miecz", "")
        )));

        assertEquals(1, form.getSocketing().getSocketCount());
        assertEquals(1, form.getSocketing().getOccupiedSocketCount());
        assertEquals(1, form.getSocketing().getDetectedStats().size());
        assertEquals("Mnożnik x32% obrażeń (Fizyczne)", form.getSocketing().socketAt(0).getDetectedStat().getDisplayText());
        assertEquals("PHYSICAL_DAMAGE_MULTIPLIER|32|PHYSICAL|DATA_ONLY",
                form.getSocketing().socketAt(0).getDetectedStat().getSemanticKey());
    }

    @Test
    void shouldBindPhysicalSocketMultiplierToLocalXValueOnly() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Miecz",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 417 pkt. obrażeń na sek.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.OTHER, "Niezniszczalność"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX,
                                "Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100] ale dodatkowo Mnożnik x32% obrażeń (Fizyczne)")
                ),
                details("Generyczny Miecz", "")
        )));

        assertEquals(1, form.getSocketing().getDetectedStats().size());
        assertEquals(32.0d, form.getSocketing().socketAt(0).getDetectedStat().getValue(), 0.0001d);
        assertEquals("Mnożnik x32% obrażeń (Fizyczne)", form.getSocketing().socketAt(0).getDetectedStat().getDisplayText());
        assertFalse(form.getSocketing().getDetectedStats().stream().anyMatch(stat -> stat.getValue() == 100.0d));
        assertFalse(form.getSocketing().getDetectedStats().stream().anyMatch(stat -> stat.getValue() == 70.0d));
        assertFalse(form.getSocketing().getDetectedStats().stream().anyMatch(stat -> stat.getValue() == 170.0d));
    }

    @Test
    void shouldRejectSocketX170FalsePositiveFromUniqueEffectTail() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Miecz",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 417 pkt. obrażeń na sek.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Przeistoczony"),
                        new FullItemReadLine(FullItemReadLineType.OTHER, "Niezniszczalność"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX,
                                "zwiększone 0 170 przez efekt unikatowy MNOZNIK X32% OBRAZEN (FIZYCZNE) TO OSTRZE W REKACH ANIOLA")
                ),
                details("Generyczny Miecz", "")
        )));

        assertEquals(1, form.getSocketing().getSocketCount());
        assertEquals(1, form.getSocketing().getOccupiedSocketCount());
        assertEquals(1, form.getSocketing().getDetectedStats().size());
        assertEquals(32.0d, form.getSocketing().socketAt(0).getDetectedStat().getValue(), 0.0001d);
        assertEquals("PHYSICAL", form.getSocketing().socketAt(0).getDetectedStat().getDamageType());
        assertFalse(form.getSocketing().getDetectedStats().stream().anyMatch(stat -> stat.getValue() == 170.0d));
    }

    @Test
    void shouldMarkMasterworkingAsRequiringConfirmationWhenGaMarkersAreGlobalOnly() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Miecz",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 417 pkt. obrażeń na sek.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.OTHER, "25 (+25) jakości"),
                        new FullItemReadLine(FullItemReadLineType.OTHER, "★"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+270 siły")
                ),
                details("Generyczny Miecz", "")
        )));

        assertEquals(1, form.getAffixes().size());
        assertFalse(form.getAffixes().getFirst().isGreaterAffix());
        assertEquals("REQUIRES_CONFIRMATION", form.getMasterworking().getPerfectedAffix().getRawSource());
        assertEquals("ambiguous_ga_markers", form.getMasterworking().getPerfectedAffix().getKey());
        assertTrue(form.getAffixes().getFirst().isGreaterAffixConfirmationRequired());
    }

    @Test
    void shouldPrecheckProbableGreaterAffixesWhenTypedOrdinaryBlockHasGlobalMarker() {
        FullItemReadLine marker = new FullItemReadLine(FullItemReadLineType.AFFIX, "★", typedSource("★", 0));
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Miecz",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 417 pkt. obrażeń na sek.",
                List.of(
                        marker,
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+314 obrażeń od broni", typedSource("+314 obrażeń od broni", 1)),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+270 siły", typedSource("+270 siły", 2)),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+948 pkt. zdrowia przy trafieniu", typedSource("+948 pkt. zdrowia przy trafieniu", 3)),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Mnożnik x15% wszystkich obrażeń", typedSource("Mnożnik x15% wszystkich obrażeń", 4))
                ),
                details("Generyczny Miecz", "")
        )));

        assertEquals(4, form.getAffixes().size());
        for (ImportedItemAffix affix : form.getAffixes()) {
            assertTrue(affix.isGreaterAffix(), affix.getType().name());
            assertTrue(affix.isGreaterAffixConfirmationRequired(), affix.getType().name());
        }
    }

    @Test
    void shouldUseSelectedTemperingSourceLineAndRejectArtifactCandidate() {
        ItemImportEditableForm form = factory.create(parseResult(new FullItemRead(
                "Generyczny Miecz",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "2 417 pkt. obrażeń na sek.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+314 obrażeń od broni"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+270 siły"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+948 pkt. zdrowia przy trafieniu"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "Mnożnik x15% wszystkich obrażeń"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+47,5% szansy na trafienie krytyczne"),
                        new FullItemReadLine(FullItemReadLineType.AFFIX, "+7,5% szansy na trafienie krytyczne")
                ),
                details("Generyczny Miecz", "")
        )));

        assertEquals(1, form.getTemperingAffixes().size());
        assertEquals("offense_critical_strike_chance", form.getTemperingAffixes().getFirst().getDefinitionId());
        assertEquals(7.5d, form.getTemperingAffixes().getFirst().getValue(), 0.0001d);
        assertEquals("+7,5% szansy na trafienie krytyczne", form.getTemperingAffixes().getFirst().getSourceLine());
        assertFalse(form.getTemperingAffixes().getFirst().getSourceLine().contains("47,5"));
    }

    private static ItemImageImportCandidateParseResult parseResult(FullItemRead fullItemRead) {
        return new ItemImageImportCandidateParseResult(
                new ItemImageMetadata("test.png", "image/png", "png", 1, 1),
                fullItemRead,
                new ItemImportFieldCandidate<>("", EquipmentSlot.HELMET, ItemImportFieldConfidence.HIGH, ""),
                ItemImportFieldCandidate.unknown(""),
                ItemImportFieldCandidate.unknown(""),
                ItemImportFieldCandidate.unknown(""),
                ItemImportFieldCandidate.unknown(""),
                ItemImportFieldCandidate.unknown(""),
                ItemImportFieldCandidate.unknown(""),
                ""
        );
    }

    private static ItemImportDetails details(String itemName, String effectText) {
        return new ItemImportDetails(itemName, "Hełm", "UNIQUE", true, EquipmentSlot.HELMET,
                900L, null, null, null, null, null, 2004L, effectText, itemName.equals("Dziedzic Zatracenia"));
    }

    private static FullItemReadLineSource typedSource(String line, int lineOrder) {
        return new FullItemReadLineSource(
                0,
                "typed-test",
                lineOrder,
                0,
                line.length(),
                line,
                line,
                "ORDINARY_AFFIX_REGION",
                lineOrder,
                lineOrder,
                0,
                line.length(),
                line
        );
    }

    private static ImportedItemAffix affix(ItemImportEditableForm form, ImportedItemAffixType type) {
        return form.getAffixes().stream()
                .filter(affix -> affix.getType() == type)
                .findFirst()
                .orElseThrow();
    }
}

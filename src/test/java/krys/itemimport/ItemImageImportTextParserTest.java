package krys.itemimport;

import krys.hero.HeroClass;
import krys.item.EquipmentSlot;
import krys.itemlibrary.FileItemLibraryRepository;
import krys.itemlibrary.ItemLibraryService;
import krys.itemlibrary.SavedImportedItem;
import krys.masterworking.MasterworkedAffixSelection;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingCategory;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.TransfigurationValueProvenance;
import krys.web.HeroItemSelection;
import krys.web.HeroProfile;
import krys.web.ItemImportPageModel;
import krys.web.ItemImportPageRenderer;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje polskie frazy OCR dla foundation importu itemu. */
class ItemImageImportTextParserTest {
    private final ItemImageImportTextParser parser = new ItemImageImportTextParser();
    private final ItemImageMetadata metadata = new ItemImageMetadata("shield.png", "image/png", "PNG", 1200, 1600);

    @Test
    void shouldRecognizePolishShieldSlotAndFoundationAffixes() {
        String ocrText = """
                Tarcza
                +114 do siły
                +494 do cierni
                +20,0% szansy na blok
                """;

        ItemImageImportCandidateParseResult result = parser.parse(metadata, ocrText);

        assertEquals(EquipmentSlot.OFF_HAND, result.getSlotCandidate().getSuggestedValue());
        assertEquals(ItemImportFieldConfidence.HIGH, result.getSlotCandidate().getConfidence());
        assertEquals(114.0d, result.getStrengthCandidate().getSuggestedValue());
        assertEquals(494.0d, result.getThornsCandidate().getSuggestedValue());
        assertEquals(20.0d, result.getBlockChanceCandidate().getSuggestedValue());
        assertNull(result.getIntelligenceCandidate().getSuggestedValue());
        assertNull(result.getRetributionChanceCandidate().getSuggestedValue());
    }

    @Test
    void shouldPreferMainRollOutsideReferenceRangeForPolishFoundationAffixes() {
        String ocrText = """
                Tarcza
                +114 do siły [107 - 121]
                +494 do cierni [473 - 506]
                +20,0% szansy na blok [18,0 - 22,5]
                """;

        ItemImageImportCandidateParseResult result = parser.parse(metadata, ocrText);

        assertEquals(EquipmentSlot.OFF_HAND, result.getSlotCandidate().getSuggestedValue());
        assertEquals(114.0d, result.getStrengthCandidate().getSuggestedValue());
        assertEquals(494.0d, result.getThornsCandidate().getSuggestedValue());
        assertEquals(20.0d, result.getBlockChanceCandidate().getSuggestedValue());
    }

    @Test
    void shouldIgnoreShieldBaseArmorWhenAffixRollIsMissingFromOcrLine() {
        String ocrText = """
                Tarcza
                1 131 pkt. pancerza do siły [107 - 121]
                1 131 pkt. pancerza do cierni [473 - 506]
                1 131 pkt. pancerza szansy na blok [18,0 - 22,5]
                """;

        ItemImageImportCandidateParseResult result = parser.parse(metadata, ocrText);

        assertEquals(EquipmentSlot.OFF_HAND, result.getSlotCandidate().getSuggestedValue());
        assertNull(result.getStrengthCandidate().getSuggestedValue());
        assertNull(result.getThornsCandidate().getSuggestedValue());
        assertNull(result.getBlockChanceCandidate().getSuggestedValue());
    }

    @Test
    void shouldRecognizeShieldRollsWithoutLeakingBaseArmorIntoAffixes() {
        String ocrText = """
                Tarcza
                1 131 pkt. pancerza
                +114 do siły [107 - 121]
                +494 cierni [473 - 506]
                +20,0% szansy na blok [18,0 - 22,5]
                """;

        ItemImageImportCandidateParseResult result = parser.parse(metadata, ocrText);

        assertEquals(EquipmentSlot.OFF_HAND, result.getSlotCandidate().getSuggestedValue());
        assertEquals(114.0d, result.getStrengthCandidate().getSuggestedValue());
        assertEquals(494.0d, result.getThornsCandidate().getSuggestedValue());
        assertEquals(20.0d, result.getBlockChanceCandidate().getSuggestedValue());
    }

    @Test
    void shouldExtractMultilineShieldNameWithoutTreatingNameTarczaAsTypeLine() {
        ItemImageImportCandidateParseResult result = parser.parse(metadata, """
                MIAŻDŻĄCA TARCZA
                KOŚCIANYCH ŁUSEK
                Starożytna legendarna tarcza
                Moc przedmiotu: 900
                Pancerz: 1 202 pkt.
                """);

        assertEquals("Miażdżąca Tarcza Kościanych Łusek", result.getFullItemRead().getDetails().getItemName());
        assertEquals("Starożytna legendarna tarcza", result.getFullItemRead().getItemTypeLine());
        assertEquals("Tarcza", result.getFullItemRead().getDetails().getItemType());
        assertEquals(1202L, result.getFullItemRead().getDetails().getItemArmor());
    }

    @Test
    void shouldPreferCleanerShieldNameCandidateOverSuspiciousMixedOcrTitle() {
        ItemImageImportCandidateParseResult noisyVariant = parser.parse(metadata, """
                mŁAŻDŻĄCA TARCZA KOŚCIANYCH ŁUSEK
                Starożytna legendarna tarcza
                """);
        ItemImageImportCandidateParseResult cleanVariant = parser.parse(metadata, """
                MIAŻDŻĄCA TARCZA KOŚCIANYCH ŁUSEK
                Starożytna legendarna tarcza
                """);

        ItemImageImportCandidateParseResult merged = new ItemImageImportCandidateMerger().merge(
                metadata,
                2,
                List.of(noisyVariant, cleanVariant)
        );

        assertEquals("Miażdżąca Tarcza Kościanych Łusek", merged.getFullItemRead().getDetails().getItemName());
        assertFalse(merged.getFullItemRead().getDetails().getItemName().contains("mŁAŻ"));
    }

    @Test
    void shouldNormalizeShieldBaselineLinesForUserFacingForm() {
        ItemImageImportCandidateParseResult result = parser.parse(metadata, """
                Generyczna Tarcza
                Legendarna tarcza
                20,0% szansy na blok
                +100% obrażeń od broni w głównej ręce [1001
                """);

        assertLineText(result, FullItemReadLineType.IMPLICIT, "20,0% szansy na blok [20,0]%");
        assertLineText(result, FullItemReadLineType.IMPLICIT, "+100% obrażeń od broni w głównej ręce [100]%");
        assertFalse(result.getFullItemRead().getLines().stream()
                .map(FullItemReadLine::getText)
                .anyMatch(line -> line.contains("[1001")));
    }

    @Test
    void shouldExtractNestorskaEgidaAsNameNotAspectOrType() {
        ItemImageImportCandidateParseResult result = parser.parse(metadata, """
                NESTORSKA EGIDA WEWNĘTRZNEGO SPOKOJU
                Starożytna legendarna tarcza
                Moc przedmiotu: 800
                1 131 pkt. pancerza
                Zadajesz obrażenia zwiększone o 11,0%[x] [5,0 - 13,0]%.
                Ta premia jest trzy razy większa, jeśli stoisz w bezruchu przez co najmniej 3 sek.
                """);
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals("Nestorska Egida Wewnętrznego Spokoju", result.getFullItemRead().getDetails().getItemName());
        assertEquals("Starożytna legendarna tarcza", result.getFullItemRead().getItemTypeLine());
        assertEquals("inner-calm", form.getSelectedAspectId());
        assertFalse(result.getFullItemRead().getLines().stream()
                .filter(line -> line.getText().contains("NESTORSKA"))
                .anyMatch(line -> line.getType() == FullItemReadLineType.ASPECT || line.getType() == FullItemReadLineType.TYPE_OR_SLOT));
    }

    @Test
    void shouldPreserveGreaterAffixFromAnyOcrVariantWithoutFixtureName() {
        ItemImageImportCandidateParseResult plainVariant = parser.parse(metadata, """
                Generyczna Tarcza Testowa
                Legendarna tarcza
                +225 siły
                """);
        ItemImageImportCandidateParseResult starredVariant = parser.parse(metadata, """
                Inna Tarcza Testowa
                Legendarna tarcza
                * +225 siły
                """);

        ItemImageImportCandidateParseResult merged = new ItemImageImportCandidateMerger().merge(
                metadata,
                2,
                List.of(plainVariant, starredVariant)
        );
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(merged);

        assertEquals(1, form.getAffixes().size());
        assertEquals(ImportedItemAffixType.STRENGTH, form.getAffixes().getFirst().getType());
        assertTrue(form.getAffixes().getFirst().isGreaterAffix());
    }

    @Test
    void shouldNotInferGreaterAffixWhenNoOcrVariantHasStarMarker() {
        ItemImageImportCandidateParseResult result = parser.parse(metadata, """
                MIAŻDŻĄCA TARCZA KOŚCIANYCH ŁUSEK
                Starożytna legendarna tarcza
                +225 siły
                +490 do odporności na wszystkie żywioły
                +787 do odporności na: Ogień
                """);
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals(3, form.getAffixes().size());
        assertTrue(form.getAffixes().stream().noneMatch(ImportedItemAffix::isGreaterAffix));
    }

    @Test
    void shouldUseKoscianychLusekSnapshotContextForStableGreaterAffixesWhenMarkerIsMissing() {
        ItemImageImportCandidateParseResult result = parser.parse(metadata, """
                MIAŻDŻĄCA TARCZA KOŚCIANYCH ŁUSEK
                Starożytna legendarna tarcza
                Moc przedmiotu: 900
                1 202 pkt. pancerza
                +225 siły
                +490 do odporności na wszystkie żywioły
                +787 do odporności na: Ogień
                11,4% redukcji obrażeń [11,0 - 15,0]
                Gdy masz umocnienie, zadajesz obrażenia zwiększone 0 610/01x] [45 - 651%. 70 poziomu
                """);
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals(4, form.getAffixes().size());
        assertAffixGreaterFlag(form, ImportedItemAffixType.STRENGTH, true);
        assertAffixGreaterFlag(form, ImportedItemAffixType.ALL_RESISTANCE, true);
        assertAffixGreaterFlag(form, ImportedItemAffixType.FIRE_RESISTANCE, true);
        assertAffixGreaterFlag(form, ImportedItemAffixType.DAMAGE_REDUCTION, false);
    }

    @Test
    void shouldParseFortifyAspectRollGenericallyAndNeverSelectInnerCalm() {
        assertFortifyAspectRoll("Gdy masz umocnienie, zadajesz obrażenia zwiększone o 58%[x] [45 - 65]%.", "58%[x]");
        assertFortifyAspectRoll("Gdy masz umocnienie, zadajesz obrażenia zwiększone o 610[x] [45 - 65]%. 70 poziomu", "61%[x]");
        assertFortifyAspectRoll("Gdy masz umocnienie, zadajesz obrażenia zwiększone o 550[x] [45 - 65]%. 70 poziomu", "55%[x]");
    }

    @Test
    void shouldNormalizeRealBrokenFortifyAspectFromShieldScreenshot() {
        String brokenAspect = "Gdy masz umocnienie, zadajesz obrażenia zwiększone 0 610/01x] [45 - 651%. 70 poziomu";

        ItemImageImportCandidateParseResult result = parser.parse(metadata, """
                Generyczna Tarcza Próbna
                Legendarna tarcza
                %s
                """.formatted(brokenAspect));
        String normalized = result.getFullItemRead().getDetails().getUniqueEffectText();
        ItemImportDetails details = new ItemImportDetails(
                "Generyczna Tarcza Próbna",
                "Tarcza",
                "LEGENDARY",
                true,
                EquipmentSlot.OFF_HAND,
                900L,
                null,
                null,
                null,
                null,
                null,
                1202L,
                brokenAspect
        );

        assertTrue(normalized.contains("61%[x]"), normalized);
        assertTrue(normalized.contains("45 - 65"), normalized);
        assertFalse(normalized.contains("610"), normalized);
        assertFalse(normalized.contains("651"), normalized);
        assertFalse(normalized.contains("70 poziomu"), normalized);
        assertEquals(normalized, details.getUniqueEffectText());
    }

    @Test
    void shouldNormalizeKnownKoscianychLusekShieldNameOcrVariants() {
        for (String nameLine : List.of(
                "MIAŻDŻĄCA TARCZA KOŚCIANYCH L U Sek *",
                "MIAŻDŻĄCA TARCZA KOŚCIANYCH LUSEK"
        )) {
            ItemImageImportCandidateParseResult result = parser.parse(metadata, """
                    %s
                    Starożytna legendarna tarcza
                    Moc przedmiotu: 900
                    """.formatted(nameLine));

            assertEquals("Miażdżąca Tarcza Kościanych Łusek",
                    result.getFullItemRead().getDetails().getItemName(), nameLine);
        }
    }

    @Test
    void shouldRemoveKnownOcrUiNoiseFromFortifyAspectRange() {
        ItemImageImportCandidateParseResult result = parser.parse(metadata, """
                Generyczna Tarcza Próbna
                Legendarna tarcza
                Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - Przewiń w dół 65]%.
                """);

        assertEquals("Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%.",
                result.getFullItemRead().getDetails().getUniqueEffectText());
    }

    @Test
    void shouldImportTemperedMaxAnimusFromHardenedShieldScreenshot() {
        ItemImageImportCandidateParseResult result = parser.parse(metadata, hardenedShieldRawText());
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals("Miażdżąca Tarcza Kościanych Łusek", form.getItemName());
        assertEquals(1202L, result.getFullItemRead().getDetails().getItemArmor());
        assertEquals(900L, result.getFullItemRead().getDetails().getItemPower());
        assertEquals(1, form.getTemperingAffixes().size());
        ItemTemperingAffix tempering = form.getTemperingAffixes().getFirst();
        assertEquals(TemperingCategory.DEFENSE, tempering.getCategory());
        assertEquals("defense_max_animus", tempering.getDefinitionId());
        assertEquals(5.0d, tempering.getValue());
        assertTrue(tempering.isGreaterAffix());
        assertLineText(result, FullItemReadLineType.TEMPERING, "★ +5 do maksymalnej liczby kumulacji Animuszu");

        String effectText = result.getFullItemRead().getDetails().getUniqueEffectText();
        assertEquals("Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%.", effectText);
        assertFalse(effectText.contains("maksymalnej liczby kumulacji Animuszu"));
        assertFalse(effectText.contains("+5"));
        assertFalse(effectText.contains("★"));

        assertEquals(4, form.getAffixes().size());
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.STRENGTH, 225.0d, true);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.ALL_RESISTANCE, 490.0d, true);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.FIRE_RESISTANCE, 787.0d, true);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, false);
        assertTrue(form.getAffixes().stream()
                .noneMatch(affix -> affix.getSourceText().contains("maksymalnej liczby kumulacji Animuszu")));
    }

    @Test
    void shouldImportRealTransfiguredMasterworkedShieldFromMergedScreens() {
        String mergedText = new ItemScreenshotTextMerger().merge(List.of(
                ItemImportTextFixtures.realShieldTopText(),
                ItemImportTextFixtures.realShieldBottomText()
        ));
        ItemImageImportCandidateParseResult result = parser.parse(metadata, mergedText);
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals("Miażdżąca Tarcza Kościanych Łusek", form.getItemName());
        assertEquals("Tarcza", form.getItemType());
        assertEquals("Starożytna legendarna tarcza", result.getFullItemRead().getItemTypeLine());
        assertEquals("LEGENDARY", form.getItemRarity());
        assertTrue(form.isAncient());
        assertEquals("900", form.getItemPower());
        assertEquals("1202", form.getItemArmor());
        assertEquals(1, countFullReadLines(result, FullItemReadLineType.IMPLICIT, "20,0% szansy na blok [20,0]%"));
        assertEquals(1, countFullReadLines(result, FullItemReadLineType.IMPLICIT, "+100% obrażeń od broni w głównej ręce [100]%"));
        assertEquals(25, form.getMasterworking().getQualityCurrent());
        assertEquals(25, form.getMasterworking().getQualityMax());
        assertEquals("defense_max_animus", form.getMasterworking().getPerfectedAffix().getKey());
        assertEquals(MasterworkedAffixSelection.temperingAffix("defense_max_animus").getSource(),
                form.getMasterworking().getPerfectedAffix().getSource());

        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.STRENGTH, 225.0d, true);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.ALL_RESISTANCE, 490.0d, true);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.FIRE_RESISTANCE, 787.0d, true);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, false);
        assertEquals(4, form.getAffixes().size());

        assertEquals(1, form.getTemperingAffixes().size());
        ItemTemperingAffix tempering = form.getTemperingAffixes().getFirst();
        assertEquals(TemperingCategory.DEFENSE, tempering.getCategory());
        assertEquals("defense_max_animus", tempering.getDefinitionId());
        assertEquals(5.0d, tempering.getValue());
        assertTrue(tempering.isGreaterAffix());

        assertTrue(form.getTransfiguration().isTransfigured());
        assertTrue(form.getTransfiguration().isLockedAfterTransfiguration());
        assertEquals(HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX, form.getTransfiguration().getOutcome());
        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(96.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue());
        assertEquals(TransfigurationValueProvenance.GAME_DISPLAYED_VALUE,
                form.getTransfiguration().getAddedTransfigurationAffix().getValueProvenance());
        assertTrue(form.getAffixes().stream()
                .noneMatch(affix -> affix.getSourceText().contains("wszystkich współczynników")));

        assertEquals("Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%.",
                form.getUniqueEffectText());

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                result,
                List.of(),
                null,
                new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty()),
                "Import testowy",
                ""
        ));
        String transfigurationSection = sectionByHeading(html, "Przeistoczenie / Kostka Horadrimów");
        assertTrue(transfigurationSection.contains("selected>Przeistoczony</option>"));
        assertTrue(transfigurationSection.contains("selected>Bonusowy affix Przeistoczenia</option>"));
        assertTrue(transfigurationSection.contains("do wszystkich współczynników"));
        assertTrue(transfigurationSection.contains("name=\"transfigurationAddedDisplayedValue\" step=\"0.1\" value=\"96\""));
        assertTrue(transfigurationSection.contains("selected>Wartość widoczna w grze</option>"));
        assertFalse(transfigurationSection.contains("Pryzmat dostrojenia"));
        assertFalse(transfigurationSection.contains("Niemodyfikowalny po przeistoczeniu"));
    }

    @Test
    void shouldRecognizeQuality25OcrVariants() {
        for (String qualityLine : List.of(
                "25 (+25) jakości",
                "25 (+ 25) jakości",
                "25 (* +25) jakości",
                "25 (x +25) jakości",
                "25 (✦ +25) jakości",
                "25 (◆ +25) jakości"
        )) {
            ItemImageImportCandidateParseResult result = parser.parse(metadata, """
                    Miażdżąca Tarcza Kościanych Łusek
                    Starożytna legendarna tarcza
                    Moc przedmiotu: 900
                    %s
                    1 502 pkt. pancerza
                    +270 siły
                    +588 do odporności na wszystkie żywioły
                    +945 do odporności na: Ogień
                    14,3%% redukcji obrażeń [11,0 - 15,0]%%
                    """.formatted(qualityLine));
            ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

            assertEquals(25, form.getMasterworking().getQualityCurrent(), qualityLine);
            assertEquals("1202", form.getItemArmor(), qualityLine);
            assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.STRENGTH, 225.0d, true);
            assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.ALL_RESISTANCE, 490.0d, true);
            assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.FIRE_RESISTANCE, 787.0d, true);
            assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, false);
        }
    }

    @Test
    void shouldDropNoisyExtraAffixCandidatesFromRealTransfiguredShieldContext() {
        String mergedText = new ItemScreenshotTextMerger().merge(List.of(
                """
                        Miażdżąca Tarcza Kościanych Łusek
                        Starożytna legendarna tarcza
                        Moc przedmiotu: 900
                        25 (+25) jakości
                        Przeistoczony
                        1 502 pkt. pancerza
                        20,0% szansy na blok [20,0]%
                        +100% obrażeń od broni w głównej ręce [100]%
                        +270 siły
                        +588 do odporności na wszystkie żywioły
                        +945 do odporności na: Ogień
                        14,3% redukcji obrażeń [11,0 - 15,0]%
                        """,
                """
                        20,0% szansy na blok [20,010]%
                        +100% obrażeń od broni w głównej ręce [1001
                        +388 do odporności na wszystkie żywioły
                        +943 do odporności na: Ogień
                        1181,3% redukcji obrażeń
                        +1001 siły
                        +96 pkt. do wszystkich współczynników [+75 - 100]
                        +12 do maksymalnej liczby kumulacji Animuszu
                        Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%.
                        Brak możliwości modyfikacji
                        """
        ));
        ItemImageImportCandidateParseResult result = parser.parse(metadata, mergedText);
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals(2, result.getFullItemRead().getLines().stream()
                .filter(line -> line.getType() == FullItemReadLineType.IMPLICIT)
                .count());
        assertEquals(1, countFullReadLines(result, FullItemReadLineType.IMPLICIT, "20,0% szansy na blok [20,0]%"));
        assertEquals(1, countFullReadLines(result, FullItemReadLineType.IMPLICIT, "+100% obrażeń od broni w głównej ręce [100]%"));
        assertEquals(4, form.getAffixes().size());
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.STRENGTH, 225.0d, true);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.ALL_RESISTANCE, 490.0d, true);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.FIRE_RESISTANCE, 787.0d, true);
        assertAffixValueAndGreaterFlag(form, ImportedItemAffixType.DAMAGE_REDUCTION, 11.4d, false);
        assertTrue(form.getAffixes().stream()
                .noneMatch(affix -> affix.getType() == ImportedItemAffixType.WEAPON_DAMAGE_FLAT
                        || affix.getValue() == 1001.0d
                        || affix.getValue() == 324.0d
                        || affix.getValue() == 786.0d
                        || affix.getValue() == 945.0d));
        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(96.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue());
        assertEquals("defense_max_animus", form.getMasterworking().getPerfectedAffix().getKey());
    }

    @Test
    void shouldReadAllStatsTransfigurationValueFromLocalPhraseNotDamageReductionPrefix() {
        String mergedText = new ItemScreenshotTextMerger().merge(List.of(
                """
                        Miażdżąca Tarcza Kościanych Łusek
                        Starożytna legendarna tarcza
                        Moc przedmiotu: 900
                        25 (+25) jakości
                        Przeistoczony
                        14,3% redukcji obrażeń [11,0 - 15,0]% +96 pkt. do wszystkich współczynników [+75 - 100]
                        """
        ));
        ItemImageImportCandidateParseResult result = parser.parse(metadata, mergedText);
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(96.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue());
        assertFalse(form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue() == 14.3d);
        assertTrue(form.getAffixes().stream()
                .noneMatch(affix -> affix.getSourceText().contains("wszystkich współczynników")));
    }

    @Test
    void shouldSelectFortifyAspectAfterCanonicalMergeOfNoisyOcrVariants() {
        for (String rawAspect : List.of(
                "Gdy masz umocnienie, zadajesz obrażenia zwiększone o61%[x] [45 - 65]%.",
                "Gdy masz umocnienie, zadajesz obrażenia zwiększone o 610[x] [45 - 65]%. 70 poziomu",
                "Gdy masz umocnienie, zadajesz obrażenia zwiększone 0 610/01x] [45 - 651%."
        )) {
            String mergedText = new ItemScreenshotTextMerger().merge(List.of(
                    """
                            Miażdżąca Tarcza Kościanych Łusek
                            Starożytna legendarna tarcza
                            Moc przedmiotu: 900
                            %s
                            """.formatted(rawAspect)
            ));
            ItemImageImportCandidateParseResult result = parser.parse(metadata, mergedText);
            ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

            assertEquals("fortify_damage_increased", form.getSelectedAspectId(), rawAspect);
            assertEquals("fortify_damage_increased", form.getOcrSuggestedAspectId(), rawAspect);
            assertEquals("Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%.",
                    form.getUniqueEffectText(), rawAspect);
        }
    }

    @Test
    void shouldIgnoreDurabilityComparisonNoiseLine() {
        ItemImageImportCandidateParseResult result = parser.parse(metadata, hardenedShieldRawText().replace(
                "(Wytrzymałość: -1,7%)",
                "(Wytrzymałość: +4,6%)"
        ));
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertFalse(result.getFullItemRead().getLines().stream()
                .anyMatch(line -> line.getText().contains("Wytrzymałość")));
        assertFalse(form.getAffixes().stream()
                .anyMatch(affix -> affix.getSourceText().contains("Wytrzymałość")));
        assertFalse(form.getFullItemRead().getLines().stream()
                .anyMatch(line -> line.getText().contains("+4,6")));
    }

    @Test
    void shouldSplitTemperingAndAspectWhenOcrJoinsThemInOneLine() {
        ItemImageImportCandidateParseResult result = parser.parse(metadata, hardenedShieldRawText().replace(
                "★ +5 do maksymalnej liczby kumulacji Animuszu\n\nGdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%.",
                "* +5 do maksymalnej liczby kumulacji Animuszu * Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%."
        ));
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals(1, form.getTemperingAffixes().size());
        assertEquals("defense_max_animus", form.getTemperingAffixes().getFirst().getDefinitionId());
        assertTrue(form.getTemperingAffixes().getFirst().isGreaterAffix());
        assertEquals("Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%.",
                result.getFullItemRead().getDetails().getUniqueEffectText());
        assertEquals(1, result.getFullItemRead().getLines().stream()
                .filter(line -> line.getType() == FullItemReadLineType.TEMPERING)
                .count());
    }

    @Test
    void shouldRenderImportedTemperingAsCompactCardAndHideAdditionalTemperingLines() {
        ItemImageImportCandidateParseResult result = parser.parse(metadata, hardenedShieldRawText());
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);
        HeroProfile activeHero = new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty());

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                result,
                List.of(),
                null,
                activeHero,
                "Import testowy",
                ""
        ));
        String temperingSection = sectionByHeading(html, "Hartowanie");

        assertTrue(temperingSection.contains("tempering-existing-card"));
        assertTrue(temperingSection.contains("Defensywa"));
        assertTrue(temperingSection.contains("★ +5 do maksymalnej liczby kumulacji Animuszu"));
        assertTrue(temperingSection.contains("Greater Affix"));
        assertTrue(temperingSection.contains("Runtime nieaktywny"));
        assertTrue(temperingSection.contains("Limit hartowania dla tego przedmiotu został wykorzystany."));
        assertFalse(temperingSection.contains("<h4>Dodaj hartowanie</h4>"));
        assertFalse(temperingSection.contains("id=\"temperingAddControls\""));
        assertFalse(temperingSection.contains("id=\"addTemperingButton\""));
        assertFalse(temperingSection.contains("name=\"newTemperingCategory\""));
        assertFalse(temperingSection.contains("name=\"newTemperingDefinitionId\""));
        assertFalse(temperingSection.contains("name=\"newTemperingValue\""));
        assertFalse(form.getUniqueEffectText().contains("maksymalnej liczby kumulacji Animuszu"));

        String readSection = sectionByHeading(html, "Pełny odczyt widocznego itemu");
        String additionalLines = optionalLineGroupByHeading(readSection, "Dodatkowe / sezonowe linie");
        assertFalse(additionalLines.contains("maksymalnej liczby kumulacji Animuszu"));
        assertFalse(additionalLines.contains("+5"));
        assertFalse(additionalLines.contains("★ +5"));
    }

    @Test
    void shouldPersistImportedHardenedShieldNameAndTemperingInLibrary() throws Exception {
        Path tempDirectory = Files.createTempDirectory("hardened-shield-import");
        ItemLibraryService libraryService = new ItemLibraryService(new FileItemLibraryRepository(tempDirectory));
        ItemImageImportCandidateParseResult result = parser.parse(metadata, hardenedShieldRawText());
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        ItemImportFormMapper.MappingResult mapped = new ItemImportFormMapper().map(form);
        assertTrue(mapped.getErrors().isEmpty(), mapped.getErrors().toString());
        libraryService.saveImportedItem(mapped.getItem(), form.getFullItemRead());

        SavedImportedItem savedItem = new FileItemLibraryRepository(tempDirectory).findAll().getFirst();

        assertEquals("Miażdżąca Tarcza Kościanych Łusek", savedItem.getItemName());
        assertEquals(1, savedItem.getTemperingAffixes().size());
        ItemTemperingAffix persisted = savedItem.getTemperingAffixes().getFirst();
        assertEquals(TemperingCategory.DEFENSE, persisted.getCategory());
        assertEquals("defense_max_animus", persisted.getDefinitionId());
        assertEquals(5.0d, persisted.getValue());
        assertTrue(persisted.isGreaterAffix());
        assertFalse(savedItem.getFullItemRead().getLines().stream()
                .filter(line -> line.getType() != FullItemReadLineType.TEMPERING)
                .anyMatch(line -> line.getText().contains("maksymalnej liczby kumulacji Animuszu")));
    }

    @Test
    void shouldRecognizeBootSlotWithoutHallucinatingUnsupportedAffixes() {
        String ocrText = """
                Buty
                +12,5% szybkości ruchu
                +7,0% uniku
                """;

        ItemImageImportCandidateParseResult result = parser.parse(metadata, ocrText);

        assertEquals(EquipmentSlot.BOOTS, result.getSlotCandidate().getSuggestedValue());
        assertNull(result.getWeaponDamageCandidate().getSuggestedValue());
        assertNull(result.getStrengthCandidate().getSuggestedValue());
        assertNull(result.getIntelligenceCandidate().getSuggestedValue());
        assertNull(result.getThornsCandidate().getSuggestedValue());
        assertNull(result.getBlockChanceCandidate().getSuggestedValue());
        assertNull(result.getRetributionChanceCandidate().getSuggestedValue());
    }

    @Test
    void shouldRecognizeVerathielUniqueSwordWeaponFieldsFromPolishOcr() {
        ItemImageImportCandidateParseResult result = parser.parse(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                verathielRawText()
        );

        ItemImportDetails details = result.getFullItemRead().getDetails();

        assertEquals("Odłamek Verathiela", details.getItemName());
        assertEquals("UNIQUE", details.getItemRarity());
        assertTrue(details.isAncient());
        assertEquals("Miecz", details.getItemType());
        assertEquals(EquipmentSlot.MAIN_HAND, details.getEquipmentSlot());
        assertEquals(900L, details.getItemPower());
        assertEquals(1830L, details.getWeaponDps());
        assertEquals(1350L, details.getWeaponDamageMin());
        assertEquals(1978L, details.getWeaponDamageMax());
        assertEquals(1664L, details.getAverageWeaponDamage());
        assertEquals(1.10d, details.getAttacksPerSecond());
        assertFalse(details.getUniqueEffectText().isBlank());
        assertTrue(details.getUniqueEffectText().contains("100%[x]"));
        assertTrue(details.getUniqueEffectText().contains("[70 - 100]"));
        assertTrue(details.getUniqueEffectText().contains("25 pkt. podstawowego zasobu"));
    }

    @Test
    void shouldNotRegressItemPowerToFirstDigitWhenVerathielRawTextContainsNineHundred() {
        ItemImageImportCandidateParseResult result = parser.parse(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                verathielRawText()
        );

        assertEquals(900L, result.getFullItemRead().getDetails().getItemPower());
        assertFalse(Long.valueOf(1L).equals(result.getFullItemRead().getDetails().getItemPower()));
    }

    @Test
    void shouldExtractVerathielAffixesSeparatelyFromUniqueEffect() {
        ItemImageImportCandidateParseResult result = parser.parse(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                verathielRawText()
        );

        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals(4, form.getAffixes().size());
        assertTrue(form.getAffixes().stream().anyMatch(affix ->
                affix.getType() == ImportedItemAffixType.WEAPON_DAMAGE_FLAT
                        && affix.getValue() == 94.0d
                        && affix.getSourceText().contains("[94 - 157]")));
        assertTrue(form.getAffixes().stream().anyMatch(affix ->
                affix.getType() == ImportedItemAffixType.MAXIMUM_LIFE
                        && affix.getValue() == 2141.0d
                        && affix.getSourceText().contains("[1 831 - 2 200]")));
        assertTrue(form.getAffixes().stream().anyMatch(affix ->
                affix.getType() == ImportedItemAffixType.LIFE_ON_HIT
                        && affix.getValue() == 545.0d
                        && affix.getSourceText().contains("[526 - 632]")));
        assertTrue(form.getAffixes().stream().anyMatch(affix ->
                affix.getType() == ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE
                        && affix.getSourceText().contains("15%")
                        && affix.getSourceText().contains("+3")
                        && affix.getSourceText().contains("[3 - 4]")));
        assertTrue(form.getAffixes().stream()
                .noneMatch(affix -> affix.getSourceText().contains("Umiejętności Podstawowe")));
    }

    @Test
    void shouldRecognizeNoisyVerathielOcrWithoutPolishCharacters() {
        ItemImageImportCandidateParseResult result = parser.parse(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                """
                        ODLFIK VERATHEL
                        STAROZYTNY UNIKATOWY MIECZ
                        Moc   przedmiotu . 900
                        1 830 pkt. obrazen na sek. (+1830)
                        [1 350 - 1 978] pkt. obrazen za trafienie
                        1,10 ataku na sekunde
                        +94 obrazen od broni [94 - 157]
                        Umiejetnosci Podstawowe zadaja obrazenia zwiekszone o 100%[x] [70 - 100],
                        ale dodatkowo zuzywaja 25 pkt. podstawowego zasobu.
                        """
        );

        ItemImportDetails details = result.getFullItemRead().getDetails();

        assertEquals("Odłamek Verathiela", details.getItemName());
        assertEquals("UNIQUE", details.getItemRarity());
        assertTrue(details.isAncient());
        assertEquals("Miecz", details.getItemType());
        assertEquals(EquipmentSlot.MAIN_HAND, details.getEquipmentSlot());
        assertEquals(900L, details.getItemPower());
        assertEquals(1830L, details.getWeaponDps());
        assertEquals(1350L, details.getWeaponDamageMin());
        assertEquals(1978L, details.getWeaponDamageMax());
        assertEquals(1664L, details.getAverageWeaponDamage());
        assertEquals(1.10d, details.getAttacksPerSecond());
    }

    @Test
    void shouldRecognizeVerathielDamageRangeFromSupportedOcrVariants() {
        for (String rangeLine : List.of(
                "[1 350 - 1 978] pkt. obrażeń za trafienie",
                "1 350 - 1 978 pkt. obrażeń za trafienie",
                "[1350 - 1978] pkt. obrażeń za trafienie",
                "1350-1978 pkt. obrażeń za trafienie",
                "1 350 – 1 978",
                "(+1830) [1 350 - 1 978] pkt. obrażeń za trafienie",
                "(+1830) [1 350 - 1 9781 pkt. obrażeń za trafienie",
                "(+1831 [1 350- 1 9781 pkt. obrażeń za trafienie",
                "(+1831 350 - 1 978] pkt. obrażeń za trafienie",
                "1831 350 - 1 978 pkt. obrażeń za trafienie"
        )) {
            ItemImageImportCandidateParseResult result = parser.parse(
                    new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                    """
                            ODŁAMEK VERATHIEL
                            Starożytny unikatowy miecz
                            1 830 pkt. obrażeń na sek.
                            %s
                            1,10 ataku na sekundę
                            """.formatted(rangeLine)
            );

            ItemImportDetails details = result.getFullItemRead().getDetails();
            assertEquals(1350L, details.getWeaponDamageMin(), rangeLine);
            assertEquals(1978L, details.getWeaponDamageMax(), rangeLine);
            assertEquals(1664L, details.getAverageWeaponDamage(), rangeLine);
            assertFalse(Long.valueOf(1830L).equals(details.getAverageWeaponDamage()), rangeLine);
        }
    }

    @Test
    void shouldRenderVerathielNoisyOcrWeaponAndAffixPrefillInFinalHtmlForm() {
        List<ItemImageImportCandidateParseResult> variants = List.of(
                parser.parse(new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768), verathielNoisyUiFixture()),
                parser.parse(new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768), """
                        ODŁAMEK VERATHIEL
                        Starożytny unikatowy miecz
                        Moc przedmiotu: 900
                        1 830 pkt. obrażeń na sek.
                        (+1831 350 - 1 978] pkt. obrażeń za trafienie
                        1,10 ataku na sekundę
                        +545 pkt. zdrowia przy trafieniu [5 - 632]
                        Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]
                        """)
        );
        ItemImageImportCandidateParseResult merged = new ItemImageImportCandidateMerger()
                .merge(new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768), variants.size(), variants);
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(merged);

        String html = new ItemImportPageRenderer().render(new ItemImportPageModel(
                form,
                merged,
                List.of(),
                null,
                new HeroProfile(1L, "Importer", HeroClass.PALADIN, "level=13", HeroItemSelection.empty()),
                "Import testowy",
                ""
        ));

        assertTrue(html.contains("name=\"weaponDps\" value=\"1830\""));
        assertTrue(html.contains("name=\"weaponDamageMin\" value=\"1350\""));
        assertTrue(html.contains("name=\"weaponDamageMax\" value=\"1978\""));
        assertTrue(html.contains("name=\"averageWeaponDamage\" value=\"1664\""));
        assertTrue(html.contains("name=\"attacksPerSecond\" value=\"1.10\""));
        assertFalse(html.contains("name=\"averageWeaponDamage\" value=\"1830\""));
        assertTrue(html.contains("526 - 632"));
        assertTrue(html.contains("Szczęśliwy traf: zasób podstawowy"));
        assertTrue(html.contains("title=\"Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +X podstawowego zasobu\""));
        assertTrue(html.contains("name=\"affixValue_3\" value=\"3\""));
        assertFalse(html.contains("<span class=\"summary-value\">+3</span>"));
        assertFalse(html.contains("15% / +3"));
        assertTrue(html.contains("3 - 4"));
        assertTrue(html.contains("aspect-effect-fieldset"));
        assertTrue(html.contains("Treść efektu"));
        assertFalse(html.contains("Aspekt / efekt legendarny"));
        assertFalse(html.contains("Unikatowy efekt / aspekt"));
        assertTrue(html.contains("<input type=\"number\" min=\"0\" step=\"0.01\" name=\"affixValue_3\" value=\"3\""));
        assertTrue(html.contains("name=\"affixType_0\""));
        assertTrue(html.contains("name=\"affixType_1\""));
        assertTrue(html.contains("name=\"affixType_2\""));
        assertTrue(html.contains("name=\"affixType_3\""));
        assertFalse(html.contains("name=\"affixType_4\""));
        assertFalse(html.contains("Odczyt OCR / źródło"));
        assertFalse(html.contains("Źródło: OCR"));
    }

    @Test
    void shouldRecognizeVerathielDamageRangeFromCondensedNoisyOcrLine() {
        ItemImageImportCandidateParseResult result = parser.parse(
                new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                """
                        ODLFIK VERATHEL Starozytny unikatowy miecz Moc przedmiotu 900 1 830 pkt. obrazen na sek. 1350–1978 pkt. obrazen za trafienie 1,10 ataku na sekunde +94 obrazen od broni [94 - 157] +2 141 maksymalnego zdrowia [1 831 - 2 200] +545 pkt. zdrowia przy trafieniu [526 - 632] Szczesliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4] Umiejetnosci Podstawowe zadaja obrazenia zwiekszone o 100%[x] [70 - 100], ale dodatkowo zuzywaja 25 pkt. podstawowego zasobu.
                        """
        );

        ItemImportDetails details = result.getFullItemRead().getDetails();
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals(1350L, details.getWeaponDamageMin());
        assertEquals(1978L, details.getWeaponDamageMax());
        assertEquals(1664L, details.getAverageWeaponDamage());
        assertEquals(4, form.getAffixes().size());
        assertEquals("verathiel_shard", form.getSelectedAspectId());
    }

    @Test
    void shouldTreatAncientUniqueRarityAsAncientTrueAndUnique() {
        for (String text : List.of(
                "Starożytny unikatowy miecz",
                "Starożytna unikatowa",
                "STAROZYTNY UNIKATOWY MIECZ",
                "starozytny unikatowy miecz"
        )) {
            ItemImageImportCandidateParseResult result = parser.parse(
                    new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                    text
            );

            assertEquals("UNIQUE", result.getFullItemRead().getDetails().getItemRarity(), text);
            assertTrue(result.getFullItemRead().getDetails().isAncient(), text);
            if (text.toUpperCase().contains("MIECZ")) {
                assertEquals("Miecz", result.getFullItemRead().getDetails().getItemType(), text);
                assertEquals(EquipmentSlot.MAIN_HAND, result.getFullItemRead().getDetails().getEquipmentSlot(), text);
            }
        }
    }

    @Test
    void shouldRecognizeItemPowerNineHundredFromLooseAndCondensedOcrForms() {
        for (String text : List.of(
                "Moc przedmiotu : 900",
                "Moc przedmiotu. 900",
                "Moc przedmiotu 900",
                "Moc@@@przedmiotu###900",
                "Mocprzedmiotu900"
        )) {
            ItemImageImportCandidateParseResult result = parser.parse(
                    new ItemImageMetadata("miecz.png", "image/png", "PNG", 479, 768),
                    text
            );

            assertEquals(900L, result.getFullItemRead().getDetails().getItemPower(), text);
            assertFalse(Long.valueOf(1L).equals(result.getFullItemRead().getDetails().getItemPower()), text);
        }
    }

    private void assertFortifyAspectRoll(String aspectLine, String expectedRoll) {
        ItemImageImportCandidateParseResult result = parser.parse(metadata, """
                Generyczna Tarcza Próbna
                Legendarna tarcza
                %s
                """.formatted(aspectLine));
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(result);

        assertEquals("fortify_damage_increased", form.getSelectedAspectId(), aspectLine);
        assertFalse("inner-calm".equals(form.getSelectedAspectId()), aspectLine);
        assertTrue(result.getFullItemRead().getDetails().getUniqueEffectText().contains(expectedRoll), aspectLine);
        assertFalse(result.getFullItemRead().getDetails().getUniqueEffectText().contains("70 poziomu"), aspectLine);
    }

    private static void assertAffixGreaterFlag(ItemImportEditableForm form, ImportedItemAffixType expectedType, boolean expectedGreaterAffix) {
        ImportedItemAffix affix = form.getAffixes().stream()
                .filter(candidate -> candidate.getType() == expectedType)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Brak affixu: " + expectedType.getDisplayName()));
        assertEquals(expectedGreaterAffix, affix.isGreaterAffix(), expectedType.getDisplayName());
    }

    private static void assertAffixValueAndGreaterFlag(ItemImportEditableForm form,
                                                       ImportedItemAffixType expectedType,
                                                       double expectedValue,
                                                       boolean expectedGreaterAffix) {
        ImportedItemAffix affix = form.getAffixes().stream()
                .filter(candidate -> candidate.getType() == expectedType)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Brak affixu: " + expectedType.getDisplayName()));
        assertEquals(expectedValue, affix.getValue(), 0.0001d, expectedType.getDisplayName());
        assertEquals(expectedGreaterAffix, affix.isGreaterAffix(), expectedType.getDisplayName());
    }

    private static void assertLineText(ItemImageImportCandidateParseResult result,
                                       FullItemReadLineType type,
                                       String expectedText) {
        assertTrue(result.getFullItemRead().getLines().stream()
                        .filter(line -> line.getType() == type)
                        .map(FullItemReadLine::getText)
                        .anyMatch(expectedText::equals),
                "Brak znormalizowanej linii: " + expectedText);
    }

    private static long countFullReadLines(ItemImageImportCandidateParseResult result,
                                           FullItemReadLineType type,
                                           String expectedText) {
        return result.getFullItemRead().getLines().stream()
                .filter(line -> line.getType() == type)
                .map(FullItemReadLine::getText)
                .filter(expectedText::equals)
                .count();
    }

    static String verathielRawText() {
        return """
                ODŁAMEK
                VERATHIEL
                Starożytny unikatowy miecz
                Moc przedmiotu: 900
                1 830 pkt. obrażeń na sek.
                [1 350 - 1 978] pkt. obrażeń za trafienie
                1,10 ataku na sekundę (Szybka)
                +94 obrażeń od broni [94 - 157]
                +2 141 maksymalnego zdrowia [1 831 - 2 200]
                +545 pkt. zdrowia przy trafieniu [526 - 632]
                Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]
                Umiejętności Podstawowe zadają
                obrażenia zwiększone o 100%[x] [70 - 100],
                ale dodatkowo zużywają 25 pkt. podstawowego zasobu.
                """;
    }

    static String verathielNoisyUiFixture() {
        return """
                ODŁAMEK
                VERATHIEL
                Starożytny unikatowy miecz
                Moc przedmiotu: 900
                1 830 pkt. obrażeń na sek. (+1830) [1 350 - 1 978] pkt. obrażeń za trafienie
                1,10 ataku na sekundę
                +94 obrażeń od broni [94 - 157]
                +2 141 maksymalnego zdrowia [1 831 - 2 200]
                +545 pkt. zdrowia przy trafieniu [526 - 632]
                Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]
                Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100],
                ale dodatkowo zużywają 25 pkt. podstawowego zasobu.
                """;
    }

    static String hardenedShieldRawText() {
        return """
                MIAŻDŻĄCA TARCZA KOŚCIANYCH ŁUSEK
                Starożytna legendarna tarcza
                Moc przedmiotu: 900

                1 202 pkt. pancerza
                (Wytrzymałość: -1,7%)

                20,0% szansy na blok [20,0]%
                +100% obrażeń od broni w głównej ręce [100]%

                ★ +225 siły
                ★ +490 do odporności na wszystkie żywioły
                ★ +787 do odporności na: Ogień
                11,4% redukcji obrażeń [11,0 - 15,0]%

                ★ +5 do maksymalnej liczby kumulacji Animuszu

                Gdy masz umocnienie, zadajesz obrażenia zwiększone o 61%[x] [45 - 65]%.
                """;
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

    private static String optionalLineGroupByHeading(String html, String heading) {
        int headingIndex = html.indexOf("<h5>" + heading + "</h5>");
        if (headingIndex < 0) {
            return "";
        }
        int start = html.lastIndexOf("<section", headingIndex);
        int end = html.indexOf("</section>", headingIndex);
        if (start < 0 || end < 0) {
            throw new AssertionError("Nie udało się wyciąć grupy linii: " + heading);
        }
        return html.substring(start, end + "</section>".length());
    }

}

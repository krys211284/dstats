package krys.itemimport.image;

import krys.item.EquipmentSlot;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportFormMapper;
import krys.itemimport.ItemImportValueMode;
import krys.itemimport.ValidatedImportedItem;
import krys.masterworking.MasterworkedAffixSource;
import krys.masterworking.MasterworkingResolvedItemValueResolver;
import krys.socketing.SocketContentType;
import krys.tempering.ItemTemperingAffix;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.TransfigurationValueProvenance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regresje draftu formularza budowanego z neutralnych wynikow importu obrazu. */
class ImageItemImportDraftMapperTest {
    private final ImageItemImportDraftMapper mapper = new ImageItemImportDraftMapper();
    private final MasterworkingResolvedItemValueResolver masterworkingResolver = new MasterworkingResolvedItemValueResolver();

    @Test
    void shouldBuildEditableShieldDraftFromNeutralReads() {
        ImageItemImportDraftMapper.DraftResult draft = mapper.map(
                part1("TARCZA BURZY KSIĘŻYCOWEGO SZAŁU", "Tarcza", "LEGENDARY",
                        EquipmentSlot.OFF_HAND, true, false, 900, 25),
                List.of(
                        baseArmor(1, "1502 pancerza", 1502),
                        valueBlock(2, PropertyBlockSemanticKind.IMPLICIT, "block_chance", false, true,
                                "20,0% szansy na blok\n20,0%", 20.0d, "%", null, null, 20.0d),
                        valueBlock(3, PropertyBlockSemanticKind.AFFIX, "strength", false, true,
                                "+217 siły [150 - 180]", 217.0d, "", 150.0d, 180.0d, null),
                        valueBlock(4, PropertyBlockSemanticKind.AFFIX, "critical_strike_chance", false, true,
                                "+11,0% szansy na trafienie krytyczne", 11.0d, "%", null, null, null),
                        valueBlock(5, PropertyBlockSemanticKind.AFFIX, "damage_reduction", false, true,
                                "17,6% redukcji obrażeń", 17.6d, "%", null, null, null),
                        valueBlock(6, PropertyBlockSemanticKind.AFFIX, "cooldown_reduction", true, true,
                                "12,3% redukcji czasu odnowienia [10 - 12]%", 12.3d, "%", 10.0d, 12.0d, null),
                        valueBlock(7, PropertyBlockSemanticKind.TEMPERING, "defense_max_animus", true, true,
                                "+12 do maksymalnej liczby kumulacji Animuszu", 12.0d, "", null, null, null),
                        valueBlock(8, PropertyBlockSemanticKind.TRANSFIGURATION, "BONUS_ITEM_QUALITY", false, true,
                                "+4 do jakości przedmiotu", 4.0d, "", null, null, null,
                                HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY),
                        socketBlock(9, "EMPTY_SOCKET", "Puste gniazdo", null),
                        aspectBlock(10, "naznaczenie_aspect",
                                "Zyskujesz 4% szybkości ataku. Przy 5 kumulacjach zadajesz 60%[x] więcej i zyskujesz 15% szybkości ruchu.",
                                params(valueParam(1, "4"), valueParam(2, "5"), valueParam(3, "60"), valueParam(4, "15")))
                ),
                "tarcza.png"
        );

        ItemImportEditableForm form = draft.form();
        assertEquals(ItemImportValueMode.GAME_DISPLAYED_VALUE, form.getValueMode());
        assertEquals("OFF_HAND", form.getSlot());
        assertEquals("1202", form.getItemArmor());
        assertEquals(25, form.getMasterworking().getQualityCurrent());
        assertNotNull(form.getMasterworking().getPerfectedAffix());
        assertEquals(MasterworkedAffixSource.TEMPERING_AFFIX, form.getMasterworking().getPerfectedAffix().getSource());
        assertEquals("defense_max_animus", form.getMasterworking().getPerfectedAffix().getKey());
        assertEquals("naznaczenie_aspect", form.getSelectedAspectId());
        assertEquals(1, form.getSocketing().getSocketCount());
        assertEquals(SocketContentType.EMPTY, form.getSocketing().socketAt(0).getContentType());
        assertTrue(form.getFullItemRead().getLines().stream()
                .anyMatch(line -> line.getType() == FullItemReadLineType.IMPLICIT
                        && line.getText().contains("20,0% szansy na blok")));
        assertEquals(4, form.getAffixes().size());
        assertAffix(form, ImportedItemAffixType.STRENGTH, 173.6d, false, 150.0d, 180.0d, null, "217");
        assertAffix(form, ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 8.8d, false, null, null, null, "11%");
        assertAffix(form, ImportedItemAffixType.DAMAGE_REDUCTION, 14.08d, false, null, null, null, "17,6%");
        assertAffix(form, ImportedItemAffixType.COOLDOWN_REDUCTION, 10.25d, true, 10.0d, 12.0d, null, "12,3%");
        assertEquals(1, form.getTemperingAffixes().size());
        assertEquals("defense_max_animus", form.getTemperingAffixes().get(0).getDefinitionId());
        assertEquals(5.0d, form.getTemperingAffixes().get(0).getValue(), 0.0001d);
        assertTrue(form.getTemperingAffixes().get(0).getDisplayText().contains("+12"));
        assertEquals(HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY, form.getTransfiguration().getOutcome());
        assertEquals(4, form.getTransfiguration().getBonusQuality());

        ValidatedImportedItem item = validate(form);
        assertEquals(EquipmentSlot.OFF_HAND, item.getSlot());
        assertEquals(1202L, item.getItemArmor());
        assertEquals(20.0d, item.getBlockChance(), 0.0001d);
        assertEquals("naznaczenie_aspect", item.getSelectedAspectId());

        assertEquals(1502L, masterworkingResolver.resolveArmor(item.getItemArmor(), item.getMasterworking()));
        assertResolvedAffix(item, ImportedItemAffixType.STRENGTH, 217.0d);
        assertResolvedAffix(item, ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 11.0d);
        assertResolvedAffix(item, ImportedItemAffixType.DAMAGE_REDUCTION, 17.6d);
        assertResolvedAffix(item, ImportedItemAffixType.COOLDOWN_REDUCTION, 12.3d);
        ItemTemperingAffix maxAnimus = item.getTemperingAffixes().getFirst();
        assertEquals(12.0d, masterworkingResolver.resolveTemperingValue(maxAnimus, item.getMasterworking()), 0.0001d);
    }

    @Test
    void shouldKeepAmbiguousHelmetEvidenceAsReviewAndSocketStatsOutsideOrdinaryAffixes() {
        ImageItemImportDraftMapper.DraftResult draft = mapper.map(
                part1("DZIEDZIC ZATRACENIA", "Hełm", "UNIQUE",
                        EquipmentSlot.HELMET, true, true, 900, 25),
                List.of(
                        baseArmor(1, "2004 pancerza", 2004),
                        valueBlock(2, PropertyBlockSemanticKind.AFFIX, "cooldown_reduction", false, false,
                                "12,3% redukcji czasu odnowienia", null, "%", null, null, null, true),
                        valueBlock(3, PropertyBlockSemanticKind.TEMPERING, "defense_max_animus", true, true,
                                "+12 do maksymalnej liczby kumulacji Animuszu", 12.0d, "", null, null, null),
                        valueBlock(4, PropertyBlockSemanticKind.TRANSFIGURATION, "ALL_STATS", false, true,
                                "+115 do wszystkich współczynników [75 - 100]", 115.0d, "", 75.0d, 100.0d, null,
                                HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX),
                        socketBlock(5, "FILLED_SOCKET", "+150 siły", 150.0d),
                        socketBlock(6, "FILLED_SOCKET", "+120 inteligencji", 120.0d),
                        aspectBlock(7, "heir_of_perdition", "Łaska Matki pozwala poddać się Nienawiści i zyskać 80%[x].",
                                params(valueParam(1, "80")))
                ),
                "dziedzic.png"
        );

        ItemImportEditableForm form = draft.form();
        assertEquals(ItemImportValueMode.GAME_DISPLAYED_VALUE, form.getValueMode());
        assertEquals("2004", form.getItemArmor());
        assertTrue(draft.reviewNotes().stream().anyMatch(note -> note.contains("NUMERIC_OCR_AMBIGUOUS")));
        assertEquals(0, form.getAffixes().size());
        assertEquals(2, form.getSocketing().getSocketCount());
        assertEquals(SocketContentType.DETECTED_STAT, form.getSocketing().socketAt(0).getContentType());
        assertEquals(SocketContentType.DETECTED_STAT, form.getSocketing().socketAt(1).getContentType());
        assertEquals("+150 siły", form.getSocketing().socketAt(0).getDetectedStat().getDisplayText());
        assertTrue(form.getSocketing().socketAt(0).getDetectedStat().isNeedsReview());
        assertEquals("+120 inteligencji", form.getSocketing().socketAt(1).getDetectedStat().getDisplayText());
        assertEquals("heir_of_perdition", form.getSelectedAspectId());
        assertEquals(HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX, form.getTransfiguration().getOutcome());
        assertNotNull(form.getTransfiguration().getAddedTransfigurationAffix());
        assertEquals("ALL_STATS", form.getTransfiguration().getAddedTransfigurationAffix().getDefinitionId());
        assertEquals(115.0d, form.getTransfiguration().getAddedTransfigurationAffix().getDisplayedValue(), 0.0001d);
        assertEquals(TransfigurationValueProvenance.GAME_DISPLAYED_VALUE,
                form.getTransfiguration().getAddedTransfigurationAffix().getValueProvenance());

        ValidatedImportedItem item = validate(form);
        assertEquals(EquipmentSlot.HELMET, item.getSlot());
        assertEquals(2004L, item.getItemArmor());
        assertEquals(0.0d, item.getStrength(), 0.0001d);
        assertEquals(0.0d, item.getIntelligence(), 0.0001d);
        assertEquals(2, item.getSocketing().getOccupiedSocketCount());
    }

    @Test
    void shouldKeepMythicUniqueOrdinaryAffixDisplayedValueWithoutReferenceValue() {
        ImageItemImportDraftMapper.DraftResult draft = mapper.map(
                part1("DZIEDZIC ZATRACENIA", "Hełm", "UNIQUE",
                        EquipmentSlot.HELMET, true, true, 900, 25),
                List.of(
                        valueBlock(1, PropertyBlockSemanticKind.AFFIX, "critical_strike_chance", false, true,
                                "+15,0% szansy na trafienie krytyczne", 15.0d, "%", null, null, null),
                        aspectBlock(2, "heir_of_perdition", "Łaska Matki pozwala poddać się Nienawiści i zyskać 80%[x].",
                                params(valueParam(1, "80")))
                ),
                "dziedzic.png"
        );

        ItemImportEditableForm form = draft.form();

        assertEquals(ItemImportValueMode.GAME_DISPLAYED_VALUE, form.getValueMode());
        assertAffix(form, ImportedItemAffixType.CRITICAL_STRIKE_CHANCE, 15.0d, false, null, null, null, "15%");
        ValidatedImportedItem item = validate(form);
        assertEquals(15.0d, item.getAffixes().getFirst().getValue(), 0.0001d);
        assertEquals(15.0d, masterworkingResolver.resolveAffixValue(
                item.getAffixes().getFirst(),
                item.getMasterworking(),
                item.getDetails().isMythicUnique()
        ), 0.0001d);
    }

    @Test
    void shouldBuildVerathielWeaponDraftWithAverageLegacyDamageAndOrderedAspectText() {
        ImageItemImportDraftMapper.DraftResult draft = mapper.map(
                part1("ODŁAMEK VERATHIELA", "Miecz", "UNIQUE",
                        EquipmentSlot.MAIN_HAND, true, false, 900, 0),
                List.of(
                        weaponBase(1, "1874 obrażeń na sekundę", 1874),
                        weaponDamage(2, "1390-2018 obrażeń za trafienie", 1390, 2018),
                        attacksPerSecond(3, "1,10 ataków na sekundę", 1.10d),
                        valueBlock(4, PropertyBlockSemanticKind.AFFIX, "strength", false, true,
                                "+172 siły [150 - 180]", 172.0d, "", 150.0d, 180.0d, null),
                        aspectBlock(5, "verathiel_shard",
                                "Umiejętności Podstawowe zadają 35%[x], kosztują 25 zasobu i mają 70-100% premii.",
                                params(valueParam(1, "35"), valueParam(2, "25"), rangeParam(3, "70-100", 70.0d, 100.0d)))
                ),
                "verathiel.png"
        );

        ItemImportEditableForm form = draft.form();
        assertEquals("1874", form.getWeaponDps());
        assertEquals("1390", form.getWeaponDamageMin());
        assertEquals("2018", form.getWeaponDamageMax());
        assertEquals("1704", form.getAverageWeaponDamage());
        assertEquals("1.10", form.getAttacksPerSecond());
        assertEquals("1704", form.getWeaponDamage());
        assertEquals("verathiel_shard", form.getSelectedAspectId());
        assertTrue(form.getFullItemRead().getLines().stream()
                .anyMatch(line -> line.getType() == FullItemReadLineType.ASPECT
                        && line.getText().indexOf("35") < line.getText().indexOf("25")
                        && line.getText().indexOf("25") < line.getText().indexOf("70-100")));
        assertAffix(form, ImportedItemAffixType.STRENGTH, 172.0d, false, 150.0d, 180.0d, null);

        ValidatedImportedItem item = validate(form);
        assertEquals(EquipmentSlot.MAIN_HAND, item.getSlot());
        assertEquals(1874L, item.getWeaponDps());
        assertEquals(1390L, item.getWeaponDamageMin());
        assertEquals(2018L, item.getWeaponDamageMax());
        assertEquals(1704L, item.getAverageWeaponDamage());
        assertEquals(1704L, item.getWeaponDamage());
        assertEquals(1.10d, item.getAttacksPerSecond(), 0.0001d);
        assertEquals("verathiel_shard", item.getSelectedAspectId());
    }

    private static ValidatedImportedItem validate(ItemImportEditableForm form) {
        ItemImportFormMapper.MappingResult result = new ItemImportFormMapper().map(form);
        assertTrue(result.getErrors().isEmpty(), () -> result.getErrors().toString());
        assertNotNull(result.getItem());
        return result.getItem();
    }

    private static Part1ReadResult part1(String name,
                                         String itemType,
                                         String rarity,
                                         EquipmentSlot slot,
                                         boolean ancient,
                                         boolean mythicUnique,
                                         int itemPower,
                                         int masterworkingQuality) {
        return new Part1ReadResult(
                name,
                0,
                rarity,
                itemType,
                rarity,
                Optional.of(ancient),
                Optional.of(mythicUnique),
                Optional.of(slot),
                OptionalInt.of(itemPower),
                OptionalInt.empty(),
                OptionalInt.of(masterworkingQuality),
                Map.of(),
                List.of()
        );
    }

    private static ImageItemImportDraftMapper.BlockRead baseArmor(int order, String rawText, long armor) {
        return block(order, PropertyBlockSemanticKind.BASE_STAT, "", false, true, rawText,
                null, "", null, null, null, armor, null, null, null, null, List.of(), false,
                HoradricTransfigurationOutcome.NONE);
    }

    private static ImageItemImportDraftMapper.BlockRead weaponBase(int order, String rawText, long dps) {
        return block(order, PropertyBlockSemanticKind.BASE_STAT, "", false, true, rawText,
                null, "", null, null, null, null, dps, null, null, null, List.of(), false,
                HoradricTransfigurationOutcome.NONE);
    }

    private static ImageItemImportDraftMapper.BlockRead weaponDamage(int order, String rawText, long min, long max) {
        return block(order, PropertyBlockSemanticKind.BASE_STAT, "", false, true, rawText,
                null, "", null, null, null, null, null, min, max, null, List.of(), false,
                HoradricTransfigurationOutcome.NONE);
    }

    private static ImageItemImportDraftMapper.BlockRead attacksPerSecond(int order, String rawText, double value) {
        return block(order, PropertyBlockSemanticKind.BASE_STAT, "", false, true, rawText,
                null, "", null, null, null, null, null, null, null, value, List.of(), false,
                HoradricTransfigurationOutcome.NONE);
    }

    private static ImageItemImportDraftMapper.BlockRead valueBlock(int order,
                                                                  PropertyBlockSemanticKind kind,
                                                                  String definitionId,
                                                                  boolean greaterAffix,
                                                                  boolean numericCertain,
                                                                  String rawText,
                                                                  Double value,
                                                                  String unit,
                                                                  Double rangeMin,
                                                                  Double rangeMax,
                                                                  Double referenceValue) {
        return valueBlock(order, kind, definitionId, greaterAffix, numericCertain, rawText, value, unit,
                rangeMin, rangeMax, referenceValue, false);
    }

    private static ImageItemImportDraftMapper.BlockRead valueBlock(int order,
                                                                  PropertyBlockSemanticKind kind,
                                                                  String definitionId,
                                                                  boolean greaterAffix,
                                                                  boolean numericCertain,
                                                                  String rawText,
                                                                  Double value,
                                                                  String unit,
                                                                  Double rangeMin,
                                                                  Double rangeMax,
                                                                  Double referenceValue,
                                                                  boolean ambiguous) {
        return valueBlock(order, kind, definitionId, greaterAffix, numericCertain, rawText, value, unit,
                rangeMin, rangeMax, referenceValue, ambiguous, HoradricTransfigurationOutcome.NONE);
    }

    private static ImageItemImportDraftMapper.BlockRead valueBlock(int order,
                                                                  PropertyBlockSemanticKind kind,
                                                                  String definitionId,
                                                                  boolean greaterAffix,
                                                                  boolean numericCertain,
                                                                  String rawText,
                                                                  Double value,
                                                                  String unit,
                                                                  Double rangeMin,
                                                                  Double rangeMax,
                                                                  Double referenceValue,
                                                                  HoradricTransfigurationOutcome outcome) {
        return valueBlock(order, kind, definitionId, greaterAffix, numericCertain, rawText, value, unit,
                rangeMin, rangeMax, referenceValue, false, outcome);
    }

    private static ImageItemImportDraftMapper.BlockRead valueBlock(int order,
                                                                  PropertyBlockSemanticKind kind,
                                                                  String definitionId,
                                                                  boolean greaterAffix,
                                                                  boolean numericCertain,
                                                                  String rawText,
                                                                  Double value,
                                                                  String unit,
                                                                  Double rangeMin,
                                                                  Double rangeMax,
                                                                  Double referenceValue,
                                                                  boolean ambiguous,
                                                                  HoradricTransfigurationOutcome outcome) {
        return block(order, kind, definitionId, greaterAffix, numericCertain, rawText, value, unit,
                rangeMin, rangeMax, referenceValue, null, null, null, null, null, List.of(), ambiguous, outcome);
    }

    private static ImageItemImportDraftMapper.BlockRead socketBlock(int order,
                                                                   String definitionId,
                                                                   String rawText,
                                                                   Double value) {
        return block(order, PropertyBlockSemanticKind.SOCKET, definitionId, false, true, rawText,
                value, "", null, null, null, null, null, null, null, null,
                value == null ? List.of() : params(valueParam(1, value.toString())), false,
                HoradricTransfigurationOutcome.NONE);
    }

    private static ImageItemImportDraftMapper.BlockRead aspectBlock(int order,
                                                                   String definitionId,
                                                                   String rawText,
                                                                   List<PropertyBlockValueReadResult.NumericParameter> params) {
        return block(order, PropertyBlockSemanticKind.ASPECT, definitionId, false, true, rawText,
                null, "", null, null, null, null, null, null, null, null, params, false,
                HoradricTransfigurationOutcome.NONE);
    }

    private static ImageItemImportDraftMapper.BlockRead block(int order,
                                                             PropertyBlockSemanticKind kind,
                                                             String definitionId,
                                                             boolean greaterAffix,
                                                             boolean numericCertain,
                                                             String rawText,
                                                             Double value,
                                                             String unit,
                                                             Double rangeMin,
                                                             Double rangeMax,
                                                             Double referenceValue,
                                                             Long itemArmor,
                                                             Long weaponDps,
                                                             Long weaponDamageMin,
                                                             Long weaponDamageMax,
                                                             Double attacksPerSecond,
                                                             List<PropertyBlockValueReadResult.NumericParameter> params,
                                                             boolean ambiguous,
                                                             HoradricTransfigurationOutcome outcome) {
        List<PropertyBlockNumericEvidence> evidence = ambiguous
                ? List.of(new PropertyBlockNumericEvidence(1, "", List.of("profile-a"), List.of("12,3", "12,8"), 1, true))
                : List.of();
        StructuralItemPartSlot sourcePart = kind == PropertyBlockSemanticKind.BASE_STAT
                || kind == PropertyBlockSemanticKind.IMPLICIT
                || kind == PropertyBlockSemanticKind.AFFIX
                ? StructuralItemPartSlot.PART_2
                : StructuralItemPartSlot.PART_3;
        PropertyBlockReadResult read = new PropertyBlockReadResult(
                sourcePart,
                order,
                greaterAffix ? PropertyBlockAnchorType.GREATER_STAR : PropertyBlockAnchorType.NONE,
                rawText,
                1L,
                "test",
                List.of(),
                evidence,
                List.of()
        );
        PropertyBlockSemanticResult semantic = new PropertyBlockSemanticResult(
                sourcePart,
                order,
                read.getAnchorType(),
                kind,
                definitionId,
                List.of(),
                greaterAffix,
                numericCertain,
                rawText,
                evidence,
                outcome,
                ambiguous ? List.of("NUMERIC_OCR_AMBIGUOUS") : List.of()
        );
        PropertyBlockValueReadResult valueResult = new PropertyBlockValueReadResult(
                sourcePart,
                order,
                kind,
                definitionId,
                "",
                value,
                unit,
                rangeMin,
                rangeMax,
                referenceValue,
                itemArmor,
                weaponDps,
                weaponDamageMin,
                weaponDamageMax,
                attacksPerSecond,
                params,
                !ambiguous,
                ambiguous,
                ambiguous ? List.of("NUMERIC_OCR_AMBIGUOUS") : List.of(),
                evidence
        );
        return new ImageItemImportDraftMapper.BlockRead(read, semantic, valueResult);
    }

    private static List<PropertyBlockValueReadResult.NumericParameter> params(PropertyBlockValueReadResult.NumericParameter... params) {
        return List.of(params);
    }

    private static PropertyBlockValueReadResult.NumericParameter valueParam(int order, String rawToken) {
        return new PropertyBlockValueReadResult.NumericParameter(order, "PARAMETER", rawToken,
                Double.parseDouble(rawToken.replace(',', '.')), null, null, "", false, List.of());
    }

    private static PropertyBlockValueReadResult.NumericParameter rangeParam(int order,
                                                                           String rawToken,
                                                                           double min,
                                                                           double max) {
        return new PropertyBlockValueReadResult.NumericParameter(order, "PARAMETER", rawToken,
                null, min, max, "", false, List.of());
    }

    private static void assertAffix(ItemImportEditableForm form,
                                    ImportedItemAffixType type,
                                    double value,
                                    boolean greaterAffix,
                                    Double rangeMin,
                                    Double rangeMax,
                                    Double referenceValue) {
        assertAffix(form, type, value, greaterAffix, rangeMin, rangeMax, referenceValue, null);
    }

    private static void assertAffix(ItemImportEditableForm form,
                                    ImportedItemAffixType type,
                                    double value,
                                    boolean greaterAffix,
                                    Double rangeMin,
                                    Double rangeMax,
                                    Double referenceValue,
                                    String displayValue) {
        var affix = form.getAffixes().stream()
                .filter(candidate -> candidate.getType() == type)
                .findFirst()
                .orElseThrow();
        assertEquals(value, affix.getValue(), 0.0001d);
        assertEquals(greaterAffix, affix.isGreaterAffix());
        assertEquals(rangeMin, affix.getRollRangeMin());
        assertEquals(rangeMax, affix.getRollRangeMax());
        assertEquals(referenceValue, affix.getReferenceValue());
        if (displayValue != null) {
            assertEquals(displayValue, affix.getDisplayValue());
        }
    }

    private void assertResolvedAffix(ValidatedImportedItem item,
                                     ImportedItemAffixType type,
                                     double expectedValue) {
        var affix = item.getAffixes().stream()
                .filter(candidate -> candidate.getType() == type)
                .findFirst()
                .orElseThrow();
        assertEquals(expectedValue,
                masterworkingResolver.resolveAffixValue(affix, item.getMasterworking()),
                0.0001d);
    }
}

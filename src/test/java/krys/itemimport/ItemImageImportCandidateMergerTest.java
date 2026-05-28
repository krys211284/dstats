package krys.itemimport;

import krys.item.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testuje deterministyczne scalanie wyników z wielu wariantów OCR. */
class ItemImageImportCandidateMergerTest {
    private final ItemImageMetadata metadata = new ItemImageMetadata("shield.png", "image/png", "PNG", 1200, 1600);

    @Test
    void shouldPreferHigherConfidenceValueFromAnotherVariant() {
        ItemImageImportCandidateParseResult lowQuality = parseResult(
                ItemImportFieldCandidate.unknown("slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                new ItemImportFieldCandidate<>("+111 do siły", 111.0d, ItemImportFieldConfidence.MEDIUM, "medium"),
                ItemImportFieldCandidate.unknown("intelligence"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution")
        );
        ItemImageImportCandidateParseResult betterVariant = parseResult(
                new ItemImportFieldCandidate<>("Tarcza", EquipmentSlot.OFF_HAND, ItemImportFieldConfidence.HIGH, "high"),
                ItemImportFieldCandidate.unknown("weapon"),
                new ItemImportFieldCandidate<>("+114 do siły", 114.0d, ItemImportFieldConfidence.HIGH, "high"),
                ItemImportFieldCandidate.unknown("intelligence"),
                new ItemImportFieldCandidate<>("+494 do cierni", 494.0d, ItemImportFieldConfidence.HIGH, "high"),
                new ItemImportFieldCandidate<>("+20,0% szansy na blok", 20.0d, ItemImportFieldConfidence.HIGH, "high"),
                ItemImportFieldCandidate.unknown("retribution")
        );

        ItemImageImportCandidateParseResult merged = new ItemImageImportCandidateMerger()
                .merge(metadata, 5, java.util.List.of(lowQuality, betterVariant));

        assertEquals(EquipmentSlot.OFF_HAND, merged.getSlotCandidate().getSuggestedValue());
        assertEquals(114.0d, merged.getStrengthCandidate().getSuggestedValue());
        assertEquals(ItemImportFieldConfidence.HIGH, merged.getStrengthCandidate().getConfidence());
        assertEquals(494.0d, merged.getThornsCandidate().getSuggestedValue());
        assertEquals(20.0d, merged.getBlockChanceCandidate().getSuggestedValue());
    }

    @Test
    void shouldLowerConfidenceWhenTopVariantsConflict() {
        ItemImageImportCandidateParseResult firstVariant = parseResult(
                ItemImportFieldCandidate.unknown("slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                new ItemImportFieldCandidate<>("+114 do siły", 114.0d, ItemImportFieldConfidence.HIGH, "high"),
                ItemImportFieldCandidate.unknown("intelligence"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution")
        );
        ItemImageImportCandidateParseResult secondVariant = parseResult(
                ItemImportFieldCandidate.unknown("slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                new ItemImportFieldCandidate<>("+111 do siły", 111.0d, ItemImportFieldConfidence.HIGH, "high"),
                ItemImportFieldCandidate.unknown("intelligence"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution")
        );

        ItemImageImportCandidateParseResult merged = new ItemImageImportCandidateMerger()
                .merge(metadata, 5, java.util.List.of(firstVariant, secondVariant));

        assertEquals(114.0d, merged.getStrengthCandidate().getSuggestedValue());
        assertEquals(ItemImportFieldConfidence.MEDIUM, merged.getStrengthCandidate().getConfidence());
        assertTrue(merged.getStrengthCandidate().getNote().contains("sprzeczne wartości"));
    }

    @Test
    void shouldRebuildStructuredDetailsFromMergedFullReadLines() {
        ItemImageImportCandidateParseResult nameVariant = parseResult(
                ItemImageImportTextParser.buildFullItemRead(List.of(
                        "ODLFIK VERATHEL",
                        "Starożytny unikatowy miecz",
                        "Moc przedmiotu. 900"
                ))
        );
        ItemImageImportCandidateParseResult weaponVariant = parseResult(
                ItemImageImportTextParser.buildFullItemRead(List.of(
                        "1 830 pkt. obrażeń na sek.",
                        "[1 350 - 1 978] pkt. obrażeń za trafienie",
                        "1,10 ataku na sekundę"
                ))
        );

        ItemImageImportCandidateParseResult merged = new ItemImageImportCandidateMerger()
                .merge(metadata, 2, java.util.List.of(nameVariant, weaponVariant));

        ItemImportDetails details = merged.getFullItemRead().getDetails();
        assertEquals("Odłamek Verathiela", details.getItemName());
        assertEquals("UNIQUE", details.getItemRarity());
        assertTrue(details.isAncient());
        assertEquals(EquipmentSlot.MAIN_HAND, details.getEquipmentSlot());
        assertEquals(900L, details.getItemPower());
        assertEquals(1830L, details.getWeaponDps());
        assertEquals(1664L, details.getAverageWeaponDamage());
    }

    @Test
    void shouldPreserveWeaponDamageRangeWhenMergedLinesMissParsedRange() {
        ItemImportDetails parsedWeaponDetails = new ItemImportDetails(
                "Odłamek Verathiela",
                "Miecz",
                "UNIQUE",
                true,
                EquipmentSlot.MAIN_HAND,
                900L,
                1830L,
                1350L,
                1978L,
                null,
                1.10d,
                "Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100], ale dodatkowo zużywają 25 pkt. podstawowego zasobu."
        );
        ItemImageImportCandidateParseResult detailsOnlyVariant = parseResult(new FullItemRead(
                "Odłamek Verathiela",
                "Starożytny unikatowy miecz",
                "UNIQUE",
                "Moc przedmiotu: 900",
                "1 830 pkt. obrażeń na sek.",
                List.of(
                        new FullItemReadLine(FullItemReadLineType.ITEM_NAME, "Odłamek Verathiela"),
                        new FullItemReadLine(FullItemReadLineType.TYPE_OR_SLOT, "Starożytny unikatowy miecz"),
                        new FullItemReadLine(FullItemReadLineType.ITEM_POWER, "Moc przedmiotu: 900"),
                        new FullItemReadLine(FullItemReadLineType.BASE_STAT, "1 830 pkt. obrażeń na sek.")
                ),
                parsedWeaponDetails
        ));

        ItemImageImportCandidateParseResult merged = new ItemImageImportCandidateMerger()
                .merge(metadata, 1, List.of(detailsOnlyVariant));
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(merged);

        assertEquals(1350L, merged.getFullItemRead().getDetails().getWeaponDamageMin());
        assertEquals(1978L, merged.getFullItemRead().getDetails().getWeaponDamageMax());
        assertEquals(1664L, merged.getFullItemRead().getDetails().getAverageWeaponDamage());
        assertEquals("1350", form.getWeaponDamageMin());
        assertEquals("1978", form.getWeaponDamageMax());
        assertEquals("1664", form.getAverageWeaponDamage());
    }

    @Test
    void shouldMergeVerathielAffixesToFourUniqueRowsAfterOcrVariants() {
        ItemImageImportCandidateParseResult firstVariant = parseResult(
                ItemImageImportTextParser.buildFullItemRead(List.of(
                        "ODŁAMEK VERATHIEL",
                        "Starożytny unikatowy miecz",
                        "+94 obrażeń od broni [94 - 157]",
                        "+2 141 maksymalnego zdrowia [1 831 - 2 200]",
                        "+545 pkt. zdrowia przy trafieniu [526 - 632]",
                        "Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]",
                        "Umiejętności Podstawowe zadają obrażenia zwiększone o 100%[x] [70 - 100], ale dodatkowo zużywają 25 pkt. podstawowego zasobu."
                ))
        );
        ItemImageImportCandidateParseResult secondVariant = parseResult(
                ItemImageImportTextParser.buildFullItemRead(List.of(
                        "+94 obrazen od broni [94 - 157] +2 141 maksymalnego zdrowia [1 831 - 2 200]",
                        "+545 pkt. zdrowia przy trafieniu [5 - 632] Szczęśliwy traf: maksymalnie 15% szans na odzyskanie +3 podstawowego zasobu [3 - 4]"
                ))
        );

        ItemImageImportCandidateParseResult merged = new ItemImageImportCandidateMerger()
                .merge(metadata, 2, java.util.List.of(firstVariant, secondVariant));
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(merged);

        assertEquals(4, form.getAffixes().size());
        assertEquals(1L, countAffix(form, ImportedItemAffixType.WEAPON_DAMAGE_FLAT));
        assertEquals(1L, countAffix(form, ImportedItemAffixType.MAXIMUM_LIFE));
        assertEquals(1L, countAffix(form, ImportedItemAffixType.LIFE_ON_HIT));
        assertEquals(1L, countAffix(form, ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE));
        ImportedItemAffix lifeOnHit = form.getAffixes().stream()
                .filter(affix -> affix.getType() == ImportedItemAffixType.LIFE_ON_HIT)
                .findFirst()
                .orElseThrow();
        assertEquals(526.0d, lifeOnHit.getRollRangeMin());
        assertEquals(632.0d, lifeOnHit.getRollRangeMax());
        assertTrue(form.getAffixes().stream().noneMatch(affix -> affix.getSourceText().contains("Umiejętności Podstawowe")));
    }

    @Test
    void shouldPreferAffixVariantWithActualRollRangeBeforeLineQualityScore() {
        ItemImageImportCandidateParseResult polishWithoutRange = parseResult(
                ItemImageImportTextParser.buildFullItemRead(List.of("+172 siły"))
        );
        ItemImageImportCandidateParseResult plainWithRange = parseResult(
                ItemImageImportTextParser.buildFullItemRead(List.of("+172 sily [150 - 180]"))
        );

        ItemImageImportCandidateParseResult merged = new ItemImageImportCandidateMerger()
                .merge(metadata, 2, List.of(polishWithoutRange, plainWithRange));
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(merged);
        ImportedItemAffix strength = form.getAffixes().stream()
                .filter(affix -> affix.getType() == ImportedItemAffixType.STRENGTH)
                .findFirst()
                .orElseThrow();

        assertEquals(172.0d, strength.getValue());
        assertEquals(150.0d, strength.getRollRangeMin());
        assertEquals(180.0d, strength.getRollRangeMax());
    }

    @Test
    void shouldPreferSameAffixVariantWithSingleValueBracketBeforeLineQualityScore() {
        ItemImageImportTextParser parser = new ItemImageImportTextParser();
        ItemImageImportCandidateParseResult plainVariant = parser.parse(metadata, """
                Generyczny Helm Testowy
                Starożytny mityczny unikatowy hełm
                +15,0% szansy na trafienie krytyczne
                """);
        ItemImageImportCandidateParseResult richerVariant = parser.parse(metadata, """
                Generyczny Helm Testowy
                Starożytny mityczny unikatowy hełm
                +15,0% szansy na trafienie krytyczne [12,0]%
                """);

        ItemImageImportCandidateParseResult merged = new ItemImageImportCandidateMerger()
                .merge(metadata, 2, List.of(plainVariant, richerVariant));
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(merged);
        ImportedItemAffix criticalChance = form.getAffixes().stream()
                .filter(affix -> affix.getType() == ImportedItemAffixType.CRITICAL_STRIKE_CHANCE)
                .findFirst()
                .orElseThrow();

        assertEquals(15.0d, criticalChance.getValue());
        assertEquals(12.0d, criticalChance.getReferenceValue());
        assertNull(criticalChance.getRollRangeMin());
        assertNull(criticalChance.getRollRangeMax());
    }

    @Test
    void shouldPreferAffixVariantWithOrphanRollRangeAfterParserAttachedIt() {
        ItemImageImportTextParser parser = new ItemImageImportTextParser();
        ItemImageImportCandidateParseResult polishWithoutRange = parser.parse(metadata, "+172 siły");
        ItemImageImportCandidateParseResult plainWithOrphanRange = parser.parse(metadata, """
                +172 sily
                [150 - 180]
                """);

        ItemImageImportCandidateParseResult merged = new ItemImageImportCandidateMerger()
                .merge(metadata, 2, List.of(polishWithoutRange, plainWithOrphanRange));
        ItemImportEditableForm form = new ItemImportEditableFormFactory().create(merged);
        ImportedItemAffix strength = form.getAffixes().stream()
                .filter(affix -> affix.getType() == ImportedItemAffixType.STRENGTH)
                .findFirst()
                .orElseThrow();

        assertEquals(172.0d, strength.getValue());
        assertEquals(150.0d, strength.getRollRangeMin());
        assertEquals(180.0d, strength.getRollRangeMax());
    }

    private ItemImageImportCandidateParseResult parseResult(ItemImportFieldCandidate<EquipmentSlot> slotCandidate,
                                                            ItemImportFieldCandidate<Long> weaponDamageCandidate,
                                                            ItemImportFieldCandidate<Double> strengthCandidate,
                                                            ItemImportFieldCandidate<Double> intelligenceCandidate,
                                                            ItemImportFieldCandidate<Double> thornsCandidate,
                                                            ItemImportFieldCandidate<Double> blockChanceCandidate,
                                                            ItemImportFieldCandidate<Double> retributionChanceCandidate) {
        return new ItemImageImportCandidateParseResult(
                metadata,
                FullItemRead.empty(),
                slotCandidate,
                weaponDamageCandidate,
                strengthCandidate,
                intelligenceCandidate,
                thornsCandidate,
                blockChanceCandidate,
                retributionChanceCandidate,
                "test"
        );
    }

    private ItemImageImportCandidateParseResult parseResult(FullItemRead fullItemRead) {
        return new ItemImageImportCandidateParseResult(
                metadata,
                fullItemRead,
                ItemImportFieldCandidate.unknown("slot"),
                ItemImportFieldCandidate.unknown("weapon"),
                ItemImportFieldCandidate.unknown("strength"),
                ItemImportFieldCandidate.unknown("intelligence"),
                ItemImportFieldCandidate.unknown("thorns"),
                ItemImportFieldCandidate.unknown("block"),
                ItemImportFieldCandidate.unknown("retribution"),
                "test"
        );
    }

    private static long countAffix(ItemImportEditableForm form, ImportedItemAffixType type) {
        return form.getAffixes().stream()
                .filter(affix -> affix.getType() == type)
                .count();
    }
}

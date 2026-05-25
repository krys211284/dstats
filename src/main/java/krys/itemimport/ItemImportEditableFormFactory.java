package krys.itemimport;

import krys.item.EquipmentSlot;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.tempering.ItemTemperingAffix;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.HoradricTuningPrism;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationAffixRoll;
import krys.transfiguration.TransfigurationValueProvenance;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Buduje formularz ręcznego potwierdzenia z wstępnie rozpoznanych pól. */
public final class ItemImportEditableFormFactory {
    private final ImportedItemAffixExtractor affixExtractor = new ImportedItemAffixExtractor();
    private final ImportedItemTemperingExtractor temperingExtractor = new ImportedItemTemperingExtractor();
    private final AspectRegistry aspectRegistry;

    public ItemImportEditableFormFactory() {
        this(ApplicationAspectRegistry.get());
    }

    ItemImportEditableFormFactory(AspectRegistry aspectRegistry) {
        this.aspectRegistry = aspectRegistry;
    }

    public ItemImportEditableForm create(ItemImageImportCandidateParseResult parseResult) {
        ItemImportDraft draft = createDraft(parseResult);
        return new ItemImportEditableForm(
                parseResult.getImageMetadata().getOriginalFilename(),
                toSlotValue(parseResult.getSlotCandidate().getSuggestedValue() == null
                        ? parseResult.getFullItemRead().getDetails().getEquipmentSlot()
                        : parseResult.getSlotCandidate().getSuggestedValue()),
                "0",
                toDoubleValue(parseResult.getStrengthCandidate().getSuggestedValue()),
                toDoubleValue(parseResult.getIntelligenceCandidate().getSuggestedValue()),
                toDoubleValue(parseResult.getThornsCandidate().getSuggestedValue()),
                toDoubleValue(parseResult.getBlockChanceCandidate().getSuggestedValue()),
                toDoubleValue(parseResult.getRetributionChanceCandidate().getSuggestedValue()),
                parseResult.getFullItemRead(),
                draft.getAffixes(),
                draft.getOcrSuggestedAspectId(),
                draft.getOcrAspectConfidence(),
                draft.getOcrSuggestedAspectId(),
                parseResult.getFullItemRead().getDetails(),
                draft.getTemperingAffixes(),
                draft.getMasterworking(),
                draft.getTransfiguration()
        );
    }

    public ItemImportDraft createDraft(ItemImageImportCandidateParseResult parseResult) {
        AspectRegistry.AspectMatch aspectMatch = aspectRegistry.suggestFromFullRead(parseResult.getFullItemRead())
                .orElse(new AspectRegistry.AspectMatch("", ItemImportFieldConfidence.UNKNOWN));
        EquipmentSlot effectiveSlot = parseResult.getSlotCandidate().getSuggestedValue() == null
                ? parseResult.getFullItemRead().getDetails().getEquipmentSlot()
                : parseResult.getSlotCandidate().getSuggestedValue();
        if (!aspectMatch.aspectId().isBlank()
                && aspectRegistry.findById(aspectMatch.aspectId())
                .filter(aspect -> aspect.allowsSlot(effectiveSlot))
                .isEmpty()) {
            aspectMatch = new AspectRegistry.AspectMatch("", ItemImportFieldConfidence.UNKNOWN);
        }
        List<ImportedItemAffix> affixes = affixExtractor.extractEditableAffixes(parseResult.getFullItemRead());
        List<ItemTemperingAffix> temperingAffixes = temperingExtractor.extractTemperingAffixes(parseResult.getFullItemRead());
        ItemMasterworking masterworking = detectMasterworking(parseResult.getFullItemRead(), temperingAffixes);
        ItemTransfiguration transfiguration = detectTransfiguration(parseResult.getFullItemRead());
        return new ItemImportDraft(
                parseResult,
                aspectMatch.aspectId(),
                aspectMatch.confidence(),
                affixes,
                temperingAffixes,
                masterworking,
                transfiguration
        );
    }

    private static ItemMasterworking detectMasterworking(FullItemRead fullItemRead, List<ItemTemperingAffix> temperingAffixes) {
        if (fullItemRead == null || !ItemImageImportTextParser.containsQuality25(
                fullItemRead.getLines().stream().map(FullItemReadLine::getText).toList())) {
            return ItemMasterworking.defaultState();
        }
        MasterworkedAffixSelection perfectedAffix = null;
        if (hasDisplayedPerfectedMaxAnimus(fullItemRead, temperingAffixes)) {
            perfectedAffix = MasterworkedAffixSelection.temperingAffix("defense_max_animus");
        }
        return new ItemMasterworking(25, 25, perfectedAffix);
    }

    private static boolean hasDisplayedPerfectedMaxAnimus(FullItemRead fullItemRead, List<ItemTemperingAffix> temperingAffixes) {
        boolean hasStoredMaxAnimus = temperingAffixes.stream()
                .anyMatch(affix -> "defense_max_animus".equals(affix.getDefinitionId())
                        && affix.isGreaterAffix()
                        && Math.abs(affix.getValue() - 5.0d) < 0.0001d);
        if (!hasStoredMaxAnimus) {
            return false;
        }
        return fullItemRead.getLines().stream()
                .map(FullItemReadLine::getText)
                .anyMatch(line -> normalize(line).contains("+12 DO MAKSYMALNEJ LICZBY KUMULACJI ANIMUSZU"));
    }

    private static ItemTransfiguration detectTransfiguration(FullItemRead fullItemRead) {
        if (fullItemRead == null || !fullItemRead.hasAnyData()) {
            return ItemTransfiguration.none();
        }
        boolean transfigured = hasLineContaining(fullItemRead, "PRZEISTOCZONY");
        boolean locked = hasLineContaining(fullItemRead, "BRAK MOZLIWOSCI MODYFIKACJI");
        Optional<TransfigurationAffixRoll> allStatsRoll = detectAllStatsTransfigurationRoll(fullItemRead);
        if (!transfigured && allStatsRoll.isEmpty()) {
            return ItemTransfiguration.none();
        }
        HoradricTransfigurationOutcome outcome = allStatsRoll.isPresent()
                ? HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX
                : HoradricTransfigurationOutcome.UNKNOWN;
        return new ItemTransfiguration(
                true,
                locked || transfigured,
                HoradricTuningPrism.NONE,
                outcome,
                "",
                allStatsRoll.orElse(null),
                "",
                null,
                null,
                false,
                ""
        );
    }

    private static Optional<TransfigurationAffixRoll> detectAllStatsTransfigurationRoll(FullItemRead fullItemRead) {
        Pattern valuePattern = Pattern.compile("\\+\\s*([0-9]+(?:[,.][0-9]+)?)");
        for (FullItemReadLine line : fullItemRead.getLines()) {
            String normalized = normalize(line.getText());
            if (!normalized.contains("DO WSZYSTKICH WSPOLCZYNNIKOW")
                    || normalized.contains("ODPORNOSCI")) {
                continue;
            }
            Matcher matcher = valuePattern.matcher(line.getText());
            if (!matcher.find()) {
                continue;
            }
            double displayedValue = Double.parseDouble(matcher.group(1).replace(',', '.'));
            return Optional.of(new TransfigurationAffixRoll(
                    "ALL_STATS",
                    displayedValue,
                    TransfigurationValueProvenance.GAME_DISPLAYED_VALUE,
                    ""
            ));
        }
        return Optional.empty();
    }

    private static boolean hasLineContaining(FullItemRead fullItemRead, String normalizedNeedle) {
        return fullItemRead.getLines().stream()
                .map(FullItemReadLine::getText)
                .anyMatch(line -> normalize(line).contains(normalizedNeedle));
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String toSlotValue(EquipmentSlot slot) {
        return slot == null ? "" : slot.name();
    }

    private static String toLongValue(Long value) {
        return value == null ? "" : Long.toString(value);
    }

    private static String toDoubleValue(Double value) {
        return value == null ? "" : String.format(Locale.US, "%.0f", value);
    }
}

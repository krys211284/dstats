package krys.itemimport.image;

import krys.item.EquipmentSlot;
import krys.itemimport.AffixDefinition;
import krys.itemimport.AffixRegistry;
import krys.itemimport.AffixValueUnit;
import krys.itemimport.ApplicationAffixRegistry;
import krys.itemimport.ApplicationAspectRegistry;
import krys.itemimport.FullItemRead;
import krys.itemimport.FullItemReadLine;
import krys.itemimport.FullItemReadLineType;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixSource;
import krys.itemimport.ImportedItemAffixType;
import krys.itemimport.ItemImportDetails;
import krys.itemimport.ItemImportEditableForm;
import krys.itemimport.ItemImportValueMode;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.socketing.ItemSocket;
import krys.socketing.ItemSocketing;
import krys.socketing.SocketGemRuneStat;
import krys.tempering.ApplicationTemperingAffixRegistry;
import krys.tempering.ItemTemperingAffix;
import krys.tempering.TemperingAffixDefinition;
import krys.tempering.TemperingAffixRegistry;
import krys.tempering.TemperingRuntimeStatus;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.HoradricTuningPrism;
import krys.transfiguration.ItemTransfiguration;
import krys.transfiguration.TransfigurationAffixCatalog;
import krys.transfiguration.TransfigurationAffixDefinition;
import krys.transfiguration.TransfigurationAffixRoll;
import krys.transfiguration.TransfigurationValueProvenance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/** Buduje edytowalny draft itemu z neutralnego odczytu obrazu, bez zapisu do biblioteki. */
public final class ImageItemImportDraftMapper {
    private final AffixRegistry affixRegistry;
    private final TemperingAffixRegistry temperingAffixRegistry;
    private final DisplayedItemValueSourceNormalizer valueSourceNormalizer;

    public ImageItemImportDraftMapper() {
        this(ApplicationAffixRegistry.get(), ApplicationTemperingAffixRegistry.get());
    }

    ImageItemImportDraftMapper(AffixRegistry affixRegistry,
                               TemperingAffixRegistry temperingAffixRegistry) {
        this.affixRegistry = Objects.requireNonNull(affixRegistry, "affixRegistry");
        this.temperingAffixRegistry = Objects.requireNonNull(temperingAffixRegistry, "temperingAffixRegistry");
        this.valueSourceNormalizer = new DisplayedItemValueSourceNormalizer();
    }

    public DraftResult map(Part1ReadResult part1ReadResult,
                           List<BlockRead> blockReads,
                           String sourceImageName) {
        Objects.requireNonNull(part1ReadResult, "part1ReadResult");
        List<BlockRead> safeBlocks = sorted(blockReads);
        List<String> needsReview = new ArrayList<>();
        needsReview.addAll(part1ReadResult.getParseFailures());

        Long itemArmor = null;
        Long weaponDps = null;
        Long weaponDamageMin = null;
        Long weaponDamageMax = null;
        Double attacksPerSecond = null;
        String baseItemValue = "";
        List<FullItemReadLine> fullReadLines = new ArrayList<>();
        List<ImportedItemAffix> affixes = new ArrayList<>();
        List<ItemTemperingAffix> temperingAffixes = new ArrayList<>();
        List<DisplayedItemValueSourceNormalizer.DisplayedAffix> displayedAffixes = new ArrayList<>();
        List<DisplayedItemValueSourceNormalizer.DisplayedTempering> displayedTemperingAffixes = new ArrayList<>();
        List<ItemSocket> sockets = new ArrayList<>();
        String selectedAspectId = "";
        String uniqueEffectText = "";
        ItemTransfiguration transfiguration = ItemTransfiguration.none();
        ItemMasterworking baseMasterworking = toMasterworking(part1ReadResult.getMasterworkingQuality(), needsReview);

        int ordinaryAffixOrder = 0;
        int socketIndex = 0;
        for (BlockRead blockRead : safeBlocks) {
            PropertyBlockSemanticResult semantic = blockRead.semanticResult();
            PropertyBlockValueReadResult value = blockRead.valueReadResult();
            String rawText = rawText(blockRead);
            fullReadLines.add(new FullItemReadLine(toFullReadLineType(semantic.getKind()), rawText));
            appendReviewDiagnostics(needsReview, semantic, value, rawText);

            if (semantic.getKind() == PropertyBlockSemanticKind.BASE_STAT) {
                if (value.getItemArmor() != null && itemArmor == null) {
                    itemArmor = part1ReadResult.getMythicUnique().orElse(false)
                            ? value.getItemArmor()
                            : reverseArmor(value.getItemArmor(), baseMasterworking, rawText, needsReview);
                    baseItemValue = rawText;
                }
                if (value.getWeaponDps() != null && weaponDps == null) {
                    weaponDps = value.getWeaponDps();
                    baseItemValue = baseItemValue.isBlank() ? rawText : baseItemValue;
                }
                if (value.getWeaponDamageMin() != null && weaponDamageMin == null) {
                    weaponDamageMin = value.getWeaponDamageMin();
                }
                if (value.getWeaponDamageMax() != null && weaponDamageMax == null) {
                    weaponDamageMax = value.getWeaponDamageMax();
                }
                if (value.getAttacksPerSecond() != null && attacksPerSecond == null) {
                    attacksPerSecond = value.getAttacksPerSecond();
                }
                continue;
            }

            if (semantic.getKind() == PropertyBlockSemanticKind.AFFIX) {
                toAffix(semantic, value, rawText, ++ordinaryAffixOrder, baseMasterworking,
                        part1ReadResult.getMythicUnique().orElse(false), needsReview)
                        .ifPresent(affix -> {
                            affixes.add(affix);
                            displayedAffixes.add(new DisplayedItemValueSourceNormalizer.DisplayedAffix(
                                    affix,
                                    value.getValue() == null ? affix.getValue() : value.getValue()
                            ));
                        });
                continue;
            }

            if (semantic.getKind() == PropertyBlockSemanticKind.TEMPERING) {
                toTempering(semantic, value, rawText, needsReview)
                        .ifPresent(temperingAffix -> {
                            temperingAffixes.add(temperingAffix);
                            displayedTemperingAffixes.add(new DisplayedItemValueSourceNormalizer.DisplayedTempering(
                                    temperingAffix,
                                    value.getValue() == null ? temperingAffix.getValue() : value.getValue()
                            ));
                        });
                continue;
            }

            if (semantic.getKind() == PropertyBlockSemanticKind.TRANSFIGURATION) {
                transfiguration = toTransfiguration(semantic, value, rawText, needsReview)
                        .orElse(transfiguration);
                continue;
            }

            if (semantic.getKind() == PropertyBlockSemanticKind.ASPECT) {
                if (selectedAspectId.isBlank()
                        && semantic.hasDefinitionId()
                        && ApplicationAspectRegistry.get().findById(semantic.getDefinitionId()).isPresent()) {
                    selectedAspectId = semantic.getDefinitionId();
                }
                if (uniqueEffectText.isBlank()) {
                    uniqueEffectText = rawText;
                }
                continue;
            }

            if (semantic.getKind() == PropertyBlockSemanticKind.SOCKET) {
                sockets.add(toSocket(socketIndex++, semantic, value, rawText));
            }
        }

        EquipmentSlot slot = part1ReadResult.getEquipmentSlot().orElse(null);
        Long averageWeaponDamage = average(weaponDamageMin, weaponDamageMax);
        ItemMasterworking masterworking = detectMasterworking(
                baseMasterworking,
                displayedAffixes,
                displayedTemperingAffixes,
                needsReview
        );
        String legacyWeaponDamage = slot == EquipmentSlot.MAIN_HAND && averageWeaponDamage != null
                ? Long.toString(averageWeaponDamage)
                : "0";
        ItemImportDetails details = new ItemImportDetails(
                part1ReadResult.getItemName(),
                part1ReadResult.getItemType(),
                part1ReadResult.getItemRarity(),
                part1ReadResult.getAncient().orElse(false),
                slot,
                optionalIntAsLong(part1ReadResult.getItemPower()),
                weaponDps,
                weaponDamageMin,
                weaponDamageMax,
                averageWeaponDamage,
                attacksPerSecond,
                itemArmor,
                uniqueEffectText,
                part1ReadResult.getMythicUnique().orElse(false)
        );
        FullItemRead fullItemRead = new FullItemRead(
                part1ReadResult.getItemName(),
                part1ReadResult.getItemType(),
                part1ReadResult.getItemRarity(),
                part1ReadResult.getItemPower().isPresent() ? Integer.toString(part1ReadResult.getItemPower().getAsInt()) : "",
                baseItemValue,
                fullReadLines,
                details
        );
        ItemImportEditableForm form = new ItemImportEditableForm(
                sourceImageName == null ? "" : sourceImageName,
                slot == null ? "" : slot.name(),
                legacyWeaponDamage,
                "",
                "",
                "",
                "",
                "",
                fullItemRead,
                affixes,
                selectedAspectId,
                details,
                temperingAffixes,
                masterworking,
                transfiguration,
                sockets.isEmpty() ? ItemSocketing.empty() : new ItemSocketing(sockets.size(), sockets),
                ItemImportValueMode.GAME_DISPLAYED_VALUE
        );
        return new DraftResult(form, List.copyOf(needsReview));
    }

    private Optional<ImportedItemAffix> toAffix(PropertyBlockSemanticResult semantic,
                                               PropertyBlockValueReadResult value,
                                               String rawText,
                                               int order,
                                               ItemMasterworking masterworking,
                                               boolean mythicUnique,
                                               List<String> needsReview) {
        if (!semantic.hasDefinitionId()) {
            needsReview.add("Wymaga sprawdzenia: affix bez jednoznacznej definicji: " + rawText);
            return Optional.empty();
        }
        AffixDefinition definition = affixRegistry.findById(semantic.getDefinitionId()).orElse(null);
        if (definition == null) {
            needsReview.add("Wymaga sprawdzenia: affix poza katalogiem: " + semantic.getDefinitionId());
            return Optional.empty();
        }
        if (value.getValue() == null) {
            needsReview.add("Wymaga sprawdzenia: affix bez aktualnej wartości: " + rawText);
            return Optional.empty();
        }
        String unit = value.getUnit().isBlank() ? defaultUnit(definition.getValueUnit()) : value.getUnit();
        double displayedValue = value.getValue();
        double sourceValue = reverseAffixSourceValue(
                new ImportedItemAffix(
                        definition.getFormType(),
                        displayedValue,
                        unit,
                        semantic.isGreaterAffix(),
                        order,
                        rawText,
                        ImportedItemAffixSource.OCR,
                        definition.getId(),
                        value.getRollRangeMin(),
                        value.getRollRangeMax(),
                        value.getReferenceValue(),
                        displayValue(value)
                ),
                displayedValue,
                masterworking,
                mythicUnique,
                rawText,
                needsReview
        );
        return Optional.of(new ImportedItemAffix(
                definition.getFormType(),
                sourceValue,
                unit,
                semantic.isGreaterAffix(),
                order,
                rawText,
                ImportedItemAffixSource.OCR,
                definition.getId(),
                value.getRollRangeMin(),
                value.getRollRangeMax(),
                value.getReferenceValue(),
                displayValue(value)
        ));
    }

    private Optional<ItemTemperingAffix> toTempering(PropertyBlockSemanticResult semantic,
                                                    PropertyBlockValueReadResult value,
                                                    String rawText,
                                                    List<String> needsReview) {
        if (!semantic.hasDefinitionId()) {
            needsReview.add("Wymaga sprawdzenia: hartowanie bez jednoznacznej definicji: " + rawText);
            return Optional.empty();
        }
        TemperingAffixDefinition definition = temperingAffixRegistry.findById(semantic.getDefinitionId()).orElse(null);
        if (definition == null) {
            needsReview.add("Wymaga sprawdzenia: hartowanie poza katalogiem: " + semantic.getDefinitionId());
            return Optional.empty();
        }
        if (value.getValue() == null || value.isAmbiguous() || !semantic.isNumericValueCertain()) {
            needsReview.add("Wymaga sprawdzenia: hartowanie bez pewnej aktualnej wartości: " + rawText);
            return Optional.empty();
        }
        double savedValue = semantic.isGreaterAffix() ? definition.greaterAffixValue() : value.getValue();
        if (semantic.isGreaterAffix() && !sameValue(savedValue, value.getValue())) {
            needsReview.add("Wymaga sprawdzenia: tooltip hartowania pokazuje " + displayValue(value)
                    + ", a istniejący zapis GA używa source value "
                    + formatNumber(savedValue) + ".");
        }
        return Optional.of(new ItemTemperingAffix(
                definition.getId(),
                definition.getCategory(),
                savedValue,
                rawText,
                rawText,
                TemperingRuntimeStatus.DATA_ONLY,
                semantic.isGreaterAffix()
        ));
    }

    private Optional<ItemTransfiguration> toTransfiguration(PropertyBlockSemanticResult semantic,
                                                           PropertyBlockValueReadResult value,
                                                           String rawText,
                                                           List<String> needsReview) {
        HoradricTransfigurationOutcome outcome = semantic.getTransfigurationOutcome();
        if (outcome == HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY
                || "BONUS_ITEM_QUALITY".equals(semantic.getDefinitionId())) {
            if (value.getValue() == null) {
                needsReview.add("Wymaga sprawdzenia: bonus jakości bez pewnej wartości: " + rawText);
                return Optional.empty();
            }
            if (value.isAmbiguous() || !semantic.isNumericValueCertain()) {
                needsReview.add("Wymaga sprawdzenia: bonus jakości z niepewną wartością: " + rawText);
            }
            return Optional.of(new ItemTransfiguration(true, true, HoradricTuningPrism.NONE,
                    HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY, "", null, "", null,
                    (int) Math.round(value.getValue()), false, rawText));
        }
        if (!semantic.hasDefinitionId()) {
            needsReview.add("Wymaga sprawdzenia: Przeistoczenie bez jednoznacznej definicji: " + rawText);
            return Optional.empty();
        }
        TransfigurationAffixDefinition definition = TransfigurationAffixCatalog.findById(semantic.getDefinitionId()).orElse(null);
        if (definition == null || value.getValue() == null) {
            needsReview.add("Wymaga sprawdzenia: affix Przeistoczenia bez pewnego katalogu/wartości: " + rawText);
            return Optional.empty();
        }
        if (value.isAmbiguous() || !semantic.isNumericValueCertain()) {
            needsReview.add("Wymaga sprawdzenia: affix Przeistoczenia z niepewną wartością: " + rawText);
        }
        TransfigurationAffixRoll roll = new TransfigurationAffixRoll(
                definition.getId(),
                value.getValue(),
                TransfigurationValueProvenance.GAME_DISPLAYED_VALUE,
                "",
                value.getRollRangeMin(),
                value.getRollRangeMax()
        );
        return Optional.of(new ItemTransfiguration(true, true, HoradricTuningPrism.NONE,
                HoradricTransfigurationOutcome.BONUS_TRANSFIGURATION_AFFIX, "", roll,
                "", null, null, false, rawText));
    }

    private static ItemSocket toSocket(int socketIndex,
                                       PropertyBlockSemanticResult semantic,
                                       PropertyBlockValueReadResult value,
                                       String rawText) {
        if ("EMPTY_SOCKET".equals(semantic.getDefinitionId())) {
            return ItemSocket.empty(socketIndex);
        }
        Double parameterValue = value.getParameters().stream()
                .map(PropertyBlockValueReadResult.NumericParameter::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(value.getValue());
        SocketGemRuneStat stat = new SocketGemRuneStat(
                rawText,
                "",
                parameterValue,
                ImportedItemAffixType.detectFromLine(rawText).orElse(null),
                rawText,
                "PROPERTY_BLOCK_SOCKET",
                SocketGemRuneStat.RUNTIME_STATUS,
                true
        );
        return ItemSocket.detectedStat(socketIndex, stat);
    }

    private static ItemMasterworking toMasterworking(OptionalInt quality, List<String> needsReview) {
        if (quality == null || quality.isEmpty()) {
            return ItemMasterworking.defaultState();
        }
        int value = quality.getAsInt();
        if (ItemMasterworking.isAllowedQualityStep(value)) {
            return ItemMasterworking.quality(value);
        }
        needsReview.add("Wymaga sprawdzenia: jakość Doskonalenia " + value
                + " nie jest dozwolonym krokiem (" + ItemMasterworking.allowedQualityStepsLabel() + ").");
        return ItemMasterworking.defaultState();
    }

    private Long reverseArmor(long displayedArmor,
                              ItemMasterworking masterworking,
                              String rawText,
                              List<String> needsReview) {
        OptionalLong sourceArmor = valueSourceNormalizer.reverseArmor(displayedArmor, masterworking);
        if (sourceArmor.isPresent()) {
            return sourceArmor.getAsLong();
        }
        needsReview.add("Wymaga sprawdzenia: niejednoznaczne odwrócenie pancerza DISPLAYED->SOURCE: " + rawText);
        return displayedArmor;
    }

    private double reverseAffixSourceValue(ImportedItemAffix displayedAffix,
                                           double displayedValue,
                                           ItemMasterworking masterworking,
                                           boolean mythicUnique,
                                           String rawText,
                                           List<String> needsReview) {
        if (mythicUnique) {
            return displayedValue;
        }
        OptionalDouble sourceValue = valueSourceNormalizer.reverseAffix(displayedAffix, displayedValue, masterworking);
        if (sourceValue.isPresent()) {
            return sourceValue.getAsDouble();
        }
        needsReview.add("Wymaga sprawdzenia: niejednoznaczne odwrócenie affixu DISPLAYED->SOURCE: " + rawText);
        return displayedValue;
    }

    private ItemMasterworking detectMasterworking(ItemMasterworking baseMasterworking,
                                                  List<DisplayedItemValueSourceNormalizer.DisplayedAffix> displayedAffixes,
                                                  List<DisplayedItemValueSourceNormalizer.DisplayedTempering> displayedTemperingAffixes,
                                                  List<String> needsReview) {
        if (baseMasterworking == null || baseMasterworking.getQualityCurrent() != ItemMasterworking.DEFAULT_QUALITY_MAX) {
            return baseMasterworking == null ? ItemMasterworking.defaultState() : baseMasterworking;
        }
        Optional<MasterworkedAffixSelection> perfectedAffix = valueSourceNormalizer.detectPerfectedAffix(
                displayedAffixes,
                displayedTemperingAffixes,
                baseMasterworking
        );
        if (perfectedAffix.isPresent()) {
            return new ItemMasterworking(
                    baseMasterworking.getQualityCurrent(),
                    baseMasterworking.getQualityMax(),
                    perfectedAffix.get()
            );
        }
        if (valueSourceNormalizer.hasAmbiguousPerfectedAffix(displayedAffixes, displayedTemperingAffixes, baseMasterworking)) {
            needsReview.add("Wymaga sprawdzenia: więcej niż jeden affix pasuje jako aktualnie doskonalony.");
        }
        return baseMasterworking;
    }

    private static FullItemReadLineType toFullReadLineType(PropertyBlockSemanticKind kind) {
        return switch (kind) {
            case BASE_STAT -> FullItemReadLineType.BASE_STAT;
            case IMPLICIT -> FullItemReadLineType.IMPLICIT;
            case AFFIX -> FullItemReadLineType.AFFIX;
            case TEMPERING -> FullItemReadLineType.TEMPERING;
            case ASPECT -> FullItemReadLineType.ASPECT;
            case SOCKET -> FullItemReadLineType.SOCKET;
            case TRANSFIGURATION, UNKNOWN, AMBIGUOUS -> FullItemReadLineType.OTHER;
        };
    }

    private static void appendReviewDiagnostics(List<String> needsReview,
                                                PropertyBlockSemanticResult semantic,
                                                PropertyBlockValueReadResult value,
                                                String rawText) {
        if (semantic.getKind() == PropertyBlockSemanticKind.UNKNOWN
                || semantic.getKind() == PropertyBlockSemanticKind.AMBIGUOUS) {
            needsReview.add("Wymaga sprawdzenia: blok " + semantic.getKind() + ": " + rawText);
        }
        if (!semantic.isNumericValueCertain() || value.isAmbiguous()) {
            needsReview.add("Wymaga sprawdzenia: NUMERIC_OCR_AMBIGUOUS / niepewna liczba: " + rawText);
        }
    }

    private static String rawText(BlockRead blockRead) {
        String semanticText = blockRead.semanticResult().getRawText();
        if (semanticText != null && !semanticText.isBlank()) {
            return semanticText;
        }
        return blockRead.readResult().getRawText();
    }

    private static List<BlockRead> sorted(List<BlockRead> blockReads) {
        if (blockReads == null) {
            return List.of();
        }
        return blockReads.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing((BlockRead block) -> block.readResult().getSourcePart().name())
                        .thenComparingInt(block -> block.readResult().getBlockOrder()))
                .toList();
    }

    private static Long optionalIntAsLong(OptionalInt value) {
        return value == null || value.isEmpty() ? null : (long) value.getAsInt();
    }

    private static Long average(Long min, Long max) {
        if (min == null || max == null) {
            return null;
        }
        return Math.round((min + max) / 2.0d);
    }

    private static String defaultUnit(AffixValueUnit unit) {
        return unit == AffixValueUnit.PERCENT ? "%" : "";
    }

    private static String displayValue(PropertyBlockValueReadResult value) {
        if (value.getValue() == null) {
            return "";
        }
        return formatNumber(value.getValue()) + value.getUnit();
    }

    private static String formatNumber(double value) {
        BigDecimal decimal = BigDecimal.valueOf(value).stripTrailingZeros();
        return decimal.scale() <= 0
                ? decimal.toPlainString()
                : String.format(Locale.US, "%s", decimal.toPlainString()).replace('.', ',');
    }

    private static boolean sameValue(double left, double right) {
        return Math.abs(left - right) < 0.0000001d;
    }

    public record BlockRead(PropertyBlockReadResult readResult,
                            PropertyBlockSemanticResult semanticResult,
                            PropertyBlockValueReadResult valueReadResult) {
        public BlockRead {
            Objects.requireNonNull(readResult, "readResult");
            Objects.requireNonNull(semanticResult, "semanticResult");
            Objects.requireNonNull(valueReadResult, "valueReadResult");
        }
    }

    public record DraftResult(ItemImportEditableForm form, List<String> reviewNotes) {
        public DraftResult {
            Objects.requireNonNull(form, "form");
            reviewNotes = List.copyOf(reviewNotes == null ? List.of() : reviewNotes);
        }
    }
}

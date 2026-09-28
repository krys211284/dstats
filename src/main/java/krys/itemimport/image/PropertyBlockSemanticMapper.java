package krys.itemimport.image;

import krys.itemimport.AffixRegistry;
import krys.itemimport.ApplicationAffixRegistry;
import krys.itemimport.ApplicationAspectRegistry;
import krys.itemimport.AspectDefinition;
import krys.itemimport.AspectRegistry;
import krys.tempering.ApplicationTemperingAffixRegistry;
import krys.tempering.TemperingAffixDefinition;
import krys.tempering.TemperingAffixRegistry;
import krys.transfiguration.HoradricTransfigurationOutcome;
import krys.transfiguration.TransfigurationAffixCatalog;
import krys.transfiguration.TransfigurationAffixDefinition;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/** Klasyfikuje neutralny odczyt PropertyBlock bez tworzenia domenowego affixu lub itemu. */
public final class PropertyBlockSemanticMapper {
    private static final String NUMERIC_AMBIGUOUS = "NUMERIC_OCR_AMBIGUOUS";
    private static final Pattern DIGIT = Pattern.compile("\\d");
    private static final Set<String> ASPECT_STOP_WORDS = Set.of(
            "ABY", "ALBO", "ALE", "DLA", "GDY", "JEST", "KTORA", "KTORY", "ORAZ", "PRZEZ",
            "TWOJE", "TWOJA", "TYCH", "W", "Z", "ZA", "ZE", "ZWIEKSZA", "ZWIEKSZONE"
    );

    private final AffixRegistry affixRegistry;
    private final TemperingAffixRegistry temperingRegistry;
    private final AspectRegistry aspectRegistry;

    public PropertyBlockSemanticMapper() {
        this(ApplicationAffixRegistry.get(), ApplicationTemperingAffixRegistry.get(), ApplicationAspectRegistry.get());
    }

    public PropertyBlockSemanticMapper(AffixRegistry affixRegistry,
                                       TemperingAffixRegistry temperingRegistry,
                                       AspectRegistry aspectRegistry) {
        this.affixRegistry = Objects.requireNonNull(affixRegistry, "affixRegistry");
        this.temperingRegistry = Objects.requireNonNull(temperingRegistry, "temperingRegistry");
        this.aspectRegistry = Objects.requireNonNull(aspectRegistry, "aspectRegistry");
    }

    public PropertyBlockSemanticResult map(PropertyBlock block, PropertyBlockReadResult readResult) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(readResult, "readResult");
        validatePair(block, readResult);

        String normalizedText = normalize(readResult.getRawText());
        PropertyBlockAnchorType anchorType = block.getAnchorType();
        boolean greaterAffix = anchorType == PropertyBlockAnchorType.GREATER_STAR;
        boolean numericValueCertain = numericValueCertain(readResult);
        List<String> diagnostics = new ArrayList<>(readResult.getFailureDiagnostics());
        if (!numericValueCertain && diagnostics.stream().noneMatch(value -> value.contains(NUMERIC_AMBIGUOUS))) {
            diagnostics.add(NUMERIC_AMBIGUOUS);
        }

        Classification classification;
        if (anchorType == PropertyBlockAnchorType.EMPTY_SOCKET
                || anchorType == PropertyBlockAnchorType.FILLED_SOCKET) {
            classification = new Classification(
                    PropertyBlockSemanticKind.SOCKET,
                    anchorType.name(),
                    List.of(anchorType.name()),
                    HoradricTransfigurationOutcome.NONE);
        } else if (anchorType == PropertyBlockAnchorType.TRANSFIGURATION_MARK) {
            classification = classifyTransfiguration(normalizedText);
        } else if (anchorType == PropertyBlockAnchorType.LEGENDARY_POWER_STAR
                || anchorType == PropertyBlockAnchorType.UNIQUE_POWER_STAR) {
            classification = classifyAspect(normalizedText);
        } else if (isBaseStat(normalizedText)) {
            classification = Classification.of(PropertyBlockSemanticKind.BASE_STAT);
        } else if (isImplicit(normalizedText)) {
            classification = Classification.of(PropertyBlockSemanticKind.IMPLICIT);
        } else {
            classification = classifyRegistryBackedText(normalizedText);
        }

        if (classification.kind() == PropertyBlockSemanticKind.AMBIGUOUS) {
            diagnostics.add("SEMANTIC_MATCH_AMBIGUOUS candidates=" + classification.candidateDefinitionIds());
        } else if ((classification.kind() == PropertyBlockSemanticKind.ASPECT
                || classification.kind() == PropertyBlockSemanticKind.TRANSFIGURATION)
                && classification.definitionId().isBlank()) {
            diagnostics.add("SEMANTIC_DEFINITION_NOT_FOUND kind=" + classification.kind());
        }

        return new PropertyBlockSemanticResult(
                block.getSourcePart(),
                block.getOrder(),
                anchorType,
                classification.kind(),
                classification.definitionId(),
                classification.candidateDefinitionIds(),
                greaterAffix,
                numericValueCertain,
                readResult.getRawText(),
                readResult.getNumericEvidence(),
                classification.transfigurationOutcome(),
                diagnostics
        );
    }

    private Classification classifyRegistryBackedText(String normalizedText) {
        List<String> affixIds = affixRegistry.findMatches(normalizedText).stream()
                .map(match -> match.definition().getId())
                .distinct()
                .toList();
        if (affixIds.size() == 1) {
            return Classification.defined(PropertyBlockSemanticKind.AFFIX, affixIds.getFirst());
        }
        if (affixIds.size() > 1) {
            return Classification.ambiguous(affixIds);
        }

        List<String> temperingIds = temperingRegistry.all().stream()
                .filter(definition -> textContainsDefinition(normalizedText, definition.getDisplayName()))
                .map(TemperingAffixDefinition::getId)
                .distinct()
                .toList();
        if (temperingIds.size() == 1) {
            return Classification.defined(PropertyBlockSemanticKind.TEMPERING, temperingIds.getFirst());
        }
        if (temperingIds.size() > 1) {
            return Classification.ambiguous(temperingIds);
        }
        return Classification.of(PropertyBlockSemanticKind.UNKNOWN);
    }

    private Classification classifyTransfiguration(String normalizedText) {
        if (normalizedText.contains("JAKOSCI PRZEDMIOTU") || normalizedText.contains("ITEM QUALITY")) {
            String id = HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY.name();
            return new Classification(
                    PropertyBlockSemanticKind.TRANSFIGURATION,
                    id,
                    List.of(id),
                    HoradricTransfigurationOutcome.BONUS_ITEM_QUALITY);
        }
        List<String> ids = TransfigurationAffixCatalog.definitions().stream()
                .filter(definition -> transfigurationMatches(normalizedText, definition))
                .map(TransfigurationAffixDefinition::getId)
                .distinct()
                .toList();
        if (ids.size() == 1) {
            return new Classification(
                    PropertyBlockSemanticKind.TRANSFIGURATION,
                    ids.getFirst(),
                    ids,
                    HoradricTransfigurationOutcome.UNKNOWN);
        }
        if (ids.size() > 1) {
            return new Classification(
                    PropertyBlockSemanticKind.AMBIGUOUS,
                    "",
                    ids,
                    HoradricTransfigurationOutcome.UNKNOWN);
        }
        return new Classification(
                PropertyBlockSemanticKind.TRANSFIGURATION,
                "",
                List.of(),
                HoradricTransfigurationOutcome.UNKNOWN);
    }

    private Classification classifyAspect(String normalizedText) {
        List<AspectCandidate> candidates = aspectRegistry.all().stream()
                .map(definition -> new AspectCandidate(definition, aspectScore(normalizedText, definition)))
                .filter(candidate -> candidate.score() >= 0.45d)
                .sorted(Comparator.comparingDouble(AspectCandidate::score).reversed())
                .toList();
        if (candidates.isEmpty()) {
            return Classification.of(PropertyBlockSemanticKind.ASPECT);
        }
        AspectCandidate best = candidates.getFirst();
        List<String> competingIds = candidates.stream()
                .filter(candidate -> best.score() - candidate.score() < 0.08d)
                .map(candidate -> candidate.definition().getId())
                .toList();
        if (competingIds.size() > 1) {
            return Classification.ambiguous(competingIds);
        }
        return Classification.defined(PropertyBlockSemanticKind.ASPECT, best.definition().getId());
    }

    private static double aspectScore(String normalizedText, AspectDefinition definition) {
        String normalizedName = normalize(definition.getDisplayName());
        if (!normalizedName.isBlank() && normalizedText.contains(normalizedName)) {
            return 1.0d;
        }
        Set<String> expectedTokens = significantTokens(normalize(definition.getEffectDescription()));
        if (expectedTokens.isEmpty()) {
            return 0.0d;
        }
        Set<String> actualTokens = significantTokens(normalizedText);
        long matches = expectedTokens.stream().filter(actualTokens::contains).count();
        if (matches < 5) {
            return 0.0d;
        }
        return (double) matches / expectedTokens.size();
    }

    private static Set<String> significantTokens(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : text.replaceAll("[^A-Z0-9]", " ").split("\\s+")) {
            if (token.length() >= 4 && !ASPECT_STOP_WORDS.contains(token) && !token.matches("\\d+")) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static boolean transfigurationMatches(String text, TransfigurationAffixDefinition definition) {
        return textContainsDefinition(text, definition.getDisplayName())
                || textContainsDefinition(text, definition.getSourceName());
    }

    private static boolean textContainsDefinition(String text, String definitionText) {
        String normalizedDefinition = normalize(definitionText)
                .replace("[+]", "")
                .replace("[X]", "")
                .trim();
        return !normalizedDefinition.isBlank() && text.contains(normalizedDefinition);
    }

    private static boolean isBaseStat(String text) {
        return text.contains("PKT. PANCERZA")
                || text.contains("PKT PANCERZA")
                || text.contains("OBRAZEN NA SEK")
                || text.contains("OBRAZEN ZA TRAFIENIE")
                || text.contains("ATAKU NA SEKUNDE");
    }

    private static boolean isImplicit(String text) {
        if (text.contains("OBRAZEN OD BRONI W GLOWNEJ RECE") || text.contains("OBRAZEN OD BRONI")) {
            return true;
        }
        return text.contains("SZANSY NA BLOK") && !text.stripLeading().startsWith("+");
    }

    private static boolean numericValueCertain(PropertyBlockReadResult result) {
        boolean containsNumber = DIGIT.matcher(result.getRawText()).find();
        boolean ambiguous = result.hasAmbiguousNumericEvidence()
                || result.getFailureDiagnostics().stream().anyMatch(value -> value.contains(NUMERIC_AMBIGUOUS));
        if (ambiguous) {
            return false;
        }
        return !containsNumber || !result.getNumericEvidence().isEmpty();
    }

    private static void validatePair(PropertyBlock block, PropertyBlockReadResult result) {
        if (block.getSourcePart() != result.getSourcePart() || block.getOrder() != result.getBlockOrder()) {
            throw new IllegalArgumentException("PropertyBlock i PropertyBlockReadResult muszą opisywać ten sam blok.");
        }
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

    private record AspectCandidate(AspectDefinition definition, double score) {
    }

    private record Classification(PropertyBlockSemanticKind kind,
                                  String definitionId,
                                  List<String> candidateDefinitionIds,
                                  HoradricTransfigurationOutcome transfigurationOutcome) {
        private Classification {
            definitionId = definitionId == null ? "" : definitionId;
            candidateDefinitionIds = List.copyOf(candidateDefinitionIds == null ? List.of() : candidateDefinitionIds);
            transfigurationOutcome = transfigurationOutcome == null
                    ? HoradricTransfigurationOutcome.NONE
                    : transfigurationOutcome;
        }

        private static Classification of(PropertyBlockSemanticKind kind) {
            return new Classification(kind, "", List.of(), HoradricTransfigurationOutcome.NONE);
        }

        private static Classification defined(PropertyBlockSemanticKind kind, String definitionId) {
            return new Classification(kind, definitionId, List.of(definitionId), HoradricTransfigurationOutcome.NONE);
        }

        private static Classification ambiguous(List<String> candidateDefinitionIds) {
            return new Classification(PropertyBlockSemanticKind.AMBIGUOUS, "", candidateDefinitionIds,
                    HoradricTransfigurationOutcome.NONE);
        }
    }
}

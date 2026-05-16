package krys.itemimport;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Katalog znanych affixów używany przez OCR i ręczną walidację itemu. */
public final class AffixRegistry {
    private final List<AffixDefinition> definitions;

    public AffixRegistry() {
        this(ApplicationAffixRegistry.seedDefinitions());
    }

    public AffixRegistry(List<AffixDefinition> definitions) {
        this.definitions = definitions == null ? List.of() : List.copyOf(definitions);
    }

    public List<AffixDefinition> all() {
        return definitions;
    }

    public Optional<AffixDefinition> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return definitions.stream()
                .filter(definition -> definition.getId().equals(id))
                .findFirst();
    }

    public Optional<AffixDefinition> findByType(ImportedItemAffixType type) {
        if (type == null) {
            return Optional.empty();
        }
        return definitions.stream()
                .filter(definition -> definition.getFormType() == type)
                .findFirst();
    }

    public List<AffixTextMatch> findMatches(String text) {
        String normalizedText = normalize(text);
        if (normalizedText.isBlank()) {
            return List.of();
        }
        List<AffixTextMatch> matches = new ArrayList<>();
        for (AffixDefinition definition : definitions) {
            if (!definition.isAutomaticMatchingAllowed()) {
                continue;
            }
            for (String alias : definition.getOcrAliases()) {
                String normalizedAlias = normalize(alias);
                int start = normalizedText.indexOf(normalizedAlias);
                if (start >= 0) {
                    matches.add(new AffixTextMatch(definition, start, start + normalizedAlias.length()));
                    break;
                }
            }
        }
        return matches.stream()
                .sorted(Comparator
                        .comparingInt(AffixTextMatch::start)
                        .thenComparing(match -> match.definition().getId()))
                .toList();
    }

    static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .trim();
    }

    /** Dopasowanie definicji affixu do fragmentu OCR. */
    public record AffixTextMatch(AffixDefinition definition, int start, int end) {
    }
}

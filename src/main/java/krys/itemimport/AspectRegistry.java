package krys.itemimport;

import krys.hero.HeroClass;
import krys.item.EquipmentSlot;

import java.text.Normalizer;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Katalog znanych aspektów używany przez import OCR i walidację formularza. */
public final class AspectRegistry {
    private final List<AspectDefinition> definitions;

    public AspectRegistry() {
        this(List.of(
                new AspectDefinition(
                        "inner-calm",
                        "Aspekt Wewnętrznego Spokoju",
                        EnumSet.of(EquipmentSlot.OFF_HAND),
                        EnumSet.of(HeroClass.PALADIN),
                        List.of("legendary", "damage")
                ),
                new AspectDefinition(
                        "main-hand-test-aspect",
                        "Aspekt testowy broni głównej",
                        EnumSet.of(EquipmentSlot.MAIN_HAND),
                        EnumSet.of(HeroClass.PALADIN),
                        List.of("test", "weapon")
                )
        ));
    }

    public AspectRegistry(List<AspectDefinition> definitions) {
        this.definitions = definitions == null ? List.of() : List.copyOf(definitions);
    }

    public List<AspectDefinition> all() {
        return definitions;
    }

    public List<AspectDefinition> allowedForSlot(EquipmentSlot slot) {
        return definitions.stream()
                .filter(definition -> definition.allowsSlot(slot))
                .toList();
    }

    public Optional<AspectDefinition> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return definitions.stream()
                .filter(definition -> definition.getId().equals(id))
                .findFirst();
    }

    public Optional<AspectMatch> suggestFromFullRead(FullItemRead fullItemRead) {
        if (fullItemRead == null || !fullItemRead.hasAnyData()) {
            return Optional.empty();
        }
        String aspectText = fullItemRead.getLines().stream()
                .filter(line -> line.getType() == FullItemReadLineType.ASPECT)
                .map(FullItemReadLine::getText)
                .reduce("", (left, right) -> left + " " + right);
        String normalized = normalize(aspectText);
        if (normalized.contains("ZADAJESZ OBRAZENIA ZWIEKSZONE")
                && normalized.contains("TA PREMIA JEST TRZY RAZY WIEKSZA")) {
            return findById("inner-calm")
                    .map(definition -> new AspectMatch(definition.getId(), ItemImportFieldConfidence.HIGH));
        }
        if (normalized.contains("ZADAJESZ OBRAZENIA ZWIEKSZONE")) {
            return findById("inner-calm")
                    .map(definition -> new AspectMatch(definition.getId(), ItemImportFieldConfidence.MEDIUM));
        }
        return Optional.empty();
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

    /** Sugestia aspektu pochodząca z OCR. */
    public record AspectMatch(String aspectId, ItemImportFieldConfidence confidence) {
    }
}

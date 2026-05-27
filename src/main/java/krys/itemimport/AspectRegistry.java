package krys.itemimport;

import krys.item.EquipmentSlot;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/** Katalog znanych aspektów używany przez import OCR i walidację formularza. */
public final class AspectRegistry {
    private final List<AspectDefinition> definitions;

    public AspectRegistry() {
        this(ApplicationAspectRegistry.seedDefinitions());
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
        String allText = fullItemRead.getItemName() + " "
                + fullItemRead.getItemTypeLine() + " "
                + fullItemRead.getRarity() + " "
                + fullItemRead.getDetails().getItemName() + " "
                + fullItemRead.getDetails().getItemType() + " "
                + fullItemRead.getDetails().getItemRarity() + " "
                + fullItemRead.getDetails().getUniqueEffectText() + " "
                + fullItemRead.getLines().stream()
                .map(FullItemReadLine::getText)
                .reduce("", (left, right) -> left + " " + right);
        String normalized = normalize(aspectText);
        String normalizedAll = normalize(allText);
        if (looksLikeVerathielUniqueAspect(normalizedAll)) {
            return findById("verathiel_shard")
                    .map(definition -> new AspectMatch(definition.getId(), ItemImportFieldConfidence.HIGH));
        }
        if (looksLikeHeirOfPerditionUniqueAspect(normalizedAll)) {
            return findById("heir_of_perdition")
                    .map(definition -> new AspectMatch(definition.getId(), ItemImportFieldConfidence.HIGH));
        }
        for (AspectDefinition definition : definitions) {
            String normalizedName = normalize(definition.getDisplayName());
            if (!normalizedName.isBlank() && normalizedAll.contains(normalizedName)) {
                return Optional.of(new AspectMatch(definition.getId(), ItemImportFieldConfidence.HIGH));
            }
        }
        if (normalized.contains("GDY MASZ UMOCNIENIE")
                && normalized.contains("ZADAJESZ OBRAZENIA ZWIEKSZONE")) {
            return findById("fortify_damage_increased")
                    .map(definition -> new AspectMatch(definition.getId(), ItemImportFieldConfidence.HIGH));
        }
        if (normalized.contains("ZADAJESZ OBRAZENIA ZWIEKSZONE")
                && normalized.contains("TA PREMIA JEST TRZY RAZY WIEKSZA")) {
            return findById("inner-calm")
                    .map(definition -> new AspectMatch(definition.getId(), ItemImportFieldConfidence.HIGH));
        }
        return Optional.empty();
    }

    private static boolean looksLikeVerathielUniqueAspect(String normalizedAll) {
        String collapsed = normalizedAll.replaceAll("[^A-Z0-9]", "");
        boolean itemContext = (collapsed.contains("VERATHEL") || collapsed.contains("VERATHIEL"))
                && (collapsed.contains("MIECZ") || collapsed.contains("SWORD"))
                && (collapsed.contains("UNIKAT") || collapsed.contains("UNIQUE"));
        boolean effectContext = collapsed.contains("UMIEJETNOSCIPODSTAWOWE")
                && (collapsed.contains("PODSTAWOWEGOZASOBU") || collapsed.contains("PODSTAWOWYZASOB"))
                && collapsed.contains("25")
                && collapsed.contains("70")
                && collapsed.contains("100");
        return itemContext && effectContext;
    }

    private static boolean looksLikeHeirOfPerditionUniqueAspect(String normalizedAll) {
        String collapsed = normalizedAll.replaceAll("[^A-Z0-9]", "");
        boolean itemContext = collapsed.contains("DZIEDZICZATRACENIA")
                || (collapsed.contains("HELM") && collapsed.contains("MITYCZNY") && collapsed.contains("UNIKAT"));
        boolean effectContext = collapsed.contains("LASKIMATKI")
                && collapsed.contains("PODDAJSIENIENAWISCI")
                && collapsed.contains("80");
        return itemContext && effectContext;
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

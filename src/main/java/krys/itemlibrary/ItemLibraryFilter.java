package krys.itemlibrary;

import krys.item.EquipmentSlot;
import krys.itemimport.ApplicationAspectRegistry;
import krys.itemimport.AspectDefinition;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/** Strukturalne filtry SSR dla widoku biblioteki zapisanych itemów. */
public final class ItemLibraryFilter {
    public static final String ASPECT_NONE = "__NONE__";

    private final String query;
    private final String slot;
    private final String itemType;
    private final String status;
    private final String aspect;
    private final String affix;
    private final boolean greaterOnly;

    public ItemLibraryFilter(String query,
                             String slot,
                             String itemType,
                             String status,
                             String aspect,
                             String affix,
                             boolean greaterOnly) {
        this.query = clean(query);
        this.slot = clean(slot);
        this.itemType = clean(itemType);
        this.status = clean(status);
        this.aspect = clean(aspect);
        this.affix = clean(affix);
        this.greaterOnly = greaterOnly;
    }

    public static ItemLibraryFilter empty() {
        return new ItemLibraryFilter("", "", "", "", "", "", false);
    }

    public static ItemLibraryFilter fromFields(Map<String, String> fields) {
        return new ItemLibraryFilter(
                fields.getOrDefault("q", ""),
                fields.getOrDefault("slot", ""),
                fields.getOrDefault("type", ""),
                fields.getOrDefault("status", ""),
                fields.getOrDefault("aspect", ""),
                fields.getOrDefault("affix", ""),
                "true".equals(fields.getOrDefault("greater", ""))
        );
    }

    public boolean matches(SavedImportedItem item, boolean active) {
        if (item == null) {
            return false;
        }
        if (!slot.isBlank() && !slot.equals(item.getSlot().name())) {
            return false;
        }
        if (!itemType.isBlank() && !normalize(itemType).equals(normalize(resolvedItemType(item)))) {
            return false;
        }
        if ("used".equals(status) && !active) {
            return false;
        }
        if ("unused".equals(status) && active) {
            return false;
        }
        if (!aspect.isBlank()) {
            if (ASPECT_NONE.equals(aspect)) {
                if (!item.getSelectedAspectId().isBlank()) {
                    return false;
                }
            } else if (!aspect.equals(item.getSelectedAspectId())) {
                return false;
            }
        }
        if (!affix.isBlank() && item.getAffixes().stream().noneMatch(candidate -> candidate.getType().name().equals(affix))) {
            return false;
        }
        if (greaterOnly && item.getAffixes().stream().noneMatch(ImportedItemAffix::isGreaterAffix)) {
            return false;
        }
        if (!query.isBlank() && !matchesTextQuery(item)) {
            return false;
        }
        return true;
    }

    private boolean matchesTextQuery(SavedImportedItem item) {
        String normalizedQuery = normalize(query);
        if (normalize(item.getFullItemRead().getItemName()).contains(normalizedQuery)
                || normalize(item.getSourceImageName()).contains(normalizedQuery)
                || normalize(item.getDisplayName()).contains(normalizedQuery)
                || normalize(aspectDisplayName(item.getSelectedAspectId())).contains(normalizedQuery)) {
            return true;
        }
        for (ImportedItemAffix affixValue : item.getAffixes()) {
            if (normalize(affixValue.getType().getDisplayName()).contains(normalizedQuery)
                    || normalize(affixValue.toDisplayLine()).contains(normalizedQuery)) {
                return true;
            }
        }
        return false;
    }

    public boolean isEmpty() {
        return query.isBlank()
                && slot.isBlank()
                && itemType.isBlank()
                && status.isBlank()
                && aspect.isBlank()
                && affix.isBlank()
                && !greaterOnly;
    }

    public String getQuery() {
        return query;
    }

    public String getSlot() {
        return slot;
    }

    public String getItemType() {
        return itemType;
    }

    public String getStatus() {
        return status;
    }

    public String getAspect() {
        return aspect;
    }

    public String getAffix() {
        return affix;
    }

    public boolean isGreaterOnly() {
        return greaterOnly;
    }

    public static String resolvedItemType(SavedImportedItem item) {
        String typeLine = item.getFullItemRead().getItemTypeLine();
        String normalized = normalize(typeLine);
        if (normalized.contains("TARCZA")) {
            return "Tarcza";
        }
        if (normalized.contains("BUTY")) {
            return "Buty";
        }
        if (normalized.contains("BRON GLOWNA") || item.getSlot() == EquipmentSlot.MAIN_HAND) {
            return "Broń główna";
        }
        if (typeLine != null && !typeLine.isBlank()) {
            return typeLine;
        }
        return ItemLibraryPresentationSupport.slotDisplayName(item.getSlot());
    }

    private static String aspectDisplayName(String selectedAspectId) {
        if (selectedAspectId == null || selectedAspectId.isBlank()) {
            return "";
        }
        return ApplicationAspectRegistry.get()
                .findById(selectedAspectId)
                .map(AspectDefinition::getDisplayName)
                .orElse(selectedAspectId);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
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
}

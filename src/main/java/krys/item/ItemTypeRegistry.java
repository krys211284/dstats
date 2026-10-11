package krys.item;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Jedyny katalog typów i aliasów na granicy OCR, formularza i historycznego zapisu. */
public final class ItemTypeRegistry {
    private ItemTypeRegistry() { }

    public static List<ItemType> selectableTypes() {
        return Arrays.stream(ItemType.values()).filter(ItemType::isSelectable).toList();
    }

    public static ItemType fromTechnicalId(String id) {
        if (id == null) { return ItemType.UNKNOWN; }
        return Arrays.stream(ItemType.values()).filter(type -> type.getTechnicalId().equalsIgnoreCase(id.trim()))
                .findFirst().orElse(ItemType.UNKNOWN);
    }

    public static ItemType resolve(String raw) {
        ItemType technical = fromTechnicalId(raw);
        if (technical != ItemType.UNKNOWN) { return technical; }
        String normalized = normalize(raw);
        return Arrays.stream(ItemType.values()).filter(type -> type.getLegacyAliases().stream()
                .anyMatch(alias -> normalize(alias).equals(normalized))).findFirst().orElse(ItemType.UNKNOWN);
    }

    public static List<ItemType> matchingTypes(String classification) {
        String normalized = normalize(classification);
        return Arrays.stream(ItemType.values()).filter(type -> type != ItemType.UNKNOWN)
                .filter(type -> type.getLegacyAliases().stream().anyMatch(alias ->
                        Pattern.compile("(?<![a-z0-9])" + Pattern.quote(normalize(alias)) + "(?![a-z0-9])")
                                .matcher(normalized).find()))
                // Ogólne słowo Miecz nie potwierdza nieobsługiwanego wariantu dwuręcznego.
                .filter(type -> type != ItemType.SWORD || !Pattern.compile("(?:miecz|sword)\\s+(?:dwureczn\\w*|two.hand\\w*)")
                        .matcher(normalized).find())
                .toList();
    }

    public static String displayOrLegacy(String raw) {
        ItemType type = resolve(raw);
        if (type == ItemType.UNKNOWN && Pattern.compile("^(?:starozytn\\w*|mityczn\\w*|unikatow\\w*|legendarn\\w*|rzadk\\w*|ancestral|unique|legendary|rare)\\b")
                .matcher(normalize(raw)).find()) {
            List<ItemType> matches = matchingTypes(raw);
            if (matches.size() == 1) { type = matches.getFirst(); }
        }
        return type == ItemType.UNKNOWN ? (raw == null ? "" : raw.trim()) : type.getDisplayName();
    }

    private static String normalize(String raw) {
        return Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFD).replace('ł', 'l').replace('Ł', 'L')
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
    }
}

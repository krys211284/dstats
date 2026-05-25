package krys.socketing;

import krys.item.EquipmentSlot;
import krys.itemimport.ItemImportDetails;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Helpery prezentacji gniazd i gemów. Efekty pozostają runtime nieaktywne. */
public final class SocketingPresentationSupport {
    private SocketingPresentationSupport() {
    }

    public static Optional<SocketEffectContext> resolveContext(EquipmentSlot slot, ItemImportDetails details) {
        if (slot == EquipmentSlot.MAIN_HAND) {
            return Optional.of(SocketEffectContext.WEAPON);
        }
        if (slot == EquipmentSlot.AMULET || slot == EquipmentSlot.RING) {
            return Optional.of(SocketEffectContext.JEWELRY);
        }
        if (slot == EquipmentSlot.OFF_HAND) {
            return Optional.of(SocketEffectContext.ARMOR);
        }
        if (slot == EquipmentSlot.HELMET || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.GLOVES
                || slot == EquipmentSlot.PANTS || slot == EquipmentSlot.BOOTS) {
            return Optional.of(SocketEffectContext.ARMOR);
        }
        if (details != null) {
            String normalized = normalize(details.getItemType());
            if (normalized.contains("TARCZA")) {
                return Optional.of(SocketEffectContext.ARMOR);
            }
        }
        return Optional.empty();
    }

    public static String effectLabel(GemDefinition definition, Optional<SocketEffectContext> context) {
        if (definition == null) {
            return "";
        }
        if (context.isPresent()) {
            return definition.effectFor(context.get()).getDisplayText();
        }
        return "Broń: " + definition.getWeaponEffect().getDisplayText()
                + " · Pancerz: " + definition.getArmorEffect().getDisplayText()
                + " · Biżuteria: " + definition.getJewelryEffect().getDisplayText();
    }

    public static List<String> compactLines(ItemSocketing socketing, EquipmentSlot slot, ItemImportDetails details) {
        ItemSocketing safe = socketing == null ? ItemSocketing.empty() : socketing;
        if (safe.getSocketCount() <= 0) {
            return List.of();
        }
        Optional<SocketEffectContext> context = resolveContext(slot, details);
        List<String> lines = new ArrayList<>();
        for (int index = 0; index < safe.getSocketCount(); index++) {
            ItemSocket socket = safe.socketAt(index);
            lines.add(socketLine(socket, context));
        }
        return List.copyOf(lines);
    }

    public static String socketLine(ItemSocket socket, Optional<SocketEffectContext> context) {
        if (socket == null || socket.getContentType() != SocketContentType.GEM || socket.getGemId().isBlank()) {
            return (socket == null ? "1" : Integer.toString(socket.getIndex() + 1)) + ": Puste";
        }
        GemDefinition definition = GemCatalog.findById(socket.getGemId()).orElse(null);
        if (definition == null) {
            return (socket.getIndex() + 1) + ": Nieznany gem (" + socket.getGemId() + ")";
        }
        return (socket.getIndex() + 1) + ": " + definition.getDisplayName()
                + " · " + effectLabel(definition, context)
                + " · Runtime nieaktywny";
    }

    public static String compactSummary(ItemSocketing socketing, EquipmentSlot slot, ItemImportDetails details) {
        List<String> lines = compactLines(socketing, slot, details);
        if (lines.isEmpty()) {
            return "";
        }
        return "Gniazda · " + String.join(" · ", lines);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace('Ł', 'L')
                .replace('ł', 'l')
                .replaceAll("\\p{M}", "")
                .toUpperCase(java.util.Locale.ROOT);
    }
}

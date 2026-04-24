package krys.itemlibrary;

import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.item.ItemStatType;
import krys.itemimport.CurrentBuildImportableStats;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Wspólne formatowanie slotów i wkładu itemów dla SSR oraz prezentacji searcha. */
public final class ItemLibraryPresentationSupport {
    private ItemLibraryPresentationSupport() {
    }

    public static String slotDisplayName(EquipmentSlot slot) {
        return switch (slot) {
            case HELMET -> "Hełm";
            case AMULET -> "Amulet";
            case MAIN_HAND -> "Broń główna";
            case OFF_HAND -> "Ręka dodatkowa";
            case CHEST -> "Pancerz";
            case GLOVES -> "Rękawice";
            case PANTS -> "Spodnie";
            case RING -> "Pierścień";
            case BOOTS -> "Buty";
        };
    }

    public static String heroSlotDisplayName(HeroEquipmentSlot slot) {
        return slot.getDisplayName();
    }

    public static String itemStatDisplayName(ItemStatType statType) {
        return switch (statType) {
            case STRENGTH -> "Siła";
            case INTELLIGENCE -> "Inteligencja";
            case CRIT_DAMAGE -> "Obrażenia krytyczne";
            case MAIN_HAND_WEAPON_DAMAGE -> "Obrażenia broni głównej";
            case THORNS -> "Kolce";
            case BLOCK_CHANCE -> "Szansa bloku";
            case RETRIBUTION_CHANCE -> "Szansa retribution";
        };
    }

    public static String itemContributionLabel(SavedImportedItem item) {
        return contributionLabel(new CurrentBuildImportableStats(
                item.getWeaponDamage(),
                item.getStrength(),
                item.getIntelligence(),
                item.getThorns(),
                item.getBlockChance(),
                item.getRetributionChance()
        ));
    }

    public static String contributionLabel(CurrentBuildImportableStats contribution) {
        List<String> labels = new ArrayList<>();
        if (contribution.getWeaponDamage() > 0L) {
            labels.add("obrażenia broni=" + contribution.getWeaponDamage());
        }
        if (contribution.getStrength() > 0.0d) {
            labels.add("siła=" + formatWhole(contribution.getStrength()));
        }
        if (contribution.getIntelligence() > 0.0d) {
            labels.add("inteligencja=" + formatWhole(contribution.getIntelligence()));
        }
        if (contribution.getThorns() > 0.0d) {
            labels.add("kolce=" + formatWhole(contribution.getThorns()));
        }
        if (contribution.getBlockChance() > 0.0d) {
            labels.add("szansa bloku=" + formatDecimal(contribution.getBlockChance()) + "%");
        }
        if (contribution.getRetributionChance() > 0.0d) {
            labels.add("szansa retribution=" + formatDecimal(contribution.getRetributionChance()) + "%");
        }
        return labels.isEmpty() ? "Brak wkładu do buildu" : String.join(", ", labels);
    }

    public static String shortContributionLabel(SavedImportedItem item) {
        List<String> labels = new ArrayList<>();
        if (item.getWeaponDamage() > 0L) {
            labels.add("obr. broni +" + item.getWeaponDamage());
        }
        if (item.getStrength() > 0.0d) {
            labels.add("siła +" + formatWhole(item.getStrength()));
        }
        if (item.getIntelligence() > 0.0d) {
            labels.add("int. +" + formatWhole(item.getIntelligence()));
        }
        if (item.getThorns() > 0.0d) {
            labels.add("kolce +" + formatWhole(item.getThorns()));
        }
        if (item.getBlockChance() > 0.0d) {
            labels.add("blok +" + formatDecimal(item.getBlockChance()) + "%");
        }
        if (item.getRetributionChance() > 0.0d) {
            labels.add("retribution +" + formatDecimal(item.getRetributionChance()) + "%");
        }
        return labels.isEmpty() ? "Brak wkładu" : String.join(" • ", labels);
    }

    public static String userItemIdentifier(SavedImportedItem item) {
        return "#" + item.getItemId() + " / " + item.getSourceImageName();
    }

    public static String formatWhole(double value) {
        return String.format(Locale.US, "%.0f", value);
    }

    public static String formatDecimal(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}

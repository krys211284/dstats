package krys.itemlibrary;

import krys.item.EquipmentSlot;
import krys.item.HeroEquipmentSlot;
import krys.item.ItemStatType;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemimport.ImportedItemAffix;
import krys.itemimport.ImportedItemAffixType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Wspólne formatowanie slotów, wkładu itemów i affixów dla SSR oraz prezentacji searcha. */
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
        if (item.getWeaponDps() != null) {
            labels.add("DPS " + item.getWeaponDps());
        }
        if (item.getWeaponDamageMin() != null && item.getWeaponDamageMax() != null) {
            labels.add(item.getWeaponDamageMin() + " - " + item.getWeaponDamageMax() + " obr.");
        }
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
        for (ImportedItemAffix affix : item.getAffixes()) {
            if (affix.getType() == ImportedItemAffixType.MAXIMUM_LIFE) {
                labels.add("+" + formatWhole(affix.getValue()) + " zdrowia");
            } else if (affix.getType() == ImportedItemAffixType.WEAPON_DAMAGE_FLAT) {
                labels.add("+" + formatWhole(affix.getValue()) + " obr. broni");
            } else if (affix.getType() == ImportedItemAffixType.LIFE_ON_HIT) {
                labels.add("+" + formatWhole(affix.getValue()) + " zdrowia przy traf.");
            } else if (affix.getType() == ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE) {
                labels.add("Lucky Hit 15%: " + rollValueWithPlus(affix) + " zasobu");
            }
        }
        return labels.isEmpty() ? "Brak wkładu" : String.join(" • ", labels);
    }

    public static String userItemIdentifier(SavedImportedItem item) {
        return "#" + item.getItemId() + " / " + item.getSourceImageName();
    }

    public static String canonicalItemName(SavedImportedItem item) {
        if (item == null) {
            return "Item";
        }
        if (item.getItemName() != null && !item.getItemName().isBlank()) {
            return item.getItemName();
        }
        if (item.getDisplayName() != null && !item.getDisplayName().isBlank()) {
            return item.getDisplayName();
        }
        if (item.getFullItemRead() != null && !item.getFullItemRead().getItemName().isBlank()) {
            return item.getFullItemRead().getItemName();
        }
        return slotDisplayName(item.getSlot()) + " / " + item.getSourceImageName();
    }

    public static String formatAffixForList(ImportedItemAffix affix) {
        if (affix == null) {
            return "";
        }
        String line;
        if (affix.getType() == ImportedItemAffixType.LUCKY_HIT_PRIMARY_RESOURCE) {
            line = "Szczęśliwy traf: maks. 15% szans na odzyskanie "
                    + rollValueWithPlus(affix)
                    + " podstawowego zasobu";
        } else {
            line = affix.getType().formatLine(affix.getValue());
        }
        return withGreaterPrefix(affix, line);
    }

    public static String formatAffixForDetails(ImportedItemAffix affix) {
        if (affix == null) {
            return "";
        }
        String line = switch (affix.getType()) {
            case WEAPON_DAMAGE_FLAT -> "+" + formatAffixNumber(affix.getValue()) + " obrażeń od broni";
            case MAXIMUM_LIFE -> "+" + formatAffixNumber(affix.getValue()) + " maksymalnego zdrowia";
            case LIFE_ON_HIT -> "+" + formatAffixNumber(affix.getValue()) + " zdrowia przy trafieniu";
            case LIFE_ON_KILL -> "+" + formatAffixNumber(affix.getValue()) + " zdrowia za zabicie";
            case ALL_DAMAGE_MULTIPLIER -> "Mnożnik x" + formatAffixNumber(affix.getValue()) + "% wszystkich obrażeń";
            case DAMAGE_OVER_TIME_MULTIPLIER -> "Mnożnik x" + formatAffixNumber(affix.getValue()) + "% obrażeń z upływem czasu";
            case LUCKY_HIT_PRIMARY_RESOURCE -> "Szczęśliwy traf: maksymalnie 15% szans na odzyskanie "
                    + rollValueWithPlus(affix)
                    + " podstawowego zasobu";
            default -> affix.getType().formatLine(affix.getValue());
        };
        String range = formatRollRange(affix);
        if (!range.isBlank()) {
            line += " [" + range + "]";
        }
        return withGreaterPrefix(affix, line);
    }

    public static String formatRollRange(ImportedItemAffix affix) {
        if (affix == null || affix.getRollRangeMin() == null || affix.getRollRangeMax() == null) {
            return "";
        }
        return formatAffixNumber(affix.getRollRangeMin()) + " - " + formatAffixNumber(affix.getRollRangeMax());
    }

    private static String withGreaterPrefix(ImportedItemAffix affix, String line) {
        if (!affix.isGreaterAffix()) {
            return line;
        }
        return "★ " + line.replaceFirst("^[*★⭐✦]\\s*", "");
    }

    private static String rollValueWithPlus(ImportedItemAffix affix) {
        if (affix.getDisplayValue() != null && !affix.getDisplayValue().isBlank()) {
            return affix.getDisplayValue();
        }
        return "+" + formatAffixNumber(affix.getValue());
    }

    private static String formatAffixNumber(double value) {
        if (Math.rint(value) == value) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.1f", value).replace('.', ',');
    }

    public static String formatWhole(double value) {
        return String.format(Locale.US, "%.0f", value);
    }

    public static String formatDecimal(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}

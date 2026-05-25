package krys.socketing;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Katalog GemCatalog v1 z wartościami potwierdzonymi screenami z gry. */
public final class GemCatalog {
    private static final int[] STANDARD_WEAPON_VALUES = {14, 16, 18, 20, 22, 24, 28, 32};
    private static final int[] STANDARD_ARMOR_VALUES = {10, 20, 30, 40, 60, 90, 120, 150};
    private static final int[] STANDARD_JEWELRY_VALUES = {50, 250, 450, 900, 1750, 2625, 3500, 4375};
    private static final int[] DIAMOND_WEAPON_VALUES = {10, 12, 14, 16, 18, 20, 24, 28};
    private static final int[] DIAMOND_ARMOR_VALUES = {3, 7, 10, 13, 20, 30, 40, 50};
    private static final int[] DIAMOND_JEWELRY_VALUES = {7, 36, 64, 129, 250, 375, 500, 625};
    private static final String[] AMETHYST_ARMOR_VALUES = {"2,0", "4,0", "6,0", "8,0", "12,0", "18,0", "24,0", "30,0"};
    private static final String[] SKULL_ARMOR_VALUES = {"1,0", "1,2", "1,5", "1,8", "2,1", "2,5", "2,8", "3,0"};
    private static final List<GemDefinition> DEFINITIONS = buildDefinitions();
    private static final Map<String, GemDefinition> BY_ID = buildById(DEFINITIONS);

    private GemCatalog() {
    }

    public static List<GemDefinition> all() {
        return DEFINITIONS;
    }

    public static Optional<GemDefinition> findById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static List<GemDefinition> byFamily(GemFamily family) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.getFamily() == family)
                .toList();
    }

    private static List<GemDefinition> buildDefinitions() {
        List<GemDefinition> definitions = new ArrayList<>();
        addStandardFamily(definitions, GemFamily.RUBY, "Ognia i Świętości", "siły", "odporności na Ogień", rubyRequiredLevels());
        addStandardFamily(definitions, GemFamily.SAPPHIRE, "Zimna", "siły woli", "odporności na Zimno", null);
        addStandardFamily(definitions, GemFamily.EMERALD, "Trucizny", "zręczności", "odporności na Truciznę", null);
        addStandardFamily(definitions, GemFamily.TOPAZ, "Błyskawic", "inteligencji", "odporności na Błyskawice", null);
        addAmethyst(definitions);
        addSkull(definitions);
        addDiamond(definitions);
        return List.copyOf(definitions);
    }

    private static void addStandardFamily(List<GemDefinition> definitions,
                                          GemFamily family,
                                          String weaponLabel,
                                          String armorLabel,
                                          String jewelryLabel,
                                          Integer[] requiredLevels) {
        GemTier[] tiers = GemTier.values();
        for (int index = 0; index < tiers.length; index++) {
            definitions.add(new GemDefinition(
                    id(family, tiers[index]),
                    family,
                    tiers[index],
                    masculineName(family.getDisplayName(), tiers[index]),
                    rarityLabel(tiers[index]),
                    requiredLevels == null ? null : requiredLevels[index],
                    "x" + STANDARD_WEAPON_VALUES[index] + "% " + weaponLabel,
                    "+" + STANDARD_ARMOR_VALUES[index] + " " + armorLabel,
                    "+" + formatWhole(STANDARD_JEWELRY_VALUES[index]) + " " + jewelryLabel,
                    GemValueVerificationStatus.VERIFIED_SCREENSHOT
            ));
        }
    }

    private static void addAmethyst(List<GemDefinition> definitions) {
        GemTier[] tiers = GemTier.values();
        for (int index = 0; index < tiers.length; index++) {
            definitions.add(new GemDefinition(
                    id(GemFamily.AMETHYST, tiers[index]),
                    GemFamily.AMETHYST,
                    tiers[index],
                    masculineName("Ametyst", tiers[index]),
                    rarityLabel(tiers[index]),
                    null,
                    "x" + STANDARD_WEAPON_VALUES[index] + "% Cienia",
                    "+" + AMETHYST_ARMOR_VALUES[index] + "% generowania bariery",
                    "+" + formatWhole(STANDARD_JEWELRY_VALUES[index]) + " odporności na Cień",
                    GemValueVerificationStatus.VERIFIED_SCREENSHOT
            ));
        }
    }

    private static void addSkull(List<GemDefinition> definitions) {
        GemTier[] tiers = GemTier.values();
        for (int index = 0; index < tiers.length; index++) {
            definitions.add(new GemDefinition(
                    id(GemFamily.SKULL, tiers[index]),
                    GemFamily.SKULL,
                    tiers[index],
                    skullName(tiers[index]),
                    rarityLabel(tiers[index]),
                    null,
                    "x" + STANDARD_WEAPON_VALUES[index] + "% Fizycznych",
                    "+" + SKULL_ARMOR_VALUES[index] + "% otrzymywanego leczenia",
                    "+" + formatWhole(STANDARD_JEWELRY_VALUES[index]) + " odporności na obrażenia Fizyczne",
                    GemValueVerificationStatus.VERIFIED_SCREENSHOT
            ));
        }
    }

    private static void addDiamond(List<GemDefinition> definitions) {
        GemTier[] tiers = GemTier.values();
        for (int index = 0; index < tiers.length; index++) {
            definitions.add(new GemDefinition(
                    id(GemFamily.DIAMOND, tiers[index]),
                    GemFamily.DIAMOND,
                    tiers[index],
                    masculineName("Diament", tiers[index]),
                    rarityLabel(tiers[index]),
                    null,
                    "x" + DIAMOND_WEAPON_VALUES[index] + "% wszystkich obrażeń",
                    "+" + DIAMOND_ARMOR_VALUES[index] + " pkt. do wszystkich współczynników",
                    "+" + formatWhole(DIAMOND_JEWELRY_VALUES[index]) + " odporności na wszystkie żywioły",
                    GemValueVerificationStatus.VERIFIED_SCREENSHOT
            ));
        }
    }

    private static String id(GemFamily family, GemTier tier) {
        return family.getIdPrefix() + "_" + tier.getIdSuffix();
    }

    private static String masculineName(String familyName, GemTier tier) {
        return switch (tier) {
            case CHIPPED -> "Surowy " + familyName;
            case CRUDE -> "Nadkruszony " + familyName;
            case STANDARD -> familyName;
            case FLAWLESS -> "Nieskazitelny " + familyName;
            case ROYAL -> "Królewski " + familyName;
            case GRAND -> "Wspaniały " + familyName;
            case HORADRIC -> "Horadryjski " + familyName;
            case FLAWLESS_HORADRIC -> "Nieskazitelny Horadryjski " + familyName;
        };
    }

    private static String skullName(GemTier tier) {
        return switch (tier) {
            case CHIPPED -> "Surowa Czaszka";
            case CRUDE -> "Nadkruszona Czaszka";
            case STANDARD -> "Czaszka";
            case FLAWLESS -> "Nieskazitelna Czaszka";
            case ROYAL -> "Królewska Czaszka";
            case GRAND -> "Wspaniała Czaszka";
            case HORADRIC -> "Horadryjska Czaszka";
            case FLAWLESS_HORADRIC -> "Nieskazitelna Horadryjska Czaszka";
        };
    }

    private static String rarityLabel(GemTier tier) {
        return switch (tier) {
            case HORADRIC -> "Magiczny klejnot";
            case FLAWLESS_HORADRIC -> "Rzadki klejnot";
            default -> "Klejnot";
        };
    }

    private static Integer[] rubyRequiredLevels() {
        return new Integer[]{1, 20, 15, 20, 25, 30, 30, 30};
    }

    private static Map<String, GemDefinition> buildById(List<GemDefinition> definitions) {
        Map<String, GemDefinition> byId = new java.util.HashMap<>();
        for (GemDefinition definition : definitions) {
            if (byId.put(definition.getId(), definition) != null) {
                throw new IllegalStateException("Zduplikowane id gema: " + definition.getId());
            }
        }
        return Map.copyOf(byId);
    }

    public static Map<GemFamily, List<GemDefinition>> groupedByFamily() {
        Map<GemFamily, List<GemDefinition>> grouped = new EnumMap<>(GemFamily.class);
        for (GemFamily family : GemFamily.values()) {
            grouped.put(family, byFamily(family));
        }
        return Map.copyOf(grouped);
    }

    private static String formatWhole(int value) {
        return String.format(java.util.Locale.US, "%,d", value).replace(',', ' ');
    }
}

package krys.web;

import krys.hero.HeroClassDefs;
import krys.hero.HeroClassStatBaseline;
import krys.hero.HeroClassStatBaselines;
import krys.item.HeroEquipmentSlot;
import krys.itemimport.CurrentBuildImportableStats;
import krys.itemlibrary.CurrentHeroActiveItemStats;
import krys.itemlibrary.HeroSlotItemAssignment;
import krys.itemlibrary.ItemLibraryPresentationSupport;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillState;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Prezentuje faktyczne wejście runtime current build razem z jawnym źródłem każdej wartości. */
final class CurrentBuildRuntimeInputPresentation {
    private final List<Field> fields;

    private CurrentBuildRuntimeInputPresentation(List<Field> fields) {
        this.fields = List.copyOf(fields);
    }

    static CurrentBuildRuntimeInputPresentation from(CurrentBuildPageModel model) {
        CurrentBuildImportableStats runtimeStats = model.getEffectiveStats();
        if (runtimeStats == null) {
            return new CurrentBuildRuntimeInputPresentation(List.of());
        }

        Optional<HeroClassStatBaseline> baseline = model.getActiveHero() == null
                ? Optional.empty()
                : HeroClassStatBaselines.find(model.getActiveHero().getHeroClass(), parseLevel(model.getFormData()));
        CurrentBuildImportableStats activeItemContribution = model.getActiveLibraryContribution();

        List<Field> fields = new ArrayList<>();
        fields.add(new Field("Obrażenia broni", Long.toString(runtimeStats.getWeaponDamage()),
                weaponDamageSource(model, runtimeStats, activeItemContribution)));
        fields.add(new Field("Siła", ItemLibraryPresentationSupport.formatWhole(runtimeStats.getStrength()),
                statSource(model, baseline, activeItemContribution.getStrength())));
        fields.add(new Field("Inteligencja", ItemLibraryPresentationSupport.formatWhole(runtimeStats.getIntelligence()),
                statSource(model, baseline, activeItemContribution.getIntelligence())));
        fields.add(new Field("Kolce", ItemLibraryPresentationSupport.formatWhole(runtimeStats.getThorns()),
                contributionSource(activeItemContribution.getThorns())));
        fields.add(new Field("Szansa bloku [%]", formatNumber(runtimeStats.getBlockChance()),
                contributionSource(activeItemContribution.getBlockChance())));
        fields.add(new Field("Szansa retribution [%]", formatNumber(runtimeStats.getRetributionChance()),
                contributionSource(activeItemContribution.getRetributionChance())));
        fields.add(new Field("Pancerz", Long.toString(model.getActiveHeroItemStats().getItemArmor()),
                contributionSource(model.getActiveHeroItemStats().getItemArmor())));
        fields.add(new Field("Odporność na Ogień", ItemLibraryPresentationSupport.formatWhole(model.getActiveHeroItemStats().getFireResistance()),
                contributionSource(model.getActiveHeroItemStats().getFireResistance())));
        fields.add(new Field("Odporność na wszystkie żywioły", ItemLibraryPresentationSupport.formatWhole(model.getActiveHeroItemStats().getAllResistance()),
                contributionSource(model.getActiveHeroItemStats().getAllResistance())));
        fields.add(new Field("Redukcja obrażeń", formatPercent(model.getActiveHeroItemStats().getDamageReduction()),
                contributionSource(model.getActiveHeroItemStats().getDamageReduction())));
        fields.add(new Field("Początkowa Wiara", CurrentBuildNumberFormatter.resource(parseDouble(model.getFormData().getInitialPrimaryResource())),
                "Jawne pole current build"));
        fields.add(new Field("Maksymalna Wiara", CurrentBuildNumberFormatter.resource(parseDouble(model.getFormData().getMaxPrimaryResource())),
                "Jawne pole current build"));
        fields.add(new Field("Regeneracja Wiary/s", CurrentBuildNumberFormatter.resourceRegenPerSecond(parseDouble(model.getFormData().getPrimaryResourceRegenPerSecond())),
                "Jawne pole current build"));
        fields.add(new Field("Początkowy Animusz", CurrentBuildNumberFormatter.resource(parseDouble(model.getFormData().getInitialAnimus())),
                "Jawne pole current build"));
        fields.add(new Field("Maksymalny Animusz",
                CurrentBuildNumberFormatter.resource(CurrentBuildRuntimeInputResolver.resolveMaximumAnimus(model.getFormData(), model.getEffectiveCurrentBuildResolution())),
                maxAnimusSource(model)));
        CurrentBuildFormData.SkillConfigFormData clashConfig = model.getFormData().getSkillConfig(SkillId.CLASH);
        if (clashConfig != null && (SkillRuntimeModifierChoice.ANIMUS.name().equals(clashConfig.getRuntimeModifierChoice())
                || SkillState.CLASH_ANIMUS_CHOICE.equals(clashConfig.getChoiceGroup1()))) {
            fields.add(new Field("Starcie: Animusz", "aktywny", "choiceGroup1_CLASH=animusz; +2 Animuszu po legalnym trafieniu"));
        }
        return new CurrentBuildRuntimeInputPresentation(fields);
    }

    List<Field> getFields() {
        return fields;
    }

    boolean isEmpty() {
        return fields.isEmpty();
    }

    private static String weaponDamageSource(CurrentBuildPageModel model,
                                             CurrentBuildImportableStats runtimeStats,
                                             CurrentBuildImportableStats activeItemContribution) {
        CurrentHeroActiveItemStats activeItemStats = model.getActiveHeroItemStats();
        Long averageWeaponDamage = activeItemStats.getAverageWeaponDamage();
        if (averageWeaponDamage != null && averageWeaponDamage == runtimeStats.getWeaponDamage()) {
            String itemName = activeWeaponName(model);
            if (itemName.isBlank()) {
                return "Aktywna broń: średnie obrażenia trafienia";
            }
            return "Aktywna broń: " + itemName + ", średnie obrażenia trafienia";
        }
        if (activeItemContribution.getWeaponDamage() > 0L
                && activeItemContribution.getWeaponDamage() == runtimeStats.getWeaponDamage()) {
            return "Aktywny item: zapisane obrażenia broni";
        }
        return "Brak aktywnej broni";
    }

    private static String statSource(CurrentBuildPageModel model,
                                     Optional<HeroClassStatBaseline> baseline,
                                     double activeItemContribution) {
        if (baseline.isPresent()) {
            String baselineLabel = "Baseline " + HeroClassDefs.get(model.getActiveHero().getHeroClass()).getDisplayName()
                    + " poziom " + baseline.get().getLevel();
            if (activeItemContribution > 0.0d) {
                return baselineLabel + " + aktywne itemy";
            }
            return baselineLabel;
        }
        if (activeItemContribution > 0.0d) {
            return "Aktywne itemy";
        }
        return "Brak jawnego wkładu current build";
    }

    private static String contributionSource(double activeItemContribution) {
        if (activeItemContribution > 0.0d) {
            return "Aktywne itemy";
        }
        return "Brak jawnego wkładu current build";
    }

    private static String maxAnimusSource(CurrentBuildPageModel model) {
        double baseMaxAnimus = parseDouble(model.getFormData().getMaxAnimus());
        CurrentHeroActiveItemStats activeItemStats = model.getActiveHeroItemStats();
        if (activeItemStats.getMaxAnimusFromTempering() <= 0.0d) {
            return "bazowe: " + CurrentBuildNumberFormatter.resource(baseMaxAnimus);
        }
        List<String> parts = new ArrayList<>();
        parts.add("bazowe: " + CurrentBuildNumberFormatter.resource(baseMaxAnimus));
        parts.addAll(activeItemStats.getMaxAnimusTemperingSources());
        return String.join(" + ", parts);
    }

    private static String activeWeaponName(CurrentBuildPageModel model) {
        for (HeroSlotItemAssignment assignment : model.getActiveLibraryItems()) {
            if (assignment.getHeroSlot() == HeroEquipmentSlot.MAIN_HAND) {
                return assignment.getItem().getDisplayName();
            }
        }
        return "";
    }

    private static int parseLevel(CurrentBuildFormData formData) {
        try {
            return Math.max(1, Integer.parseInt(formData.getLevel()));
        } catch (NumberFormatException exception) {
            return 1;
        }
    }

    private static String formatNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String formatPercent(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, java.math.RoundingMode.HALF_UP)
                .toPlainString()
                .replace('.', ',') + "%";
    }

    private static double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return 0.0d;
        }
    }

    static final class Field {
        private final String label;
        private final String value;
        private final String source;

        Field(String label, String value, String source) {
            this.label = label;
            this.value = value;
            this.source = source;
        }

        String getLabel() {
            return label;
        }

        String getValue() {
            return value;
        }

        String getSource() {
            return source;
        }
    }
}

package krys.web;

import krys.app.CurrentBuildRequest;
import krys.paladin.PaladinSkillTreeRegistry;
import krys.paladin.PaladinOathRegistry;
import krys.paladin.PaladinSkillUpgrade;
import krys.paladin.PaladinSkillUpgradeGroup;
import krys.paladin.PaladinTreeSkill;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Mapuje surowe dane formularza M8 do aplikacyjnego requestu i zbiera błędy walidacji. */
final class CurrentBuildFormMapper {
    MappingResult map(CurrentBuildFormData formData) {
        return map(formData, true, true);
    }

    MappingResult map(CurrentBuildFormData formData, boolean hasActiveWeapon, boolean hasActiveShield) {
        List<String> errors = new ArrayList<>();

        Integer level = parseInt(formData.getLevel(), "Level bohatera", 1, errors);
        Long weaponDamage = parseLong(formData.getWeaponDamage(), "Weapon damage", 0L, errors);
        Double strength = parseDouble(formData.getStrength(), "Strength", 0.0d, errors);
        Double intelligence = parseDouble(formData.getIntelligence(), "Intelligence", 0.0d, errors);
        Double thorns = parseDouble(formData.getThorns(), "Thorns", 0.0d, errors);
        Double blockChance = parseDouble(formData.getBlockChance(), "Block chance", 0.0d, errors);
        Double retributionChance = parseDouble(formData.getRetributionChance(), "Retribution chance", 0.0d, errors);
        Integer horizonSeconds = parseInt(formData.getHorizonSeconds(), "Horyzont symulacji", 1, errors);
        Double initialPrimaryResource = parseDouble(formData.getInitialPrimaryResource(), "Początkowa Wiara", 0.0d, errors);
        Double maxPrimaryResource = parseDouble(formData.getMaxPrimaryResource(), "Maksymalna Wiara", 0.0d, errors);
        Double primaryResourceRegenPerSecond = parseDouble(formData.getPrimaryResourceRegenPerSecond(), "Regeneracja Wiary/s", 0.0d, errors);
        Double initialAnimus = parseDouble(formData.getInitialAnimus(), "Początkowy Animusz", 0.0d, errors);
        Double maxAnimus = parseDouble(formData.getMaxAnimus(), "Maksymalny Animusz", 0.0d, errors);
        validatePaladinOath(formData.getSelectedPaladinOathId(), errors);
        if (initialPrimaryResource != null && maxPrimaryResource != null && initialPrimaryResource > maxPrimaryResource) {
            errors.add("Początkowa Wiara nie może być większa niż Maksymalna Wiara.");
        }
        if (initialAnimus != null && maxAnimus != null && initialAnimus > maxAnimus) {
            errors.add("Początkowy Animusz nie może być większy niż Maksymalny Animusz.");
        }

        Map<SkillId, SkillState> learnedSkills = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            mapSkillConfig(formData, skillId, learnedSkills, errors);
        }

        List<SkillId> actionBar = mapActionBar(formData, learnedSkills, errors);
        if (level == null || weaponDamage == null || strength == null || intelligence == null
                || thorns == null || blockChance == null || retributionChance == null || horizonSeconds == null
                || initialPrimaryResource == null || maxPrimaryResource == null || primaryResourceRegenPerSecond == null
                || initialAnimus == null || maxAnimus == null
                || !errors.isEmpty()) {
            return new MappingResult(null, errors);
        }

        try {
            CurrentBuildRequest request = new CurrentBuildRequest(
                    level,
                    weaponDamage,
                    strength,
                    intelligence,
                    thorns,
                    blockChance,
                    retributionChance,
                    hasActiveWeapon,
                    hasActiveShield,
                    learnedSkills,
                    actionBar,
                    horizonSeconds,
                    initialPrimaryResource,
                    maxPrimaryResource,
                    primaryResourceRegenPerSecond,
                    formData.getSelectedPaladinOathId(),
                    initialAnimus,
                    maxAnimus,
                    List.of()
            );
            return new MappingResult(request, errors);
        } catch (IllegalArgumentException exception) {
            errors.add(exception.getMessage());
            return new MappingResult(null, errors);
        }
    }

    private static void mapSkillConfig(CurrentBuildFormData formData,
                                       SkillId skillId,
                                       Map<SkillId, SkillState> learnedSkills,
                                       List<String> errors) {
        CurrentBuildFormData.SkillConfigFormData skillConfig = formData.getSkillConfig(skillId);
        Integer rank = parseInt(skillConfig.getRank(), "Rank skilla " + HeroSkillCatalogAdapter.displayName(skillId), 0, errors);
        SkillUpgradeChoice choiceUpgrade = parseChoice(skillConfig.getChoiceUpgrade(), skillId, errors);
        SkillRuntimeModifierChoice runtimeModifierChoice = parseRuntimeModifier(skillConfig.getRuntimeModifierChoice(), skillId, errors);
        String choiceGroup1 = skillConfig.getChoiceGroup1();
        String choiceGroup2 = skillConfig.getChoiceGroup2();
        String choiceGroup3 = skillConfig.getChoiceGroup3();
        validateSkillTreeGroupChoice(skillId, 1, choiceGroup1, errors);
        validateSkillTreeGroupChoice(skillId, 2, choiceGroup2, errors);
        validateSkillTreeGroupChoice(skillId, 3, choiceGroup3, errors);
        boolean baseUpgrade = skillConfig.isBaseUpgrade();

        if (rank == null || choiceUpgrade == null || runtimeModifierChoice == null) {
            return;
        }
        if (skillId == SkillId.CLASH) {
            baseUpgrade = rank > 0;
            if (SkillState.CLASH_ANIMUS_CHOICE.equals(choiceGroup1)) {
                runtimeModifierChoice = SkillRuntimeModifierChoice.ANIMUS;
            }
            choiceUpgrade = SkillState.CLASH_PUNISHMENT_CHOICE.equals(choiceGroup3)
                    ? SkillUpgradeChoice.LEFT
                    : SkillUpgradeChoice.NONE;
        }
        if (!baseUpgrade && choiceUpgrade != SkillUpgradeChoice.NONE) {
            errors.add("Dodatkowy modyfikator dla " + HeroSkillCatalogAdapter.displayName(skillId) + " wymaga bazowego rozszerzenia.");
            return;
        }
        LinkedHashSet<SkillUpgradeChoice> validChoices = new LinkedHashSet<>();
        validChoices.add(SkillUpgradeChoice.NONE);
        validChoices.addAll(PaladinSkillDefs.get(skillId).getAvailableChoiceUpgrades());
        if (!validChoices.contains(choiceUpgrade)) {
            errors.add("Wybrany dodatkowy modyfikator nie jest dostępny dla skilla " + HeroSkillCatalogAdapter.displayName(skillId) + ".");
            return;
        }
        if (runtimeModifierChoice != SkillRuntimeModifierChoice.NONE && skillId != SkillId.CLASH) {
            errors.add("Modyfikator runtime Animusz jest dostępny tylko dla skilla Starcie.");
            return;
        }
        if (rank <= 0) {
            return;
        }

        try {
            learnedSkills.put(skillId, new SkillState(
                    skillId,
                    rank,
                    baseUpgrade,
                    choiceUpgrade,
                    runtimeModifierChoice,
                    choiceGroup1,
                    choiceGroup2,
                    choiceGroup3
            ));
        } catch (IllegalArgumentException exception) {
            errors.add(exception.getMessage());
        }
    }

    private static List<SkillId> mapActionBar(CurrentBuildFormData formData,
                                              Map<SkillId, SkillState> learnedSkills,
                                              List<String> errors) {
        List<SkillId> actionBar = new ArrayList<>();
        LinkedHashSet<SkillId> dedupe = new LinkedHashSet<>();
        for (int slot = 1; slot <= CurrentBuildFormData.ACTION_BAR_SLOT_COUNT; slot++) {
            String rawSkillId = formData.getActionBarSlot(slot);
            if (rawSkillId == null || rawSkillId.isBlank() || "NONE".equalsIgnoreCase(rawSkillId)) {
                continue;
            }
            SkillId skillId = parseSkillId(rawSkillId, "Action bar slot " + slot, errors);
            if (skillId == null) {
                continue;
            }
            if (!learnedSkills.containsKey(skillId)) {
                errors.add("Action bar slot " + slot + " wskazuje skill bez rank > 0: " + HeroSkillCatalogAdapter.displayName(skillId));
                continue;
            }
            if (!dedupe.add(skillId)) {
                errors.add("Action bar nie może zawierać duplikatu skilla: " + HeroSkillCatalogAdapter.displayName(skillId));
                continue;
            }
            actionBar.add(skillId);
        }
        return actionBar;
    }

    private static SkillUpgradeChoice parseChoice(String rawChoiceUpgrade, SkillId skillId, List<String> errors) {
        try {
            return SkillUpgradeChoice.valueOf(rawChoiceUpgrade.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            errors.add("Niepoprawny dodatkowy modyfikator dla skilla " + HeroSkillCatalogAdapter.displayName(skillId) + ".");
            return null;
        }
    }

    private static SkillRuntimeModifierChoice parseRuntimeModifier(String rawModifier, SkillId skillId, List<String> errors) {
        try {
            return SkillRuntimeModifierChoice.valueOf(rawModifier.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            errors.add("Niepoprawny modyfikator runtime dla skilla " + HeroSkillCatalogAdapter.displayName(skillId) + ".");
            return null;
        }
    }

    private static void validateSkillTreeGroupChoice(SkillId skillId,
                                                     int groupIndex,
                                                     String rawChoice,
                                                     List<String> errors) {
        if (rawChoice == null || rawChoice.isBlank() || SkillState.NO_TREE_CHOICE.equals(rawChoice)) {
            return;
        }
        if (skillId != SkillId.CLASH) {
            errors.add("Wybory grupowe są dostępne tylko dla skilla Starcie.");
            return;
        }
        PaladinTreeSkill clash = PaladinSkillTreeRegistry.requireSkill("starcie");
        String expectedGroupId = "grupa_" + groupIndex;
        for (PaladinSkillUpgradeGroup group : clash.getUpgradeGroups()) {
            if (!expectedGroupId.equals(group.getId())) {
                continue;
            }
            for (PaladinSkillUpgrade upgrade : group.getUpgrades()) {
                if (upgrade.getId().equals(rawChoice)) {
                    return;
                }
            }
        }
        errors.add("Wybrany modyfikator grupy " + groupIndex + " nie jest dostępny dla Starcia.");
    }

    private static SkillId parseSkillId(String rawSkillId, String label, List<String> errors) {
        try {
            return SkillId.valueOf(rawSkillId.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            errors.add(label + " zawiera niepoprawny skill.");
            return null;
        }
    }

    private static void validatePaladinOath(String rawOathId, List<String> errors) {
        if (rawOathId == null || rawOathId.isBlank() || "NONE".equals(rawOathId)) {
            return;
        }
        if (PaladinOathRegistry.findByRawId(rawOathId).isEmpty()) {
            errors.add("Wybrano niepoprawną Przysięgę Paladyna.");
        }
    }

    private static Integer parseInt(String rawValue, String label, int minimumInclusive, List<String> errors) {
        try {
            int value = Integer.parseInt(rawValue);
            if (value < minimumInclusive) {
                errors.add(label + " musi być >= " + minimumInclusive + ".");
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            errors.add(label + " musi być liczbą całkowitą.");
            return null;
        }
    }

    private static Long parseLong(String rawValue, String label, long minimumInclusive, List<String> errors) {
        try {
            long value = Long.parseLong(rawValue);
            if (value < minimumInclusive) {
                errors.add(label + " musi być >= " + minimumInclusive + ".");
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            errors.add(label + " musi być liczbą całkowitą.");
            return null;
        }
    }

    private static Double parseDouble(String rawValue, String label, double minimumInclusive, List<String> errors) {
        try {
            double value = Double.parseDouble(rawValue);
            if (value < minimumInclusive) {
                errors.add(label + " nie może być mniejszy niż " + String.format(Locale.US, "%.0f", minimumInclusive) + ".");
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            errors.add(label + " musi być liczbą.");
            return null;
        }
    }

    static final class MappingResult {
        private final CurrentBuildRequest request;
        private final List<String> errors;

        MappingResult(CurrentBuildRequest request, List<String> errors) {
            this.request = request;
            this.errors = List.copyOf(errors);
        }

        CurrentBuildRequest getRequest() {
            return request;
        }

        List<String> getErrors() {
            return errors;
        }
    }
}

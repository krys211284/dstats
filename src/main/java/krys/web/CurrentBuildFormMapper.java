package krys.web;

import krys.app.CurrentBuildRequest;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillId;
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
        List<String> errors = new ArrayList<>();

        Integer level = parseInt(formData.getLevel(), "Level bohatera", 1, errors);
        Long weaponDamage = parseLong(formData.getWeaponDamage(), "Weapon damage", 1L, errors);
        Double strength = parseDouble(formData.getStrength(), "Strength", 0.0d, errors);
        Double intelligence = parseDouble(formData.getIntelligence(), "Intelligence", 0.0d, errors);
        Double thorns = parseDouble(formData.getThorns(), "Thorns", 0.0d, errors);
        Double blockChance = parseDouble(formData.getBlockChance(), "Block chance", 0.0d, errors);
        Double retributionChance = parseDouble(formData.getRetributionChance(), "Retribution chance", 0.0d, errors);
        Integer horizonSeconds = parseInt(formData.getHorizonSeconds(), "Horyzont symulacji", 1, errors);

        Map<SkillId, SkillState> learnedSkills = new EnumMap<>(SkillId.class);
        for (SkillId skillId : SkillId.values()) {
            mapSkillConfig(formData, skillId, learnedSkills, errors);
        }

        List<SkillId> actionBar = mapActionBar(formData, learnedSkills, errors);
        if (level == null || weaponDamage == null || strength == null || intelligence == null
                || thorns == null || blockChance == null || retributionChance == null || horizonSeconds == null
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
                    learnedSkills,
                    actionBar,
                    horizonSeconds
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
        Integer rank = parseInt(skillConfig.getRank(), "Rank skilla " + PaladinSkillDefs.get(skillId).getName(), 0, errors);
        SkillUpgradeChoice choiceUpgrade = parseChoice(skillConfig.getChoiceUpgrade(), skillId, errors);
        boolean baseUpgrade = skillConfig.isBaseUpgrade();

        if (rank == null || choiceUpgrade == null) {
            return;
        }
        if (!baseUpgrade && choiceUpgrade != SkillUpgradeChoice.NONE) {
            errors.add("Dodatkowy modyfikator dla " + PaladinSkillDefs.get(skillId).getName() + " wymaga bazowego rozszerzenia.");
            return;
        }
        LinkedHashSet<SkillUpgradeChoice> validChoices = new LinkedHashSet<>();
        validChoices.add(SkillUpgradeChoice.NONE);
        validChoices.addAll(PaladinSkillDefs.get(skillId).getAvailableChoiceUpgrades());
        if (!validChoices.contains(choiceUpgrade)) {
            errors.add("Wybrany dodatkowy modyfikator nie jest dostępny dla skilla " + PaladinSkillDefs.get(skillId).getName() + ".");
            return;
        }
        if (rank <= 0) {
            return;
        }

        try {
            learnedSkills.put(skillId, new SkillState(skillId, rank, baseUpgrade, choiceUpgrade));
        } catch (IllegalArgumentException exception) {
            errors.add(exception.getMessage());
        }
    }

    private static List<SkillId> mapActionBar(CurrentBuildFormData formData,
                                              Map<SkillId, SkillState> learnedSkills,
                                              List<String> errors) {
        List<SkillId> actionBar = new ArrayList<>();
        LinkedHashSet<SkillId> dedupe = new LinkedHashSet<>();
        for (int slot = 1; slot <= 4; slot++) {
            String rawSkillId = formData.getActionBarSlot(slot);
            if (rawSkillId == null || rawSkillId.isBlank() || "NONE".equalsIgnoreCase(rawSkillId)) {
                continue;
            }
            SkillId skillId = parseSkillId(rawSkillId, "Action bar slot " + slot, errors);
            if (skillId == null) {
                continue;
            }
            if (!learnedSkills.containsKey(skillId)) {
                errors.add("Action bar slot " + slot + " wskazuje skill bez rank > 0: " + skillId);
                continue;
            }
            if (!dedupe.add(skillId)) {
                errors.add("Action bar nie może zawierać duplikatu skilla: " + skillId);
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
            errors.add("Niepoprawny dodatkowy modyfikator dla skilla " + PaladinSkillDefs.get(skillId).getName() + ".");
            return null;
        }
    }

    private static SkillId parseSkillId(String rawSkillId, String label, List<String> errors) {
        try {
            return SkillId.valueOf(rawSkillId.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            errors.add(label + " zawiera niepoprawny skill.");
            return null;
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

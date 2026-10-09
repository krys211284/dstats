package krys.runtime.legacy;

import krys.runtime.RuntimeSkillConfiguration;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Bezstratny most stanu legacy do neutralnych slotów; znaczenie kombinacji rozstrzyga istniejący stan. */
public final class LegacyPaladinRuntimeSkillConfigurationAdapter {
    private static final String BASE_UPGRADE = "legacy_base_upgrade";
    private static final String CHOICE_UPGRADE = "legacy_choice_upgrade";
    private static final String RUNTIME_MODIFIER = "legacy_runtime_modifier";
    private static final String GROUP_1 = "grupa_1";
    private static final String GROUP_2 = "grupa_2";
    private static final String GROUP_3 = "grupa_3";
    private static final Set<String> ALLOWED_SLOTS = Set.of(BASE_UPGRADE, CHOICE_UPGRADE, RUNTIME_MODIFIER,
            GROUP_1, GROUP_2, GROUP_3);

    private LegacyPaladinRuntimeSkillConfigurationAdapter() { }

    public static RuntimeSkillConfiguration toRuntimeConfiguration(SkillState state) {
        Objects.requireNonNull(state, "state");
        Map<String, String> selections = new HashMap<>();
        if (state.isBaseUpgrade()) {
            selections.put(BASE_UPGRADE, "enabled");
        }
        if (state.getChoiceUpgrade() != SkillUpgradeChoice.NONE) {
            selections.put(CHOICE_UPGRADE, state.getChoiceUpgrade().name().toLowerCase(Locale.ROOT));
        }
        if (state.getRuntimeModifierChoice() != SkillRuntimeModifierChoice.NONE) {
            selections.put(RUNTIME_MODIFIER, state.getRuntimeModifierChoice().name().toLowerCase(Locale.ROOT));
        }
        putGroupSelection(selections, GROUP_1, state.getChoiceGroup1());
        putGroupSelection(selections, GROUP_2, state.getChoiceGroup2());
        putGroupSelection(selections, GROUP_3, state.getChoiceGroup3());
        return new RuntimeSkillConfiguration(LegacyPaladinRuntimeSkillKeyAdapter.toRuntimeKey(state.getSkillId()),
                state.getRank(), selections);
    }

    public static Optional<SkillState> toLegacySkillState(RuntimeSkillConfiguration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        var legacyId = LegacyPaladinRuntimeSkillKeyAdapter.toLegacySkillId(configuration.skillKey());
        if (legacyId.isEmpty()) {
            return Optional.empty();
        }
        var selections = configuration.selections();
        for (String slot : selections.keySet()) {
            if (!ALLOWED_SLOTS.contains(slot)) {
                throw new IllegalArgumentException("Nieznany slot legacy: " + slot);
            }
        }
        boolean baseUpgrade = selections.containsKey(BASE_UPGRADE);
        if (baseUpgrade && !"enabled".equals(selections.get(BASE_UPGRADE))) {
            throw new IllegalArgumentException("Slot " + BASE_UPGRADE + " wymaga enabled");
        }
        SkillUpgradeChoice choice = selections.containsKey(CHOICE_UPGRADE)
                ? switch (selections.get(CHOICE_UPGRADE)) {
                    case "left" -> SkillUpgradeChoice.LEFT;
                    case "middle" -> SkillUpgradeChoice.MIDDLE;
                    case "right" -> SkillUpgradeChoice.RIGHT;
                    default -> throw new IllegalArgumentException("Nieznany wybór legacy: " + selections.get(CHOICE_UPGRADE));
                } : SkillUpgradeChoice.NONE;
        SkillRuntimeModifierChoice modifier = SkillRuntimeModifierChoice.NONE;
        if (selections.containsKey(RUNTIME_MODIFIER)) {
            if (!"animus".equals(selections.get(RUNTIME_MODIFIER))) {
                throw new IllegalArgumentException("Nieznany modyfikator legacy: " + selections.get(RUNTIME_MODIFIER));
            }
            modifier = SkillRuntimeModifierChoice.ANIMUS;
        }
        return Optional.of(new SkillState(legacyId.orElseThrow(), configuration.rank(), baseUpgrade, choice, modifier,
                groupSelection(selections, GROUP_1), groupSelection(selections, GROUP_2), groupSelection(selections, GROUP_3)));
    }

    private static void putGroupSelection(Map<String, String> selections, String slot, String option) {
        if (!SkillState.NO_TREE_CHOICE.equals(option)) {
            selections.put(slot, option);
        }
    }

    private static String groupSelection(Map<String, String> selections, String slot) {
        String option = selections.get(slot);
        if (option == null) {
            return SkillState.NO_TREE_CHOICE;
        }
        if (SkillState.NO_TREE_CHOICE.equalsIgnoreCase(option)) {
            throw new IllegalArgumentException("Brak wyboru wymaga braku wpisu w slocie " + slot);
        }
        return option;
    }
}

package krys.skill;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Minimalny rejestr definicji skilli Paladina potrzebnych do pierwszego foundation. */
public final class PaladinSkillDefs {
    private static final SkillDef BRANDISH = new SkillDef(
            SkillId.BRANDISH,
            "Brandish",
            new long[]{75, 83, 90, 98, 105},
            createBrandishEffects()
    );

    private PaladinSkillDefs() {
    }

    public static SkillDef get(SkillId skillId) {
        return switch (skillId) {
            case BRANDISH -> BRANDISH;
        };
    }

    private static Map<SkillUpgradeChoice, List<SkillRuntimeEffect>> createBrandishEffects() {
        Map<SkillUpgradeChoice, List<SkillRuntimeEffect>> effects = new EnumMap<>(SkillUpgradeChoice.class);
        effects.put(SkillUpgradeChoice.LEFT, List.of(
                SkillRuntimeEffect.replaceBaseDamage("Główny hit", StatusId.NONE, 73),
                SkillRuntimeEffect.damage("Powrót światłości", StatusId.NONE, 73, 1, true)
        ));
        effects.put(SkillUpgradeChoice.RIGHT, List.of(
                SkillRuntimeEffect.replaceBaseDamage("Główny hit", StatusId.NONE, 168),
                SkillRuntimeEffect.applyStatus(StatusId.VULNERABLE, 2),
                SkillRuntimeEffect.damage("Dodatkowe łuki", StatusId.VULNERABLE, 168, 2, false)
        ));
        return effects;
    }
}

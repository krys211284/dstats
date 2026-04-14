package krys.skill;

import java.util.EnumMap;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Minimalny rejestr definicji skilli Paladina potrzebnych do pierwszego foundation. */
public final class PaladinSkillDefs {
    private static final SkillDef BRANDISH = new SkillDef(
            SkillId.BRANDISH,
            "Brandish",
            0,
            0,
            new long[]{75, 83, 90, 98, 105},
            List.of(),
            createBrandishEffects(),
            createBrandishCriticalPolicies()
    );
    private static final SkillDef HOLY_BOLT = new SkillDef(
            SkillId.HOLY_BOLT,
            "Holy Bolt",
            0,
            0,
            new long[]{90, 99, 108, 117, 126},
            List.of(
                    SkillRuntimeEffect.applyDelayedHit("Judgement", 3, 80)
            ),
            Map.of(),
            Map.of()
    );

    private PaladinSkillDefs() {
    }

    public static SkillDef get(SkillId skillId) {
        return switch (skillId) {
            case BRANDISH -> BRANDISH;
            case HOLY_BOLT -> HOLY_BOLT;
        };
    }

    public static Set<SkillUpgradeChoice> getFoundationChoiceUpgrades() {
        LinkedHashSet<SkillUpgradeChoice> choices = new LinkedHashSet<>();
        choices.add(SkillUpgradeChoice.NONE);
        for (SkillId skillId : SkillId.values()) {
            choices.addAll(get(skillId).getAvailableChoiceUpgrades());
        }
        return Collections.unmodifiableSet(new LinkedHashSet<>(choices));
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

    private static Map<SkillUpgradeChoice, CriticalRoundingPolicy> createBrandishCriticalPolicies() {
        Map<SkillUpgradeChoice, CriticalRoundingPolicy> policies = new EnumMap<>(SkillUpgradeChoice.class);
        policies.put(SkillUpgradeChoice.RIGHT, CriticalRoundingPolicy.ROUNDED_RAW_HIT);
        return policies;
    }
}

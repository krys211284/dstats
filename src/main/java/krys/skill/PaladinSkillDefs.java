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
            createBrandishCriticalPolicies(),
            null,
            Map.of(),
            createBrandishChoiceDisplayNames()
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
            Map.of(),
            null,
            Map.of(),
            Map.of()
    );
    private static final SkillDef CLASH = new SkillDef(
            SkillId.CLASH,
            "Clash",
            0,
            0,
            new long[]{0, 0, 0, 0, 0},
            List.of(),
            Map.of(),
            Map.of(),
            new ReactiveSelfBuffProfile(true, 3, 25.0d, 0.0d),
            Map.of(
                    SkillUpgradeChoice.LEFT,
                    new ReactiveSelfBuffProfile(false, 3, 0.0d, 50.0d)
            ),
            createClashChoiceDisplayNames()
    );
    private static final SkillDef ADVANCE = new SkillDef(
            SkillId.ADVANCE,
            "Advance",
            0,
            0,
            new long[]{147, 147, 147, 147, 147},
            List.of(),
            createAdvanceEffects(),
            Map.of(),
            null,
            Map.of(),
            createAdvanceChoiceDisplayNames()
    );

    private PaladinSkillDefs() {
    }

    public static SkillDef get(SkillId skillId) {
        return switch (skillId) {
            case BRANDISH -> BRANDISH;
            case HOLY_BOLT -> HOLY_BOLT;
            case CLASH -> CLASH;
            case ADVANCE -> ADVANCE;
        };
    }

    public static String getChoiceDisplayName(SkillId skillId, SkillUpgradeChoice choiceUpgrade) {
        return get(skillId).getChoiceDisplayName(choiceUpgrade);
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

    private static Map<SkillUpgradeChoice, String> createBrandishChoiceDisplayNames() {
        Map<SkillUpgradeChoice, String> labels = new EnumMap<>(SkillUpgradeChoice.class);
        labels.put(SkillUpgradeChoice.LEFT, "Powrót światłości");
        labels.put(SkillUpgradeChoice.RIGHT, "Krzyżowe uderzenie (Vulnerable)");
        return labels;
    }

    private static Map<SkillUpgradeChoice, String> createClashChoiceDisplayNames() {
        Map<SkillUpgradeChoice, String> labels = new EnumMap<>(SkillUpgradeChoice.class);
        labels.put(SkillUpgradeChoice.LEFT, "Punishment");
        return labels;
    }

    private static Map<SkillUpgradeChoice, List<SkillRuntimeEffect>> createAdvanceEffects() {
        Map<SkillUpgradeChoice, List<SkillRuntimeEffect>> effects = new EnumMap<>(SkillUpgradeChoice.class);
        effects.put(SkillUpgradeChoice.LEFT, List.of(
                SkillRuntimeEffect.damage("Wave Dash", StatusId.NONE, 191, 1, true)
        ));
        effects.put(SkillUpgradeChoice.RIGHT, List.of(
                SkillRuntimeEffect.replaceBaseDamage("Główny hit", StatusId.NONE, 322),
                SkillRuntimeEffect.applyStatus(StatusId.VULNERABLE, 2),
                SkillRuntimeEffect.setCooldown(8)
        ));
        return effects;
    }

    private static Map<SkillUpgradeChoice, String> createAdvanceChoiceDisplayNames() {
        Map<SkillUpgradeChoice, String> labels = new EnumMap<>(SkillUpgradeChoice.class);
        labels.put(SkillUpgradeChoice.LEFT, "Wave Dash");
        labels.put(SkillUpgradeChoice.RIGHT, "Flash of the Blade");
        return labels;
    }
}

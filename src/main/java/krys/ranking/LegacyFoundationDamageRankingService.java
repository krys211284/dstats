package krys.ranking;

import krys.combat.DamageBreakdown;
import krys.combat.DamageEngine;
import krys.simulation.HeroBuildSnapshot;
import krys.skill.EffectType;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillDef;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeEffect;
import krys.skill.SkillState;
import krys.skill.StatusId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/** Izolowany ranking legacy foundation; zachowuje dotychczasowe wywolania DamageEngine. */
final class LegacyFoundationDamageRankingService {
    private final DamageEngine damageEngine;

    LegacyFoundationDamageRankingService(DamageEngine damageEngine) { this.damageEngine = damageEngine; }

    public List<SkillDamageRankingEntry> rankLegacyFoundationDamageSkills(HeroBuildSnapshot snapshot,
                                                                        DamageRankingMetric metric) {
        List<SkillDamageRankingEntry> entries = new ArrayList<>();
        for (SkillId skillId : SkillId.values()) {
            SkillState state = snapshot.getSkillState(skillId);
            if (state == null || state.getRank() <= 0) {
                continue;
            }

            SkillDamageRankingEntry entry = createLegacyFoundationEntry(snapshot, skillId, state, DamageTargetMode.SINGLE_TARGET);
            if (entry.getVerificationStatus() == SkillDamageVerificationStatus.NON_DAMAGE) {
                continue;
            }
            entries.add(entry);
        }

        entries.sort(comparatorFor(metric).reversed().thenComparing(SkillDamageRankingEntry::getSkillName));
        return List.copyOf(entries);
    }

    public List<SkillDamageRankingEntry> describeLegacyConfiguredFoundationSkills(HeroBuildSnapshot snapshot) {
        List<SkillDamageRankingEntry> entries = new ArrayList<>();
        for (SkillId skillId : SkillId.values()) {
            SkillState state = snapshot.getSkillState(skillId);
            if (state == null || state.getRank() <= 0) {
                continue;
            }
            entries.add(createLegacyFoundationEntry(snapshot, skillId, state, DamageTargetMode.SINGLE_TARGET));
        }
        return List.copyOf(entries);
    }

    private SkillDamageRankingEntry createLegacyFoundationEntry(HeroBuildSnapshot snapshot,
                                                                      SkillId skillId,
                                                                      SkillState state,
                                                              DamageTargetMode targetMode) {
        SkillDef skillDef = PaladinSkillDefs.get(skillId);
        PaladinFoundationSkillSource.SourceMetadata sourceMetadata = PaladinFoundationSkillSource.get(skillId);
        DamageBreakdown directBreakdown = damageEngine.calculate(snapshot, skillId, EnumSet.noneOf(StatusId.class));
        long delayedDamage = calculateDelayedDamage(snapshot, skillDef, state);
        long damagePerUse = directBreakdown.getFinalDamage() + delayedDamage;

        SkillDamageModelType modelType = delayedDamage > 0
                ? SkillDamageModelType.DIRECT_PLUS_DELAYED_HIT
                : SkillDamageModelType.DIRECT_HIT;

        if (damagePerUse <= 0) {
            return new SkillDamageRankingEntry(
                    skillId.name(),
                    skillDef.getName(),
                    sourceMetadata.sourcePdf(),
                    sourceMetadata.skillGroup(),
                    SkillDamageModelType.NON_DAMAGE,
                    0L,
                    resolveEffectiveCooldownSeconds(skillDef, state),
                    null,
                    null,
                    targetMode,
                    SkillDamageVerificationStatus.NON_DAMAGE,
                    "Legacy/test-only: skill nie wnosi direct ani delayed damage w starym foundation."
            );
        }

        int cooldownSeconds = resolveEffectiveCooldownSeconds(skillDef, state);
        int effectiveCycleSeconds = Math.max(1, cooldownSeconds);
        double theoreticalDps = (double) damagePerUse / effectiveCycleSeconds;

        return new SkillDamageRankingEntry(
                skillId.name(),
                skillDef.getName(),
                sourceMetadata.sourcePdf(),
                sourceMetadata.skillGroup(),
                modelType,
                damagePerUse,
                cooldownSeconds,
                effectiveCycleSeconds,
                theoreticalDps,
                targetMode,
                SkillDamageVerificationStatus.SUPPORTED,
                "Legacy/test-only: obliczone przez stary DamageEngine dla trybu SINGLE_TARGET."
        );
    }

    private long calculateDelayedDamage(HeroBuildSnapshot snapshot, SkillDef skillDef, SkillState state) {
        long delayedDamage = 0L;
        for (SkillRuntimeEffect effect : resolvePostCastEffects(skillDef, state)) {
            if (effect.getEffectType() != EffectType.APPLY_DELAYED_HIT) {
                continue;
            }
            DamageBreakdown delayedBreakdown = damageEngine.calculateStandaloneHit(
                    snapshot,
                    effect.getSkillDamagePercent(),
                    effect.getComponentName(),
                    "delayed",
                    EnumSet.noneOf(StatusId.class)
            );
            delayedDamage += delayedBreakdown.getFinalDamage();
        }
        return delayedDamage;
    }

    private static List<SkillRuntimeEffect> resolvePostCastEffects(SkillDef skillDef, SkillState state) {
        if (!state.isBaseUpgrade()) {
            return List.of();
        }

        List<SkillRuntimeEffect> effects = new ArrayList<>(skillDef.getBaseUpgradeEffects());
        effects.addAll(skillDef.getChoiceEffects(state.getChoiceUpgrade()));
        return effects;
    }

    private static int resolveEffectiveCooldownSeconds(SkillDef skillDef, SkillState state) {
        int cooldownSeconds = skillDef.getCooldownSeconds();
        for (SkillRuntimeEffect effect : resolvePostCastEffects(skillDef, state)) {
            if (effect.getEffectType() == EffectType.SET_COOLDOWN) {
                cooldownSeconds = Math.max(cooldownSeconds, effect.getCooldownSeconds());
            }
        }
        return cooldownSeconds;
    }

    private static Comparator<SkillDamageRankingEntry> comparatorFor(DamageRankingMetric metric) {
        return switch (metric) {
            case BASE_DAMAGE_PERCENT_RANK_1 -> Comparator.comparingInt(entry -> entry.getBaseDamagePercentAtRank1() == null
                    ? Integer.MIN_VALUE
                    : entry.getBaseDamagePercentAtRank1());
            case BASE_DAMAGE_PERCENT_TREE_MAX -> Comparator.comparingInt(entry -> entry.getBaseDamagePercentAtTreeMaxRank() == null
                    ? Integer.MIN_VALUE
                    : entry.getBaseDamagePercentAtTreeMaxRank());
            case DAMAGE_PER_USE -> Comparator.comparingLong(entry -> entry.getDamagePerUse() == null ? Long.MIN_VALUE : entry.getDamagePerUse());
            case THEORETICAL_DPS, SINGLE_TARGET_DPS -> Comparator.comparingDouble(entry -> entry.getTheoreticalDps() == null ? Double.NEGATIVE_INFINITY : entry.getTheoreticalDps());
        };
    }
}

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
import krys.verification.VerificationMatrix;
import krys.verification.VerificationMatrixEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

/** Ranking obrażeń Paladyna oparty wyłącznie o już zaimplementowany foundation runtime. */
public final class PaladinSkillDamageRankingService {
    private final DamageEngine damageEngine;

    public PaladinSkillDamageRankingService(DamageEngine damageEngine) {
        this.damageEngine = damageEngine;
    }

    public List<PaladinSkillDamageRankingEntry> rankDamageSkills(HeroBuildSnapshot snapshot,
                                                                 PaladinDamageRankingMetric metric) {
        return rankDamageSkills(snapshot, metric, PaladinDamageTargetMode.SINGLE_TARGET);
    }

    public List<PaladinSkillDamageRankingEntry> rankDamageSkills(HeroBuildSnapshot snapshot,
                                                                 PaladinDamageRankingMetric metric,
                                                                 PaladinDamageTargetMode targetMode) {
        if (targetMode != PaladinDamageTargetMode.SINGLE_TARGET) {
            throw new IllegalArgumentException("Obsługiwany jest tylko tryb SINGLE_TARGET.");
        }

        List<PaladinSkillDamageRankingEntry> entries = new ArrayList<>();
        for (SkillId skillId : SkillId.values()) {
            SkillState state = snapshot.getSkillState(skillId);
            if (state == null || state.getRank() <= 0) {
                continue;
            }

            PaladinSkillDamageRankingEntry entry = createFoundationEntry(snapshot, skillId, state, targetMode);
            if (entry.getVerificationStatus() == PaladinSkillDamageVerificationStatus.NON_DAMAGE) {
                continue;
            }
            entries.add(entry);
        }

        entries.sort(comparatorFor(metric).reversed().thenComparing(PaladinSkillDamageRankingEntry::getSkillName));
        return List.copyOf(entries);
    }

    public List<PaladinSkillDamageRankingEntry> describeConfiguredFoundationSkills(HeroBuildSnapshot snapshot) {
        List<PaladinSkillDamageRankingEntry> entries = new ArrayList<>();
        for (SkillId skillId : SkillId.values()) {
            SkillState state = snapshot.getSkillState(skillId);
            if (state == null || state.getRank() <= 0) {
                continue;
            }
            entries.add(createFoundationEntry(snapshot, skillId, state, PaladinDamageTargetMode.SINGLE_TARGET));
        }
        return List.copyOf(entries);
    }

    public List<PaladinSkillDamageRankingEntry> describeVerificationGatedMechanics() {
        List<PaladinSkillDamageRankingEntry> entries = new ArrayList<>();
        for (VerificationMatrixEntry verificationEntry : VerificationMatrix.all()) {
            entries.add(new PaladinSkillDamageRankingEntry(
                    verificationEntry.getSkillId(),
                    verificationEntry.getSkillId(),
                    verificationEntry.getSourcePdf(),
                    verificationEntry.getSkillGroup(),
                    PaladinSkillDamageModelType.VERIFICATION_GATED,
                    null,
                    null,
                    null,
                    null,
                    PaladinDamageTargetMode.SINGLE_TARGET,
                    PaladinSkillDamageVerificationStatus.NEEDS_VERIFICATION,
                    verificationEntry.getQuestion()
            ));
        }
        return List.copyOf(entries);
    }

    private PaladinSkillDamageRankingEntry createFoundationEntry(HeroBuildSnapshot snapshot,
                                                                SkillId skillId,
                                                                SkillState state,
                                                                PaladinDamageTargetMode targetMode) {
        SkillDef skillDef = PaladinSkillDefs.get(skillId);
        PaladinFoundationSkillSource.SourceMetadata sourceMetadata = PaladinFoundationSkillSource.get(skillId);
        DamageBreakdown directBreakdown = damageEngine.calculate(snapshot, skillId, EnumSet.noneOf(StatusId.class));
        long delayedDamage = calculateDelayedDamage(snapshot, skillDef, state);
        long damagePerUse = directBreakdown.getFinalDamage() + delayedDamage;

        PaladinSkillDamageModelType modelType = delayedDamage > 0
                ? PaladinSkillDamageModelType.DIRECT_PLUS_DELAYED_HIT
                : PaladinSkillDamageModelType.DIRECT_HIT;

        if (damagePerUse <= 0) {
            return new PaladinSkillDamageRankingEntry(
                    skillId.name(),
                    skillDef.getName(),
                    sourceMetadata.sourcePdf(),
                    sourceMetadata.skillGroup(),
                    PaladinSkillDamageModelType.NON_DAMAGE,
                    0L,
                    resolveEffectiveCooldownSeconds(skillDef, state),
                    null,
                    null,
                    targetMode,
                    PaladinSkillDamageVerificationStatus.NON_DAMAGE,
                    "Skill nie wnosi zaimplementowanego direct ani delayed damage w obecnym foundation."
            );
        }

        int cooldownSeconds = resolveEffectiveCooldownSeconds(skillDef, state);
        int effectiveCycleSeconds = Math.max(1, cooldownSeconds);
        double theoreticalDps = (double) damagePerUse / effectiveCycleSeconds;

        return new PaladinSkillDamageRankingEntry(
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
                PaladinSkillDamageVerificationStatus.VERIFIED,
                "Obliczone przez istniejący DamageEngine dla trybu SINGLE_TARGET; komponenty oznaczone jako nietrafiające głównego celu nie zwiększają damagePerUse."
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

    private static Comparator<PaladinSkillDamageRankingEntry> comparatorFor(PaladinDamageRankingMetric metric) {
        return switch (metric) {
            case DAMAGE_PER_USE -> Comparator.comparingLong(entry -> entry.getDamagePerUse() == null ? Long.MIN_VALUE : entry.getDamagePerUse());
            case THEORETICAL_DPS, SINGLE_TARGET_DPS -> Comparator.comparingDouble(entry -> entry.getTheoreticalDps() == null ? Double.NEGATIVE_INFINITY : entry.getTheoreticalDps());
        };
    }
}

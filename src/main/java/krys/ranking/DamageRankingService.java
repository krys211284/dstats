package krys.ranking;

import krys.combat.DamageBreakdown;
import krys.combat.DamageEngine;
import krys.paladin.PaladinSkillTreeStatus;
import krys.paladin.PaladinTreeSkill;
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

/** Ranking obrażeń oparty o rejestr klasy; stary foundation Paladyna pozostaje tylko jako legacy/test-only. */
public final class DamageRankingService {
    private final DamageEngine damageEngine;
    private final SkillTreeRegistryProvider skillTreeRegistryProvider;

    public DamageRankingService(DamageEngine damageEngine) {
        this(damageEngine, SkillTreeRegistryProvider.paladinOnly());
    }

    public DamageRankingService(DamageEngine damageEngine,
                                SkillTreeRegistryProvider skillTreeRegistryProvider) {
        this.damageEngine = damageEngine;
        this.skillTreeRegistryProvider = skillTreeRegistryProvider;
    }

    public List<PaladinSkillDamageRankingEntry> rankDamageSkills(PaladinDamageRankingMetric metric) {
        return rankDamageSkills(PlayableClass.PALADIN, metric, PaladinDamageTargetMode.SINGLE_TARGET);
    }

    public List<PaladinSkillDamageRankingEntry> rankDamageSkills(PaladinDamageRankingMetric metric,
                                                                 PaladinDamageTargetMode targetMode) {
        return rankDamageSkills(PlayableClass.PALADIN, metric, targetMode);
    }

    public List<PaladinSkillDamageRankingEntry> rankDamageSkills(PlayableClass playableClass,
                                                                 PaladinDamageRankingMetric metric) {
        return rankDamageSkills(playableClass, metric, PaladinDamageTargetMode.SINGLE_TARGET);
    }

    public List<PaladinSkillDamageRankingEntry> rankDamageSkills(PlayableClass playableClass,
                                                                 PaladinDamageRankingMetric metric,
                                                                 PaladinDamageTargetMode targetMode) {
        if (targetMode != PaladinDamageTargetMode.SINGLE_TARGET) {
            throw new IllegalArgumentException("Obsługiwany jest tylko tryb SINGLE_TARGET.");
        }

        List<PaladinSkillDamageRankingEntry> entries = new ArrayList<>();
        for (PaladinTreeSkill skill : skillTreeRegistryProvider.registryFor(playableClass).allSkills()) {
            PaladinSkillDamageRankingEntry entry = createTreeEntry(skill, targetMode);
            if (entry.getVerificationStatus() == PaladinSkillDamageVerificationStatus.NON_DAMAGE) {
                continue;
            }
            entries.add(entry);
        }

        entries.sort(comparatorFor(metric).reversed().thenComparing(PaladinSkillDamageRankingEntry::getSkillName));
        return List.copyOf(entries);
    }

    public List<PaladinSkillDamageRankingEntry> rankDamageSkills(HeroBuildSnapshot snapshot,
                                                                 PaladinDamageRankingMetric metric) {
        return rankDamageSkills(metric, PaladinDamageTargetMode.SINGLE_TARGET);
    }

    public List<PaladinSkillDamageRankingEntry> rankDamageSkills(HeroBuildSnapshot snapshot,
                                                                 PaladinDamageRankingMetric metric,
                                                                 PaladinDamageTargetMode targetMode) {
        return rankDamageSkills(metric, targetMode);
    }

    public List<PaladinSkillDamageRankingEntry> describePaladinTreeSkills() {
        return describeTreeSkills(PlayableClass.PALADIN);
    }

    public List<PaladinSkillDamageRankingEntry> describeTreeSkills(PlayableClass playableClass) {
        List<PaladinSkillDamageRankingEntry> entries = new ArrayList<>();
        for (PaladinTreeSkill skill : skillTreeRegistryProvider.registryFor(playableClass).allSkills()) {
            entries.add(createTreeEntry(skill, PaladinDamageTargetMode.SINGLE_TARGET));
        }
        return List.copyOf(entries);
    }

    public List<PaladinSkillDamageRankingEntry> rankLegacyFoundationDamageSkills(HeroBuildSnapshot snapshot,
                                                                                PaladinDamageRankingMetric metric) {
        List<PaladinSkillDamageRankingEntry> entries = new ArrayList<>();
        for (SkillId skillId : SkillId.values()) {
            SkillState state = snapshot.getSkillState(skillId);
            if (state == null || state.getRank() <= 0) {
                continue;
            }

            PaladinSkillDamageRankingEntry entry = createLegacyFoundationEntry(snapshot, skillId, state, PaladinDamageTargetMode.SINGLE_TARGET);
            if (entry.getVerificationStatus() == PaladinSkillDamageVerificationStatus.NON_DAMAGE) {
                continue;
            }
            entries.add(entry);
        }

        entries.sort(comparatorFor(metric).reversed().thenComparing(PaladinSkillDamageRankingEntry::getSkillName));
        return List.copyOf(entries);
    }

    public List<PaladinSkillDamageRankingEntry> describeLegacyConfiguredFoundationSkills(HeroBuildSnapshot snapshot) {
        List<PaladinSkillDamageRankingEntry> entries = new ArrayList<>();
        for (SkillId skillId : SkillId.values()) {
            SkillState state = snapshot.getSkillState(skillId);
            if (state == null || state.getRank() <= 0) {
                continue;
            }
            entries.add(createLegacyFoundationEntry(snapshot, skillId, state, PaladinDamageTargetMode.SINGLE_TARGET));
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

    private PaladinSkillDamageRankingEntry createTreeEntry(PaladinTreeSkill skill, PaladinDamageTargetMode targetMode) {
        return new PaladinSkillDamageRankingEntry(
                skill.getSkillId(),
                skill.getSkillName(),
                skill.getSourcePdf(),
                skill.getSkillGroup(),
                skill.getBaseDamagePercentAtRank1(),
                skill.getBaseDamagePercentAtTreeMaxRank(),
                mapTreeDamageModelType(skill.getStatus()),
                null,
                null,
                null,
                null,
                targetMode,
                mapTreeStatus(skill.getStatus()),
                skill.getNotes()
        );
    }

    private PaladinSkillDamageRankingEntry createLegacyFoundationEntry(HeroBuildSnapshot snapshot,
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
                    "Legacy/test-only: skill nie wnosi direct ani delayed damage w starym foundation."
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
                PaladinSkillDamageVerificationStatus.SUPPORTED,
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

    private static PaladinSkillDamageModelType mapTreeDamageModelType(PaladinSkillTreeStatus status) {
        return switch (status) {
            case SUPPORTED -> PaladinSkillDamageModelType.DIRECT_HIT;
            case NEEDS_VERIFICATION -> PaladinSkillDamageModelType.VERIFICATION_GATED;
            case NON_DAMAGE -> PaladinSkillDamageModelType.NON_DAMAGE;
            case UNSUPPORTED -> PaladinSkillDamageModelType.UNSUPPORTED;
        };
    }

    private static PaladinSkillDamageVerificationStatus mapTreeStatus(PaladinSkillTreeStatus status) {
        return switch (status) {
            case SUPPORTED -> PaladinSkillDamageVerificationStatus.SUPPORTED;
            case NEEDS_VERIFICATION -> PaladinSkillDamageVerificationStatus.NEEDS_VERIFICATION;
            case NON_DAMAGE -> PaladinSkillDamageVerificationStatus.NON_DAMAGE;
            case UNSUPPORTED -> PaladinSkillDamageVerificationStatus.UNSUPPORTED;
        };
    }

    private static Comparator<PaladinSkillDamageRankingEntry> comparatorFor(PaladinDamageRankingMetric metric) {
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

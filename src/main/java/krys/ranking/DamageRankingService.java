package krys.ranking;

import krys.combat.DamageEngine;
import krys.simulation.HeroBuildSnapshot;
import krys.skilltree.CharacterTreeSkill;
import krys.skilltree.SkillTreeStatus;
import krys.verification.VerificationMatrix;
import krys.verification.VerificationMatrixEntry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Ranking obrażeń oparty o rejestr klasy; stary foundation Paladyna pozostaje tylko jako legacy/test-only. */
public final class DamageRankingService {
    private final LegacyFoundationDamageRankingService legacyRankingService;
    private final SkillTreeRegistryProvider skillTreeRegistryProvider;

    public DamageRankingService(DamageEngine damageEngine) {
        this(damageEngine, SkillTreeRegistryProvider.production());
    }

    public DamageRankingService(DamageEngine damageEngine,
                                SkillTreeRegistryProvider skillTreeRegistryProvider) {
        this.legacyRankingService = new LegacyFoundationDamageRankingService(damageEngine);
        this.skillTreeRegistryProvider = skillTreeRegistryProvider;
    }

    public List<SkillDamageRankingEntry> rankDamageSkills(DamageRankingMetric metric) {
        return rankDamageSkills(skillTreeRegistryProvider.defaultClass(), metric, DamageTargetMode.SINGLE_TARGET);
    }

    public List<SkillDamageRankingEntry> rankDamageSkills(DamageRankingMetric metric,
                                                         DamageTargetMode targetMode) {
        return rankDamageSkills(skillTreeRegistryProvider.defaultClass(), metric, targetMode);
    }

    public List<SkillDamageRankingEntry> rankDamageSkills(PlayableClass playableClass,
                                                         DamageRankingMetric metric) {
        return rankDamageSkills(playableClass, metric, DamageTargetMode.SINGLE_TARGET);
    }

    public List<SkillDamageRankingEntry> rankDamageSkills(PlayableClass playableClass,
                                                         DamageRankingMetric metric,
                                                         DamageTargetMode targetMode) {
        if (targetMode != DamageTargetMode.SINGLE_TARGET) {
            throw new IllegalArgumentException("Obsługiwany jest tylko tryb SINGLE_TARGET.");
        }

        List<SkillDamageRankingEntry> entries = new ArrayList<>();
        for (CharacterTreeSkill skill : skillTreeRegistryProvider.registryFor(playableClass).allSkills()) {
            SkillDamageRankingEntry entry = createTreeEntry(skill, targetMode);
            if (entry.getVerificationStatus() == SkillDamageVerificationStatus.NON_DAMAGE) {
                continue;
            }
            entries.add(entry);
        }

        entries.sort(comparatorFor(metric).reversed().thenComparing(SkillDamageRankingEntry::getSkillName));
        return List.copyOf(entries);
    }

    public List<SkillDamageRankingEntry> rankDamageSkills(HeroBuildSnapshot snapshot,
                                                         DamageRankingMetric metric) {
        return rankDamageSkills(metric, DamageTargetMode.SINGLE_TARGET);
    }

    public List<SkillDamageRankingEntry> rankDamageSkills(HeroBuildSnapshot snapshot,
                                                         DamageRankingMetric metric,
                                                         DamageTargetMode targetMode) {
        return rankDamageSkills(metric, targetMode);
    }

    public List<SkillDamageRankingEntry> describeCharacterTreeSkills() {
        return describeTreeSkills(skillTreeRegistryProvider.defaultClass());
    }

    public List<SkillDamageRankingEntry> describeTreeSkills(PlayableClass playableClass) {
        List<SkillDamageRankingEntry> entries = new ArrayList<>();
        for (CharacterTreeSkill skill : skillTreeRegistryProvider.registryFor(playableClass).allSkills()) {
            entries.add(createTreeEntry(skill, DamageTargetMode.SINGLE_TARGET));
        }
        return List.copyOf(entries);
    }

    public List<SkillDamageRankingEntry> rankLegacyFoundationDamageSkills(HeroBuildSnapshot snapshot,
                                                                 DamageRankingMetric metric) {
        return legacyRankingService.rankLegacyFoundationDamageSkills(snapshot, metric);
    }

    public List<SkillDamageRankingEntry> describeLegacyConfiguredFoundationSkills(HeroBuildSnapshot snapshot) {
        return legacyRankingService.describeLegacyConfiguredFoundationSkills(snapshot);
    }
    public List<SkillDamageRankingEntry> describeVerificationGatedMechanics() {
        List<SkillDamageRankingEntry> entries = new ArrayList<>();
        for (VerificationMatrixEntry verificationEntry : VerificationMatrix.all()) {
            entries.add(new SkillDamageRankingEntry(
                    verificationEntry.getSkillId(),
                    verificationEntry.getSkillId(),
                    verificationEntry.getSourcePdf(),
                    verificationEntry.getSkillGroup(),
                    SkillDamageModelType.VERIFICATION_GATED,
                    null,
                    null,
                    null,
                    null,
                    DamageTargetMode.SINGLE_TARGET,
                    SkillDamageVerificationStatus.NEEDS_VERIFICATION,
                    verificationEntry.getQuestion()
            ));
        }
        return List.copyOf(entries);
    }

    private SkillDamageRankingEntry createTreeEntry(CharacterTreeSkill skill, DamageTargetMode targetMode) {
        return new SkillDamageRankingEntry(
                skill.getSkillId(),
                skill.getSkillName(),
                skill.getSourceReference(),
                skill.getSkillGroup(),
                skill.getBaseDamagePercentAtRank1(),
                skill.getBaseDamagePercentAtTreeMaxRank(),
                mapTreeDamageModelType(skill.getStatus()),
                null,
                skill.getCooldownSeconds(),
                null,
                null,
                targetMode,
                mapTreeStatus(skill.getStatus()),
                skill.getNotes()
        );
    }

    private static SkillDamageModelType mapTreeDamageModelType(SkillTreeStatus status) {
        return switch (status) {
            case SUPPORTED -> SkillDamageModelType.DIRECT_HIT;
            case NEEDS_VERIFICATION -> SkillDamageModelType.VERIFICATION_GATED;
            case NON_DAMAGE -> SkillDamageModelType.NON_DAMAGE;
            case UNSUPPORTED -> SkillDamageModelType.UNSUPPORTED;
        };
    }

    private static SkillDamageVerificationStatus mapTreeStatus(SkillTreeStatus status) {
        return switch (status) {
            case SUPPORTED -> SkillDamageVerificationStatus.SUPPORTED;
            case NEEDS_VERIFICATION -> SkillDamageVerificationStatus.NEEDS_VERIFICATION;
            case NON_DAMAGE -> SkillDamageVerificationStatus.NON_DAMAGE;
            case UNSUPPORTED -> SkillDamageVerificationStatus.UNSUPPORTED;
        };
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

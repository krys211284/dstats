package krys.simulation;

import krys.combat.DamageBreakdown;
import krys.combat.DamageEngine;
import krys.combat.DelayedHitBreakdown;
import krys.combat.ReactiveHitBreakdown;
import krys.item.Item;
import krys.item.ItemStatType;
import krys.paladin.PaladinSkillTreeRegistry;
import krys.paladin.PaladinOathId;
import krys.paladin.PaladinTreeSkill;
import krys.paladin.SkillCategory;
import krys.skill.EffectType;
import krys.skill.PaladinSkillDefs;
import krys.skill.ReactiveSelfBuffProfile;
import krys.skill.SkillDef;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeModifierChoice;
import krys.skill.SkillRuntimeEffect;
import krys.skill.SkillState;
import krys.skill.StatusId;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tickowa ręczna symulacja dla M7.
 * Ten sam przebieg pętli runtime liczy wynik końcowy, delayed hity, reactive damage i stepTrace.
 */
public final class ManualSimulationService {
    private static final String TICK_ORDER_LABEL = "delayed -> reactive -> active cast";
    private static final String VERATHIEL_SHARD_ASPECT_ID = "verathiel_shard";
    private static final double VERATHIEL_BASIC_SKILL_RESOURCE_COST = 25.0d;
    private static final double MOLOCH_ANIMUS_COST = 8.0d;
    private static final double MOLOCH_MIN_ANIMUS = 1.0d;
    private static final double CLASH_ANIMUS_GENERATION = 2.0d;
    private static final int MOLOCH_BUFF_DURATION_SECONDS = 5;
    private static final double MOLOCH_DAMAGE_MULTIPLIER = 1.60d;
    private static final double RESOURCE_EPSILON = 0.0000001d;
    private final DamageEngine damageEngine;

    public ManualSimulationService(DamageEngine damageEngine) {
        this.damageEngine = damageEngine;
    }

    public SimulationResult calculateCurrentBuild(HeroBuildSnapshot snapshot, int horizonSeconds) {
        long totalDamage = 0L;
        List<DelayedHitBreakdown> delayedHitBreakdowns = new ArrayList<>();
        List<ReactiveHitBreakdown> reactiveHitBreakdowns = new ArrayList<>();
        List<SimulationStepTrace> stepTrace = new ArrayList<>();
        List<PendingDelayedHit> pendingDelayedHits = new ArrayList<>();
        Map<SkillId, Integer> lastUsedSeconds = new EnumMap<>(SkillId.class);
        Map<SkillId, SkillHitDebugSnapshot> directHitDebugBySkill = new LinkedHashMap<>();
        long totalReactiveDamage = 0L;
        ReactiveBuffState reactiveBuffState = new ReactiveBuffState();
        CooldownState cooldownState = new CooldownState();
        TargetStatusState targetStatusState = new TargetStatusState();
        double currentPrimaryResource = clampResource(snapshot.getInitialPrimaryResource(), snapshot.getMaxPrimaryResource());
        double totalPrimaryResourceCost = 0.0d;
        double totalPrimaryResourceGenerated = 0.0d;
        double totalPrimaryResourceRegenerated = 0.0d;
        boolean molochRuntimeActive = isMolochOathActive(snapshot);
        double maxAnimus = molochRuntimeActive
                ? Math.max(MOLOCH_MIN_ANIMUS, snapshot.getMaxAnimus())
                : Math.max(0.0d, snapshot.getMaxAnimus());
        double minAnimus = molochRuntimeActive ? MOLOCH_MIN_ANIMUS : 0.0d;
        double currentAnimus = molochRuntimeActive
                ? clampAnimus(snapshot.getInitialAnimus(), maxAnimus, minAnimus)
                : clampAnimus(snapshot.getInitialAnimus(), maxAnimus, minAnimus);
        double initialAnimus = currentAnimus;
        int molochBuffExpiresAtSecond = 0;
        int molochBuffActivationCount = 0;
        double totalClashAnimusGenerated = 0.0d;

        for (int second = 1; second <= horizonSeconds; second++) {
            double primaryResourceBefore = currentPrimaryResource;
            double animusBefore = currentAnimus;
            EnumSet<StatusId> activeTargetStatuses = targetStatusState.getActiveStatuses(second);
            long delayedDamage = 0L;
            Iterator<PendingDelayedHit> delayedIterator = pendingDelayedHits.iterator();
            while (delayedIterator.hasNext()) {
                PendingDelayedHit pendingDelayedHit = delayedIterator.next();
                if (pendingDelayedHit.triggerSecond != second) {
                    continue;
                }

                DamageBreakdown delayedBreakdown = damageEngine.calculateStandaloneHit(
                        snapshot,
                        pendingDelayedHit.skillDamagePercent,
                        pendingDelayedHit.delayedHitName,
                        "delayed",
                        activeTargetStatuses
                );
                delayedDamage += delayedBreakdown.getFinalDamage();
                delayedHitBreakdowns.add(new DelayedHitBreakdown(
                        pendingDelayedHit.sourceSkillName,
                        pendingDelayedHit.delayedHitName,
                        pendingDelayedHit.appliedSecond,
                        pendingDelayedHit.triggerSecond,
                        second,
                        false,
                        delayedBreakdown
                ));
                delayedIterator.remove();
            }
            totalDamage += delayedDamage;

            long reactiveDamage = 0L;
            if (isEnemyHitSecond(second) && damageEngine.hasReactiveFoundation(snapshot)) {
                ReactiveHitBreakdown reactiveHitBreakdown = damageEngine.calculateReactiveHit(
                        snapshot,
                        second,
                        reactiveBuffState.getActiveBlockChanceBonusPercent(second),
                        reactiveBuffState.getActiveThornsBonus(second),
                        reactiveBuffState.isResolveActive(second),
                        reactiveBuffState.getResolveRemainingSeconds(second),
                        reactiveBuffState.isPunishmentActive(second)
                );
                reactiveDamage = reactiveHitBreakdown.getReactiveFinalDamage();
                totalDamage += reactiveDamage;
                totalReactiveDamage += reactiveDamage;
                reactiveHitBreakdowns.add(reactiveHitBreakdown);
            }

            SkillSelectionResult selectionResult = selectSkillForTick(snapshot, second, lastUsedSeconds, cooldownState, primaryResourceBefore);
            long directDamage = 0L;
            SimulationActionType actionType = SimulationActionType.WAIT;
            String actionName = "WAIT";
            double primaryResourceCost = 0.0d;
            double primaryResourceGenerated = 0.0d;
            double animusGenerated = 0.0d;
            double animusSpent = 0.0d;
            boolean molochBuffActivated = false;
            boolean molochBuffActiveForDamage = false;
            boolean molochBuffActiveBeforeHit = false;

            if (selectionResult.selectedSkillId != null) {
                actionType = SimulationActionType.SKILL;
                actionName = selectionResult.selectedSkillName;
                primaryResourceCost = selectionResult.primaryResourceCost;
                currentPrimaryResource = Math.max(0.0d, currentPrimaryResource - primaryResourceCost);
                totalPrimaryResourceCost += primaryResourceCost;

                molochBuffActiveBeforeHit = molochRuntimeActive
                        && isMolochSkill(selectionResult.selectedSkillId)
                        && second <= molochBuffExpiresAtSecond;
                if (molochBuffActiveBeforeHit) {
                    molochBuffActiveForDamage = true;
                } else if (molochRuntimeActive && isMolochSkill(selectionResult.selectedSkillId)
                        && currentAnimus + RESOURCE_EPSILON >= MOLOCH_ANIMUS_COST) {
                    animusSpent = MOLOCH_ANIMUS_COST;
                    currentAnimus = clampAnimus(currentAnimus - MOLOCH_ANIMUS_COST, maxAnimus, minAnimus);
                    molochBuffExpiresAtSecond = second + MOLOCH_BUFF_DURATION_SECONDS - 1;
                    molochBuffActivationCount++;
                    molochBuffActivated = true;
                    molochBuffActiveForDamage = true;
                }
                double molochMultiplier = molochBuffActiveForDamage ? MOLOCH_DAMAGE_MULTIPLIER : 1.0d;

                DamageBreakdown directHitBreakdown = damageEngine.calculate(
                        snapshot,
                        selectionResult.selectedSkillId,
                        activeTargetStatuses,
                        molochMultiplier
                );
                directDamage = directHitBreakdown.getFinalDamage();
                totalDamage += directDamage;
                lastUsedSeconds.put(selectionResult.selectedSkillId, second);
                directHitDebugBySkill.putIfAbsent(
                        selectionResult.selectedSkillId,
                        new SkillHitDebugSnapshot(
                                selectionResult.selectedSkillId,
                                selectionResult.selectedSkillName,
                                snapshot.getSkillState(selectionResult.selectedSkillId).getRank(),
                                directHitBreakdown
                        )
                );

                SkillState state = snapshot.getSkillState(selectionResult.selectedSkillId);
                SkillDef skillDef = PaladinSkillDefs.get(selectionResult.selectedSkillId);
                if (state != null) {
                    for (SkillRuntimeEffect effect : resolvePostCastEffects(skillDef, state)) {
                        if (effect.getEffectType() == EffectType.APPLY_DELAYED_HIT
                                && !hasActiveDelayedHit(pendingDelayedHits, effect.getComponentName())) {
                            pendingDelayedHits.add(new PendingDelayedHit(
                                    skillDef.getName(),
                                    effect.getComponentName(),
                                    second,
                                    second + effect.getDurationSeconds(),
                                    effect.getSkillDamagePercent()
                            ));
                        }
                        if (effect.getEffectType() == EffectType.APPLY_STATUS) {
                            targetStatusState.apply(effect.getAppliedStatus(), second, effect.getDurationSeconds());
                        }
                    }
                    cooldownState.apply(selectionResult.selectedSkillId, second, resolveEffectiveCooldownSeconds(skillDef, state));
                }

                if (state != null && state.isBaseUpgrade()) {
                    applyReactiveBuffProfile(skillDef.getBaseReactiveBuffProfile(), second, reactiveBuffState);
                    applyReactiveBuffProfile(skillDef.getChoiceReactiveBuffProfile(state.getChoiceUpgrade()), second, reactiveBuffState);
                }
                primaryResourceGenerated = resolvePrimaryResourceGeneration(selectionResult.selectedSkillId);
                currentPrimaryResource = clampResource(currentPrimaryResource + primaryResourceGenerated, snapshot.getMaxPrimaryResource());
                totalPrimaryResourceGenerated += primaryResourceGenerated;
            }
            double primaryResourceBeforeRegen = currentPrimaryResource;
            currentPrimaryResource = clampResource(currentPrimaryResource + snapshot.getPrimaryResourceRegenPerSecond(), snapshot.getMaxPrimaryResource());
            double primaryResourceRegenerated = currentPrimaryResource - primaryResourceBeforeRegen;
            totalPrimaryResourceRegenerated += primaryResourceRegenerated;
            if (selectionResult.selectedSkillId != null && hasClashAnimusModifier(snapshot, selectionResult.selectedSkillId)) {
                double animusBeforeGeneration = currentAnimus;
                currentAnimus = clampAnimus(currentAnimus + CLASH_ANIMUS_GENERATION, maxAnimus, minAnimus);
                animusGenerated = currentAnimus - animusBeforeGeneration;
                totalClashAnimusGenerated += animusGenerated;
            }

            long totalStepDamage = delayedDamage + reactiveDamage + directDamage;
            stepTrace.add(new SimulationStepTrace(
                    second,
                    actionType,
                    actionName,
                    directDamage,
                    delayedDamage,
                    reactiveDamage,
                    totalStepDamage,
                    totalDamage,
                    selectionResult.skillBarStates,
                    selectionResult.selectionReason,
                    TICK_ORDER_LABEL,
                    primaryResourceBefore,
                    primaryResourceCost,
                    primaryResourceGenerated,
                    primaryResourceRegenerated,
                    currentPrimaryResource,
                    animusBefore,
                    animusSpent,
                    animusSpent > 0.0d ? minAnimus : 0.0d,
                    animusGenerated,
                    currentAnimus,
                    molochBuffActivated,
                    molochBuffActiveForDamage,
                    molochBuffRemainingSeconds(second, molochBuffExpiresAtSecond)
            ));
        }

        for (PendingDelayedHit pendingDelayedHit : pendingDelayedHits) {
            delayedHitBreakdowns.add(new DelayedHitBreakdown(
                    pendingDelayedHit.sourceSkillName,
                    pendingDelayedHit.delayedHitName,
                    pendingDelayedHit.appliedSecond,
                    pendingDelayedHit.triggerSecond,
                    null,
                    true,
                    null
            ));
        }

        double dps = horizonSeconds <= 0 ? 0.0d : (double) totalDamage / horizonSeconds;
        return new SimulationResult(
                totalDamage,
                dps,
                horizonSeconds,
                orderDirectHitDebugSnapshots(snapshot, directHitDebugBySkill),
                delayedHitBreakdowns,
                reactiveHitBreakdowns,
                totalReactiveDamage,
                reactiveBuffState.isResolveActive(horizonSeconds),
                resolveActiveBlockChanceAtEnd(snapshot, reactiveBuffState, horizonSeconds),
                reactiveBuffState.getActiveThornsBonus(horizonSeconds),
                stepTrace,
                hasActiveDelayedHit(pendingDelayedHits, "Judgement"),
                snapshot.getInitialPrimaryResource(),
                currentPrimaryResource,
                snapshot.getMaxPrimaryResource(),
                snapshot.getPrimaryResourceRegenPerSecond(),
                totalPrimaryResourceCost,
                totalPrimaryResourceGenerated,
                totalPrimaryResourceRegenerated,
                molochRuntimeActive,
                initialAnimus,
                currentAnimus,
                maxAnimus,
                minAnimus,
                molochBuffActivationCount,
                totalClashAnimusGenerated
        );
    }

    static boolean isEnemyHitSecond(int second) {
        return second >= 3 && second % 3 == 0;
    }

    private static void applyReactiveBuffProfile(ReactiveSelfBuffProfile buffProfile,
                                                 int second,
                                                 ReactiveBuffState reactiveBuffState) {
        if (buffProfile == null || buffProfile.getDurationSeconds() <= 0) {
            return;
        }
        reactiveBuffState.apply(buffProfile, second);
    }

    private static double resolveActiveBlockChanceAtEnd(HeroBuildSnapshot snapshot,
                                                        ReactiveBuffState reactiveBuffState,
                                                        int horizonSeconds) {
        double baseBlockChance = Item.sumStat(snapshot.getEquippedItems(), ItemStatType.BLOCK_CHANCE) / 100.0d;
        return baseBlockChance + (reactiveBuffState.getActiveBlockChanceBonusPercent(horizonSeconds) / 100.0d);
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

    private static List<SkillHitDebugSnapshot> orderDirectHitDebugSnapshots(HeroBuildSnapshot snapshot,
                                                                            Map<SkillId, SkillHitDebugSnapshot> directHitDebugBySkill) {
        List<SkillHitDebugSnapshot> ordered = new ArrayList<>();
        for (SkillId skillId : snapshot.getSelectedSkillBar()) {
            SkillHitDebugSnapshot debugSnapshot = directHitDebugBySkill.get(skillId);
            if (debugSnapshot != null) {
                ordered.add(debugSnapshot);
            }
        }
        for (Map.Entry<SkillId, SkillHitDebugSnapshot> entry : directHitDebugBySkill.entrySet()) {
            if (!snapshot.getSelectedSkillBar().contains(entry.getKey())) {
                ordered.add(entry.getValue());
            }
        }
        return ordered;
    }

    private static SkillSelectionResult selectSkillForTick(HeroBuildSnapshot snapshot,
                                                           int second,
                                                           Map<SkillId, Integer> lastUsedSeconds,
                                                           CooldownState cooldownState,
                                                           double currentPrimaryResource) {
        List<SkillEvaluation> evaluations = new ArrayList<>();
        SkillEvaluation selected = null;

        List<SkillId> selectedSkillBar = snapshot.getSelectedSkillBar();
        for (int index = 0; index < selectedSkillBar.size(); index++) {
            SkillId skillId = selectedSkillBar.get(index);
            SkillDef skillDef = PaladinSkillDefs.get(skillId);
            SkillState state = snapshot.getSkillState(skillId);
            int rank = state == null ? 0 : state.getRank();
            Optional<String> equipmentBlockingReason = SkillEquipmentRequirement.blockingReason(skillId, snapshot);
            boolean legalActive = state != null && rank > 0 && equipmentBlockingReason.isEmpty();
            Integer lastUsedSecond = lastUsedSeconds.get(skillId);
            boolean neverUsed = lastUsedSecond == null;
            double effectivePrimaryResourceCost = resolveEffectivePrimaryResourceCost(snapshot, skillId, skillDef);
            boolean hasRequiredResource = currentPrimaryResource + RESOURCE_EPSILON >= effectivePrimaryResourceCost;
            boolean onCooldown = cooldownState.isOnCooldown(skillId, second);
            int cooldownRemainingSeconds = cooldownState.getRemainingSeconds(skillId, second);
            SkillEvaluation evaluation = new SkillEvaluation(
                    skillId,
                    skillDef.getName(),
                    index,
                    rank,
                    legalActive,
                    onCooldown,
                    cooldownRemainingSeconds,
                    hasRequiredResource,
                    currentPrimaryResource,
                    effectivePrimaryResourceCost,
                    neverUsed,
                    lastUsedSecond,
                    equipmentBlockingReason.orElse(null)
            );
            evaluations.add(evaluation);

            if (!evaluation.isLegalCandidate()) {
                continue;
            }
            if (selected == null || isBetterLruCandidate(evaluation, selected)) {
                selected = evaluation;
            }
        }

        List<SkillBarStateTrace> skillBarStates = new ArrayList<>();
        for (SkillEvaluation evaluation : evaluations) {
            skillBarStates.add(new SkillBarStateTrace(
                    evaluation.skillId,
                    evaluation.skillName,
                    evaluation.barIndex,
                    evaluation.rank,
                    evaluation.legalActive,
                    evaluation.onCooldown,
                    evaluation.cooldownRemainingSeconds,
                    evaluation.hasRequiredResource,
                    evaluation.currentPrimaryResource,
                    evaluation.effectivePrimaryResourceCost,
                    evaluation.neverUsed,
                    evaluation.lastUsedSecond,
                    selected != null && selected.skillId == evaluation.skillId
            ));
        }

        if (selected == null) {
            String reason;
            if (selectedSkillBar.isEmpty()) {
                reason = "WAIT: pasek aktywnych skilli jest pusty.";
            } else {
                reason = firstEquipmentBlockingReason(evaluations)
                        .or(() -> firstResourceBlockingReason(evaluations))
                        .orElse("WAIT: brak legalnego skilla do użycia w tym ticku.");
            }
            return new SkillSelectionResult(null, "WAIT", 0.0d, skillBarStates, reason);
        }

        return new SkillSelectionResult(
                selected.skillId,
                selected.skillName,
                selected.effectivePrimaryResourceCost,
                skillBarStates,
                buildSelectionReason(evaluations, selected)
        );
    }

    private static double resolveEffectivePrimaryResourceCost(HeroBuildSnapshot snapshot, SkillId skillId, SkillDef skillDef) {
        double baseCost = skillDef.getResourceCost();
        if (snapshot.hasActiveAspect(VERATHIEL_SHARD_ASPECT_ID) && isBasicSkill(skillId)) {
            return baseCost + VERATHIEL_BASIC_SKILL_RESOURCE_COST;
        }
        return baseCost;
    }

    private static double resolvePrimaryResourceGeneration(SkillId skillId) {
        return findTreeSkill(skillId)
                .map(PaladinTreeSkill::getFaithGenerationBase)
                .map(Integer::doubleValue)
                .orElse(0.0d);
    }

    private static boolean isBasicSkill(SkillId skillId) {
        return findTreeSkill(skillId)
                .map(skill -> skill.hasSkillCategory(SkillCategory.PODSTAWOWE))
                .orElse(false);
    }

    private static boolean isMolochOathActive(HeroBuildSnapshot snapshot) {
        return PaladinOathId.JUGGERNAUT.name().equals(snapshot.getSelectedPaladinOathId());
    }

    static boolean isMolochSkill(SkillId skillId) {
        return findTreeSkill(skillId)
                .map(skill -> skill.hasSkillCategory(SkillCategory.MOLOCH))
                .orElse(false);
    }

    private static boolean hasClashAnimusModifier(HeroBuildSnapshot snapshot, SkillId skillId) {
        if (skillId != SkillId.CLASH) {
            return false;
        }
        SkillState state = snapshot.getSkillState(skillId);
        return state != null && (state.getRuntimeModifierChoice() == SkillRuntimeModifierChoice.ANIMUS
                || SkillState.CLASH_ANIMUS_CHOICE.equals(state.getChoiceGroup1()));
    }

    private static Optional<PaladinTreeSkill> findTreeSkill(SkillId skillId) {
        return switch (skillId) {
            case BRANDISH -> PaladinSkillTreeRegistry.findSkill("wymach");
            case HOLY_BOLT -> PaladinSkillTreeRegistry.findSkill("swiety_pocisk");
            case CLASH -> PaladinSkillTreeRegistry.findSkill("starcie");
            case ADVANCE -> PaladinSkillTreeRegistry.findSkill("natarcie");
        };
    }

    private static double clampResource(double value, double maxPrimaryResource) {
        return Math.max(0.0d, Math.min(value, maxPrimaryResource));
    }

    private static double clampAnimus(double value, double maxAnimus, double minAnimus) {
        return Math.max(minAnimus, Math.min(value, maxAnimus));
    }

    private static int molochBuffRemainingSeconds(int second, int molochBuffExpiresAtSecond) {
        if (second <= 0 || second > molochBuffExpiresAtSecond) {
            return 0;
        }
        return molochBuffExpiresAtSecond - second + 1;
    }

    private static boolean isBetterLruCandidate(SkillEvaluation candidate, SkillEvaluation currentBest) {
        if (candidate.neverUsed != currentBest.neverUsed) {
            return candidate.neverUsed;
        }
        if (candidate.neverUsed) {
            return candidate.barIndex < currentBest.barIndex;
        }
        if (!candidate.lastUsedSecond.equals(currentBest.lastUsedSecond)) {
            return candidate.lastUsedSecond < currentBest.lastUsedSecond;
        }
        return candidate.barIndex < currentBest.barIndex;
    }

    private static String buildSelectionReason(List<SkillEvaluation> evaluations, SkillEvaluation selected) {
        long legalNeverUsedCount = evaluations.stream()
                .filter(SkillEvaluation::isLegalCandidate)
                .filter(SkillEvaluation::isNeverUsed)
                .count();
        if (selected.neverUsed) {
            if (legalNeverUsedCount > 1) {
                return "Wybrano " + selected.skillName + ": skill nigdy wcześniej nieużyty ma priorytet LRU, remis rozstrzygnięto kolejnością na pasku.";
            }
            return "Wybrano " + selected.skillName + ": skill nigdy wcześniej nieużyty ma wyższy priorytet niż skill użyty wcześniej.";
        }

        long sameLastUsedCount = evaluations.stream()
                .filter(SkillEvaluation::isLegalCandidate)
                .filter(evaluation -> !evaluation.neverUsed)
                .filter(evaluation -> evaluation.lastUsedSecond.equals(selected.lastUsedSecond))
                .count();
        if (sameLastUsedCount > 1) {
            return "Wybrano " + selected.skillName + ": najdawniej użyty legalny skill w LRU, remis rozstrzygnięto kolejnością na pasku.";
        }
        return "Wybrano " + selected.skillName + ": najdawniej użyty legalny skill według LRU.";
    }

    private static Optional<String> firstEquipmentBlockingReason(List<SkillEvaluation> evaluations) {
        for (SkillEvaluation evaluation : evaluations) {
            if (evaluation.equipmentBlockingReason != null && !evaluation.equipmentBlockingReason.isBlank()) {
                return Optional.of(evaluation.equipmentBlockingReason);
            }
        }
        return Optional.empty();
    }

    private static Optional<String> firstResourceBlockingReason(List<SkillEvaluation> evaluations) {
        for (SkillEvaluation evaluation : evaluations) {
            if (evaluation.legalActive && !evaluation.onCooldown && !evaluation.hasRequiredResource) {
                return Optional.of("WAIT: brak zasobu dla " + evaluation.skillName
                        + " (Wiara " + formatResource(evaluation.currentPrimaryResource)
                        + " / koszt " + formatResource(evaluation.effectivePrimaryResourceCost) + ").");
            }
        }
        return Optional.empty();
    }

    private static String formatResource(double value) {
        return String.format(java.util.Locale.US, "%.2f", value);
    }

    private static boolean hasActiveDelayedHit(List<PendingDelayedHit> pendingDelayedHits, String delayedHitName) {
        for (PendingDelayedHit pendingDelayedHit : pendingDelayedHits) {
            if (pendingDelayedHit.delayedHitName.equals(delayedHitName)) {
                return true;
            }
        }
        return false;
    }

    private record PendingDelayedHit(String sourceSkillName,
                                     String delayedHitName,
                                     int appliedSecond,
                                     int triggerSecond,
                                     long skillDamagePercent) {
    }

    private static final class ReactiveBuffState {
        private int resolveExpiresAtSecond;
        private double activeBlockChanceBonusPercent;
        private int punishmentExpiresAtSecond;
        private double activeThornsBonus;

        private void apply(ReactiveSelfBuffProfile buffProfile, int second) {
            int expiresAtSecond = second + buffProfile.getDurationSeconds() - 1;
            if (buffProfile.isGrantsResolve()) {
                resolveExpiresAtSecond = expiresAtSecond;
                activeBlockChanceBonusPercent = buffProfile.getBlockChanceBonusPercent();
            }
            if (buffProfile.getThornsBonus() > 0.0d) {
                punishmentExpiresAtSecond = expiresAtSecond;
                activeThornsBonus = buffProfile.getThornsBonus();
            }
        }

        private boolean isResolveActive(int second) {
            return second > 0 && second <= resolveExpiresAtSecond;
        }

        private int getResolveRemainingSeconds(int second) {
            if (!isResolveActive(second)) {
                return 0;
            }
            return resolveExpiresAtSecond - second + 1;
        }

        private boolean isPunishmentActive(int second) {
            return second > 0 && second <= punishmentExpiresAtSecond;
        }

        private double getActiveBlockChanceBonusPercent(int second) {
            return isResolveActive(second) ? activeBlockChanceBonusPercent : 0.0d;
        }

        private double getActiveThornsBonus(int second) {
            return isPunishmentActive(second) ? activeThornsBonus : 0.0d;
        }
    }

    private static final class CooldownState {
        private final Map<SkillId, Integer> readyAtSecondBySkill = new EnumMap<>(SkillId.class);

        private void apply(SkillId skillId, int second, int cooldownSeconds) {
            if (cooldownSeconds <= 0) {
                readyAtSecondBySkill.remove(skillId);
                return;
            }
            readyAtSecondBySkill.put(skillId, second + cooldownSeconds);
        }

        private boolean isOnCooldown(SkillId skillId, int second) {
            Integer readyAtSecond = readyAtSecondBySkill.get(skillId);
            return readyAtSecond != null && second < readyAtSecond;
        }

        private int getRemainingSeconds(SkillId skillId, int second) {
            Integer readyAtSecond = readyAtSecondBySkill.get(skillId);
            if (readyAtSecond == null) {
                return 0;
            }
            return Math.max(0, readyAtSecond - second);
        }
    }

    private static final class TargetStatusState {
        private final Map<StatusId, Integer> expiresAtByStatus = new EnumMap<>(StatusId.class);

        private void apply(StatusId statusId, int second, int durationSeconds) {
            if (statusId == StatusId.NONE || durationSeconds <= 0) {
                return;
            }
            int expiresAtSecond = second + durationSeconds;
            Integer currentExpiry = expiresAtByStatus.get(statusId);
            if (currentExpiry == null || expiresAtSecond > currentExpiry) {
                expiresAtByStatus.put(statusId, expiresAtSecond);
            }
        }

        private EnumSet<StatusId> getActiveStatuses(int second) {
            EnumSet<StatusId> activeStatuses = EnumSet.noneOf(StatusId.class);
            for (Map.Entry<StatusId, Integer> entry : expiresAtByStatus.entrySet()) {
                if (entry.getKey() != StatusId.NONE && second <= entry.getValue()) {
                    activeStatuses.add(entry.getKey());
                }
            }
            return activeStatuses;
        }
    }

    private record SkillSelectionResult(SkillId selectedSkillId,
                                        String selectedSkillName,
                                        double primaryResourceCost,
                                        List<SkillBarStateTrace> skillBarStates,
                                        String selectionReason) {
    }

    private record SkillEvaluation(SkillId skillId,
                                   String skillName,
                                   int barIndex,
                                   int rank,
                                   boolean legalActive,
                                   boolean onCooldown,
                                   int cooldownRemainingSeconds,
                                   boolean hasRequiredResource,
                                   double currentPrimaryResource,
                                   double effectivePrimaryResourceCost,
                                   boolean neverUsed,
                                   Integer lastUsedSecond,
                                   String equipmentBlockingReason) {
        private boolean isLegalCandidate() {
            return legalActive && !onCooldown && hasRequiredResource;
        }

        private boolean isNeverUsed() {
            return neverUsed;
        }
    }
}

package krys.simulation;

import krys.combat.DamageBreakdown;
import krys.combat.DamageEngine;
import krys.combat.DelayedHitBreakdown;
import krys.combat.ReactiveHitBreakdown;
import krys.item.Item;
import krys.item.ItemStatType;
import krys.skill.EffectType;
import krys.skill.PaladinSkillDefs;
import krys.skill.ReactiveSelfBuffProfile;
import krys.skill.SkillDef;
import krys.skill.SkillId;
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

/**
 * Tickowa ręczna symulacja dla M7.
 * Ten sam przebieg pętli runtime liczy wynik końcowy, delayed hity, reactive damage i stepTrace.
 */
public final class ManualSimulationService {
    private static final String TICK_ORDER_LABEL = "delayed -> reactive -> active cast";
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

        for (int second = 1; second <= horizonSeconds; second++) {
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

            SkillSelectionResult selectionResult = selectSkillForTick(snapshot, second, lastUsedSeconds, cooldownState);
            long directDamage = 0L;
            SimulationActionType actionType = SimulationActionType.WAIT;
            String actionName = "WAIT";

            if (selectionResult.selectedSkillId != null) {
                actionType = SimulationActionType.SKILL;
                actionName = selectionResult.selectedSkillName;

                DamageBreakdown directHitBreakdown = damageEngine.calculate(snapshot, selectionResult.selectedSkillId, activeTargetStatuses);
                directDamage = directHitBreakdown.getFinalDamage();
                totalDamage += directDamage;
                lastUsedSeconds.put(selectionResult.selectedSkillId, second);
                directHitDebugBySkill.putIfAbsent(
                        selectionResult.selectedSkillId,
                        new SkillHitDebugSnapshot(selectionResult.selectedSkillId, selectionResult.selectedSkillName, directHitBreakdown)
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
                    TICK_ORDER_LABEL
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
                hasActiveDelayedHit(pendingDelayedHits, "Judgement")
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
                                                           CooldownState cooldownState) {
        List<SkillEvaluation> evaluations = new ArrayList<>();
        SkillEvaluation selected = null;

        List<SkillId> selectedSkillBar = snapshot.getSelectedSkillBar();
        for (int index = 0; index < selectedSkillBar.size(); index++) {
            SkillId skillId = selectedSkillBar.get(index);
            SkillDef skillDef = PaladinSkillDefs.get(skillId);
            SkillState state = snapshot.getSkillState(skillId);
            int rank = state == null ? 0 : state.getRank();
            boolean legalActive = state != null && rank > 0;
            Integer lastUsedSecond = lastUsedSeconds.get(skillId);
            boolean neverUsed = lastUsedSecond == null;
            boolean hasRequiredResource = skillDef.getResourceCost() <= 0;
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
                    neverUsed,
                    lastUsedSecond
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
                    evaluation.neverUsed,
                    evaluation.lastUsedSecond,
                    selected != null && selected.skillId == evaluation.skillId
            ));
        }

        if (selected == null) {
            String reason = selectedSkillBar.isEmpty()
                    ? "WAIT: pasek aktywnych skilli jest pusty."
                    : "WAIT: brak legalnego skilla do użycia w tym ticku.";
            return new SkillSelectionResult(null, "WAIT", skillBarStates, reason);
        }

        return new SkillSelectionResult(
                selected.skillId,
                selected.skillName,
                skillBarStates,
                buildSelectionReason(evaluations, selected)
        );
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
                                   boolean neverUsed,
                                   Integer lastUsedSecond) {
        private boolean isLegalCandidate() {
            return legalActive && !onCooldown && hasRequiredResource;
        }

        private boolean isNeverUsed() {
            return neverUsed;
        }
    }
}

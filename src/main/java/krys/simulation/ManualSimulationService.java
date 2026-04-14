package krys.simulation;

import krys.combat.DamageBreakdown;
import krys.combat.DamageEngine;
import krys.combat.DelayedHitBreakdown;
import krys.skill.EffectType;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillDef;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeEffect;
import krys.skill.SkillState;
import krys.skill.StatusId;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Minimalna ręczna symulacja dla M2.
 * Na tym etapie wybór skilla jest jawnie uproszczony: w każdym ticku używany jest
 * pierwszy aktywny skill z action bara. To świadomy zakres slice'u M2, a nie finalna rotacja.
 */
public final class ManualSimulationService {
    private final DamageEngine damageEngine;

    public ManualSimulationService(DamageEngine damageEngine) {
        this.damageEngine = damageEngine;
    }

    public SimulationResult calculateCurrentBuild(HeroBuildSnapshot snapshot, int horizonSeconds) {
        long totalDamage = 0L;
        DamageBreakdown singleHitBreakdown = null;
        SkillId selectedSkillId = selectSkillForCurrentFoundation(snapshot);
        String selectedSkillName = selectedSkillId == null ? "Brak aktywnego skilla" : PaladinSkillDefs.get(selectedSkillId).getName();

        PendingDelayedHit pendingJudgement = null;
        List<DelayedHitBreakdown> delayedHitBreakdowns = new ArrayList<>();

        for (int second = 1; second <= horizonSeconds; second++) {
            if (pendingJudgement != null && pendingJudgement.triggerSecond == second) {
                DamageBreakdown delayedBreakdown = damageEngine.calculateStandaloneHit(
                        snapshot,
                        pendingJudgement.skillDamagePercent,
                        pendingJudgement.delayedHitName,
                        "delayed",
                        EnumSet.noneOf(StatusId.class)
                );
                totalDamage += delayedBreakdown.getFinalDamage();
                delayedHitBreakdowns.add(new DelayedHitBreakdown(
                        pendingJudgement.sourceSkillName,
                        pendingJudgement.delayedHitName,
                        pendingJudgement.appliedSecond,
                        pendingJudgement.triggerSecond,
                        second,
                        false,
                        delayedBreakdown
                ));
                pendingJudgement = null;
            }

            if (selectedSkillId == null) {
                continue;
            }

            singleHitBreakdown = damageEngine.calculate(snapshot, selectedSkillId, EnumSet.noneOf(StatusId.class));
            totalDamage += singleHitBreakdown.getFinalDamage();

            SkillState state = snapshot.getSkillState(selectedSkillId);
            SkillDef skillDef = PaladinSkillDefs.get(selectedSkillId);
            if (state != null && state.isBaseUpgrade()) {
                for (SkillRuntimeEffect effect : skillDef.getBaseUpgradeEffects()) {
                    if (effect.getEffectType() == EffectType.APPLY_DELAYED_HIT && pendingJudgement == null) {
                        pendingJudgement = new PendingDelayedHit(
                                skillDef.getName(),
                                effect.getComponentName(),
                                second,
                                second + effect.getDurationSeconds(),
                                effect.getSkillDamagePercent()
                        );
                    }
                }
            }
        }

        if (pendingJudgement != null) {
            delayedHitBreakdowns.add(new DelayedHitBreakdown(
                    pendingJudgement.sourceSkillName,
                    pendingJudgement.delayedHitName,
                    pendingJudgement.appliedSecond,
                    pendingJudgement.triggerSecond,
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
                selectedSkillName,
                singleHitBreakdown,
                delayedHitBreakdowns,
                pendingJudgement != null
        );
    }

    private static SkillId selectSkillForCurrentFoundation(HeroBuildSnapshot snapshot) {
        for (SkillId skillId : snapshot.getSelectedSkillBar()) {
            SkillState state = snapshot.getSkillState(skillId);
            if (state != null && state.getRank() > 0) {
                return skillId;
            }
        }
        return null;
    }

    private record PendingDelayedHit(String sourceSkillName,
                                     String delayedHitName,
                                     int appliedSecond,
                                     int triggerSecond,
                                     long skillDamagePercent) {
    }
}

package krys.app;

import krys.combat.DamageBreakdown;
import krys.combat.DamageComponentBreakdown;
import krys.combat.DamageEngine;
import krys.combat.DelayedHitBreakdown;
import krys.combat.ReactiveHitBreakdown;
import krys.simulation.ManualSimulationService;
import krys.simulation.SimulationResult;
import krys.simulation.SimulationStepTrace;
import krys.simulation.SkillHitDebugSnapshot;
import krys.simulation.SkillBarStateTrace;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillId;
import krys.skill.SkillState;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Najprostszy możliwy flow użytkownika dla aktualnego foundation manual simulation.
 * Użytkownik uruchamia „Policz aktualny build” z przykładowym snapshotem bieżącego buildu.
 */
public final class CalculateCurrentBuildCli {
    private CalculateCurrentBuildCli() {
    }

    public static void main(String[] args) {
        configureUtf8Output();
        CurrentBuildRequest request = CurrentBuildCliRequestParser.parse(args);
        boolean showTrace = resolveShowTrace(args);
        CurrentBuildCalculationService calculationService = new CurrentBuildCalculationService(
                new ManualSimulationService(new DamageEngine())
        );
        CurrentBuildCalculation calculation = calculationService.calculate(request);
        printResult(calculation, showTrace);
    }

    private static void configureUtf8Output() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
    }

    private static void printResult(CurrentBuildCalculation calculation, boolean showTrace) {
        CurrentBuildRequest request = calculation.getRequest();
        SimulationResult result = calculation.getResult();
        System.out.println("=== Policz aktualny build ===");
        System.out.println("Level: " + request.getLevel());
        System.out.println("Weapon damage: " + request.getWeaponDamage());
        System.out.println("Strength z itemów: " + String.format(Locale.US, "%.0f", request.getStrength()));
        System.out.println("Intelligence z itemów: " + String.format(Locale.US, "%.0f", request.getIntelligence()));
        System.out.println("Thorns: " + String.format(Locale.US, "%.0f", request.getThorns()));
        System.out.println("Block chance: " + String.format(Locale.US, "%.2f%%", request.getBlockChance()));
        System.out.println("Retribution chance: " + String.format(Locale.US, "%.2f%%", request.getRetributionChance()));
        System.out.println("Nauczone skille: " + buildLearnedSkillsLabel(request.getLearnedSkills()));
        System.out.println("Action bar: " + buildActionBarLabel(request.getActionBar()));
        System.out.println("Horyzont: " + result.getHorizonSeconds() + " s");
        System.out.println();
        System.out.println("Total damage: " + result.getTotalDamage());
        System.out.println("DPS: " + String.format(Locale.US, "%.4f", result.getDps()));
        System.out.println("Reactive contribution: " + result.getTotalReactiveDamage());
        System.out.println("Judgement aktywny na koniec: " + (result.isJudgementActiveAtEnd() ? "tak" : "nie"));
        System.out.println("Resolve aktywny na koniec: " + (result.isResolveActiveAtEnd() ? "tak" : "nie"));
        System.out.println("Active block chance na koniec: " + String.format(Locale.US, "%.2f%%", result.getActiveBlockChanceAtEnd() * 100.0d));
        System.out.println("Active thorns bonus na koniec: " + String.format(Locale.US, "%.0f", result.getActiveThornsBonusAtEnd()));
        System.out.println();

        System.out.println("== Direct hit debug ==");
        if (result.getDirectHitDebugSnapshots().isEmpty()) {
            System.out.println("Brak bezpośrednich hitów.");
            System.out.println();
        } else {
            for (SkillHitDebugSnapshot debugSnapshot : result.getDirectHitDebugSnapshots()) {
                DamageBreakdown directHit = debugSnapshot.getBreakdown();
                System.out.println("Skill: " + debugSnapshot.getSkillName());
                System.out.println("Raw hit: " + directHit.getRawDamage());
                System.out.println("Single hit: " + directHit.getFinalDamage());
                System.out.println("Raw crit hit: " + directHit.getRawCriticalDamage());
                System.out.println("Critical hit: " + directHit.getCriticalDamage());
                for (DamageComponentBreakdown component : directHit.getComponents()) {
                    System.out.println("- komponent: " + component.getName()
                            + " | %: " + component.getSkillDamagePercent()
                            + " | raw: " + component.getRawDamage()
                            + " | final: " + component.getFinalDamage());
                }
                System.out.println();
            }
        }

        System.out.println("== Delayed hit debug ==");
        if (result.getDelayedHitBreakdowns().isEmpty()) {
            System.out.println("Brak delayed hitów.");
        } else {
            for (DelayedHitBreakdown entry : result.getDelayedHitBreakdowns()) {
                String status = entry.isDetonated()
                        ? "detonował w t=" + entry.getDetonatedSecond()
                        : "aktywny do t=" + entry.getTriggerSecond() + " (brak detonacji w horyzoncie)";
                System.out.println("- " + entry.getDelayedHitName()
                        + " | source=" + entry.getSourceSkillName()
                        + " | apply t=" + entry.getAppliedSecond()
                        + " | trigger t=" + entry.getTriggerSecond()
                        + " | " + status);
                if (entry.getBreakdown() != null) {
                    System.out.println("  raw=" + entry.getBreakdown().getRawDamage()
                            + " | final=" + entry.getBreakdown().getFinalDamage()
                            + " | rawCrit=" + entry.getBreakdown().getRawCriticalDamage()
                            + " | crit=" + entry.getBreakdown().getCriticalDamage());
                }
            }
        }

        System.out.println("== Reactive debug ==");
        if (result.getReactiveHitBreakdowns().isEmpty()) {
            System.out.println("Brak reactive damage.");
        } else {
            for (ReactiveHitBreakdown entry : result.getReactiveHitBreakdowns()) {
                System.out.println("- enemy hit t=" + entry.getTriggeredSecond()
                        + " | activeBlock=" + String.format(Locale.US, "%.2f%%", entry.getActiveBlockChance() * 100.0d)
                        + " | activeThornsBonus=" + String.format(Locale.US, "%.0f", entry.getActiveThornsBonus())
                        + " | resolve=" + entry.isResolveActive()
                        + " | resolveRemaining=" + entry.getResolveRemainingSeconds()
                        + " | punishment=" + entry.isPunishmentActive()
                        + " | thornsRaw=" + entry.getThornsRawDamage()
                        + " | thornsFinal=" + entry.getThornsFinalDamage()
                        + " | retributionExpectedRaw=" + entry.getRetributionExpectedRawDamage()
                        + " | retributionExpectedFinal=" + entry.getRetributionExpectedFinalDamage()
                        + " | reactiveFinal=" + entry.getReactiveFinalDamage());
            }
            System.out.println("Reactive total: " + result.getTotalReactiveDamage());
        }

        if (showTrace) {
            System.out.println();
            System.out.println("== Step trace ==");
            for (SimulationStepTrace step : result.getStepTrace()) {
                System.out.println("- t=" + step.getSecond()
                        + " | action=" + step.getActionName()
                        + " | direct=" + step.getDirectDamage()
                        + " | delayed=" + step.getDelayedDamage()
                        + " | reactive=" + step.getReactiveDamage()
                        + " | step=" + step.getTotalStepDamage()
                        + " | cumulative=" + step.getCumulativeDamage());
                System.out.println("  tickOrder=" + step.getTickOrderLabel());
                System.out.println("  reason=" + step.getSelectionReason());
                for (SkillBarStateTrace barState : step.getSkillBarStates()) {
                    System.out.println("  skill=" + barState.getSkillName()
                            + " | barIndex=" + barState.getBarIndex()
                            + " | rank=" + barState.getRank()
                            + " | legal=" + barState.isLegalActive()
                            + " | cooldown=" + barState.isOnCooldown()
                            + " | cooldownRemaining=" + barState.getCooldownRemainingSeconds()
                            + " | resource=" + barState.hasRequiredResource()
                            + " | neverUsed=" + barState.isNeverUsed()
                            + " | lastUsed=" + barState.getLastUsedSecond()
                            + " | selected=" + barState.isSelected());
                }
            }
        }
    }

    private static String buildLearnedSkillsLabel(java.util.Map<SkillId, SkillState> learnedSkills) {
        if (learnedSkills.isEmpty()) {
            return "Brak";
        }
        List<String> labels = new ArrayList<>();
        for (SkillId skillId : SkillId.values()) {
            SkillState state = learnedSkills.get(skillId);
            if (state == null || state.getRank() <= 0) {
                continue;
            }
            String label = PaladinSkillDefs.get(skillId).getName()
                    + " rank " + state.getRank()
                    + " | base=" + (state.isBaseUpgrade() ? "tak" : "nie")
                    + " | choice=" + PaladinSkillDefs.getChoiceDisplayName(skillId, state.getChoiceUpgrade());
            labels.add(label);
        }
        return String.join(" || ", labels);
    }

    private static String buildActionBarLabel(List<SkillId> actionBar) {
        if (actionBar.isEmpty()) {
            return "Pusty";
        }
        List<String> labels = new ArrayList<>();
        for (SkillId skillId : actionBar) {
            labels.add(PaladinSkillDefs.get(skillId).getName());
        }
        return String.join(" -> ", labels);
    }

    private static boolean resolveShowTrace(String[] args) {
        boolean showTrace = false;
        for (int index = 0; index < args.length; index++) {
            if ("--show-trace".equals(args[index]) && index + 1 < args.length) {
                showTrace = Boolean.parseBoolean(args[index + 1]);
            }
        }
        return showTrace;
    }
}

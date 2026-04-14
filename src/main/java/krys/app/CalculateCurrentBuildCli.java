package krys.app;

import krys.combat.DamageBreakdown;
import krys.combat.DamageComponentBreakdown;
import krys.combat.DamageEngine;
import krys.combat.DelayedHitBreakdown;
import krys.simulation.HeroBuildSnapshot;
import krys.simulation.ManualSimulationService;
import krys.simulation.SimulationResult;
import krys.skill.SkillId;
import krys.skill.SkillState;
import krys.skill.SkillUpgradeChoice;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Najprostszy możliwy flow użytkownika dla M2.
 * Użytkownik uruchamia „Policz aktualny build” z przykładowym snapshotem bieżącego buildu.
 */
public final class CalculateCurrentBuildCli {
    private CalculateCurrentBuildCli() {
    }

    public static void main(String[] args) {
        configureUtf8Output();
        CliArguments cliArguments = CliArguments.parse(args);
        SkillState skillState = new SkillState(
                cliArguments.skillId,
                cliArguments.rank,
                cliArguments.baseUpgrade,
                cliArguments.choiceUpgrade
        );
        HeroBuildSnapshot snapshot = SampleBuildFactory.createReferenceCurrentBuild(skillState);
        ManualSimulationService simulationService = new ManualSimulationService(new DamageEngine());
        SimulationResult result = simulationService.calculateCurrentBuild(snapshot, cliArguments.horizonSeconds);
        printResult(result, skillState);
    }

    private static void configureUtf8Output() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
    }

    private static void printResult(SimulationResult result, SkillState skillState) {
        System.out.println("=== Policz aktualny build ===");
        System.out.println("Skill: " + result.getSelectedSkillName());
        System.out.println("Rank: " + skillState.getRank());
        System.out.println("Bazowe rozszerzenie: " + (skillState.isBaseUpgrade() ? "tak" : "nie"));
        System.out.println("Dodatkowy modyfikator: " + skillState.getChoiceUpgrade().getDisplayName());
        System.out.println("Horyzont: " + result.getHorizonSeconds() + " s");
        System.out.println();
        System.out.println("Total damage: " + result.getTotalDamage());
        System.out.println("DPS: " + String.format(Locale.US, "%.4f", result.getDps()));
        System.out.println("Judgement aktywny na koniec: " + (result.isJudgementActiveAtEnd() ? "tak" : "nie"));
        System.out.println();

        DamageBreakdown singleHit = result.getSingleHitBreakdown();
        if (singleHit != null) {
            System.out.println("== Single hit debug ==");
            System.out.println("Raw hit: " + singleHit.getRawDamage());
            System.out.println("Single hit: " + singleHit.getFinalDamage());
            System.out.println("Raw crit hit: " + singleHit.getRawCriticalDamage());
            System.out.println("Critical hit: " + singleHit.getCriticalDamage());
            for (DamageComponentBreakdown component : singleHit.getComponents()) {
                System.out.println("- komponent: " + component.getName()
                        + " | %: " + component.getSkillDamagePercent()
                        + " | raw: " + component.getRawDamage()
                        + " | final: " + component.getFinalDamage());
            }
            System.out.println();
        }

        System.out.println("== Delayed hit debug ==");
        if (result.getDelayedHitBreakdowns().isEmpty()) {
            System.out.println("Brak delayed hitów.");
            return;
        }
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

    private static final class CliArguments {
        private final SkillId skillId;
        private final int rank;
        private final boolean baseUpgrade;
        private final SkillUpgradeChoice choiceUpgrade;
        private final int horizonSeconds;

        private CliArguments(SkillId skillId,
                             int rank,
                             boolean baseUpgrade,
                             SkillUpgradeChoice choiceUpgrade,
                             int horizonSeconds) {
            this.skillId = skillId;
            this.rank = rank;
            this.baseUpgrade = baseUpgrade;
            this.choiceUpgrade = choiceUpgrade;
            this.horizonSeconds = horizonSeconds;
        }

        private static CliArguments parse(String[] args) {
            SkillId skillId = SkillId.HOLY_BOLT;
            int rank = 5;
            boolean baseUpgrade = true;
            SkillUpgradeChoice choiceUpgrade = SkillUpgradeChoice.NONE;
            int horizonSeconds = 60;

            for (int index = 0; index < args.length; index++) {
                String arg = args[index];
                if ("--skill".equals(arg) && index + 1 < args.length) {
                    skillId = SkillId.valueOf(args[++index].toUpperCase(Locale.ROOT));
                } else if ("--rank".equals(arg) && index + 1 < args.length) {
                    rank = Integer.parseInt(args[++index]);
                } else if ("--base-upgrade".equals(arg) && index + 1 < args.length) {
                    baseUpgrade = Boolean.parseBoolean(args[++index]);
                } else if ("--choice".equals(arg) && index + 1 < args.length) {
                    choiceUpgrade = SkillUpgradeChoice.valueOf(args[++index].toUpperCase(Locale.ROOT));
                } else if ("--seconds".equals(arg) && index + 1 < args.length) {
                    horizonSeconds = Integer.parseInt(args[++index]);
                }
            }

            return new CliArguments(skillId, rank, baseUpgrade, choiceUpgrade, horizonSeconds);
        }
    }
}

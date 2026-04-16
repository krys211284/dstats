package krys.search;

import krys.combat.DamageEngine;
import krys.simulation.ManualSimulationService;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillId;
import krys.skill.SkillUpgradeChoice;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Prosty tekstowy entrypoint backendowego searcha M9. */
public final class SearchBuildCli {
    private SearchBuildCli() {
    }

    public static void main(String[] args) {
        configureUtf8Output();
        BuildSearchRequest request = BuildSearchCliRequestParser.parse(args);
        BuildSearchCalculationService calculationService = new BuildSearchCalculationService(
                new BuildSearchEvaluationService(new ManualSimulationService(new DamageEngine()))
        );
        BuildSearchResult result = calculationService.calculate(request);
        printResult(result);
    }

    private static void configureUtf8Output() {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));
    }

    static void printResult(BuildSearchResult result) {
        BuildSearchRequest request = result.getRequest();
        System.out.println("=== Znajdź najlepszy build ===");
        System.out.println("Przestrzeń wejścia:");
        System.out.println("Level values: " + joinIntegers(request.getLevelValues()));
        System.out.println("Weapon damage values: " + joinLongs(request.getWeaponDamageValues()));
        System.out.println("Strength values: " + joinDoubles(request.getStrengthValues()));
        System.out.println("Intelligence values: " + joinDoubles(request.getIntelligenceValues()));
        System.out.println("Thorns values: " + joinDoubles(request.getThornsValues()));
        System.out.println("Block chance values: " + joinPercents(request.getBlockChanceValues()));
        System.out.println("Retribution chance values: " + joinPercents(request.getRetributionChanceValues()));
        System.out.println("Bar sizes: " + joinIntegers(request.getActionBarSizes()));
        System.out.println("Horyzont: " + request.getHorizonSeconds() + " s");
        System.out.println("Top N: " + request.getTopResultsLimit());
        for (SkillId skillId : SkillId.values()) {
            BuildSearchSkillSpace skillSpace = request.getSkillSpace(skillId);
            System.out.println(PaladinSkillDefs.get(skillId).getName()
                    + " | ranks=" + joinIntegers(skillSpace.getRankValues())
                    + " | baseUpgrades=" + joinBooleans(skillSpace.getBaseUpgradeValues())
                    + " | choices=" + joinChoices(skillId, skillSpace.getChoiceUpgradeValues()));
        }
        System.out.println();
        System.out.println("Ocenieni kandydaci: " + result.getEvaluatedCandidateCount());
        System.out.println("Wyniki po normalizacji: " + result.getNormalizedResultCount());
        System.out.println();
        System.out.println("== Top wyniki po normalizacji ==");

        if (result.getTopResults().isEmpty()) {
            System.out.println("Brak legalnych kandydatów.");
            return;
        }

        for (BuildSearchRankedResult rankedResult : result.getTopResults()) {
            System.out.println("#" + rankedResult.getRank()
                    + " | total damage=" + rankedResult.getSimulationResult().getTotalDamage()
                    + " | DPS=" + String.format(Locale.US, "%.4f", rankedResult.getSimulationResult().getDps()));
            System.out.println("Build input: " + rankedResult.getCandidate().getInputProfileDescription());
            System.out.println("Action bar skills: " + rankedResult.getCandidate().getActionBarSkillsDescription());
            System.out.println("Action bar: " + rankedResult.getCandidate().getActionBarDescription());
            System.out.println();
        }
    }

    private static String joinIntegers(List<Integer> values) {
        List<String> labels = new ArrayList<>();
        for (Integer value : values) {
            labels.add(String.valueOf(value));
        }
        return String.join(", ", labels);
    }

    private static String joinLongs(List<Long> values) {
        List<String> labels = new ArrayList<>();
        for (Long value : values) {
            labels.add(String.valueOf(value));
        }
        return String.join(", ", labels);
    }

    private static String joinDoubles(List<Double> values) {
        List<String> labels = new ArrayList<>();
        for (Double value : values) {
            labels.add(String.format(Locale.US, "%.0f", value));
        }
        return String.join(", ", labels);
    }

    private static String joinPercents(List<Double> values) {
        List<String> labels = new ArrayList<>();
        for (Double value : values) {
            labels.add(String.format(Locale.US, "%.2f%%", value));
        }
        return String.join(", ", labels);
    }

    private static String joinBooleans(List<Boolean> values) {
        List<String> labels = new ArrayList<>();
        for (Boolean value : values) {
            labels.add(Boolean.TRUE.equals(value) ? "true" : "false");
        }
        return String.join(", ", labels);
    }

    private static String joinChoices(SkillId skillId, List<SkillUpgradeChoice> values) {
        List<String> labels = new ArrayList<>();
        for (SkillUpgradeChoice value : values) {
            labels.add(PaladinSkillDefs.getChoiceDisplayName(skillId, value));
        }
        return String.join(", ", labels);
    }
}

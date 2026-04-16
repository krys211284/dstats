package krys.search;

import krys.simulation.SimulationResult;
import krys.simulation.SimulationStepTrace;
import krys.simulation.SkillBarStateTrace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Normalizuje surowe wyniki searcha do czytelniejszej warstwy prezentacyjnej.
 * Nie zmienia generatora kandydatów ani samej oceny runtime; usuwa wyłącznie formalne duplikaty użytkowe.
 */
public final class BuildSearchPresentationNormalizer {

    public BuildSearchPresentationView normalize(List<BuildSearchEvaluation> sortedEvaluations, int topResultsLimit) {
        Map<String, BuildSearchEvaluation> firstEvaluationByPresentationKey = new LinkedHashMap<>();

        for (BuildSearchEvaluation evaluation : sortedEvaluations) {
            firstEvaluationByPresentationKey.putIfAbsent(buildPresentationKey(evaluation), evaluation);
        }

        List<BuildSearchRankedResult> topResults = new ArrayList<>();
        int normalizedRank = 1;
        for (BuildSearchEvaluation evaluation : firstEvaluationByPresentationKey.values()) {
            if (normalizedRank > topResultsLimit) {
                break;
            }
            topResults.add(new BuildSearchRankedResult(
                    normalizedRank,
                    evaluation.getCandidate(),
                    evaluation.getSnapshot(),
                    evaluation.getSimulationResult()
            ));
            normalizedRank++;
        }

        return new BuildSearchPresentationView(firstEvaluationByPresentationKey.size(), topResults);
    }

    private static String buildPresentationKey(BuildSearchEvaluation evaluation) {
        BuildSearchCandidate candidate = evaluation.getCandidate();
        return candidate.getInputProfileDescription()
                + " | bar=" + candidate.getActionBarDescription()
                + " | barSkills=" + candidate.getActionBarSkillsDescription()
                + " | runtime=" + buildRuntimeSignature(evaluation.getSimulationResult());
    }

    private static String buildRuntimeSignature(SimulationResult result) {
        StringBuilder signature = new StringBuilder();
        signature.append("total=").append(result.getTotalDamage())
                .append("|dps=").append(String.format(Locale.US, "%.8f", result.getDps()))
                .append("|reactive=").append(result.getTotalReactiveDamage())
                .append("|resolve=").append(result.isResolveActiveAtEnd())
                .append("|blockEnd=").append(String.format(Locale.US, "%.8f", result.getActiveBlockChanceAtEnd()))
                .append("|thornsBonusEnd=").append(String.format(Locale.US, "%.8f", result.getActiveThornsBonusAtEnd()))
                .append("|judgementEnd=").append(result.isJudgementActiveAtEnd());

        for (SimulationStepTrace step : result.getStepTrace()) {
            signature.append("|t=").append(step.getSecond())
                    .append(",action=").append(step.getActionName())
                    .append(",direct=").append(step.getDirectDamage())
                    .append(",delayed=").append(step.getDelayedDamage())
                    .append(",reactive=").append(step.getReactiveDamage())
                    .append(",step=").append(step.getTotalStepDamage())
                    .append(",cumulative=").append(step.getCumulativeDamage());

            for (SkillBarStateTrace barState : step.getSkillBarStates()) {
                signature.append(",skill=").append(barState.getSkillId())
                        .append(":rank=").append(barState.getRank())
                        .append(":cooldown=").append(barState.isOnCooldown())
                        .append(":cooldownRemaining=").append(barState.getCooldownRemainingSeconds())
                        .append(":selected=").append(barState.isSelected());
            }
        }

        return signature.toString();
    }

    public record BuildSearchPresentationView(int normalizedResultCount, List<BuildSearchRankedResult> topResults) {
    }
}

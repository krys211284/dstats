package krys.web;

import krys.paladin.PaladinSkillTreeType;
import krys.ranking.PaladinDamageRankingMetric;
import krys.ranking.PaladinSkillDamageVerificationStatus;

import java.util.List;

/** Model SSR opisowego rankingu umiejętności Paladyna. */
public final class PaladinSkillDamageRankingPageModel {
    private final PaladinSkillDamageRankingFilter filter;
    private final List<PaladinSkillDamageRankingRow> rows;
    private final int totalSkillCount;
    private final List<String> skillGroups;
    private final List<PaladinSkillDamageVerificationStatus> verificationStatuses;
    private final List<PaladinSkillTreeType> types;
    private final List<PaladinDamageRankingMetric> metrics;

    public PaladinSkillDamageRankingPageModel(PaladinSkillDamageRankingFilter filter,
                                              List<PaladinSkillDamageRankingRow> rows,
                                              int totalSkillCount,
                                              List<String> skillGroups,
                                              List<PaladinSkillDamageVerificationStatus> verificationStatuses,
                                              List<PaladinSkillTreeType> types,
                                              List<PaladinDamageRankingMetric> metrics) {
        this.filter = filter;
        this.rows = List.copyOf(rows);
        this.totalSkillCount = totalSkillCount;
        this.skillGroups = List.copyOf(skillGroups);
        this.verificationStatuses = List.copyOf(verificationStatuses);
        this.types = List.copyOf(types);
        this.metrics = List.copyOf(metrics);
    }

    public PaladinSkillDamageRankingFilter getFilter() {
        return filter;
    }

    public List<PaladinSkillDamageRankingRow> getRows() {
        return rows;
    }

    public int getTotalSkillCount() {
        return totalSkillCount;
    }

    public List<String> getSkillGroups() {
        return skillGroups;
    }

    public List<PaladinSkillDamageVerificationStatus> getVerificationStatuses() {
        return verificationStatuses;
    }

    public List<PaladinSkillTreeType> getTypes() {
        return types;
    }

    public List<PaladinDamageRankingMetric> getMetrics() {
        return metrics;
    }

    public long getCalculableCount() {
        return rows.stream()
                .filter(PaladinSkillDamageRankingRow::isDpsCalculable)
                .count();
    }
}

package krys.web;

import krys.paladin.PaladinSkillTreeType;
import krys.paladin.SkillTag;
import krys.ranking.CharacterSkillTreeRegistry;
import krys.ranking.PaladinDamageRankingMetric;
import krys.ranking.PaladinSkillDamageVerificationStatus;
import krys.ranking.PlayableClass;

import java.util.List;

/** Model SSR ogólnego rankingu obrażeń. */
public final class DamageRankingPageModel {
    private final DamageRankingFilter filter;
    private final CharacterSkillTreeRegistry registry;
    private final List<PlayableClass> supportedClasses;
    private final List<DamageRankingRow> rows;
    private final int totalSkillCount;
    private final List<String> skillGroups;
    private final List<PaladinSkillDamageVerificationStatus> verificationStatuses;
    private final List<PaladinSkillTreeType> types;
    private final List<SkillTag> tags;
    private final List<PaladinDamageRankingMetric> metrics;

    public DamageRankingPageModel(DamageRankingFilter filter,
                                  CharacterSkillTreeRegistry registry,
                                  List<PlayableClass> supportedClasses,
                                  List<DamageRankingRow> rows,
                                  int totalSkillCount,
                                  List<String> skillGroups,
                                  List<PaladinSkillDamageVerificationStatus> verificationStatuses,
                                  List<PaladinSkillTreeType> types,
                                  List<SkillTag> tags,
                                  List<PaladinDamageRankingMetric> metrics) {
        this.filter = filter;
        this.registry = registry;
        this.supportedClasses = List.copyOf(supportedClasses);
        this.rows = List.copyOf(rows);
        this.totalSkillCount = totalSkillCount;
        this.skillGroups = List.copyOf(skillGroups);
        this.verificationStatuses = List.copyOf(verificationStatuses);
        this.types = List.copyOf(types);
        this.tags = List.copyOf(tags);
        this.metrics = List.copyOf(metrics);
    }

    public DamageRankingFilter getFilter() {
        return filter;
    }

    public CharacterSkillTreeRegistry getRegistry() {
        return registry;
    }

    public List<PlayableClass> getSupportedClasses() {
        return supportedClasses;
    }

    public List<DamageRankingRow> getRows() {
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

    public List<SkillTag> getTags() {
        return tags;
    }

    public List<PaladinDamageRankingMetric> getMetrics() {
        return metrics;
    }

    public long getCalculableCount() {
        return rows.stream()
                .filter(DamageRankingRow::isDpsCalculable)
                .count();
    }
}

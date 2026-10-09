package krys.web;

import krys.ranking.CharacterSkillTreeRegistry;
import krys.ranking.DamageRankingMetric;
import krys.ranking.PlayableClass;
import krys.ranking.SkillDamageVerificationStatus;
import krys.skilltree.SkillTreeType;
import krys.skilltree.SourceSkillCategory;

import java.util.List;

/** Model SSR ogólnego rankingu obrażeń. */
public final class DamageRankingPageModel {
    private final DamageRankingFilter filter;
    private final CharacterSkillTreeRegistry registry;
    private final List<PlayableClass> supportedClasses;
    private final List<DamageRankingRow> rows;
    private final int totalSkillCount;
    private final List<String> skillGroups;
    private final List<SkillDamageVerificationStatus> verificationStatuses;
    private final List<SkillTreeType> types;
    private final List<SourceSkillCategory> sourceCategories;
    private final List<String> tags;
    private final List<DamageRankingMetric> metrics;

    public DamageRankingPageModel(DamageRankingFilter filter,
                                  CharacterSkillTreeRegistry registry,
                                  List<PlayableClass> supportedClasses,
                                  List<DamageRankingRow> rows,
                                  int totalSkillCount,
                                  List<String> skillGroups,
                                  List<SkillDamageVerificationStatus> verificationStatuses,
                                  List<SkillTreeType> types,
                                  List<SourceSkillCategory> sourceCategories,
                                  List<String> tags,
                                  List<DamageRankingMetric> metrics) {
        this.filter = filter;
        this.registry = registry;
        this.supportedClasses = List.copyOf(supportedClasses);
        this.rows = List.copyOf(rows);
        this.totalSkillCount = totalSkillCount;
        this.skillGroups = List.copyOf(skillGroups);
        this.verificationStatuses = List.copyOf(verificationStatuses);
        this.types = List.copyOf(types);
        this.sourceCategories = List.copyOf(sourceCategories);
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

    public List<SkillDamageVerificationStatus> getVerificationStatuses() {
        return verificationStatuses;
    }

    public List<SkillTreeType> getTypes() {
        return types;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<SourceSkillCategory> getSourceCategories() {
        return sourceCategories;
    }

    public List<DamageRankingMetric> getMetrics() {
        return metrics;
    }

    public long getCalculableCount() {
        return rows.stream()
                .filter(DamageRankingRow::isDpsCalculable)
                .count();
    }
}

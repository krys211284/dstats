package krys.web;

import krys.paladin.PaladinSkillTreeType;
import krys.ranking.PaladinDamageRankingMetric;
import krys.ranking.PaladinSkillDamageVerificationStatus;
import krys.ranking.PlayableClass;

import java.util.Map;

/** Filtry ogólnego widoku rankingu obrażeń. */
public final class DamageRankingFilter {
    private final PlayableClass character;
    private final String skillGroup;
    private final PaladinSkillDamageVerificationStatus verificationStatus;
    private final PaladinSkillTreeType type;
    private final PaladinDamageRankingMetric metric;

    public DamageRankingFilter(PlayableClass character,
                               String skillGroup,
                               PaladinSkillDamageVerificationStatus verificationStatus,
                               PaladinSkillTreeType type,
                               PaladinDamageRankingMetric metric) {
        this.character = character == null ? PlayableClass.defaultClass() : character;
        this.skillGroup = normalizeOptionalValue(skillGroup);
        this.verificationStatus = verificationStatus;
        this.type = type;
        this.metric = isVisibleRankingMetric(metric)
                ? metric
                : PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_TREE_MAX;
    }

    public static DamageRankingFilter fromQuery(Map<String, String> queryFields) {
        PlayableClass character = PlayableClass.fromQueryValueOrDefault(queryFields.get("character"));
        String group = queryFields.get("skillGroup");
        PaladinSkillDamageVerificationStatus status = parseEnum(
                PaladinSkillDamageVerificationStatus.class,
                queryFields.get("verificationStatus")
        );
        PaladinSkillTreeType type = parseEnum(
                PaladinSkillTreeType.class,
                queryFields.get("type")
        );
        PaladinDamageRankingMetric metric = parseEnum(
                PaladinDamageRankingMetric.class,
                queryFields.get("metric")
        );
        return new DamageRankingFilter(character, group, status, type, metric);
    }

    public PlayableClass getCharacter() {
        return character;
    }

    public String getSkillGroup() {
        return skillGroup;
    }

    public PaladinSkillDamageVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public PaladinSkillTreeType getType() {
        return type;
    }

    public PaladinDamageRankingMetric getMetric() {
        return metric;
    }

    public boolean hasSkillGroup() {
        return skillGroup != null;
    }

    public boolean hasVerificationStatus() {
        return verificationStatus != null;
    }

    public boolean hasType() {
        return type != null;
    }

    private static String normalizeOptionalValue(String value) {
        if (value == null || value.isBlank() || "ALL".equals(value)) {
            return null;
        }
        return value;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value) {
        String normalized = normalizeOptionalValue(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, normalized);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static boolean isVisibleRankingMetric(PaladinDamageRankingMetric metric) {
        return metric == PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_RANK_1
                || metric == PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_TREE_MAX;
    }
}

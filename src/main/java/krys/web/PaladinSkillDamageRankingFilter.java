package krys.web;

import krys.paladin.PaladinSkillTreeType;
import krys.ranking.PaladinDamageRankingMetric;
import krys.ranking.PaladinSkillDamageVerificationStatus;

import java.util.Map;

/** Filtry widoku opisowego rankingu obrażeń drzewa Paladyna. */
public final class PaladinSkillDamageRankingFilter {
    private final String skillGroup;
    private final PaladinSkillDamageVerificationStatus verificationStatus;
    private final PaladinSkillTreeType type;
    private final PaladinDamageRankingMetric metric;

    public PaladinSkillDamageRankingFilter(String skillGroup,
                                           PaladinSkillDamageVerificationStatus verificationStatus,
                                           PaladinSkillTreeType type,
                                           PaladinDamageRankingMetric metric) {
        this.skillGroup = normalizeOptionalValue(skillGroup);
        this.verificationStatus = verificationStatus;
        this.type = type;
        this.metric = metric == null ? PaladinDamageRankingMetric.SINGLE_TARGET_DPS : metric;
    }

    public static PaladinSkillDamageRankingFilter fromQuery(Map<String, String> queryFields) {
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
        return new PaladinSkillDamageRankingFilter(group, status, type, metric);
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
}

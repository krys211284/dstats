package krys.web;

import krys.paladin.PaladinSkillTreeType;
import krys.paladin.SkillTag;
import krys.ranking.PaladinDamageRankingMetric;
import krys.ranking.PaladinSkillDamageVerificationStatus;
import krys.ranking.PlayableClass;

import java.util.Locale;
import java.util.Map;

/** Filtry ogólnego widoku rankingu obrażeń. */
public final class DamageRankingFilter {
    private final PlayableClass character;
    private final String skillGroup;
    private final PaladinSkillDamageVerificationStatus verificationStatus;
    private final PaladinSkillTreeType type;
    private final PaladinDamageRankingMetric metric;
    private final SkillTag tag;
    private final FacetFilter hasDirectUpgradeDamage;
    private final FacetFilter hasNewDamageComponent;
    private final FacetFilter hasStatusDamageEnabler;
    private final FacetFilter hasResourceGeneration;
    private final FacetFilter hasCooldownOrCastSpeed;
    private final FacetFilter hasDefenseOrUtility;
    private final FacetFilter hasManualReviewUpgrade;
    private final String sort;
    private final SortDirection direction;

    public DamageRankingFilter(PlayableClass character,
                               String skillGroup,
                               PaladinSkillDamageVerificationStatus verificationStatus,
                               PaladinSkillTreeType type,
                               PaladinDamageRankingMetric metric) {
        this(character, skillGroup, verificationStatus, type, metric, null,
                FacetFilter.ALL, FacetFilter.ALL, FacetFilter.ALL, FacetFilter.ALL,
                FacetFilter.ALL, FacetFilter.ALL, FacetFilter.ALL,
                "baseDamageTreeMax", SortDirection.DESC);
    }

    public DamageRankingFilter(PlayableClass character,
                               String skillGroup,
                               PaladinSkillDamageVerificationStatus verificationStatus,
                               PaladinSkillTreeType type,
                               PaladinDamageRankingMetric metric,
                               SkillTag tag,
                               FacetFilter hasDirectUpgradeDamage,
                               FacetFilter hasNewDamageComponent,
                               FacetFilter hasStatusDamageEnabler,
                               FacetFilter hasResourceGeneration,
                               FacetFilter hasCooldownOrCastSpeed,
                               FacetFilter hasDefenseOrUtility,
                               FacetFilter hasManualReviewUpgrade,
                               String sort,
                               SortDirection direction) {
        this.character = character == null ? PlayableClass.defaultClass() : character;
        this.skillGroup = normalizeOptionalValue(skillGroup);
        this.verificationStatus = verificationStatus;
        this.type = type;
        this.metric = isVisibleRankingMetric(metric)
                ? metric
                : PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_TREE_MAX;
        this.tag = tag;
        this.hasDirectUpgradeDamage = hasDirectUpgradeDamage == null ? FacetFilter.ALL : hasDirectUpgradeDamage;
        this.hasNewDamageComponent = hasNewDamageComponent == null ? FacetFilter.ALL : hasNewDamageComponent;
        this.hasStatusDamageEnabler = hasStatusDamageEnabler == null ? FacetFilter.ALL : hasStatusDamageEnabler;
        this.hasResourceGeneration = hasResourceGeneration == null ? FacetFilter.ALL : hasResourceGeneration;
        this.hasCooldownOrCastSpeed = hasCooldownOrCastSpeed == null ? FacetFilter.ALL : hasCooldownOrCastSpeed;
        this.hasDefenseOrUtility = hasDefenseOrUtility == null ? FacetFilter.ALL : hasDefenseOrUtility;
        this.hasManualReviewUpgrade = hasManualReviewUpgrade == null ? FacetFilter.ALL : hasManualReviewUpgrade;
        this.sort = normalizeSort(sort);
        this.direction = direction == null ? SortDirection.DESC : direction;
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
        SkillTag tag = parseEnum(SkillTag.class, queryFields.get("tag"));
        String sort = normalizeOptionalValue(queryFields.get("sort"));
        SortDirection direction = parseEnum(SortDirection.class, queryFields.get("direction"));
        return new DamageRankingFilter(
                character,
                group,
                status,
                type,
                metric,
                tag,
                parseFacet(queryFields.get("hasDirectUpgradeDamage")),
                parseFacet(queryFields.get("hasNewDamageComponent")),
                parseFacet(queryFields.get("hasStatusDamageEnabler")),
                parseFacet(queryFields.get("hasResourceGeneration")),
                parseFacet(queryFields.get("hasCooldownOrCastSpeed")),
                parseFacet(queryFields.get("hasDefenseOrUtility")),
                parseFacet(queryFields.get("hasManualReviewUpgrade")),
                sort,
                direction
        );
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

    public SkillTag getTag() {
        return tag;
    }

    public FacetFilter getHasDirectUpgradeDamage() {
        return hasDirectUpgradeDamage;
    }

    public FacetFilter getHasNewDamageComponent() {
        return hasNewDamageComponent;
    }

    public FacetFilter getHasStatusDamageEnabler() {
        return hasStatusDamageEnabler;
    }

    public FacetFilter getHasResourceGeneration() {
        return hasResourceGeneration;
    }

    public FacetFilter getHasCooldownOrCastSpeed() {
        return hasCooldownOrCastSpeed;
    }

    public FacetFilter getHasDefenseOrUtility() {
        return hasDefenseOrUtility;
    }

    public FacetFilter getHasManualReviewUpgrade() {
        return hasManualReviewUpgrade;
    }

    public String getSort() {
        return sort;
    }

    public SortDirection getDirection() {
        return direction;
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

    public boolean hasTag() {
        return tag != null;
    }

    public boolean isFacetEnabled() {
        return hasDirectUpgradeDamage != FacetFilter.ALL
                || hasNewDamageComponent != FacetFilter.ALL
                || hasStatusDamageEnabler != FacetFilter.ALL
                || hasResourceGeneration != FacetFilter.ALL
                || hasCooldownOrCastSpeed != FacetFilter.ALL
                || hasDefenseOrUtility != FacetFilter.ALL
                || hasManualReviewUpgrade != FacetFilter.ALL;
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
            return Enum.valueOf(enumType, normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static FacetFilter parseFacet(String value) {
        FacetFilter parsed = parseEnum(FacetFilter.class, value);
        return parsed == null ? FacetFilter.ALL : parsed;
    }

    private static String normalizeSort(String value) {
        String normalized = normalizeOptionalValue(value);
        if (normalized == null) {
            return "baseDamageTreeMax";
        }
        return switch (normalized) {
            case "skillName",
                    "skillGroup",
                    "type",
                    "baseDamageRank1",
                    "baseDamageTreeMax",
                    "damageProfile",
                    "hasDirectUpgradeDamage",
                    "hasNewDamageComponent",
                    "hasStatusDamageEnabler",
                    "hasResourceGeneration",
                    "hasCooldownOrCastSpeed",
                    "hasDefenseOrUtility",
                    "hasManualReviewUpgrade",
                    "tags" -> normalized;
            default -> "baseDamageTreeMax";
        };
    }

    private static boolean isVisibleRankingMetric(PaladinDamageRankingMetric metric) {
        return metric == PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_RANK_1
                || metric == PaladinDamageRankingMetric.BASE_DAMAGE_PERCENT_TREE_MAX;
    }

    public enum FacetFilter {
        ALL,
        YES,
        NO
    }

    public enum SortDirection {
        ASC,
        DESC
    }
}

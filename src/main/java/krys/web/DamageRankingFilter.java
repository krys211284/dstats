package krys.web;

import krys.ranking.CharacterSkillTreeRegistry;
import krys.ranking.DamageRankingMetric;
import krys.ranking.PlayableClass;
import krys.ranking.SkillDamageVerificationStatus;
import krys.skilltree.SkillTreeType;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Filtry ogólnego widoku rankingu obrażeń. */
public final class DamageRankingFilter {
    private final PlayableClass character;
    private final String skillGroup;
    private final SkillDamageVerificationStatus verificationStatus;
    private final SkillTreeType type;
    private final DamageRankingMetric metric;
    private final String q;
    private final String sourceCategory;
    private final String tag;
    private final FacetFilter hasDirectUpgradeDamage;
    private final FacetFilter hasNewDamageComponent;
    private final FacetFilter hasStatusDamageEnabler;
    private final FacetFilter hasResourceCost;
    private final FacetFilter hasResourceGeneration;
    private final FacetFilter hasCooldownOrCastSpeed;
    private final FacetFilter hasDefenseOrUtility;
    private final FacetFilter hasManualReviewUpgrade;
    private final String sort;
    private final SortDirection direction;

    public DamageRankingFilter(PlayableClass character,
                               String skillGroup,
                               SkillDamageVerificationStatus verificationStatus,
                               SkillTreeType type,
                               DamageRankingMetric metric) {
        this(character, skillGroup, verificationStatus, type, metric, null, null,
                null,
                FacetFilter.ALL, FacetFilter.ALL, FacetFilter.ALL, FacetFilter.ALL,
                FacetFilter.ALL, FacetFilter.ALL, FacetFilter.ALL, FacetFilter.ALL,
                "baseDamageTreeMax", SortDirection.DESC);
    }

    public DamageRankingFilter(PlayableClass character,
                               String skillGroup,
                               SkillDamageVerificationStatus verificationStatus,
                               SkillTreeType type,
                               DamageRankingMetric metric,
                               String sourceCategory,
                               String tag,
                               String q,
                               FacetFilter hasDirectUpgradeDamage,
                               FacetFilter hasNewDamageComponent,
                               FacetFilter hasStatusDamageEnabler,
                               FacetFilter hasResourceCost,
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
                : DamageRankingMetric.BASE_DAMAGE_PERCENT_TREE_MAX;
        this.q = normalizeOptionalValue(q);
        this.sourceCategory = sourceCategory;
        this.tag = tag;
        this.hasDirectUpgradeDamage = hasDirectUpgradeDamage == null ? FacetFilter.ALL : hasDirectUpgradeDamage;
        this.hasNewDamageComponent = hasNewDamageComponent == null ? FacetFilter.ALL : hasNewDamageComponent;
        this.hasStatusDamageEnabler = hasStatusDamageEnabler == null ? FacetFilter.ALL : hasStatusDamageEnabler;
        this.hasResourceCost = hasResourceCost == null ? FacetFilter.ALL : hasResourceCost;
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
        SkillDamageVerificationStatus status = parseEnum(
                SkillDamageVerificationStatus.class,
                queryFields.get("verificationStatus")
        );
        SkillTreeType type = parseEnum(
                SkillTreeType.class,
                queryFields.get("type")
        );
        DamageRankingMetric metric = parseEnum(
                DamageRankingMetric.class,
                queryFields.get("metric")
        );
        String tag = normalizeFacetId(queryFields.get("tag"));
        String sourceCategory = normalizeFacetId(queryFields.get("sourceCategory"));
        String sort = normalizeOptionalValue(queryFields.get("sort"));
        SortDirection direction = parseEnum(SortDirection.class, queryFields.get("direction"));
        String q = normalizeOptionalValue(queryFields.get("q"));
        return new DamageRankingFilter(
                character,
                group,
                status,
                type,
                metric,
                sourceCategory,
                tag,
                q,
                parseFacet(queryFields.get("hasDirectUpgradeDamage")),
                parseFacet(queryFields.get("hasNewDamageComponent")),
                parseFacet(queryFields.get("hasStatusDamageEnabler")),
                parseFacet(queryFields.getOrDefault("hasResourceCost", queryFields.get("hasFaithCost"))),
                parseFacet(queryFields.get("hasResourceGeneration")),
                parseFacet(queryFields.get("hasCooldownOrCastSpeed")),
                parseFacet(queryFields.get("hasDefenseOrUtility")),
                parseFacet(queryFields.get("hasManualReviewUpgrade")),
                sort,
                direction
        );
    }

    public static DamageRankingFilter fromQuery(Map<String, String> queryFields, CharacterSkillTreeRegistry registry) {
        Map<String, String> fields = new HashMap<>(queryFields);
        if (!registry.supportsSourceCategory(fields.get("sourceCategory"))) {
            fields.remove("sourceCategory");
        }
        if (!registry.supportsMechanicTag(fields.get("tag"))) {
            fields.remove("tag");
        }
        return fromQuery(fields);
    }

    public PlayableClass getCharacter() {
        return character;
    }

    public String getSkillGroup() {
        return skillGroup;
    }

    public SkillDamageVerificationStatus getVerificationStatus() {
        return verificationStatus;
    }

    public SkillTreeType getType() {
        return type;
    }

    public DamageRankingMetric getMetric() {
        return metric;
    }

    public String getQ() {
        return q;
    }

    public String getTag() {
        return tag;
    }

    public String getSourceCategory() {
        return sourceCategory;
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

    public FacetFilter getHasResourceCost() {
        return hasResourceCost;
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

    public boolean matchesSort(String key) {
        return neutralSortKey(sort).equals(neutralSortKey(key));
    }

    private static String neutralSortKey(String key) {
        return switch (key) {
            case "faithCost" -> "resourceCost";
            case "faithGeneratedBase" -> "resourceGeneratedBase";
            case "faithGeneratedMaxKnown" -> "resourceGeneratedMaxKnown";
            default -> key;
        };
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

    public boolean hasSourceCategory() {
        return sourceCategory != null;
    }

    public boolean hasSearchQuery() {
        return q != null;
    }

    public boolean isFacetEnabled() {
        return hasDirectUpgradeDamage != FacetFilter.ALL
                || hasNewDamageComponent != FacetFilter.ALL
                || hasStatusDamageEnabler != FacetFilter.ALL
                || hasResourceCost != FacetFilter.ALL
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

    private static String normalizeFacetId(String value) {
        String normalized = normalizeOptionalValue(value);
        return normalized == null ? null : normalized.trim();
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
                    "sourceCategories",
                    "skillGroup",
                    "type",
                    "baseDamageRank1",
                    "baseDamageTreeMax",
                    "faithCost",
                    "resourceCost",
                    "faithGeneratedBase",
                    "resourceGeneratedBase",
                    "faithGeneratedMaxKnown",
                    "resourceGeneratedMaxKnown",
                    "maxDamageMultiplierPercent",
                    "maxDamageBonusPercent",
                    "maxExtraHitOrComponentPercent",
                    "maxDamageOverTimePercent",
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

    private static boolean isVisibleRankingMetric(DamageRankingMetric metric) {
        return metric == DamageRankingMetric.BASE_DAMAGE_PERCENT_RANK_1
                || metric == DamageRankingMetric.BASE_DAMAGE_PERCENT_TREE_MAX;
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

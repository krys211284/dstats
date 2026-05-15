package krys.paladin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** Bazowy wpis umiejętności w rejestrze drzewa Paladyna. */
public final class PaladinTreeSkill {
    private final String skillId;
    private final String skillName;
    private final String sourcePdf;
    private final String skillGroup;
    private final Integer baseDamagePercentAtRank1;
    private final Integer baseDamagePercentAtTreeMaxRank;
    private final DamagePercentRankTable baseDamagePercentRanks;
    private final DamagePercentComponentRankTable componentDamagePercentRanks;
    private final PaladinSkillTreeType type;
    private final PaladinSkillTreeStatus status;
    private final List<PaladinSkillUpgradeGroup> upgradeGroups;
    private final String notes;
    private final Integer faithCost;
    private final Integer faithGenerationBase;
    private final Integer faithGenerationBonusKnown;
    private final Integer luckyHitPercent;
    private final List<UpgradeDamageModifier> baseDescriptionModifiers;

    public PaladinTreeSkill(String skillId,
                            String skillName,
                            String sourcePdf,
                            String skillGroup,
                            PaladinSkillTreeType type,
                            PaladinSkillTreeStatus status,
                            List<PaladinSkillUpgradeGroup> upgradeGroups,
                            String notes) {
        this(skillId, skillName, sourcePdf, skillGroup, null, null, type, status, upgradeGroups, notes);
    }

    public PaladinTreeSkill(String skillId,
                            String skillName,
                            String sourcePdf,
                            String skillGroup,
                            Integer baseDamagePercentAtRank1,
                            Integer baseDamagePercentAtTreeMaxRank,
                            PaladinSkillTreeType type,
                            PaladinSkillTreeStatus status,
                            List<PaladinSkillUpgradeGroup> upgradeGroups,
                            String notes) {
        this(skillId, skillName, sourcePdf, skillGroup, baseDamagePercentAtRank1, baseDamagePercentAtTreeMaxRank,
                DamagePercentRankTable.empty(), DamagePercentComponentRankTable.empty(), type, status, upgradeGroups, notes);
    }

    public PaladinTreeSkill(String skillId,
                            String skillName,
                            String sourcePdf,
                            String skillGroup,
                            DamagePercentRankTable baseDamagePercentRanks,
                            PaladinSkillTreeType type,
                            PaladinSkillTreeStatus status,
                            List<PaladinSkillUpgradeGroup> upgradeGroups,
                            String notes) {
        this(skillId, skillName, sourcePdf, skillGroup, null, null, baseDamagePercentRanks, type, status, upgradeGroups, notes);
    }

    public PaladinTreeSkill(String skillId,
                            String skillName,
                            String sourcePdf,
                            String skillGroup,
                            DamagePercentComponentRankTable componentDamagePercentRanks,
                            PaladinSkillTreeType type,
                            PaladinSkillTreeStatus status,
                            List<PaladinSkillUpgradeGroup> upgradeGroups,
                            String notes) {
        this(skillId, skillName, sourcePdf, skillGroup, null, null, DamagePercentRankTable.empty(),
                componentDamagePercentRanks, type, status, upgradeGroups, notes);
    }

    public PaladinTreeSkill(String skillId,
                            String skillName,
                            String sourcePdf,
                            String skillGroup,
                            Integer baseDamagePercentAtRank1,
                            Integer baseDamagePercentAtTreeMaxRank,
                            DamagePercentRankTable baseDamagePercentRanks,
                            PaladinSkillTreeType type,
                            PaladinSkillTreeStatus status,
                            List<PaladinSkillUpgradeGroup> upgradeGroups,
                            String notes) {
        this(skillId, skillName, sourcePdf, skillGroup, baseDamagePercentAtRank1, baseDamagePercentAtTreeMaxRank,
                baseDamagePercentRanks, DamagePercentComponentRankTable.empty(), type, status, upgradeGroups, notes);
    }

    public PaladinTreeSkill(String skillId,
                            String skillName,
                            String sourcePdf,
                            String skillGroup,
                            Integer baseDamagePercentAtRank1,
                            Integer baseDamagePercentAtTreeMaxRank,
                            DamagePercentRankTable baseDamagePercentRanks,
                            DamagePercentComponentRankTable componentDamagePercentRanks,
                            PaladinSkillTreeType type,
                            PaladinSkillTreeStatus status,
                            List<PaladinSkillUpgradeGroup> upgradeGroups,
                            String notes) {
        this(skillId, skillName, sourcePdf, skillGroup, baseDamagePercentAtRank1, baseDamagePercentAtTreeMaxRank,
                baseDamagePercentRanks, componentDamagePercentRanks, type, status, upgradeGroups, notes,
                null, null, null, null, List.of());
    }

    public PaladinTreeSkill(String skillId,
                            String skillName,
                            String sourcePdf,
                            String skillGroup,
                            Integer baseDamagePercentAtRank1,
                            Integer baseDamagePercentAtTreeMaxRank,
                            DamagePercentRankTable baseDamagePercentRanks,
                            DamagePercentComponentRankTable componentDamagePercentRanks,
                            PaladinSkillTreeType type,
                            PaladinSkillTreeStatus status,
                            List<PaladinSkillUpgradeGroup> upgradeGroups,
                            String notes,
                            Integer faithCost,
                            Integer faithGenerationBase,
                            Integer faithGenerationBonusKnown) {
        this(skillId, skillName, sourcePdf, skillGroup, baseDamagePercentAtRank1, baseDamagePercentAtTreeMaxRank,
                baseDamagePercentRanks, componentDamagePercentRanks, type, status, upgradeGroups, notes,
                faithCost, faithGenerationBase, faithGenerationBonusKnown, null, List.of());
    }

    public PaladinTreeSkill(String skillId,
                            String skillName,
                            String sourcePdf,
                            String skillGroup,
                            Integer baseDamagePercentAtRank1,
                            Integer baseDamagePercentAtTreeMaxRank,
                            DamagePercentRankTable baseDamagePercentRanks,
                            DamagePercentComponentRankTable componentDamagePercentRanks,
                            PaladinSkillTreeType type,
                            PaladinSkillTreeStatus status,
                            List<PaladinSkillUpgradeGroup> upgradeGroups,
                            String notes,
                            Integer faithCost,
                            Integer faithGenerationBase,
                            Integer faithGenerationBonusKnown,
                            Integer luckyHitPercent,
                            List<UpgradeDamageModifier> baseDescriptionModifiers) {
        this.skillId = requireText(skillId, "skillId");
        this.skillName = requireText(skillName, "skillName");
        this.sourcePdf = requireText(sourcePdf, "sourcePdf");
        this.skillGroup = requireText(skillGroup, "skillGroup");
        this.baseDamagePercentAtRank1 = baseDamagePercentAtRank1;
        this.baseDamagePercentAtTreeMaxRank = baseDamagePercentAtTreeMaxRank;
        this.baseDamagePercentRanks = Objects.requireNonNull(baseDamagePercentRanks, "baseDamagePercentRanks");
        this.componentDamagePercentRanks = Objects.requireNonNull(componentDamagePercentRanks, "componentDamagePercentRanks");
        this.type = Objects.requireNonNull(type, "type");
        this.status = Objects.requireNonNull(status, "status");
        this.upgradeGroups = Collections.unmodifiableList(new ArrayList<>(upgradeGroups));
        this.notes = requireText(notes, "notes");
        this.faithCost = faithCost;
        this.faithGenerationBase = faithGenerationBase;
        this.faithGenerationBonusKnown = faithGenerationBonusKnown;
        this.luckyHitPercent = luckyHitPercent;
        this.baseDescriptionModifiers = List.copyOf(Objects.requireNonNull(baseDescriptionModifiers, "baseDescriptionModifiers"));
    }

    public String getSkillId() {
        return skillId;
    }

    public String getSkillName() {
        return skillName;
    }

    public String getSourcePdf() {
        return sourcePdf;
    }

    public String getSkillGroup() {
        return skillGroup;
    }

    public Integer getBaseDamagePercentAtRank1() {
        if (!baseDamagePercentRanks.isEmpty()) {
            return baseDamagePercentRanks.damagePercentAtRank1();
        }
        return baseDamagePercentAtRank1;
    }

    public Integer getBaseDamagePercentAtTreeMaxRank() {
        if (!baseDamagePercentRanks.isEmpty()) {
            return baseDamagePercentRanks.damagePercentAtTreeMaxRank(DamagePercentRankTable.MAX_RANK);
        }
        return baseDamagePercentAtTreeMaxRank;
    }

    public DamagePercentRankTable getBaseDamagePercentRanks() {
        return baseDamagePercentRanks;
    }

    public DamagePercentComponentRankTable getComponentDamagePercentRanks() {
        return componentDamagePercentRanks;
    }

    public Integer damagePercentAtRank(int rank) {
        return baseDamagePercentRanks.damagePercentAtRank(rank);
    }

    public Integer damagePercentAtRank1() {
        return getBaseDamagePercentAtRank1();
    }

    public Integer damagePercentAtTreeMaxRank(int treeMaxRank) {
        if (!baseDamagePercentRanks.isEmpty()) {
            return baseDamagePercentRanks.damagePercentAtTreeMaxRank(treeMaxRank);
        }
        if (treeMaxRank == DamagePercentRankTable.MAX_RANK) {
            return baseDamagePercentAtTreeMaxRank;
        }
        return null;
    }

    public PaladinSkillTreeType getType() {
        return type;
    }

    public PaladinSkillTreeStatus getStatus() {
        return status;
    }

    public List<PaladinSkillUpgradeGroup> getUpgradeGroups() {
        return upgradeGroups;
    }

    public List<UpgradeDamageImpact> getUpgradeDamageImpacts() {
        return upgradeGroups.stream()
                .flatMap(group -> group.getUpgrades().stream()
                        .map(upgrade -> UpgradeDamageImpact.fromUpgrade(group.getId(), upgrade)))
                .toList();
    }

    public List<UpgradeDamageModifier> getUpgradeDamageModifiers() {
        List<UpgradeDamageModifier> modifiers = new ArrayList<>(baseDescriptionModifiers);
        modifiers.addAll(upgradeGroups.stream()
                .flatMap(group -> group.getUpgrades().stream()
                        .map(upgrade -> UpgradeDamageModifier.fromUpgrade(skillId, group.getId(), upgrade)))
                .toList());
        return List.copyOf(modifiers);
    }

    public Set<SkillTag> getTags() {
        EnumSet<SkillTag> tags = EnumSet.noneOf(SkillTag.class);
        addGroupTags(tags);
        addTypeTags(tags);
        addNameAndIdTags(tags);
        addUpgradeTags(tags);
        if (status == PaladinSkillTreeStatus.NEEDS_VERIFICATION || status == PaladinSkillTreeStatus.UNSUPPORTED) {
            tags.add(SkillTag.NEEDS_MANUAL_REVIEW);
        }
        if (tags.isEmpty()) {
            tags.add(SkillTag.UTILITY);
        }
        return Set.copyOf(tags);
    }

    public Set<SkillCategory> getSkillCategories() {
        return Set.copyOf(sourceSkillCategories());
    }

    public String getSkillCategoriesDisplay() {
        return getSkillCategories().stream()
                .sorted(java.util.Comparator.comparingInt(SkillCategory::getDisplayOrder))
                .map(SkillCategory::getDisplayName)
                .collect(Collectors.joining(", "));
    }

    public boolean hasSkillCategory(SkillCategory category) {
        return getSkillCategories().contains(Objects.requireNonNull(category, "category"));
    }

    public String getNotes() {
        return notes;
    }

    public Integer getFaithCost() {
        return faithCost;
    }

    public Integer getFaithGenerationBase() {
        return faithGenerationBase;
    }

    public Integer getFaithGenerationBonusKnown() {
        return faithGenerationBonusKnown;
    }

    public Integer getLuckyHitPercent() {
        return luckyHitPercent;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + fieldName + " nie może być puste.");
        }
        return value;
    }

    private void addGroupTags(EnumSet<SkillTag> tags) {
        switch (skillGroup) {
            case "basic" -> tags.add(SkillTag.BASIC);
            case "core" -> tags.add(SkillTag.CORE);
            case "aura" -> tags.add(SkillTag.AURA);
            case "odwaga" -> tags.add(SkillTag.DEFENSIVE);
            case "moce_specjalne" -> tags.add(SkillTag.SPECIAL);
            default -> {
            }
        }
    }

    private Set<SkillCategory> sourceSkillCategories() {
        EnumSet<SkillCategory> categories = EnumSet.noneOf(SkillCategory.class);
        switch (skillId) {
            case "wymach" -> {
                categories.add(SkillCategory.PODSTAWOWE);
                categories.add(SkillCategory.ADEPT);
            }
            case "swiety_pocisk" -> {
                categories.add(SkillCategory.PODSTAWOWE);
                categories.add(SkillCategory.SEDZIA);
            }
            case "starcie" -> {
                categories.add(SkillCategory.PODSTAWOWE);
                categories.add(SkillCategory.MOLOCH);
            }
            case "natarcie" -> {
                categories.add(SkillCategory.PODSTAWOWE);
                categories.add(SkillCategory.MOBILNOSC);
                categories.add(SkillCategory.ZELOTY);
            }
            case "forteca" -> {
                categories.add(SkillCategory.SPECJALNA);
                categories.add(SkillCategory.DEFENSYWNA);
                categories.add(SkillCategory.MOLOCH);
            }
            default -> categories.add(SkillCategory.NEEDS_MANUAL_REVIEW);
        }
        return Set.copyOf(categories);
    }

    private void addTypeTags(EnumSet<SkillTag> tags) {
        switch (type) {
            case DAMAGE -> tags.add(SkillTag.DAMAGE);
            case SUPPORT -> tags.add(SkillTag.SUPPORT);
            case DEFENSIVE -> tags.add(SkillTag.DEFENSIVE);
            case MOBILITY -> tags.add(SkillTag.MOBILITY);
            case SPECIAL -> tags.add(SkillTag.SPECIAL);
            case NON_DAMAGE, UNCLASSIFIED -> tags.add(SkillTag.UTILITY);
        }
    }

    private void addNameAndIdTags(EnumSet<SkillTag> tags) {
        if (containsAny(skillId, skillName, "swiet", "święt", "blogoslaw", "błogosław", "bosk", "niebios", "konsekr", "wymach")) {
            tags.add(SkillTag.HOLY_DAMAGE);
        }
        if (containsAny(skillId, skillName, "tarcza", "egida", "shield", "aegis")) {
            tags.add(SkillTag.SHIELD);
        }
        if (containsAny(skillId, skillName, "pocisk", "lanca", "oszczep", "wlocznia", "włócznia", "bolt", "lance", "spear", "projectile")) {
            tags.add(SkillTag.PROJECTILE);
            tags.add(SkillTag.RANGED);
        }
        if (containsAny(skillId, skillName, "aura", "konsekracja", "oczyszczenie", "forteca", "furia", "spadajaca", "spadająca")) {
            tags.add(SkillTag.AREA);
        }
        if (containsAny(skillId, skillName, "wymach", "starcie", "zapal", "uderzenie", "bash", "clash", "zeal")) {
            tags.add(SkillTag.MELEE);
        }
        if (containsAny(skillId, skillName, "leczenie", "laska", "łaska", "healing", "konsekracja")) {
            tags.add(SkillTag.HEALING);
        }
        if (!componentDamagePercentRanks.isEmpty()) {
            tags.add(SkillTag.MULTI_HIT);
        }
    }

    private void addUpgradeTags(EnumSet<SkillTag> tags) {
        for (UpgradeDamageModifier modifier : getUpgradeDamageModifiers()) {
            if (modifier.createsNewDamageComponent()) {
                tags.add(SkillTag.MULTI_HIT);
            }
            switch (modifier.getType()) {
                case STATUS_DAMAGE_ENABLER -> {
                    tags.add(SkillTag.EXPOSED);
                    tags.add(SkillTag.VULNERABLE);
                }
                case CAST_SPEED_OR_COOLDOWN -> {
                    String name = modifier.getUpgradeName().toLowerCase();
                    if (containsAny("", name, "szybko", "speed")) {
                        tags.add(SkillTag.CAST_SPEED);
                    }
                    if (containsAny("", name, "odnowienia", "cooldown", "czas działania", "czas_dzialania")) {
                        tags.add(SkillTag.COOLDOWN);
                    }
                }
                case RESOURCE_OR_COST -> {
                    String name = modifier.getUpgradeName().toLowerCase();
                    if (containsAny("", name, "generowanie", "faith", "wiary", "zasob", "zasób")) {
                        tags.add(SkillTag.FAITH_GENERATION);
                    }
                    if (containsAny("", name, "koszt")) {
                        tags.add(SkillTag.FAITH_COST);
                    }
                }
                case DEFENSE_OR_UTILITY -> tags.add(SkillTag.UTILITY);
                case THORNS_DAMAGE_MODIFIER, NEEDS_MANUAL_REVIEW -> tags.add(SkillTag.NEEDS_MANUAL_REVIEW);
                default -> {
                }
            }
            String upgradeName = modifier.getUpgradeName().toLowerCase();
            if (containsAny("", upgradeName, "spowolnienie", "osłabienie", "oslabienie")) {
                tags.add(SkillTag.CROWD_CONTROL);
            }
            if (containsAny("", upgradeName, "leczenie", "laska", "łaska")) {
                tags.add(SkillTag.HEALING);
            }
            if (containsAny("", upgradeName, "bariera", "bastion", "tarcza wiary")) {
                tags.add(SkillTag.BARRIER);
            }
            if (containsAny("", upgradeName, "redukcja", "pancerz", "blok", "krzepkość", "krzepkosc")) {
                tags.add(SkillTag.DAMAGE_REDUCTION);
            }
        }
    }

    private static boolean containsAny(String id, String name, String... fragments) {
        String normalizedId = id.toLowerCase();
        String normalizedName = name.toLowerCase();
        for (String fragment : fragments) {
            if (normalizedId.contains(fragment) || normalizedName.contains(fragment)) {
                return true;
            }
        }
        return false;
    }
}

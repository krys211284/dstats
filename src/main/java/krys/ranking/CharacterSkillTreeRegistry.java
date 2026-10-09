package krys.ranking;

import krys.skilltree.CharacterTreeSkill;
import krys.skilltree.SkillResourceType;
import krys.skilltree.SourceSkillCategory;

import java.util.function.Function;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Rejestr drzewa umiejętności podpięty do konkretnej klasy postaci. */
public final class CharacterSkillTreeRegistry {
    private final PlayableClass playableClass;
    private final String registryName;
    private final List<CharacterTreeSkill> skills;
    private final Map<String, CharacterTreeSkill> skillsById;
    private final SkillResourceType resourceType;
    private final Map<String, String> treeGroupNames;
    private final Set<String> sourceCategoryIds;
    private final Set<String> mechanicTagIds;
    private final String searchPlaceholder;

    public CharacterSkillTreeRegistry(PlayableClass playableClass,
                                      String registryName,
                                      List<CharacterTreeSkill> skills) {
        this(playableClass, registryName, skills, SkillResourceType.UNSPECIFIED, Map.of());
    }

    public CharacterSkillTreeRegistry(PlayableClass playableClass, String registryName,
                                      List<CharacterTreeSkill> skills, SkillResourceType resourceType,
                                      Map<String, String> treeGroupNames) {
        this(playableClass, registryName, skills, resourceType, treeGroupNames,
                skills.stream().flatMap(skill -> skill.getSkillCategories().stream()).map(SourceSkillCategory::getId)
                        .collect(Collectors.toSet()),
                skills.stream().flatMap(skill -> skill.getTags().stream()).collect(Collectors.toSet()));
    }

    public CharacterSkillTreeRegistry(PlayableClass playableClass, String registryName,
                                      List<CharacterTreeSkill> skills, SkillResourceType resourceType,
                                      Map<String, String> treeGroupNames,
                                      Set<String> sourceCategoryIds, Set<String> mechanicTagIds) {
        this(playableClass, registryName, skills, resourceType, treeGroupNames, sourceCategoryIds, mechanicTagIds,
                "Nazwa, kategoria, modyfikator");
    }

    public CharacterSkillTreeRegistry(PlayableClass playableClass, String registryName,
                                      List<CharacterTreeSkill> skills, SkillResourceType resourceType,
                                      Map<String, String> treeGroupNames,
                                      Set<String> sourceCategoryIds, Set<String> mechanicTagIds,
                                      String searchPlaceholder) {
        this.playableClass = Objects.requireNonNull(playableClass, "playableClass");
        this.registryName = requireText(registryName, "registryName");
        this.skills = List.copyOf(skills);
        this.skillsById = this.skills.stream()
                .collect(Collectors.toUnmodifiableMap(CharacterTreeSkill::getSkillId, Function.identity()));
        this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
        this.treeGroupNames = Map.copyOf(treeGroupNames);
        this.sourceCategoryIds = Set.copyOf(sourceCategoryIds);
        this.mechanicTagIds = Set.copyOf(mechanicTagIds);
        this.searchPlaceholder = requireText(searchPlaceholder, "searchPlaceholder");
    }

    public PlayableClass getPlayableClass() {
        return playableClass;
    }

    public String getRegistryName() {
        return registryName;
    }

    public List<CharacterTreeSkill> allSkills() {
        return skills;
    }

    public Optional<CharacterTreeSkill> findSkill(String skillId) {
        return Optional.ofNullable(skillsById.get(skillId));
    }

    public CharacterTreeSkill requireSkill(String skillId) {
        return findSkill(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Nieznany skill w rejestrze " + registryName + ": " + skillId));
    }

    public SkillResourceType getResourceType() {
        return resourceType;
    }

    public String treeGroupDisplayName(String skillGroup) {
        return treeGroupNames.getOrDefault(skillGroup, skillGroup);
    }

    public boolean supportsSourceCategory(String id) {
        return sourceCategoryIds.stream().anyMatch(value -> value.equalsIgnoreCase(id));
    }

    public boolean supportsMechanicTag(String id) {
        return mechanicTagIds.stream().anyMatch(value -> value.equalsIgnoreCase(id));
    }

    public String getSearchPlaceholder() {
        return searchPlaceholder;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + fieldName + " nie może być puste.");
        }
        return value;
    }
}

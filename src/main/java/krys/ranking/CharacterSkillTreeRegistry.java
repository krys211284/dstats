package krys.ranking;

import krys.paladin.PaladinTreeSkill;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Rejestr drzewa umiejętności podpięty do konkretnej klasy postaci. */
public final class CharacterSkillTreeRegistry {
    private final PlayableClass playableClass;
    private final String registryName;
    private final List<PaladinTreeSkill> skills;
    private final Map<String, PaladinTreeSkill> skillsById;

    public CharacterSkillTreeRegistry(PlayableClass playableClass,
                                      String registryName,
                                      List<PaladinTreeSkill> skills) {
        this.playableClass = Objects.requireNonNull(playableClass, "playableClass");
        this.registryName = requireText(registryName, "registryName");
        this.skills = List.copyOf(skills);
        this.skillsById = this.skills.stream()
                .collect(Collectors.toUnmodifiableMap(PaladinTreeSkill::getSkillId, Function.identity()));
    }

    public PlayableClass getPlayableClass() {
        return playableClass;
    }

    public String getRegistryName() {
        return registryName;
    }

    public List<PaladinTreeSkill> allSkills() {
        return skills;
    }

    public Optional<PaladinTreeSkill> findSkill(String skillId) {
        return Optional.ofNullable(skillsById.get(skillId));
    }

    public PaladinTreeSkill requireSkill(String skillId) {
        return findSkill(skillId)
                .orElseThrow(() -> new IllegalArgumentException("Nieznany skill w rejestrze " + registryName + ": " + skillId));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Pole " + fieldName + " nie może być puste.");
        }
        return value;
    }
}

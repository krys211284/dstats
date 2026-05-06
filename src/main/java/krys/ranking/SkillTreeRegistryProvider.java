package krys.ranking;

import krys.paladin.PaladinSkillTreeRegistry;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Wybiera rejestr drzewa umiejętności dla klasy postaci bez tworzenia osobnych endpointów per klasa. */
public final class SkillTreeRegistryProvider {
    private final Map<PlayableClass, CharacterSkillTreeRegistry> registries;

    public SkillTreeRegistryProvider(List<CharacterSkillTreeRegistry> registries) {
        EnumMap<PlayableClass, CharacterSkillTreeRegistry> registryMap = new EnumMap<>(PlayableClass.class);
        for (CharacterSkillTreeRegistry registry : registries) {
            registryMap.put(registry.getPlayableClass(), registry);
        }
        this.registries = Map.copyOf(registryMap);
    }

    public static SkillTreeRegistryProvider paladinOnly() {
        return new SkillTreeRegistryProvider(List.of(new CharacterSkillTreeRegistry(
                PlayableClass.PALADIN,
                "PaladinSkillTreeRegistry",
                PaladinSkillTreeRegistry.allSkills()
        )));
    }

    public CharacterSkillTreeRegistry registryFor(PlayableClass playableClass) {
        PlayableClass selectedClass = playableClass == null ? defaultClass() : playableClass;
        CharacterSkillTreeRegistry registry = registries.get(selectedClass);
        if (registry == null) {
            throw new IllegalArgumentException("Brak rejestru drzewa umiejętności dla klasy: " + selectedClass.getQueryValue());
        }
        return registry;
    }

    public List<PlayableClass> supportedClasses() {
        return registries.keySet().stream()
                .sorted()
                .toList();
    }

    public PlayableClass defaultClass() {
        if (registries.size() == 1) {
            return registries.keySet().iterator().next();
        }
        return PlayableClass.defaultClass();
    }
}

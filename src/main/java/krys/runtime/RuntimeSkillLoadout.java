package krys.runtime;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/** Niemutowalny payload konfiguracji skilli jednej klasy; bez polityki katalogu i limitu paska UI. */
public record RuntimeSkillLoadout(String classId,
                                 Map<RuntimeSkillKey, RuntimeSkillConfiguration> configurations,
                                 List<RuntimeSkillKey> actionBar) {
    private static final Pattern TECHNICAL_ID = Pattern.compile("[a-z0-9][a-z0-9_-]*");

    public RuntimeSkillLoadout {
        Objects.requireNonNull(classId, "classId");
        if (!TECHNICAL_ID.matcher(classId).matches()) {
            throw new IllegalArgumentException("Id klasy wymaga technicznego id [a-z0-9][a-z0-9_-]*");
        }
        configurations = Map.copyOf(Objects.requireNonNull(configurations, "configurations"));
        actionBar = List.copyOf(Objects.requireNonNull(actionBar, "actionBar"));
        for (var entry : configurations.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().skillKey())) {
                throw new IllegalArgumentException("Klucz wpisu musi odpowiadać kluczowi konfiguracji");
            }
            requireSameClass(classId, entry.getKey());
        }
        var seen = new HashSet<RuntimeSkillKey>();
        for (var key : actionBar) {
            requireSameClass(classId, key);
            if (!seen.add(key)) {
                throw new IllegalArgumentException("Pasek akcji nie może zawierać duplikatów: " + key.asExternalId());
            }
            var configuration = configurations.get(key);
            if (configuration == null || !configuration.isLearned()) {
                throw new IllegalArgumentException("Pasek akcji wymaga istniejącej nauczonej konfiguracji: " + key.asExternalId());
            }
        }
    }

    public Optional<RuntimeSkillConfiguration> configuration(RuntimeSkillKey key) {
        return Optional.ofNullable(configurations.get(Objects.requireNonNull(key, "key")));
    }

    public boolean contains(RuntimeSkillKey key) {
        return configurations.containsKey(Objects.requireNonNull(key, "key"));
    }

    public boolean isOnActionBar(RuntimeSkillKey key) {
        return actionBar.contains(Objects.requireNonNull(key, "key"));
    }

    private static void requireSameClass(String classId, RuntimeSkillKey key) {
        if (!classId.equals(key.classId())) {
            throw new IllegalArgumentException("Wszystkie skille muszą należeć do klasy loadoutu: " + classId);
        }
    }
}

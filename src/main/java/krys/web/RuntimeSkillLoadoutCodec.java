package krys.web;

import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import krys.runtime.RuntimeSkillLoadout;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/** Wersjonowany format persistence; delimitery nie należą do składni identyfikatorów N9. */
final class RuntimeSkillLoadoutCodec {
    static final String PREFIX = "runtime-v1~";

    private RuntimeSkillLoadoutCodec() { }

    static String encode(RuntimeSkillLoadout loadout) {
        String configurations = loadout.configurations().values().stream()
                .sorted(Comparator.comparing(configuration -> configuration.skillKey().asExternalId()))
                .map(configuration -> configuration.skillKey().asExternalId() + "@" + configuration.rank() + "@"
                        + configuration.selections().entrySet().stream().sorted(Map.Entry.comparingByKey())
                        .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(",")))
                .collect(Collectors.joining(";"));
        String bar = loadout.actionBar().stream().map(RuntimeSkillKey::asExternalId).collect(Collectors.joining(","));
        return PREFIX + loadout.classId() + "~" + configurations + "~" + bar;
    }

    static RuntimeSkillLoadout decode(String payload) {
        String[] parts = payload.split("~", -1);
        if (parts.length != 4 || !"runtime-v1".equals(parts[0])) {
            throw new IllegalArgumentException("Niepoprawny format lub wersja runtime skill payload.");
        }
        var configurations = new HashMap<RuntimeSkillKey, RuntimeSkillConfiguration>();
        if (!parts[2].isEmpty()) {
            for (String entry : parts[2].split(";", -1)) {
                String[] fields = entry.split("@", -1);
                if (fields.length != 3) {
                    throw new IllegalArgumentException("Niepoprawny wpis konfiguracji runtime.");
                }
                var key = RuntimeSkillKey.parse(fields[0]);
                var selections = new HashMap<String, String>();
                if (!fields[2].isEmpty()) {
                    for (String selection : fields[2].split(",", -1)) {
                        String[] choice = selection.split("=", -1);
                        if (choice.length != 2 || selections.putIfAbsent(choice[0], choice[1]) != null) {
                            throw new IllegalArgumentException("Niepoprawny lub powtórzony slot wyboru runtime.");
                        }
                    }
                }
                var configuration = new RuntimeSkillConfiguration(key, Integer.parseInt(fields[1]), selections);
                if (configurations.putIfAbsent(key, configuration) != null) {
                    throw new IllegalArgumentException("Powtórzona konfiguracja runtime: " + key.asExternalId());
                }
            }
        }
        var bar = parts[3].isEmpty() ? java.util.List.<RuntimeSkillKey>of()
                : Arrays.stream(parts[3].split(",", -1)).map(RuntimeSkillKey::parse).toList();
        return new RuntimeSkillLoadout(parts[1], configurations, bar);
    }
}

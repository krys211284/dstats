package krys.web;

import krys.necromancer.app.NecromancerCurrentBuildCalculationService;
import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillLoadout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/** Edytuje tylko obecność Krwotoku; nie naprawia ani nie resetuje istniejących konfiguracji. */
final class NecromancerCurrentBuildLoadoutEditor {
    private NecromancerCurrentBuildLoadoutEditor() { }

    static RuntimeSkillLoadout apply(RuntimeSkillLoadout saved, NecromancerCurrentBuildFormData form) {
        if (!"necromancer".equals(saved.classId())) {
            throw new IllegalArgumentException("N11 wymaga loadoutu Nekromanty.");
        }
        if (!form.validationErrors().isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", form.validationErrors()));
        }
        var key = NecromancerCurrentBuildCalculationService.KRWOTOK;
        var configurations = new LinkedHashMap<>(saved.configurations());
        var bar = new ArrayList<>(saved.actionBar());
        if (form.selectedSkillId().isEmpty()) {
            configurations.remove(key);
            bar.remove(key);
        } else if (!configurations.containsKey(key)) {
            configurations.put(key, new RuntimeSkillConfiguration(key, 1, Map.of()));
            bar.add(key);
        }
        return new RuntimeSkillLoadout(saved.classId(), configurations, bar);
    }
}

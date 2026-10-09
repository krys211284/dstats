package krys.web;

import krys.runtime.RuntimeSkillConfiguration;
import krys.runtime.RuntimeSkillKey;
import krys.runtime.RuntimeSkillLoadout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeSkillLoadoutCodecTest {
    @Test
    void codecShouldSortConfigurationsAndSelectionSlotsButPreserveActionBarOrderAndRankZero() {
        var first = new RuntimeSkillKey("future_class", "skill_a");
        var last = new RuntimeSkillKey("future_class", "skill_z");
        var unlearned = new RuntimeSkillKey("future_class", "skill_unlearned");
        var choices = new LinkedHashMap<String, String>();
        choices.put("slot_z", "option_z");
        choices.put("slot_a", "option_a");
        var configs = new LinkedHashMap<RuntimeSkillKey, RuntimeSkillConfiguration>();
        configs.put(last, new RuntimeSkillConfiguration(last, 2, choices));
        configs.put(unlearned, new RuntimeSkillConfiguration(unlearned, 0, Map.of()));
        configs.put(first, new RuntimeSkillConfiguration(first, 1, Map.of()));
        var loadout = new RuntimeSkillLoadout("future_class", configs, List.of(last, first));
        String payload = RuntimeSkillLoadoutCodec.encode(loadout);
        assertEquals("runtime-v1~future_class~future_class:skill_a@1@;future_class:skill_unlearned@0@;"
                + "future_class:skill_z@2@slot_a=option_a,slot_z=option_z~future_class:skill_z,future_class:skill_a", payload);
        assertEquals(loadout, RuntimeSkillLoadoutCodec.decode(payload));
        assertEquals(payload, RuntimeSkillLoadoutCodec.encode(RuntimeSkillLoadoutCodec.decode(payload)));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "runtime-v2~necromancer~~", "runtime-v1~necromancer~", "runtime-v1~necromancer~~~extra",
            "runtime-v1~necromancer~broken~", "runtime-v1~necromancer~necromancer:krwotok@1@;~",
            "runtime-v1~necromancer~necromancer:krwotok@1@;necromancer:krwotok@1@~",
            "runtime-v1~necromancer~necromancer:krwotok@1@1=first,1=second~",
            "runtime-v1~necromancer~necromancer:krwotok@0@~necromancer:krwotok",
            "runtime-v1~necromancer~~necromancer:krwotok",
            "runtime-v1~necromancer~paladin:natarcie@1@~",
            "runtime-v1~necromancer~necromancer:krwotok@1@~necromancer:krwotok,necromancer:krwotok",
            "runtime-v1~necromancer~necromancer:krwotok@1@~necromancer:krwotok,"
    })
    void malformedOrInconsistentPayloadShouldBeRejectedWithoutSkippingEntries(String payload) {
        assertThrows(IllegalArgumentException.class, () -> RuntimeSkillLoadoutCodec.decode(payload));
    }
}

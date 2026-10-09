package krys.runtime.legacy;

import krys.paladin.PaladinSkillTreeRegistry;
import krys.runtime.RuntimeSkillKey;
import krys.skill.SkillId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class LegacyPaladinRuntimeSkillKeyAdapterTest {
    @ParameterizedTest
    @CsvSource({"BRANDISH, paladin:wymach, Wymach", "HOLY_BOLT, paladin:swiety_pocisk, Święty Pocisk",
            "CLASH, paladin:starcie, Starcie", "ADVANCE, paladin:natarcie, Natarcie"})
    void exactLegacyMappingShouldRoundTripAndMatchUnchangedCatalog(SkillId legacyId, String externalId, String name) {
        var key = LegacyPaladinRuntimeSkillKeyAdapter.toRuntimeKey(legacyId);
        assertEquals(RuntimeSkillKey.parse(externalId), key);
        assertEquals(externalId, key.asExternalId());
        assertEquals(Optional.of(legacyId), LegacyPaladinRuntimeSkillKeyAdapter.toLegacySkillId(key));
        assertEquals(name, PaladinSkillTreeRegistry.requireSkill(key.skillId()).getSkillName());
    }

    @Test
    void allFourLegacyValuesShouldHaveDistinctKeys() {
        assertEquals(4, Arrays.stream(SkillId.values())
                .map(LegacyPaladinRuntimeSkillKeyAdapter::toRuntimeKey).distinct().count());
    }

    @Test
    void nonLegacyOrWrongNamespaceKeysShouldReturnEmpty() {
        for (String externalId : new String[]{"necromancer:krwotok", "paladin:unknown", "necromancer:wymach", "paladin:krwotok"}) {
            assertEquals(Optional.empty(), LegacyPaladinRuntimeSkillKeyAdapter.toLegacySkillId(RuntimeSkillKey.parse(externalId)));
        }
    }

    @Test
    void nullInputsShouldThrowExplicitly() {
        assertThrows(NullPointerException.class, () -> LegacyPaladinRuntimeSkillKeyAdapter.toRuntimeKey(null));
        assertThrows(NullPointerException.class, () -> LegacyPaladinRuntimeSkillKeyAdapter.toLegacySkillId(null));
    }
}

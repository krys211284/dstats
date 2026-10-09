package krys.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeSkillKeyTest {
    @ParameterizedTest
    @CsvSource({"paladin, wymach", "paladin, swiety_pocisk", "necromancer, krwotok", "class-2, skill_3", "0, 1"})
    void technicalIdentityShouldPreserveFieldsAndExternalRoundTrip(String classId, String skillId) {
        var key = new RuntimeSkillKey(classId, skillId);
        assertEquals(classId, key.classId());
        assertEquals(skillId, key.skillId());
        assertEquals(classId + ":" + skillId, key.asExternalId());
        assertEquals(key, RuntimeSkillKey.parse(key.asExternalId()));
    }

    @Test
    void nullFieldsAndExternalIdentityShouldBeRejected() {
        assertThrows(NullPointerException.class, () -> new RuntimeSkillKey(null, "krwotok"));
        assertThrows(NullPointerException.class, () -> new RuntimeSkillKey("necromancer", null));
        assertThrows(NullPointerException.class, () -> RuntimeSkillKey.parse(null));
    }

    @Test
    void invalidTechnicalIdsShouldBeRejectedInBothFieldsWithoutNormalization() {
        for (String invalid : new String[]{"", " ", "Necromancer", "KRWOTOK", " krwotok", "krwotok ",
                "two words", "krwotok!", "skill:id", "święty", "_skill", "-class", "skill.id", "skill\n", "skill\t"}) {
            assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillKey(invalid, "krwotok"), invalid);
            assertThrows(IllegalArgumentException.class, () -> new RuntimeSkillKey("necromancer", invalid), invalid);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "krwotok", ":krwotok", "necromancer:", "necromancer:krwotok:extra", ":",
            "necromancer::krwotok", "Necromancer:krwotok", "necromancer:Krwotok", "necromancer:krwotok!",
            " necromancer:krwotok", "necromancer:krwotok ", "necromancer :krwotok", "necromancer:krwotok\n"})
    void malformedExternalIdentityShouldBeRejected(String externalId) {
        assertThrows(IllegalArgumentException.class, () -> RuntimeSkillKey.parse(externalId));
    }

    @Test
    void equalityAndHashCodeShouldIncludeBothIdentityParts() {
        var first = new RuntimeSkillKey("necromancer", "krwotok");
        var second = new RuntimeSkillKey("necromancer", "krwotok");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new RuntimeSkillKey("paladin", "krwotok"));
        assertNotEquals(first, new RuntimeSkillKey("necromancer", "wymach"));
        assertEquals(Set.of(first), new HashSet<>(java.util.List.of(first, second)));
    }
}

package krys.runtime;

import krys.app.CurrentBuildRequest;
import krys.simulation.HeroBuildSnapshot;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeSkillLoadoutArchitectureTest {
    @Test
    void neutralLoadoutShouldDependOnlyOnRuntimeValueObjectsAndJdk() throws Exception {
        String source = Files.readString(Path.of("src/main/java/krys/runtime/RuntimeSkillLoadout.java"));
        assertAllowedReferences(source, "(?:java\\.[\\w.]+|krys\\.runtime\\.(?:RuntimeSkillKey|RuntimeSkillConfiguration))", "krys.runtime");
        assertFalse(Pattern.compile("\\b(SkillId|SkillState|HeroClass|PlayableClass|Paladin\\w*|Necromancer\\w*|"
                + "CurrentBuild\\w*|ACTION_BAR_SLOT_COUNT|6)\\b").matcher(source).find());
        assertTrue(RuntimeSkillLoadout.class.isRecord());
        assertArrayEquals(new Class<?>[]{String.class, java.util.Map.class, java.util.List.class},
                Arrays.stream(RuntimeSkillLoadout.class.getRecordComponents()).map(component -> component.getType()).toArray(Class<?>[]::new));
        assertFalse(Arrays.stream(RuntimeSkillLoadout.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("parse") || method.getName().equals("asExternalId")));
    }

    @Test
    void legacyLoadoutAdapterShouldDependOnlyOnStateRuntimeModelsLegacyAdaptersAndJdk() throws Exception {
        String source = Files.readString(Path.of("src/main/java/krys/runtime/legacy/LegacyPaladinRuntimeSkillLoadoutAdapter.java"));
        assertAllowedReferences(source, "(?:java\\.[\\w.]+|krys\\.runtime\\.(?:RuntimeSkillLoadout|RuntimeSkillConfiguration|RuntimeSkillKey)|"
                + "krys\\.runtime\\.legacy\\.LegacyPaladinRuntimeSkill(?:Configuration|Key)Adapter|krys\\.skill\\.(?:SkillId|SkillState))",
                "krys.runtime.legacy");
        assertFalse(Pattern.compile("\\b(Hero\\w*|CurrentBuild\\w*|DamageEngine|ManualSimulationService|"
                + "PaladinSkillTreeRegistry|Necromancer\\w*)\\b").matcher(source).find());
    }

    @Test
    void requestAndSnapshotShouldStoreOnlyOneCanonicalSkillPayload() {
        for (var type : List.of(CurrentBuildRequest.class, HeroBuildSnapshot.class)) {
            var fields = Arrays.stream(type.getDeclaredFields()).filter(field -> !Modifier.isStatic(field.getModifiers())).toList();
            assertEquals(1, fields.stream().filter(field -> field.getType() == RuntimeSkillLoadout.class).count(), type.getName());
            for (var field : fields) {
                assertFalse(List.of("learnedSkills", "actionBar", "selectedSkillBar").contains(field.getName()), field.toString());
                assertFalse(field.getGenericType().getTypeName().contains("krys.skill.SkillId"), field.toString());
                assertFalse(field.getGenericType().getTypeName().contains("krys.skill.SkillState"), field.toString());
            }
        }
    }

    @Test
    void snapshotFactoryShouldTransportNeutralLoadoutWithoutReadingLegacySkillPayload() throws Exception {
        String source = Files.readString(Path.of("src/main/java/krys/app/CurrentBuildSnapshotFactory.java"));
        assertTrue(source.contains("request.getRuntimeSkillLoadout()"));
        assertFalse(Pattern.compile("request\\.get(?:LearnedSkills|ActionBar)\\s*\\(").matcher(source).find());
        assertFalse(source.contains("LegacyPaladinRuntimeSkillLoadoutAdapter"));
    }

    @Test
    void requestShouldKeepLegacyPublicApiAndSnapshotShouldExposeExactlyOneNeutralConstructor() {
        assertEquals(7, CurrentBuildRequest.class.getConstructors().length);
        assertTrue(Arrays.stream(CurrentBuildRequest.class.getConstructors())
                .noneMatch(constructor -> Arrays.asList(constructor.getParameterTypes()).contains(RuntimeSkillLoadout.class)));
        assertEquals(7, HeroBuildSnapshot.class.getConstructors().length);
        assertEquals(1, Arrays.stream(HeroBuildSnapshot.class.getConstructors())
                .filter(constructor -> Arrays.asList(constructor.getParameterTypes()).contains(RuntimeSkillLoadout.class)).count());
    }

    private static void assertAllowedReferences(String source, String allowedExpression, String ownPackage) {
        var allowed = Pattern.compile(allowedExpression);
        var imports = Pattern.compile("(?m)^import\\s+([^;]+);").matcher(source);
        while (imports.find()) {
            assertTrue(allowed.matcher(imports.group(1)).matches(), imports.group());
        }
        var qualified = Pattern.compile("krys\\.[\\w.]+").matcher(source);
        while (qualified.find()) {
            assertTrue(allowed.matcher(qualified.group()).matches() || qualified.group().equals(ownPackage), qualified.group());
        }
    }
}

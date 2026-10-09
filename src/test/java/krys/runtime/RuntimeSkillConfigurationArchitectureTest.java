package krys.runtime;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeSkillConfigurationArchitectureTest {
    @Test
    void neutralConfigurationShouldDependOnlyOnIdentityAndJdkWithoutRankPolicyOrLegacySlots() throws Exception {
        String source = Files.readString(Path.of("src/main/java/krys/runtime/RuntimeSkillConfiguration.java"));
        assertAllowedReferences(source, "(?:java\\.[\\w.]+|krys\\.runtime\\.RuntimeSkillKey)", "krys.runtime");
        assertFalse(Pattern.compile("\\b(SkillId|SkillState|HeroClass|PlayableClass|DamagePercentRankTable|MAX_RANK|"
                + "HeroSkillPointBudget|NONE|5|15)\\b|legacy_").matcher(source).find());
        assertTrue(RuntimeSkillConfiguration.class.isRecord());
        assertArrayEquals(new Class<?>[]{RuntimeSkillKey.class, int.class, java.util.Map.class},
                Arrays.stream(RuntimeSkillConfiguration.class.getRecordComponents()).map(component -> component.getType()).toArray(Class<?>[]::new));
        assertFalse(Arrays.stream(RuntimeSkillConfiguration.class.getDeclaredMethods())
                .anyMatch(method -> method.getName().equals("parse") || method.getName().equals("asExternalId")));
    }

    @Test
    void legacyConfigurationBridgeShouldHaveOnlyStateIdentityAndJdkDependencies() throws Exception {
        String source = Files.readString(Path.of("src/main/java/krys/runtime/legacy/LegacyPaladinRuntimeSkillConfigurationAdapter.java"));
        assertAllowedReferences(source, "(?:java\\.[\\w.]+|krys\\.runtime\\.(?:RuntimeSkillConfiguration|RuntimeSkillKey)|"
                + "krys\\.runtime\\.legacy\\.LegacyPaladinRuntimeSkillKeyAdapter|"
                + "krys\\.skill\\.(?:SkillState|SkillId|SkillUpgradeChoice|SkillRuntimeModifierChoice))", "krys.runtime.legacy");
        assertFalse(Pattern.compile("\\b(PaladinSkillTreeRegistry|Necromancer\\w*|Hero\\w*|CurrentBuild\\w*|DamageEngine|"
                + "ManualSimulationService|PlayableClass)\\b").matcher(source).find());
        var type = krys.runtime.legacy.LegacyPaladinRuntimeSkillConfigurationAdapter.class;
        for (var field : type.getDeclaredFields()) {
            assertTrue(java.lang.reflect.Modifier.isPrivate(field.getModifiers()), field.toString());
        }
    }

    @Test
    void necromancerCatalogValidatorShouldHaveOnlyCatalogConfigurationAndJdkDependencies() throws Exception {
        String source = Files.readString(Path.of("src/main/java/krys/necromancer/runtime/NecromancerRuntimeSkillConfigurationValidator.java"));
        assertAllowedReferences(source, "(?:java\\.[\\w.]+|krys\\.runtime\\.(?:RuntimeSkillConfiguration|RuntimeSkillKey)|"
                + "krys\\.necromancer\\.NecromancerSkillTreeRegistry|krys\\.skilltree\\.[\\w.]+)", "krys.necromancer.runtime");
        assertFalse(Pattern.compile("\\b(SkillState|SkillId|HeroClass|PlayableClass|Paladin\\w*|Hero\\w*|CurrentBuild\\w*|"
                + "DamageEngine|ManualSimulationService|DirectDamage\\w*|NecromancerSeason15DirectDamageResolver)\\b").matcher(source).find());
        assertTrue(source.contains("DamagePercentRankTable.MAX_RANK"));
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

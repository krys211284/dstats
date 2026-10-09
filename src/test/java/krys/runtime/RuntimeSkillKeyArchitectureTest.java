package krys.runtime;

import krys.hero.HeroClass;
import krys.runtime.legacy.LegacyPaladinRuntimeSkillKeyAdapter;
import krys.skill.SkillId;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeSkillKeyArchitectureTest {
    @Test
    void identityShouldDependOnlyOnJdkAndItsOwnFields() throws Exception {
        String source = Files.readString(Path.of("src/main/java/krys/runtime/RuntimeSkillKey.java"));
        assertAllowedReferences(source, "java\\.[\\w.]+", "krys.runtime");
        assertFalse(Pattern.compile("\\b(SkillId|HeroClass|PlayableClass|CharacterTreeSkill)\\b").matcher(source).find());
        assertTrue(RuntimeSkillKey.class.isRecord());
        assertEquals(2, RuntimeSkillKey.class.getRecordComponents().length);
        for (var component : RuntimeSkillKey.class.getRecordComponents()) {
            assertEquals(String.class, component.getType());
        }
    }

    @Test
    void legacyAdapterShouldDependOnlyOnIdentityLegacyEnumAndJdk() throws Exception {
        String source = Files.readString(Path.of("src/main/java/krys/runtime/legacy/LegacyPaladinRuntimeSkillKeyAdapter.java"));
        assertAllowedReferences(source, "(?:java\\.[\\w.]+|krys\\.runtime\\.RuntimeSkillKey|krys\\.skill\\.SkillId)", "krys.runtime.legacy");
        assertFalse(Pattern.compile("\\b(CurrentBuild\\w*|Hero\\w*|DamageEngine|PlayableClass|PaladinSkillTreeRegistry)\\b")
                .matcher(source).find());
    }

    @Test
    void migratedResolverShouldExposeOnlyTypedSkillIdentity() throws Exception {
        var type = krys.necromancer.combat.NecromancerSeason15DirectDamageResolver.class;
        var calculateMethods = java.util.Arrays.stream(type.getDeclaredMethods())
                .filter(method -> method.getName().equals("calculate") && java.lang.reflect.Modifier.isPublic(method.getModifiers())).toList();
        assertEquals(1, calculateMethods.size());
        assertEquals(RuntimeSkillConfiguration.class, calculateMethods.getFirst().getParameterTypes()[0]);
        assertEquals(2, calculateMethods.getFirst().getParameterCount());
        String presentation = Files.readString(Path.of("src/main/java/krys/web/HeroSkillCatalogAdapter.java"));
        assertTrue(presentation.contains("LegacyPaladinRuntimeSkillKeyAdapter.toRuntimeKey(skillId)"));
        assertTrue(presentation.contains("PaladinSkillTreeRegistry.findSkill(key.skillId())"));
        assertFalse(Pattern.compile("case\\s+(BRANDISH|HOLY_BOLT|CLASH|ADVANCE)\\s*->\\s*PaladinSkillTreeRegistry\\.findSkill")
                .matcher(presentation).find());
    }

    @Test
    void profileClassEnumShouldIncludeNecromancerWhileLegacySkillEnumRemainsUnchanged() {
        assertArrayEquals(new HeroClass[]{HeroClass.PALADIN, HeroClass.NECROMANCER}, HeroClass.values());
        assertArrayEquals(new SkillId[]{SkillId.BRANDISH, SkillId.HOLY_BOLT, SkillId.CLASH, SkillId.ADVANCE}, SkillId.values());
    }

    @Test
    void legacyToCatalogMappingShouldHaveOneProductionOwner() throws Exception {
        var duplicatedMapping = Pattern.compile("case\\s+(BRANDISH|HOLY_BOLT|CLASH|ADVANCE)\\s*->\\s*"
                + "(?:PaladinSkillTreeRegistry\\.(?:findSkill|requireSkill)\\(\\s*)?\"(?:wymach|swiety_pocisk|starcie|natarcie)\"");
        Path owner = Path.of("src/main/java/krys/runtime/legacy/LegacyPaladinRuntimeSkillKeyAdapter.java");
        try (var files = Files.walk(Path.of("src/main/java"))) {
            for (var file : files.filter(path -> path.toString().endsWith(".java") && !path.equals(owner)).toList()) {
                assertFalse(duplicatedMapping.matcher(Files.readString(file)).find(), file.toString());
            }
        }
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

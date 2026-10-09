package krys.necromancer.combat;

import krys.hero.HeroClass;
import krys.necromancer.NecromancerSkillTreeRegistry;
import krys.necromancer.bookofthedead.NecromancerBookOfDeadRegistry;
import krys.necromancer.bookofthedead.NecromancerMinionFamily;
import krys.paladin.PaladinSkillTreeRegistry;
import krys.skill.SkillId;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class NecromancerSeason15DirectDamageArchitectureTest {
    @Test
    void standaloneResolverAndProfileShouldHaveOnlyAllowedDependenciesWithoutCoefficientNarrowing() throws Exception {
        var forbidden = Pattern.compile("\\b(HeroClass|Hero|HeroClassDef|HeroClassDefs|HeroProfile|HeroBuildSnapshot|"
                + "SkillId|SkillState|PaladinSkillDefs|DamageEngine|DamageBreakdown|DamageComponentBreakdown|"
                + "ManualSimulationService|CurrentBuildFormData|CurrentBuildRequest|CurrentBuildSnapshotFactory|"
                + "HeroSkillLoadout|BuildSearch)\\b");
        var allowed = Pattern.compile("(?:java\\.[\\w.]+|krys\\.combat\\.DirectDamage\\w+|"
                + "krys\\.necromancer\\.NecromancerSkillTreeRegistry|krys\\.skilltree\\.[\\w.]+|krys\\.runtime\\.RuntimeSkillConfiguration)");
        var imports = Pattern.compile("(?m)^import\\s+([^;]+);");
        var qualifiedReferences = Pattern.compile("krys\\.[\\w.]+");
        for (var type : List.of(NecromancerSeason15DirectDamageProfile.class, NecromancerSeason15DirectDamageResolver.class)) {
            String source = Files.readString(Path.of("src/main/java/krys/necromancer/combat", type.getSimpleName() + ".java"));
            assertFalse(forbidden.matcher(source).find(), type.getSimpleName());
            assertFalse(Pattern.compile("\\.\\s*(longValueExact|intValue|intValueExact|longValue)\\s*\\(")
                    .matcher(source).find(), "Zawężenie współczynnika: " + type.getSimpleName());
            var importMatcher = imports.matcher(source);
            while (importMatcher.find()) {
                assertTrue(allowed.matcher(importMatcher.group(1)).matches(), importMatcher.group());
            }
            var qualifiedMatcher = qualifiedReferences.matcher(source);
            while (qualifiedMatcher.find()) {
                assertTrue(allowed.matcher(qualifiedMatcher.group()).matches()
                        || qualifiedMatcher.group().equals("krys.necromancer.combat"), qualifiedMatcher.group());
            }
        }
        String resolver = Files.readString(Path.of("src/main/java/krys/necromancer/combat/NecromancerSeason15DirectDamageResolver.java"));
        assertTrue(resolver.contains("getBaseDamagePercentRanks().damagePercentAtRank(rank)"));
        assertTrue(resolver.contains("BigDecimal damagePercent"));
        assertTrue(resolver.contains("new DirectDamageComponentSpec("));
        assertTrue(resolver.contains("EXACT_COMPONENT_SUM"));
        assertFalse(resolver.contains("ROUNDED_RAW_TOTAL"));
        assertFalse(Pattern.compile("\\b80(?:\\.0)?[dDlL]?\\b").matcher(resolver).find(), "Współczynnik należy do katalogu");
    }

    @Test
    void catalogBookAndLegacySkillsShouldRemainUnchangedWithN10ProfileClasses() {
        assertArrayEquals(new HeroClass[]{HeroClass.PALADIN, HeroClass.NECROMANCER}, HeroClass.values());
        assertArrayEquals(new SkillId[]{SkillId.BRANDISH, SkillId.HOLY_BOLT, SkillId.CLASH, SkillId.ADVANCE}, SkillId.values());
        var necro = NecromancerSkillTreeRegistry.asCharacterRegistry();
        assertEquals(23, necro.allSkills().size());
        assertEquals(161, necro.allSkills().stream().flatMap(skill -> skill.getUpgradeGroups().stream())
                .mapToInt(group -> group.getUpgrades().size()).sum());
        assertEquals(24, PaladinSkillTreeRegistry.allSkills().size());
        assertEquals(3, NecromancerMinionFamily.values().length);
        var book = NecromancerBookOfDeadRegistry.sourceCatalog();
        assertEquals(9, book.all().size());
        assertEquals(27, book.all().stream().mapToInt(definition -> definition.choices().size()).sum());
    }
}

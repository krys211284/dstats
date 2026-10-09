package krys.combat;

import krys.hero.HeroClass;
import krys.necromancer.NecromancerSkillTreeRegistry;
import krys.necromancer.bookofthedead.NecromancerBookOfDeadRegistry;
import krys.necromancer.bookofthedead.NecromancerMinionFamily;
import krys.paladin.PaladinSkillTreeRegistry;
import krys.skill.SkillId;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class DirectDamageKernelArchitectureTest {
    private static final List<Class<?>> KERNEL_TYPES = List.of(DirectDamageCalculator.class, DirectDamageContext.class,
            DirectDamageComponentSpec.class, DirectDamageComponentResult.class, DirectDamageResult.class);

    @Test
    void neutralKernelShouldUseOnlyJdkAndItsOwnModels() throws Exception {
        var forbiddenNames = Pattern.compile("\\b(SkillId|SkillState|SkillUpgradeChoice|PaladinSkillDefs|PaladinSkillTreeRegistry|"
                + "HeroClass|HeroClassDefs|HeroBuildSnapshot|strength|intelligence|faith|animus|moloch|verathiel|Essence|Corpse)\\b",
                Pattern.CASE_INSENSITIVE);
        for (var type : KERNEL_TYPES) {
            var source = Files.readString(Path.of("src/main/java/krys/combat", type.getSimpleName() + ".java"));
            assertFalse(forbiddenNames.matcher(source).find(), type.getSimpleName());
            assertFalse(source.contains("krys.paladin"));
            assertFalse(source.contains("krys.necromancer"));
            assertFalse(source.contains("krys.skill"));
            assertFalse(source.contains("krys.web"));
            assertFalse(source.contains("krys.ranking"));
            assertFalse(source.contains("krys.simulation"));
            for (String line : source.lines().filter(value -> value.startsWith("import ")).toList()) {
                assertTrue(line.startsWith("import java."), type.getSimpleName() + ": " + line);
            }
            for (var field : type.getDeclaredFields()) {
                assertTrue(field.getType().isPrimitive() || field.getType().getName().startsWith("java.")
                        || KERNEL_TYPES.contains(field.getType())
                        || field.getType() == DirectDamageContext.CriticalTotalRoundingPolicy.class,
                        type.getSimpleName() + ": " + field);
            }
        }
    }

    @Test
    void catalogAndLegacySkillIdentitiesShouldRemainUnchangedWithN10ProfileClasses() {
        assertArrayEquals(new HeroClass[]{HeroClass.PALADIN, HeroClass.NECROMANCER}, HeroClass.values());
        assertArrayEquals(new SkillId[]{SkillId.BRANDISH, SkillId.HOLY_BOLT, SkillId.CLASH, SkillId.ADVANCE}, SkillId.values());
        var necro = NecromancerSkillTreeRegistry.asCharacterRegistry();
        assertEquals(23, necro.allSkills().size());
        assertEquals(161, necro.allSkills().stream().flatMap(skill -> skill.getUpgradeGroups().stream())
                .mapToInt(group -> group.getUpgrades().size()).sum());
        assertEquals(24, PaladinSkillTreeRegistry.allSkills().size());
        var book = NecromancerBookOfDeadRegistry.sourceCatalog();
        assertEquals(3, Arrays.stream(NecromancerMinionFamily.values()).count());
        assertEquals(9, book.all().size());
        assertEquals(27, book.all().stream().mapToInt(definition -> definition.choices().size()).sum());
    }
}

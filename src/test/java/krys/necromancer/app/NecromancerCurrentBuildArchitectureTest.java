package krys.necromancer.app;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class NecromancerCurrentBuildArchitectureTest {
    @Test
    void applicationLayerShouldDependOnlyOnNeutralRuntimeN8BAndDirectResult() throws Exception {
        var forbidden = Pattern.compile("\\b(SkillId|SkillState|PaladinSkillDefs|HeroClassDefs|DamageEngine|ManualSimulationService|CurrentBuildCalculationService|HeroProfile|HeroBuildSnapshot|CurrentBuildRequest|CurrentBuildSnapshotFactory|DamageBreakdown|SimulationResult)\\b");
        var allowed = Pattern.compile("(?:java\\.[\\w.]+|krys\\.runtime\\.RuntimeSkill(?:Key|Configuration|Loadout)|krys\\.necromancer\\.combat\\.NecromancerSeason15DirectDamage(?:Profile|Resolver)|krys\\.combat\\.DirectDamageResult|krys\\.necromancer\\.app(?:\\.NecromancerCurrentBuildCalculation)?)");
        for (var type : List.of(NecromancerCurrentBuildCalculationService.class, NecromancerCurrentBuildCalculation.class)) {
            var source = Files.readString(Path.of("src/main/java/krys/necromancer/app", type.getSimpleName() + ".java"));
            assertFalse(forbidden.matcher(source).find(), type.getSimpleName());
            var imports = Pattern.compile("(?m)^import\\s+([^;]+);").matcher(source);
            while (imports.find()) { assertTrue(allowed.matcher(imports.group(1)).matches(), imports.group()); }
            var qualified = Pattern.compile("krys\\.[\\w.]+").matcher(source);
            while (qualified.find()) { assertTrue(allowed.matcher(qualified.group()).matches(), qualified.group()); }
            assertFalse(Pattern.compile("\\b80(?:\\.0)?[dDlL]?\\b").matcher(source).find(), "Coefficient pozostaje w katalogu");
        }
    }
}

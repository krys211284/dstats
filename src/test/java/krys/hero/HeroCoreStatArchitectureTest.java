package krys.hero;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import static org.junit.jupiter.api.Assertions.*;

class HeroCoreStatArchitectureTest {
    @Test
    void progressionAndPresentationShouldHaveNoLegacyDamageDependencies() throws Exception {
        var forbidden = Pattern.compile("\\b(DamageEngine|HeroClassDef|HeroClassDefs|mainStatDivisor|SkillId|SkillState|ManualSimulationService)\\b");
        for (String path : List.of("src/main/java/krys/hero/HeroCoreStats.java", "src/main/java/krys/hero/HeroClassCoreStatProgression.java",
                "src/main/java/krys/hero/HeroClassCoreStatProgressions.java", "src/main/java/krys/hero/HeroCoreStatDerivedFacts.java",
                "src/main/java/krys/hero/HeroCriticalChanceBreakdown.java", "src/main/java/krys/web/CurrentHeroStatsPresentation.java")) {
            assertFalse(forbidden.matcher(Files.readString(Path.of(path))).find(), path);
        }
        assertFalse(Files.readString(Path.of("src/main/java/krys/hero/HeroCriticalChanceBreakdown.java")).contains("criticalChanceFromIntelligencePercent"));
    }
}

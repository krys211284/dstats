package krys.necromancer.bookofthedead;

import krys.hero.HeroClass;
import krys.necromancer.NecromancerSkillTreeRegistry;
import krys.paladin.PaladinSkillTreeRegistry;
import krys.skill.SkillId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static krys.necromancer.bookofthedead.BookOfDeadChoiceType.*;
import static krys.necromancer.bookofthedead.BookOfDeadSourceFact.Unit.*;
import static krys.necromancer.bookofthedead.NecromancerMinionSpecializationId.*;
import static org.junit.jupiter.api.Assertions.*;

class NecromancerBookOfDeadRegistryTest {
    private static final NecromancerBookOfDeadRegistry BOOK = NecromancerBookOfDeadRegistry.sourceCatalog();

    @Test
    void shouldExposeThreeFamiliesNineSpecializationsAndTwentySevenChoices() {
        assertEquals(3, NecromancerMinionFamily.values().length);
        assertEquals(9, BOOK.all().size());
        assertEquals(27, BOOK.all().stream().mapToInt(definition -> definition.choices().size()).sum());
        assertEquals(List.of("Szkieletowi Wojownicy", "Szkieletowi Magowie", "Golemy"),
                Arrays.stream(NecromancerMinionFamily.values()).map(NecromancerMinionFamily::displayName).toList());
        for (var family : NecromancerMinionFamily.values()) {
            assertEquals(3, BOOK.byFamily(family).size());
            assertTrue(BOOK.byFamily(family).stream().allMatch(definition -> definition.family() == family));
        }
    }

    @ParameterizedTest
    @CsvSource({
            "SKIRMISHER,skirmisher,Harcownicy,Skirmishers,5,BONE,SKELETAL_WARRIOR",
            "DEFENDER,defender,Obrońcy,Defenders,8,BLOOD,SKELETAL_WARRIOR",
            "REAPER,reaper,Żniwiarze,Reapers,12,DARKNESS,SKELETAL_WARRIOR",
            "SHADOW_MAGE,shadow_mage,Magowie Cienia,Shadow Mages,5,DARKNESS,SKELETAL_MAGE",
            "COLD_MAGE,cold_mage,Magowie Zimna,Cold Mages,18,DARKNESS,SKELETAL_MAGE",
            "BONE_MAGE,bone_mage,Magowie Kości,Bone Mages,22,BONE,SKELETAL_MAGE",
            "BONE_GOLEM,bone_golem,Golem Kościany,Bone Golem,8,BONE,GOLEM",
            "BLOOD_GOLEM,blood_golem,Golem Krwawy,Blood Golem,28,BLOOD,GOLEM",
            "IRON_GOLEM,iron_golem,Golem Żelazny,Iron Golem,32,DARKNESS,GOLEM"
    })
    void shouldPreserveGoldenIdentityUnlockAndTheme(NecromancerMinionSpecializationId id, String rawId,
            String name, String canonical, int level, NecromancerMinionTheme theme, NecromancerMinionFamily family) {
        // Golden values z kontraktu N7, Season 15 / 3.2.1; bez wyprowadzania oczekiwań z registry.
        var definition = BOOK.find(id).orElseThrow();
        assertEquals(rawId, definition.id().rawId());
        assertSame(definition, BOOK.findByRawId(rawId).orElseThrow());
        assertEquals(name, definition.displayName());
        assertEquals(canonical, definition.canonicalName());
        assertEquals(level, definition.unlockLevel());
        assertEquals(theme, definition.theme());
        assertEquals(family, definition.family());
        assertFalse(definition.passiveDescription().isBlank());
        assertFalse(definition.passiveFacts().isEmpty());
        assertFalse(definition.sourceReference().isBlank());
        assertEquals(BookOfDeadRuntimeStatus.NOT_RUNTIME_ENABLED, definition.runtimeStatus());
        assertEquals(List.of(UPGRADE_1, UPGRADE_2, SACRIFICE), definition.choices().stream().map(BookOfDeadChoiceDefinition::type).toList());
        assertTrue(definition.choices().stream().allMatch(choice -> !choice.description().isBlank()));
        assertEquals(id == BLOOD_GOLEM || id == IRON_GOLEM, definition.additionalUnlockRequirement().isPresent());
        definition.additionalUnlockRequirement().ifPresent(quest -> assertEquals("Call of the Underworld", quest));
    }

    @Test
    void shouldKeepStableSourceOrderAndSafeLookups() {
        assertEquals(List.of("skirmisher", "defender", "reaper", "shadow_mage", "cold_mage", "bone_mage",
                        "bone_golem", "blood_golem", "iron_golem"),
                BOOK.all().stream().map(definition -> definition.id().rawId()).toList());
        assertEquals(9, BOOK.all().stream().map(NecromancerMinionSpecializationDefinition::id).distinct().count());
        assertTrue(BOOK.findByRawId("unknown").isEmpty());
        assertTrue(BOOK.findByRawId(null).isEmpty());
        assertTrue(BOOK.find(null).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("goldenNumericFacts")
    void shouldPreserveEveryGoldenNumericFact(Golden golden) {
        var definition = BOOK.find(golden.id()).orElseThrow();
        Map<String, String> actual = new LinkedHashMap<>();
        collectNumeric(actual, "P", definition.passiveFacts());
        collectNumeric(actual, "U1", definition.choice(UPGRADE_1).effects());
        collectNumeric(actual, "U2", definition.choice(UPGRADE_2).effects());
        collectNumeric(actual, "S", definition.choice(SACRIFICE).playerBonuses());
        var expected = new LinkedHashMap<String, String>();
        for (String fact : golden.facts().split(";")) {
            var parts = fact.split("=", 2);
            expected.put(parts[0], parts[1]);
        }
        assertEquals(expected, actual);
    }

    private static Stream<Golden> goldenNumericFacts() {
        // Niezależne, jawne goldeny liczb i ich podstawy; współczynniki Book nie są tabelami skilli.
        return Stream.of(
                new Golden(SKIRMISHER, "P.damage=30/PERCENT;P.life=-15/PERCENT;U1.additional_minions=2/COUNT;"
                        + "U2.slow=50/PERCENT;U2.duration=4/SECONDS;S.critical_strike_chance=5/PERCENT_ADDITIVE"),
                new Golden(DEFENDER, "P.life=15/PERCENT;U1.taunt_duration=6/SECONDS;U1.thorns_splinters=50/PERCENT_OF_THORNS;"
                        + "U2.blood_orb_chance=10/PERCENT_CHANCE;S.all_resistance=40/PERCENT_ADDITIVE"),
                new Golden(REAPER, "P.wind_up_interval=10/SECONDS;U1.cooldown_reduction=-3/SECONDS;U2.damage=50/PERCENT_MULTIPLICATIVE;"
                        + "U2.stun_chance=15/PERCENT_CHANCE;U2.stun_duration=1/SECONDS;S.player_damage=15/PERCENT_MULTIPLICATIVE"),
                new Golden(SHADOW_MAGE, "U1.corrupting_damage=100/SOURCE_DAMAGE_PERCENT;U1.duration=6/SECONDS;"
                        + "U2.barrier=3/PERCENT_OF_MAXIMUM_LIFE;U2.duration=4/SECONDS;U2.barrier_cap=30/PERCENT_OF_MAXIMUM_LIFE;"
                        + "S.essence_regeneration=20/PERCENT_ADDITIVE;S.maximum_essence=20/FLAT_AMOUNT"),
                new Golden(COLD_MAGE, "U1.forked_projectiles=2/COUNT;U1.weaken_duration=4/SECONDS;U2.blizzard_damage=200/SOURCE_DAMAGE_PERCENT;"
                        + "U2.duration=6/SECONDS;U2.chill=6/PERCENT_PER_SECOND;U2.vulnerable_duration=4/SECONDS;"
                        + "S.damage_to_vulnerable=20/PERCENT_MULTIPLICATIVE"),
                new Golden(BONE_MAGE, "U1.additional_projectiles=2/COUNT;U1.projectile_damage=75/PERCENT_OF_NORMAL_DAMAGE;"
                        + "U2.fortify=0.1/PERCENT_OF_MAXIMUM_LIFE;S.player_damage_with_overpower=20/PERCENT_MULTIPLICATIVE"),
                new Golden(BONE_GOLEM, "U1.corpses=5/COUNT;U2.minimum_interval=3/SECONDS;U2.spikes_damage=250/SOURCE_DAMAGE_PERCENT;"
                        + "U2.vulnerable_duration=4/SECONDS;S.attack_speed=10/PERCENT_ADDITIVE"),
                new Golden(BLOOD_GOLEM, "U1.life_per_minion=5/PERCENT;U1.damage_per_minion=5/PERCENT_MULTIPLICATIVE;U1.duration=20/SECONDS;"
                        + "U1.life_cap=50/PERCENT;U1.damage_cap=50/PERCENT_MULTIPLICATIVE;U2.fortify_per_enemy=10/PERCENT_OF_MAXIMUM_LIFE;"
                        + "S.maximum_life=20/PERCENT_MULTIPLICATIVE"),
                new Golden(IRON_GOLEM, "U1.shockwave_damage=125/SOURCE_DAMAGE_PERCENT_MULTIPLICATIVE;U2.size=50/PERCENT;"
                        + "S.critical_strike_damage=15/PERCENT_MULTIPLICATIVE"));
    }

    @ParameterizedTest
    @EnumSource(NecromancerMinionSpecializationId.class)
    void sacrificeShouldKeepPlayerBonusesSeparateFromSummonPenalty(NecromancerMinionSpecializationId id) {
        var sacrifice = BOOK.find(id).orElseThrow().choice(SACRIFICE);
        assertFalse(sacrifice.playerBonuses().isEmpty());
        assertTrue(sacrifice.effects().isEmpty());
        var penalty = sacrifice.summonPenalty().orElseThrow();
        assertEquals(new BigDecimal("50"), penalty.reductionPercent());
        assertEquals(id.family() == NecromancerMinionFamily.GOLEM
                        ? BookOfDeadSummonPenalty.Type.GOLEM_DAMAGE_REDUCTION_MULTIPLICATIVE
                        : BookOfDeadSummonPenalty.Type.SUMMON_COUNT_REDUCTION,
                penalty.type());
        assertTrue(penalty.description().contains("nadal można"));
        for (Class<?> record : List.of(BookOfDeadChoiceDefinition.class, BookOfDeadSummonPenalty.class,
                NecromancerMinionSpecializationDefinition.class, NecromancerMinionRules.class)) {
            assertTrue(Arrays.stream(record.getRecordComponents()).noneMatch(component ->
                    component.getName().contains("minionDisabled") || component.getType() == boolean.class
                            || component.getType() == Boolean.class));
        }
    }

    @Test
    void shouldResolveBoneMageFortifyUsingOfficialPatchInsteadOfManualReview() {
        var choice = BOOK.find(BONE_MAGE).orElseThrow().choice(UPGRADE_2);
        assertEquals(new BigDecimal("0.1"), fact(choice.effects(), "fortify").value());
        assertEquals(PERCENT_OF_MAXIMUM_LIFE, fact(choice.effects(), "fortify").unit());
        assertEquals(1, choice.sourceConflicts().size());
        var conflict = choice.sourceConflicts().getFirst();
        assertEquals("fortify", conflict.factId());
        assertEquals(new BigDecimal("3"), conflict.displayedValue());
        assertEquals(new BigDecimal("0.1"), conflict.canonicalValue());
        assertTrue(conflict.displayedSourceReference().startsWith("https://www.icy-veins.com/"));
        assertEquals("https://news.blizzard.com/en-us/article/24244466/diablo-iv-patch-notes-2-5", conflict.officialSourceReference());
        assertEquals(BookOfDeadChoiceDefinition.Resolution.OFFICIAL_SOURCE_RESOLVED, conflict.status());
        assertTrue(conflict.explanation().contains("Maximum Life"));
        assertEquals(1, BOOK.all().stream().flatMap(definition -> definition.choices().stream())
                .mapToInt(item -> item.sourceConflicts().size()).sum());
    }

    @Test
    void shouldKeepDefenderThornsDependentOnSourceContext() {
        var effects = BOOK.find(DEFENDER).orElseThrow().choice(UPGRADE_1).effects();
        var thorns = fact(effects, "thorns");
        assertNull(thorns.value());
        assertEquals(SOURCE_DEPENDENT_AMOUNT, thorns.unit());
        assertTrue(thorns.description().contains("source-dependent Thorns amount"));
        assertFalse(effects.toString().contains("3481"));
        assertEquals(PERCENT_OF_THORNS, fact(effects, "thorns_splinters").unit());
    }

    @Test
    void shouldKeepBoneMageExtraProjectilesRelativeToNormalDamage() {
        var projectile = fact(BOOK.find(BONE_MAGE).orElseThrow().choice(UPGRADE_1).effects(), "projectile_damage");
        assertEquals(PERCENT_OF_NORMAL_DAMAGE, projectile.unit());
        assertEquals(new BigDecimal("75"), projectile.value());
        assertTrue(projectile.description().contains("relative to normal damage"));
        assertTrue(projectile.description().contains("bez podstawy Weapon Damage"));
    }

    @Test
    void shouldPreserveTriggersRecipientsConditionsAndUnknownFrequency() {
        assertTrue(fact(BOOK.find(SKIRMISHER).orElseThrow().choice(UPGRADE_1).effects(), "aggression").description().contains("po przywołaniu"));
        assertTrue(fact(BOOK.find(SKIRMISHER).orElseThrow().choice(UPGRADE_2).effects(), "vulnerable").description().contains("Vulnerable"));
        assertTrue(fact(BOOK.find(REAPER).orElseThrow().choice(UPGRADE_1).effects(), "corpse").description().contains("Zwłoki"));
        assertTrue(fact(BOOK.find(SHADOW_MAGE).orElseThrow().passiveFacts(), "damage_type").description().contains("Shadow"));
        assertTrue(fact(BOOK.find(SHADOW_MAGE).orElseThrow().choice(UPGRADE_2).effects(), "recipients").description().contains("gracz oraz Mag Cienia"));
        assertTrue(fact(BOOK.find(COLD_MAGE).orElseThrow().passiveFacts(), "damage_type").description().contains("Cold"));
        assertTrue(fact(BOOK.find(COLD_MAGE).orElseThrow().choice(UPGRADE_1).effects(), "forked_projectiles").description().contains("bez założenia trzech trafień"));
        assertTrue(fact(BOOK.find(COLD_MAGE).orElseThrow().choice(UPGRADE_2).effects(), "blizzard_trigger").description().contains("częstotliwość nie jest określona"));
        assertTrue(fact(BOOK.find(BONE_MAGE).orElseThrow().choice(UPGRADE_2).effects(), "corpse_on_death").description().contains("Śmierć"));
        assertTrue(fact(BOOK.find(BONE_MAGE).orElseThrow().choice(SACRIFICE).playerBonuses(), "player_damage_with_overpower").description().contains("tylko gdy gracz ma stos Overpower"));
        assertTrue(fact(BOOK.find(BONE_GOLEM).orElseThrow().choice(UPGRADE_2).effects(), "trigger").description().contains("Po otrzymaniu obrażeń"));
        assertTrue(fact(BOOK.find(BLOOD_GOLEM).orElseThrow().choice(UPGRADE_1).effects(), "life_per_minion").description().contains("na każdego"));
        assertTrue(fact(BOOK.find(BLOOD_GOLEM).orElseThrow().choice(UPGRADE_2).effects(), "fortify_per_enemy").description().contains("na każdego wyssanego przeciwnika"));
        assertTrue(fact(BOOK.find(IRON_GOLEM).orElseThrow().choice(UPGRADE_1).effects(), "shockwave_damage").description().contains("głównemu celowi i przeciwnikom za nim"));
        assertTrue(fact(BOOK.find(IRON_GOLEM).orElseThrow().choice(UPGRADE_2).effects(), "pull").description().contains("Pull"));
    }

    @Test
    void shouldExposeGlobalRulesAsDescriptiveMetadata() {
        assertEquals(new BigDecimal("20"), BOOK.rules().baseLuckyHitChancePercent());
        assertEquals(new BigDecimal("100"), BOOK.rules().usefulPlayerStatsAndBuffsInheritancePercent());
        assertTrue(BOOK.rules().inheritanceDescription().contains("które są dla nich użyteczne"));
        assertTrue(BOOK.rules().summoningWithSacrificeRequirement().contains("wykupiony"));
        assertTrue(BOOK.rules().summoningWithSacrificeRequirement().contains("pasku umiejętności"));
    }

    @Test
    void shouldPreserveExistingCatalogsAndLegacySkillsAlongsideN10ProfileClasses() throws Exception {
        var necro = NecromancerSkillTreeRegistry.asCharacterRegistry();
        assertEquals(23, necro.allSkills().size());
        assertEquals(161, necro.allSkills().stream().flatMap(skill -> skill.getUpgradeGroups().stream())
                .mapToInt(group -> group.getUpgrades().size()).sum());
        assertEquals(24, PaladinSkillTreeRegistry.allSkills().size());
        assertArrayEquals(new HeroClass[]{HeroClass.PALADIN, HeroClass.NECROMANCER}, HeroClass.values());
        assertEquals(4, SkillId.values().length);
        // Granica domeny: produkcyjny Book ma wyłącznie zależności JDK i własnego pakietu.
        try (var sources = Files.list(Path.of("src/main/java/krys/necromancer/bookofthedead"))) {
            for (var source : sources.filter(path -> path.toString().endsWith(".java")).toList()) {
                for (String line : Files.readAllLines(source)) {
                    if (line.startsWith("import ")) {
                        String dependency = line.replace("import static ", "import ").substring(7);
                        assertTrue(dependency.startsWith("java.") || dependency.startsWith("krys.necromancer.bookofthedead."),
                                source + ": " + dependency);
                    }
                }
            }
        }
    }

    private static void collectNumeric(Map<String, String> facts, String prefix, List<BookOfDeadSourceFact> source) {
        for (var fact : source) {
            if (fact.value() != null) {
                facts.put(prefix + "." + fact.id(), fact.value().toPlainString() + "/" + fact.unit());
            }
        }
    }

    private static BookOfDeadSourceFact fact(List<BookOfDeadSourceFact> facts, String id) {
        return facts.stream().filter(fact -> fact.id().equals(id)).findFirst().orElseThrow();
    }

    private record Golden(NecromancerMinionSpecializationId id, String facts) { }
}

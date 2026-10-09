package krys.web;

import com.sun.net.httpserver.HttpServer;
import krys.necromancer.NecromancerSkillTreeRegistry;
import krys.ranking.CharacterSkillTreeRegistry;
import krys.ranking.DamageRankingService;
import krys.ranking.PlayableClass;
import krys.ranking.SkillTreeRegistryProvider;
import krys.skilltree.CharacterTreeSkill;
import krys.skilltree.UpgradeDamageModifier;
import krys.skilltree.UpgradeDamageModifierType;
import krys.skilltree.UpgradeDamageSafety;
import krys.skilltree.UpgradeDamageValueKind;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DamageRankingRowDecimalTest {
    @Test
    void shouldParseDecimalIntegerAndMultipleSourcePercentsWithoutLosingPrecision() {
        // Wartości źródłowe N1: impuls 7.5%, Selekcja Naturalna 62.5% i Premia do Obrażeń 50%.
        Map<String, BigDecimal> cases = Map.of("7.5%", new BigDecimal("7.5"), "62.5%", new BigDecimal("62.5"),
                "50%", new BigDecimal("50"), "7.5%; 62.5%; 50%", new BigDecimal("62.5"),
                "7,5%", new BigDecimal("7.5"));
        for (var expected : cases.entrySet()) {
            for (var type : List.of(UpgradeDamageModifierType.MULTIPLICATIVE_DAMAGE_PERCENT,
                    UpgradeDamageModifierType.ADDITIVE_DAMAGE_PERCENT, UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT,
                    UpgradeDamageModifierType.DAMAGE_OVER_TIME)) {
                var row = row(skill("decimal", expected.getKey(), type));
                var value = switch (type) {
                    case MULTIPLICATIVE_DAMAGE_PERCENT -> row.maxDamageMultiplierPercent();
                    case ADDITIVE_DAMAGE_PERCENT -> row.maxDamageBonusPercent();
                    case FLAT_COMPONENT_PERCENT -> row.maxExtraHitOrComponentPercent();
                    case DAMAGE_OVER_TIME -> row.maxDamageOverTimePercent();
                    default -> throw new IllegalStateException();
                };
                assertEquals(expected.getValue(), value, expected.getKey());
            }
        }
        assertNull(row(skill("without_percent", "odłamek", UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT)).maxExtraHitOrComponentPercent());
    }

    @Test
    void shouldSortDecimalMetricsThroughTheSharedHttpController() throws Exception {
        var registry = new CharacterSkillTreeRegistry(PlayableClass.NECROMANCER, "DecimalSourceFixture", List.of(
                skill("burst", "7.5%", UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT),
                skill("selection", "62.5%", UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT),
                skill("bonus", "50%", UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT),
                skill("missing", "odłamek", UpgradeDamageModifierType.FLAT_COMPONENT_PERCENT)));
        var provider = new SkillTreeRegistryProvider(List.of(registry));
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/ranking-obrazen", new DamageRankingController(new DamageRankingService(null, provider),
                new DamageRankingPageRenderer(), provider));
        server.start();
        try (var client = HttpClient.newHttpClient()) {
            for (var direction : List.of("asc", "desc")) {
                var uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort()
                        + "/ranking-obrazen?character=necromancer&sort=maxExtraHitOrComponentPercent&direction=" + direction);
                var response = client.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertEquals(200, response.statusCode());
                var expected = direction.equals("asc") ? List.of("burst", "bonus", "selection", "missing")
                        : List.of("selection", "bonus", "burst", "missing");
                int previous = -1;
                for (var id : expected) {
                    int position = response.body().indexOf("data-skill-id=\"" + id + "\"");
                    assertTrue(position > previous, direction + ": " + id);
                    previous = position;
                }
            }
        } finally {
            server.stop(0);
        }
    }

    private static DamageRankingRow row(CharacterTreeSkill skill) {
        var registry = new CharacterSkillTreeRegistry(PlayableClass.NECROMANCER, "DecimalSourceFixture", List.of(skill));
        var service = new DamageRankingService(null, new SkillTreeRegistryProvider(List.of(registry)));
        return new DamageRankingRow(service.describeTreeSkills(PlayableClass.NECROMANCER).getFirst(), skill,
                NecromancerSkillTreeRegistry.ESSENCE, "Podstawowe / Basic");
    }

    private static CharacterTreeSkill skill(String id, String value, UpgradeDamageModifierType type) {
        var reference = NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill("kosciane_odlamki");
        var modifier = new UpgradeDamageModifier("grupa_3", "Źródłowy komponent", UpgradeDamageSafety.YES,
                type, value, UpgradeDamageValueKind.COMPONENT_PERCENT, "R1", true, "Komponent", false,
                UpgradeDamageSafety.YES, UpgradeDamageSafety.NO, "Regresja wartości źródłowych N1: " + value);
        return new CharacterTreeSkill(id, id, reference.getSourceReference(), reference.getSkillGroup(),
                reference.getBaseDamagePercentAtRank1(), reference.getBaseDamagePercentAtTreeMaxRank(),
                reference.getBaseDamagePercentRanks(), reference.getComponentDamagePercentRanks(), reference.getType(),
                reference.getStatus(), List.of(), reference.getNotes(), reference.getSkillCategories(), reference.getTags(),
                reference.getResourceData(), reference.getLuckyHitPercent(), List.of(), List.of(modifier));
    }
}

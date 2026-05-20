package krys.paladin;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaladinOathRegistryTest {
    @Test
    void registry_powinien_zawierac_cztery_przysiegi_poza_runtime() {
        Map<PaladinOathId, PaladinOathDefinition> definitions = PaladinOathRegistry.all().stream()
                .collect(Collectors.toMap(PaladinOathDefinition::getId, definition -> definition));

        assertEquals(4, definitions.size());
        assertOath(definitions.get(PaladinOathId.ADEPT), "Adept", PaladinOathFamily.ADEPT);
        assertOath(definitions.get(PaladinOathId.JUDGE), "Sędzia", PaladinOathFamily.JUDGE);
        assertOath(definitions.get(PaladinOathId.JUGGERNAUT), "Moloch", PaladinOathFamily.JUGGERNAUT,
                PaladinOathRuntimeStatus.PARTIALLY_RUNTIME_ENABLED);
        assertOath(definitions.get(PaladinOathId.ZEALOT), "Zelota", PaladinOathFamily.ZEALOT);
    }

    @Test
    void source_md_powinien_zawierac_pelna_transkrypcje_przysieg() throws Exception {
        String source = Files.readString(Path.of(PaladinOathRegistry.SOURCE_REFERENCE), StandardCharsets.UTF_8);

        assertTrue(source.contains("Data źródła: 2026-05-19"));
        assertTrue(source.contains("Źródło: screeny użytkownika z gry"));
        assertTrue(source.contains("Runtime DPS nieaktywny w tym etapie"));
        assertTrue(source.contains("Adept"));
        assertTrue(source.contains("Sędzia"));
        assertTrue(source.contains("Moloch"));
        assertTrue(source.contains("Zelota"));
        assertTrue(source.contains("50%[x]"));
        assertTrue(source.contains("80% obrażeń od broni"));
        assertTrue(source.contains("8%[x]"));
        assertTrue(source.contains("60%[x]"));
        assertTrue(source.contains("17% obrażeń"));
        assertTrue(source.contains("1% twojego maksymalnego zdrowia"));
    }

    @Test
    void przysiegi_nie_sa_zwyklymi_skillami_drzewa_paladyna() {
        assertFalse(PaladinSkillTreeRegistry.findSkill("adept").isPresent());
        assertFalse(PaladinSkillTreeRegistry.findSkill("sedzia").isPresent());
        assertFalse(PaladinSkillTreeRegistry.findSkill("moloch").isPresent());
        assertFalse(PaladinSkillTreeRegistry.findSkill("zelota").isPresent());
    }

    private static void assertOath(PaladinOathDefinition definition,
                                   String displayName,
                                   PaladinOathFamily affectedFamily) {
        assertOath(definition, displayName, affectedFamily, PaladinOathRuntimeStatus.NOT_RUNTIME_ENABLED);
    }

    private static void assertOath(PaladinOathDefinition definition,
                                   String displayName,
                                   PaladinOathFamily affectedFamily,
                                   PaladinOathRuntimeStatus runtimeStatus) {
        assertEquals(displayName, definition.getDisplayName());
        assertEquals(affectedFamily, definition.getAffectedSkillFamily());
        assertEquals(runtimeStatus, definition.getRuntimeStatus());
        assertEquals(PaladinOathRegistry.SOURCE_REFERENCE, definition.getSourceReference());
        assertFalse(definition.getDescriptionLines().isEmpty());
    }
}

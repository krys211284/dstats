package krys.web;

import krys.necromancer.app.NecromancerCurrentBuildCalculationService;
import krys.runtime.RuntimeSkillLoadout;
import krys.hero.HeroClass;
import krys.hero.HeroClassCoreStatProgressions;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.stream.Collectors;

/** Query przechowuje kontekst INT/celu; jedynym źródłem wyboru skilla pozostaje loadout. */
final class NecromancerCurrentBuildFormQuerySupport {
    static final String LEGACY_INTELLIGENCE = "necromancerNonItemIntelligence";
    static final String INTELLIGENCE = "necromancerAdditionalNonItemIntelligence";
    static final String VULNERABLE = "necromancerTargetVulnerable";

    private NecromancerCurrentBuildFormQuerySupport() { }

    static NecromancerCurrentBuildFormData fromSerializedQuery(String query, RuntimeSkillLoadout loadout) {
        var fields = UrlEncodedFormSupport.parseQuery(query);
        String additional = fields.get(INTELLIGENCE);
        if (additional == null) {
            additional = fields.containsKey(LEGACY_INTELLIGENCE)
                    ? additionalFromLegacy(fields.get(LEGACY_INTELLIGENCE), baselineIntelligence(query)) : "0";
        }
        return new NecromancerCurrentBuildFormData(additional,
                Boolean.parseBoolean(fields.getOrDefault(VULNERABLE, "false")),
                loadout.configurations().containsKey(NecromancerCurrentBuildCalculationService.KRWOTOK) ? "krwotok" : "");
    }

    static String withFormData(String existingQuery, NecromancerCurrentBuildFormData form) {
        var fields = UrlEncodedFormSupport.parseQuery(existingQuery);
        fields.remove(LEGACY_INTELLIGENCE);
        fields.put(INTELLIGENCE, form.additionalNonItemIntelligence().isBlank() ? "0" : form.additionalNonItemIntelligence());
        fields.put(VULNERABLE, Boolean.toString(form.targetVulnerable()));
        return fields.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
    }

    static int level(String query) {
        return Integer.parseInt(UrlEncodedFormSupport.parseQuery(query).getOrDefault("level", "70"));
    }

    static int baselineIntelligence(String query) {
        return HeroClassCoreStatProgressions.get(HeroClass.NECROMANCER).atLevel(level(query)).intelligence();
    }

    /** Stare pole N11 zawierało również baseline; wartości niższe od niego nie tworzą ujemnego dodatku. */
    static String additionalFromLegacy(String legacy, int baseline) {
        try {
            double total = new NecromancerCurrentBuildFormData(legacy, false, "").parsedAdditionalNonItemIntelligence();
            return BigDecimal.valueOf(total).subtract(BigDecimal.valueOf(baseline)).max(BigDecimal.ZERO).stripTrailingZeros().toPlainString();
        } catch (IllegalArgumentException exception) {
            return legacy == null ? "" : legacy;
        }
    }

    static String migrationNote(String query) {
        var fields = UrlEncodedFormSupport.parseQuery(query);
        if (fields.containsKey(INTELLIGENCE) || !fields.containsKey(LEGACY_INTELLIGENCE)) { return ""; }
        String legacy = fields.get(LEGACY_INTELLIGENCE);
        int baseline = baselineIntelligence(query);
        String additional = additionalFromLegacy(legacy, baseline);
        return "Odczyt starszego zapisu N11: non-item Intelligence " + legacy + " obejmowała baseline. "
                + "Przy baseline " + baseline + " odpowiada dodatkowej Intelligence " + additional
                + ". Wartości niższe od baseline'u oznaczają dodatkową INT 0. Zapis zmian utrwali nowe pole.";
    }
}

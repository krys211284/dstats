package krys.web;

import krys.necromancer.app.NecromancerCurrentBuildCalculationService;
import krys.runtime.RuntimeSkillLoadout;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/** Query przechowuje kontekst INT/celu; jedynym źródłem wyboru skilla pozostaje loadout. */
final class NecromancerCurrentBuildFormQuerySupport {
    static final String INTELLIGENCE = "necromancerNonItemIntelligence";
    static final String VULNERABLE = "necromancerTargetVulnerable";

    private NecromancerCurrentBuildFormQuerySupport() { }

    static NecromancerCurrentBuildFormData fromSerializedQuery(String query, RuntimeSkillLoadout loadout) {
        var fields = UrlEncodedFormSupport.parseQuery(query);
        return new NecromancerCurrentBuildFormData(fields.getOrDefault(INTELLIGENCE, "0"),
                Boolean.parseBoolean(fields.getOrDefault(VULNERABLE, "false")),
                loadout.configurations().containsKey(NecromancerCurrentBuildCalculationService.KRWOTOK) ? "krwotok" : "");
    }

    static String withFormData(String existingQuery, NecromancerCurrentBuildFormData form) {
        var fields = UrlEncodedFormSupport.parseQuery(existingQuery);
        fields.put(INTELLIGENCE, form.nonItemIntelligence().isBlank() ? "0" : form.nonItemIntelligence());
        fields.put(VULNERABLE, Boolean.toString(form.targetVulnerable()));
        return fields.entrySet().stream().map(entry -> URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                + "=" + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8)).collect(Collectors.joining("&"));
    }
}

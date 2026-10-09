package krys.web;

import krys.necromancer.NecromancerSkillTreeRegistry;
import krys.necromancer.app.NecromancerCurrentBuildCalculationService;

import java.math.BigDecimal;

/** Minimalny SSR N11; wspólny paper-doll dostarcza dotychczasowy renderer Current Build. */
final class NecromancerCurrentBuildPageRenderer {
    private NecromancerCurrentBuildPageRenderer() { }

    static String renderEntry(CurrentBuildPageModel model, String equipment) {
        var form = model.getNecromancerFormData();
        var configuration = model.getActiveHero().getRuntimeSkillLoadout().configurations().get(NecromancerCurrentBuildCalculationService.KRWOTOK);
        String warning = configuration != null && !configuration.selections().isEmpty()
                ? "<p class=\"helper\">Obecna konfiguracja Krwotoku zawiera upgrade'y, które nie są jeszcze wykonywane w N11. Obliczenie zostanie odrzucone.</p>" : "";
        var coefficient = NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill("krwotok").getBaseDamagePercentRanks().damagePercentAtRank(1);
        return """
                <section class="panel panel-build-workspace">
                    <h2>N11 — bazowy direct hit Krwotoku R1</h2>
                    <p class="helper">PRE_TARGET_MITIGATION, Season 15 / live 3.2.2. Współczynnik Krwotoku R1: {{COEFFICIENT}}% z katalogu. Średnie obrażenia pochodzą z aktywnej broni; Intelligence = non-item + aktywne itemy.</p>
                    <p class="helper">N11 jeszcze nie uwzględnia: upgrade'ów, Blood Orb, Essence, Lucky Hit, Overpower, Book of the Dead, enemy mitigation, Critical Strike Chance / expected damage, innych affixów damage poza Intelligence ani DPS/symulacji.</p>
                    {{WARNING}}
                    <form method="post" action="/policz-aktualny-build">
                        <div class="current-build-sticky-actions"><div class="current-build-sticky-buttons">
                            <button type="submit" name="formAction" value="calculate">Oblicz aktualny build</button>
                            <button type="submit" name="formAction" value="save">Zapisz zmiany</button>
                            <a class="nav-link secondary-link" href="/policz-aktualny-build">Wycofaj zmiany</a>
                        </div></div>
                        <section class="layer-panel layer-panel-hero">
                            <h3>Krwotok R1 — pojedyncze trafienie</h3>
                            <label>Umiejętność <select name="selectedSkillId">
                                <option value=""{{NONE}}>Brak umiejętności</option>
                                <option value="krwotok"{{KRWOTOK}}>Krwotok — ranga 1</option>
                            </select></label>
                            <label>Inteligencja poza aktywnymi itemami
                                <input type="number" min="0" step="any" name="nonItemIntelligence" value="{{INT}}">
                            </label>
                            <p class="helper">Podaj Intelligence ze wszystkich źródeł poza aktywnymi itemami biblioteki, np. stan postaci i Paragon. Aplikacja nie dodaje niezweryfikowanej bazowej INT klasy.</p>
                            <label><input type="checkbox" name="targetVulnerable" value="true"{{VULNERABLE}}>Cel Vulnerable / Odsłonięty</label>
                        </section>
                        {{EQUIPMENT}}
                    </form>
                </section>
                """.replace("{{COEFFICIENT}}", coefficient.toString())
                .replace("{{WARNING}}", warning)
                .replace("{{NONE}}", form.selectedSkillId().isEmpty() ? " selected" : "")
                .replace("{{KRWOTOK}}", "krwotok".equals(form.selectedSkillId()) ? " selected" : "")
                .replace("{{INT}}", CurrentBuildCalculationSectionsRenderer.escapeHtml(form.nonItemIntelligence()))
                .replace("{{VULNERABLE}}", form.targetVulnerable() ? " checked" : "")
                .replace("{{EQUIPMENT}}", equipment);
    }

    static String renderResult(CurrentBuildPageModel model) {
        var calculation = model.getNecromancerCalculation();
        if (calculation == null) { return ""; }
        var profile = calculation.profile();
        var result = calculation.result();
        var form = model.getNecromancerFormData();
        return "<section class=\"panel\" id=\"necromancer-direct-hit-result\"><h2>Wynik Krwotoku — PRE_TARGET_MITIGATION</h2><div class=\"summary-grid\">"
                + card("Umiejętność", "Krwotok") + card("Ranga", Integer.toString(calculation.configuration().rank()))
                + card("Bazowy coefficient z katalogu", result.components().getFirst().spec().damagePercent().stripTrailingZeros().toPlainString() + "%")
                + card("Średnie obrażenia aktywnej broni", number(profile.averageWeaponDamage()))
                + card("Inteligencja poza aktywnymi itemami", number(form.parsedNonItemIntelligence()))
                + card("Inteligencja z aktywnych itemów", number(model.getActiveLibraryContribution().getIntelligence()))
                + card("Efektywna inteligencja", number(profile.intelligence()))
                + card("Mnożnik main stat", number(result.context().mainStatMultiplier()))
                + card("Cel Vulnerable / Odsłonięty", profile.targetVulnerable() ? "tak" : "nie")
                + card("Mnożnik Vulnerable", number(result.context().vulnerableMultiplier()))
                + card("Normal direct hit", Long.toString(result.finalDamage()))
                + card("Critical direct hit", Long.toString(result.criticalDamage()))
                + card("Granica wyniku", "PRE_TARGET_MITIGATION") + "</div></section>";
    }

    private static String card(String label, String value) { return CurrentBuildCalculationSectionsRenderer.renderSummaryCard(label, value); }
    private static String number(double value) { return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }
}

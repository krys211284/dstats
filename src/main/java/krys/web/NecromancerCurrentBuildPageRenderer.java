package krys.web;

import krys.necromancer.NecromancerSkillTreeRegistry;
import krys.necromancer.app.NecromancerCurrentBuildCalculationService;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Minimalny SSR N11; wspólny paper-doll dostarcza dotychczasowy renderer Current Build. */
final class NecromancerCurrentBuildPageRenderer {
    private NecromancerCurrentBuildPageRenderer() { }

    static String renderEntry(CurrentBuildPageModel model, String equipment, String heroStats) {
        var form = model.getNecromancerFormData();
        var configuration = model.getActiveHero().getRuntimeSkillLoadout().configurations().get(NecromancerCurrentBuildCalculationService.KRWOTOK);
        String warning = configuration != null && !configuration.selections().isEmpty()
                ? "<p class=\"helper\">Obecna konfiguracja Krwotoku zawiera upgrade'y, które nie są jeszcze wykonywane w N11. Obliczenie zostanie odrzucone.</p>" : "";
        var coefficient = NecromancerSkillTreeRegistry.asCharacterRegistry().requireSkill("krwotok").getBaseDamagePercentRanks().damagePercentAtRank(1);
        return """
                <section class="panel panel-build-workspace">
                    <h2>N11 — bazowy direct hit Krwotoku R1</h2>
                    <p class="helper">PRE_TARGET_MITIGATION, Season 15 / live 3.2.2. Współczynnik Krwotoku R1: {{COEFFICIENT}}% z katalogu. Średnie obrażenia pochodzą z aktywnej broni; Intelligence = baseline klasy/poziomu + dodatkowa non-item + aktywne itemy.</p>
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
                            <label>Dodatkowa Inteligencja poza baseline'em i aktywnymi itemami
                                <input type="number" min="0" step="any" name="additionalNonItemIntelligence" value="{{INT}}">
                            </label>
                            <p class="helper">Bazowa Intelligence wynika z klasy i poziomu ({{BASELINE_INT}}). Podaj tylko dodatki spoza tego baseline'u i aktywnych itemów. Dla nagiej postaci pozostaw 0.</p>
                            {{MIGRATION_NOTE}}
                            <label><input type="checkbox" name="targetVulnerable" value="true"{{VULNERABLE}}>Cel Vulnerable / Odsłonięty</label>
                        </section>
                        {{HERO_STATS}}
                        {{EQUIPMENT}}
                    </form>
                </section>
                """.replace("{{COEFFICIENT}}", coefficient.toString())
                .replace("{{WARNING}}", warning)
                .replace("{{NONE}}", form.selectedSkillId().isEmpty() ? " selected" : "")
                .replace("{{KRWOTOK}}", "krwotok".equals(form.selectedSkillId()) ? " selected" : "")
                .replace("{{INT}}", CurrentBuildCalculationSectionsRenderer.escapeHtml(form.additionalNonItemIntelligence()))
                .replace("{{BASELINE_INT}}", Integer.toString(NecromancerCurrentBuildFormQuerySupport.baselineIntelligence(model.getActiveHero().getCurrentBuildQuery())))
                .replace("{{MIGRATION_NOTE}}", migrationNote(model))
                .replace("{{VULNERABLE}}", form.targetVulnerable() ? " checked" : "")
                .replace("{{EQUIPMENT}}", equipment)
                .replace("{{HERO_STATS}}", heroStats);
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
                + card("Inteligencja baseline klasy/poziomu", Integer.toString(NecromancerCurrentBuildFormQuerySupport.baselineIntelligence(model.getActiveHero().getCurrentBuildQuery())))
                + card("Dodatkowa Inteligencja poza baseline'em i aktywnymi itemami", number(form.parsedAdditionalNonItemIntelligence()))
                + card("Inteligencja z aktywnych itemów", number(model.getActiveLibraryContribution().getIntelligence()))
                + card("Efektywna inteligencja", number(profile.intelligence()))
                + card("Mnożnik main stat", number(result.context().mainStatMultiplier()))
                + card("Cel Vulnerable / Odsłonięty", profile.targetVulnerable() ? "tak" : "nie")
                + card("Mnożnik Vulnerable", number(result.context().vulnerableMultiplier()))
                + card("Normal direct hit", Long.toString(result.finalDamage()))
                + card("Critical direct hit", Long.toString(result.criticalDamage()))
                + card("Granica wyniku", "PRE_TARGET_MITIGATION") + "</div></section>";
    }

    static String renderCoreStatFacts(CurrentHeroStatsPresentation stats) {
        var facts = stats.getClassLevelCoreStatFacts(new BigDecimal("0.1625"));
        return "<section class=\"subpanel\"><h3>Wpływ bazowych core stats klasy i poziomu</h3><div class=\"summary-grid\">"
                + card("Pancerz z bazowej Siły", facts.armorFromStrength().toPlainString())
                + card("Obrażenia umiejętności z bazowej Inteligencji", percent(facts.skillDamagePercent()))
                + card("Odporności z bazowej Inteligencji", facts.allResistance().setScale(0, RoundingMode.HALF_UP).toPlainString())
                + card("Generowanie zasobów z bazowej Siły woli", percent(facts.resourceGenerationPercent()))
                + card("Otrzymywane leczenie z bazowej Siły woli", percent(facts.healingReceivedPercent()))
                + card("Szansa krytyczna z bazowej Zręczności", percent(facts.criticalChancePercent()))
                + card("Szansa uniku z bazowej Zręczności", percent(facts.dodgeChancePercent())) + "</div></section>";
    }

    private static String migrationNote(CurrentBuildPageModel model) {
        String note = NecromancerCurrentBuildFormQuerySupport.migrationNote(model.getActiveHero().getCurrentBuildQuery());
        return note.isEmpty() ? "" : "<p class=\"helper\">" + CurrentBuildCalculationSectionsRenderer.escapeHtml(note) + "</p>";
    }

    private static String percent(BigDecimal value) { return value.setScale(1, RoundingMode.HALF_UP).toPlainString().replace('.', ',') + "%"; }

    private static String card(String label, String value) { return CurrentBuildCalculationSectionsRenderer.renderSummaryCard(label, value); }
    private static String number(double value) { return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString(); }
}

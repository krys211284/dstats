package krys.web;

import krys.itemlibrary.EffectiveCurrentBuildResolution;
import krys.necromancer.combat.NecromancerSeason15DirectDamageProfile;

/** Projektuje wyłącznie średnie obrażenia broni i efektywną INT z istniejącej biblioteki. */
final class NecromancerCurrentBuildInputResolver {
    NecromancerSeason15DirectDamageProfile resolve(HeroProfile hero, NecromancerCurrentBuildFormData form, EffectiveCurrentBuildResolution resolution) {
        long weapon = CurrentBuildWeaponDamageResolver.resolve(resolution.getActiveHeroItemStats(), resolution.getActiveItemsContribution());
        if (weapon <= 0L) {
            throw new IllegalArgumentException("Wybierz aktywną broń z poprawnymi średnimi obrażeniami.");
        }
        double intelligence = NecromancerCurrentBuildFormQuerySupport.baselineIntelligence(hero.getCurrentBuildQuery())
                + form.parsedAdditionalNonItemIntelligence() + resolution.getActiveItemsContribution().getIntelligence();
        return new NecromancerSeason15DirectDamageProfile(weapon, intelligence, 0, 0, 1, 0, 0, 0, 0, form.targetVulnerable());
    }
}

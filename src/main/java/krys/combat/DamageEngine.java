package krys.combat;

import krys.skill.CriticalRoundingPolicy;
import krys.hero.Hero;
import krys.hero.HeroClassDef;
import krys.hero.HeroClassDefs;
import krys.item.Item;
import krys.item.ItemStatType;
import krys.simulation.HeroBuildSnapshot;
import krys.skill.EffectType;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillDef;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeEffect;
import krys.skill.SkillState;
import krys.skill.StatusId;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Minimalny wspólny silnik obrażeń.
 * Już na tym etapie jest data-driven: skill dostarcza bazowy procent i listę efektów runtime.
 */
public final class DamageEngine {
    private static final double BASE_CRIT_DAMAGE_BONUS = 0.50d;
    private static final double CRIT_DAMAGE_FROM_INTELLIGENCE_FACTOR = 0.0004d;
    private static final double VULNERABLE_MULTIPLIER = 1.20d;

    public ReactiveHitBreakdown calculateReactiveHit(HeroBuildSnapshot snapshot, int triggeredSecond) {
        Hero hero = snapshot.getHero();
        HeroClassDef classDef = HeroClassDefs.get(hero.getHeroClass());
        List<Item> equippedItems = snapshot.getEquippedItems();

        double baseThornsFromBuild = Item.sumStat(equippedItems, ItemStatType.THORNS);
        double mainStatMultiplier = classDef.resolveMainStatMultiplier(hero.getLevel(), equippedItems);
        double blockChance = Item.sumStat(equippedItems, ItemStatType.BLOCK_CHANCE) / 100.0d;
        double retributionChance = Item.sumStat(equippedItems, ItemStatType.RETRIBUTION_CHANCE) / 100.0d;
        double levelDamageReduction = Math.min(85.0d, hero.getLevel() + 25.0d) / 100.0d;

        double thornsRawExact = baseThornsFromBuild * mainStatMultiplier;
        double retributionRawExact = thornsRawExact * blockChance * retributionChance;

        long thornsRawDamage = Math.round(thornsRawExact);
        long thornsFinalDamage = Math.round(thornsRawExact * (1.0d - levelDamageReduction));
        long retributionExpectedRawDamage = Math.round(retributionRawExact);
        long retributionExpectedFinalDamage = Math.round(retributionRawExact * (1.0d - levelDamageReduction));
        long reactiveFinalDamage = thornsFinalDamage + retributionExpectedFinalDamage;

        return new ReactiveHitBreakdown(
                triggeredSecond,
                baseThornsFromBuild,
                mainStatMultiplier,
                blockChance,
                retributionChance,
                thornsRawDamage,
                thornsFinalDamage,
                retributionExpectedRawDamage,
                retributionExpectedFinalDamage,
                reactiveFinalDamage
        );
    }

    public boolean hasReactiveFoundation(HeroBuildSnapshot snapshot) {
        List<Item> equippedItems = snapshot.getEquippedItems();
        return Item.sumStat(equippedItems, ItemStatType.THORNS) > 0.0d
                || Item.sumStat(equippedItems, ItemStatType.BLOCK_CHANCE) > 0.0d
                || Item.sumStat(equippedItems, ItemStatType.RETRIBUTION_CHANCE) > 0.0d;
    }

    public DamageBreakdown calculate(HeroBuildSnapshot snapshot, SkillId skillId, EnumSet<StatusId> targetStatuses) {
        SkillState state = snapshot.getSkillState(skillId);
        if (state == null || state.getRank() <= 0) {
            throw new IllegalArgumentException("Snapshot nie zawiera aktywnego stanu dla skilla " + skillId);
        }

        SkillDef skillDef = PaladinSkillDefs.get(skillId);
        Hero hero = snapshot.getHero();
        HeroClassDef classDef = HeroClassDefs.get(hero.getHeroClass());
        List<Item> equippedItems = snapshot.getEquippedItems();

        double weaponMultiplier = 1.0d + (Item.sumStat(equippedItems, ItemStatType.MAIN_HAND_WEAPON_DAMAGE) / 100.0d);
        double mainStat = classDef.resolveTotalMainStat(hero.getLevel(), equippedItems);
        double mainStatMultiplier = classDef.resolveMainStatMultiplier(hero.getLevel(), equippedItems);
        double intelligence = classDef.resolveTotalIntelligence(hero.getLevel(), equippedItems);
        double critDamageBonusFromItems = Item.sumStat(equippedItems, ItemStatType.CRIT_DAMAGE) / 100.0d;
        double critDamageBonusFromIntelligence = intelligence * CRIT_DAMAGE_FROM_INTELLIGENCE_FACTOR;
        double critDamageBonusTotal = BASE_CRIT_DAMAGE_BONUS + critDamageBonusFromItems + critDamageBonusFromIntelligence;
        double critMultiplier = 1.0d + critDamageBonusTotal;
        double additivePercent = snapshot.getTotalPercentDamageBonus();
        double additiveMultiplier = 1.0d + (additivePercent / 100.0d);
        double vulnerableMultiplier = targetStatuses.contains(StatusId.VULNERABLE) ? VULNERABLE_MULTIPLIER : 1.0d;
        double levelDamageReduction = Math.min(85.0d, hero.getLevel() + 25.0d) / 100.0d;

        long baseSkillDamagePercent = resolveBaseSkillDamagePercent(skillDef, state, targetStatuses);
        long baseDamage = Math.round(snapshot.getAverageWeaponDamage() * (baseSkillDamagePercent / 100.0d));

        List<DamageComponentBreakdown> components = new ArrayList<>();
        double totalRawNormal = 0.0d;
        double totalRawCrit = 0.0d;

        DamageComponentBreakdown baseComponent = createComponent(
                "Główny hit",
                "bazowy",
                baseSkillDamagePercent,
                1,
                null,
                true,
                true,
                null,
                snapshot.getAverageWeaponDamage(),
                weaponMultiplier,
                mainStatMultiplier,
                additiveMultiplier,
                vulnerableMultiplier,
                critMultiplier,
                levelDamageReduction
        );
        components.add(baseComponent);
        totalRawNormal += exactRawDamage(
                snapshot.getAverageWeaponDamage(),
                baseSkillDamagePercent,
                1,
                weaponMultiplier,
                mainStatMultiplier,
                additiveMultiplier,
                vulnerableMultiplier,
                1.0d
        );
        totalRawCrit += exactRawDamage(
                snapshot.getAverageWeaponDamage(),
                baseSkillDamagePercent,
                1,
                weaponMultiplier,
                mainStatMultiplier,
                additiveMultiplier,
                vulnerableMultiplier,
                critMultiplier
        );

        for (SkillRuntimeEffect effect : skillDef.getChoiceEffects(state.getChoiceUpgrade())) {
            if (effect.getEffectType() != EffectType.DAMAGE) {
                continue;
            }
            boolean active = isEffectActive(effect, targetStatuses);
            String conditionLabel = effect.getConditionStatus() == StatusId.NONE ? null : effect.getConditionStatus().name();
            boolean includedInSingleTarget = effect.isIncludedInSingleTarget();
            String exclusionReason = includedInSingleTarget ? null : "Komponent nie trafia głównego celu w modelu single target";
            DamageComponentBreakdown component = createComponent(
                    effect.getComponentName(),
                    "modyfikator",
                    effect.getSkillDamagePercent(),
                    effect.getHitCount(),
                    conditionLabel,
                    active,
                    includedInSingleTarget,
                    exclusionReason,
                    snapshot.getAverageWeaponDamage(),
                    weaponMultiplier,
                    mainStatMultiplier,
                    additiveMultiplier,
                    vulnerableMultiplier,
                    critMultiplier,
                    levelDamageReduction
            );
            components.add(component);
            if (active && includedInSingleTarget) {
                totalRawNormal += exactRawDamage(
                        snapshot.getAverageWeaponDamage(),
                        effect.getSkillDamagePercent(),
                        effect.getHitCount(),
                        weaponMultiplier,
                        mainStatMultiplier,
                        additiveMultiplier,
                        vulnerableMultiplier,
                        1.0d
                );
                totalRawCrit += exactRawDamage(
                        snapshot.getAverageWeaponDamage(),
                        effect.getSkillDamagePercent(),
                        effect.getHitCount(),
                        weaponMultiplier,
                        mainStatMultiplier,
                        additiveMultiplier,
                        vulnerableMultiplier,
                        critMultiplier
                );
            }
        }

        long rawDamage = Math.round(totalRawNormal);
        long finalDamage = Math.round(totalRawNormal * (1.0d - levelDamageReduction));
        CriticalDamageResult criticalDamageResult = resolveCriticalDamage(skillDef, state, rawDamage, totalRawCrit, critMultiplier, levelDamageReduction);

        return new DamageBreakdown(
                baseDamage,
                rawDamage,
                finalDamage,
                criticalDamageResult.rawCriticalDamage(),
                criticalDamageResult.criticalDamage(),
                weaponMultiplier,
                mainStat,
                mainStatMultiplier,
                additivePercent,
                additiveMultiplier,
                vulnerableMultiplier,
                critMultiplier,
                critDamageBonusTotal,
                critDamageBonusFromItems,
                critDamageBonusFromIntelligence,
                intelligence,
                levelDamageReduction,
                state.getChoiceUpgrade().getDisplayName(),
                components
        );
    }

    public DamageBreakdown calculateStandaloneHit(HeroBuildSnapshot snapshot,
                                                  long skillDamagePercent,
                                                  String componentName,
                                                  String source,
                                                  EnumSet<StatusId> targetStatuses) {
        Hero hero = snapshot.getHero();
        HeroClassDef classDef = HeroClassDefs.get(hero.getHeroClass());
        List<Item> equippedItems = snapshot.getEquippedItems();

        double weaponMultiplier = 1.0d + (Item.sumStat(equippedItems, ItemStatType.MAIN_HAND_WEAPON_DAMAGE) / 100.0d);
        double mainStat = classDef.resolveTotalMainStat(hero.getLevel(), equippedItems);
        double mainStatMultiplier = classDef.resolveMainStatMultiplier(hero.getLevel(), equippedItems);
        double intelligence = classDef.resolveTotalIntelligence(hero.getLevel(), equippedItems);
        double critDamageBonusFromItems = Item.sumStat(equippedItems, ItemStatType.CRIT_DAMAGE) / 100.0d;
        double critDamageBonusFromIntelligence = intelligence * CRIT_DAMAGE_FROM_INTELLIGENCE_FACTOR;
        double critDamageBonusTotal = BASE_CRIT_DAMAGE_BONUS + critDamageBonusFromItems + critDamageBonusFromIntelligence;
        double critMultiplier = 1.0d + critDamageBonusTotal;
        double additivePercent = snapshot.getTotalPercentDamageBonus();
        double additiveMultiplier = 1.0d + (additivePercent / 100.0d);
        double vulnerableMultiplier = targetStatuses.contains(StatusId.VULNERABLE) ? VULNERABLE_MULTIPLIER : 1.0d;
        double levelDamageReduction = Math.min(85.0d, hero.getLevel() + 25.0d) / 100.0d;

        long baseDamage = Math.round(snapshot.getAverageWeaponDamage() * (skillDamagePercent / 100.0d));
        DamageComponentBreakdown component = createComponent(
                componentName,
                source,
                skillDamagePercent,
                1,
                null,
                true,
                true,
                null,
                snapshot.getAverageWeaponDamage(),
                weaponMultiplier,
                mainStatMultiplier,
                additiveMultiplier,
                vulnerableMultiplier,
                critMultiplier,
                levelDamageReduction
        );

        double rawNormalExact = exactRawDamage(
                snapshot.getAverageWeaponDamage(),
                skillDamagePercent,
                1,
                weaponMultiplier,
                mainStatMultiplier,
                additiveMultiplier,
                vulnerableMultiplier,
                1.0d
        );
        double rawCritExact = exactRawDamage(
                snapshot.getAverageWeaponDamage(),
                skillDamagePercent,
                1,
                weaponMultiplier,
                mainStatMultiplier,
                additiveMultiplier,
                vulnerableMultiplier,
                critMultiplier
        );

        return new DamageBreakdown(
                baseDamage,
                Math.round(rawNormalExact),
                Math.round(rawNormalExact * (1.0d - levelDamageReduction)),
                Math.round(rawCritExact),
                Math.round(rawCritExact * (1.0d - levelDamageReduction)),
                weaponMultiplier,
                mainStat,
                mainStatMultiplier,
                additivePercent,
                additiveMultiplier,
                vulnerableMultiplier,
                critMultiplier,
                critDamageBonusTotal,
                critDamageBonusFromItems,
                critDamageBonusFromIntelligence,
                intelligence,
                levelDamageReduction,
                "Brak",
                List.of(component)
        );
    }

    /**
     * README w sekcjach o single hit i golden values wymaga, aby dla Brandish rank 5
     * z prawym modyfikatorem raw crit hit był liczony od już zaokrąglonego raw hit.
     * To nie jest przypadkowy wyjątek w pipeline, tylko jawna polityka kontraktowa foundation.
     */
    private static CriticalDamageResult resolveCriticalDamage(SkillDef skillDef,
                                                             SkillState state,
                                                             long rawDamage,
                                                             double totalRawCritExact,
                                                             double critMultiplier,
                                                             double levelDamageReduction) {
        CriticalRoundingPolicy roundingPolicy = skillDef.getCriticalRoundingPolicy(state.getChoiceUpgrade());
        if (roundingPolicy == CriticalRoundingPolicy.ROUNDED_RAW_HIT) {
            long rawCriticalDamage = Math.round(rawDamage * critMultiplier);
            long criticalDamage = Math.round(rawCriticalDamage * (1.0d - levelDamageReduction));
            return new CriticalDamageResult(rawCriticalDamage, criticalDamage);
        }

        long rawCriticalDamage = Math.round(totalRawCritExact);
        long criticalDamage = Math.round(totalRawCritExact * (1.0d - levelDamageReduction));
        return new CriticalDamageResult(rawCriticalDamage, criticalDamage);
    }

    private static long resolveBaseSkillDamagePercent(SkillDef skillDef, SkillState state, EnumSet<StatusId> targetStatuses) {
        long basePercent = skillDef.getBaseSkillDamagePercent(state.getRank());
        for (SkillRuntimeEffect effect : skillDef.getChoiceEffects(state.getChoiceUpgrade())) {
            if (effect.getEffectType() == EffectType.REPLACE_BASE_DAMAGE && isEffectActive(effect, targetStatuses)) {
                basePercent = effect.getSkillDamagePercent();
            }
        }
        return basePercent;
    }

    private static boolean isEffectActive(SkillRuntimeEffect effect, EnumSet<StatusId> targetStatuses) {
        return effect.getConditionStatus() == StatusId.NONE || targetStatuses.contains(effect.getConditionStatus());
    }

    private static DamageComponentBreakdown createComponent(String name,
                                                            String source,
                                                            long skillDamagePercent,
                                                            int hitCount,
                                                            String conditionLabel,
                                                            boolean active,
                                                            boolean includedInSingleTarget,
                                                            String exclusionReason,
                                                            long weaponDamage,
                                                            double weaponMultiplier,
                                                            double mainStatMultiplier,
                                                            double additiveMultiplier,
                                                            double vulnerableMultiplier,
                                                            double critMultiplier,
                                                            double levelDamageReduction) {
        double rawNormalExact = active
                ? exactRawDamage(weaponDamage, skillDamagePercent, hitCount, weaponMultiplier, mainStatMultiplier, additiveMultiplier, vulnerableMultiplier, 1.0d)
                : 0.0d;
        double rawCritExact = active
                ? exactRawDamage(weaponDamage, skillDamagePercent, hitCount, weaponMultiplier, mainStatMultiplier, additiveMultiplier, vulnerableMultiplier, critMultiplier)
                : 0.0d;
        return new DamageComponentBreakdown(
                name,
                source,
                skillDamagePercent,
                hitCount,
                conditionLabel,
                active,
                includedInSingleTarget,
                exclusionReason,
                Math.round(rawNormalExact),
                Math.round(rawNormalExact * (1.0d - levelDamageReduction)),
                Math.round(rawCritExact),
                Math.round(rawCritExact * (1.0d - levelDamageReduction))
        );
    }

    private static double exactRawDamage(long weaponDamage,
                                         long skillDamagePercent,
                                         int hitCount,
                                         double weaponMultiplier,
                                         double mainStatMultiplier,
                                         double additiveMultiplier,
                                         double vulnerableMultiplier,
                                         double critMultiplier) {
        return weaponDamage
                * (skillDamagePercent / 100.0d)
                * weaponMultiplier
                * mainStatMultiplier
                * additiveMultiplier
                * vulnerableMultiplier
                * critMultiplier
                * Math.max(1, hitCount);
    }

    private record CriticalDamageResult(long rawCriticalDamage, long criticalDamage) {
    }
}

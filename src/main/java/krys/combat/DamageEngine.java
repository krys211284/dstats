package krys.combat;

import krys.runtime.legacy.LegacyPaladinRuntimeSkillKeyAdapter;

import krys.skill.CriticalRoundingPolicy;
import krys.hero.Hero;
import krys.hero.HeroClassDef;
import krys.hero.HeroClassDefs;
import krys.item.Item;
import krys.item.ItemStatType;
import krys.paladin.PaladinSkillTreeRegistry;
import krys.paladin.SkillCategory;
import krys.simulation.HeroBuildSnapshot;
import krys.skill.EffectType;
import krys.skill.PaladinSkillDefs;
import krys.skill.SkillDef;
import krys.skill.SkillId;
import krys.skill.SkillRuntimeEffect;
import krys.skill.SkillState;
import krys.skill.StatusId;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Adapter legacy Paladyna: rozwiązuje statystyki, itemy, efekty i politykę zaokrągleń.
 * Czystą matematykę direct hit deleguje do neutralnego DirectDamageCalculator.
 */
public final class DamageEngine {
    private static final DirectDamageCalculator DIRECT_DAMAGE_CALCULATOR = new DirectDamageCalculator();
    // Reguły statystyk i efektów legacy pozostają w adapterze, poza neutralnym kernelem.
    private static final double BASE_CRIT_DAMAGE_BONUS = 0.50d;
    private static final double CRIT_DAMAGE_FROM_INTELLIGENCE_FACTOR = 0.0004d;
    private static final double VULNERABLE_MULTIPLIER = 1.20d;
    private static final String VERATHIEL_SHARD_ASPECT_ID = "verathiel_shard";
    private static final double VERATHIEL_BASIC_SKILL_MULTIPLIER = 2.0d;

    public ReactiveHitBreakdown calculateReactiveHit(HeroBuildSnapshot snapshot,
                                                     int triggeredSecond,
                                                     double activeBlockChanceBonusPercent,
                                                     double activeThornsBonus,
                                                     boolean resolveActive,
                                                     int resolveRemainingSeconds,
                                                     boolean punishmentActive) {
        Hero hero = snapshot.getHero();
        HeroClassDef classDef = HeroClassDefs.get(hero.getHeroClass());
        List<Item> equippedItems = snapshot.getEquippedItems();

        double baseThornsFromBuild = Item.sumStat(equippedItems, ItemStatType.THORNS);
        double effectiveThorns = baseThornsFromBuild + activeThornsBonus;
        double mainStatMultiplier = classDef.resolveMainStatMultiplier(hero.getLevel(), equippedItems);
        double blockChanceFromBuild = Item.sumStat(equippedItems, ItemStatType.BLOCK_CHANCE) / 100.0d;
        double activeBlockChance = blockChanceFromBuild + (activeBlockChanceBonusPercent / 100.0d);
        double retributionChance = Item.sumStat(equippedItems, ItemStatType.RETRIBUTION_CHANCE) / 100.0d;
        double levelDamageReduction = resolveLevelDamageReduction(hero.getLevel());

        double thornsRawExact = effectiveThorns * mainStatMultiplier;
        double retributionRawExact = thornsRawExact * activeBlockChance * retributionChance;

        long thornsRawDamage = Math.round(thornsRawExact);
        long thornsFinalDamage = Math.round(thornsRawExact * (1.0d - levelDamageReduction));
        long retributionExpectedRawDamage = Math.round(retributionRawExact);
        long retributionExpectedFinalDamage = Math.round(retributionRawExact * (1.0d - levelDamageReduction));
        long reactiveFinalDamage = thornsFinalDamage + retributionExpectedFinalDamage;

        return new ReactiveHitBreakdown(
                triggeredSecond,
                baseThornsFromBuild,
                mainStatMultiplier,
                blockChanceFromBuild,
                activeBlockChance,
                activeThornsBonus,
                retributionChance,
                resolveActive,
                resolveRemainingSeconds,
                punishmentActive,
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
        return calculate(snapshot, skillId, targetStatuses, 1.0d);
    }

    public DamageBreakdown calculate(HeroBuildSnapshot snapshot,
                                     SkillId skillId,
                                     EnumSet<StatusId> targetStatuses,
                                     double molochOathMultiplier) {
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
        double levelDamageReduction = resolveLevelDamageReduction(hero.getLevel());
        double verathielBasicSkillMultiplier = resolveVerathielBasicSkillMultiplier(snapshot, skillId);
        double normalizedMolochOathMultiplier = Math.max(1.0d, molochOathMultiplier);
        double itemMultiplicativeMultiplier = verathielBasicSkillMultiplier * normalizedMolochOathMultiplier;

        long baseSkillDamagePercent = resolveBaseSkillDamagePercent(skillDef, state, targetStatuses);
        DirectDamageContext.CriticalTotalRoundingPolicy roundingPolicy =
                skillDef.getCriticalRoundingPolicy(state.getChoiceUpgrade()) == CriticalRoundingPolicy.ROUNDED_RAW_HIT
                        ? DirectDamageContext.CriticalTotalRoundingPolicy.ROUNDED_RAW_TOTAL
                        : DirectDamageContext.CriticalTotalRoundingPolicy.EXACT_COMPONENT_SUM;
        DirectDamageContext context = new DirectDamageContext(snapshot.getAverageWeaponDamage(),
                weaponMultiplier, mainStatMultiplier, additiveMultiplier, vulnerableMultiplier,
                itemMultiplicativeMultiplier, critMultiplier, 1.0d - levelDamageReduction, roundingPolicy);
        List<DirectDamageComponentSpec> specs = new ArrayList<>();
        specs.add(new DirectDamageComponentSpec("Główny hit", "bazowy", BigDecimal.valueOf(baseSkillDamagePercent),
                1, null, true, true, null));
        for (SkillRuntimeEffect effect : skillDef.getChoiceEffects(state.getChoiceUpgrade())) {
            if (effect.getEffectType() != EffectType.DAMAGE) {
                continue;
            }
            boolean active = isEffectActive(effect, targetStatuses);
            String conditionLabel = effect.getConditionStatus() == StatusId.NONE ? null : effect.getConditionStatus().name();
            boolean includedInSingleTarget = effect.isIncludedInSingleTarget();
            String exclusionReason = includedInSingleTarget ? null : "Komponent nie trafia głównego celu w modelu single target";
            specs.add(new DirectDamageComponentSpec(effect.getComponentName(), "modyfikator",
                    BigDecimal.valueOf(effect.getSkillDamagePercent()), effect.getHitCount(), conditionLabel,
                    active, includedInSingleTarget, exclusionReason));
        }
        DirectDamageResult directDamage = DIRECT_DAMAGE_CALCULATOR.calculate(context, specs);
        List<DamageComponentBreakdown> components = directDamage.components().stream()
                .map(DamageEngine::toLegacyComponent).toList();

        return new DamageBreakdown(
                directDamage.components().getFirst().baseDamage(),
                directDamage.rawDamage(),
                directDamage.finalDamage(),
                directDamage.rawCriticalDamage(),
                directDamage.criticalDamage(),
                weaponMultiplier,
                mainStat,
                mainStatMultiplier,
                additivePercent,
                additiveMultiplier,
                vulnerableMultiplier,
                verathielBasicSkillMultiplier,
                normalizedMolochOathMultiplier,
                itemMultiplicativeMultiplier,
                critMultiplier,
                critDamageBonusTotal,
                critDamageBonusFromItems,
                critDamageBonusFromIntelligence,
                intelligence,
                levelDamageReduction,
                skillDef.getChoiceDisplayName(state.getChoiceUpgrade()),
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
        double levelDamageReduction = resolveLevelDamageReduction(hero.getLevel());
        double itemMultiplicativeMultiplier = 1.0d;

        DirectDamageContext context = new DirectDamageContext(snapshot.getAverageWeaponDamage(),
                weaponMultiplier, mainStatMultiplier, additiveMultiplier, vulnerableMultiplier,
                itemMultiplicativeMultiplier, critMultiplier, 1.0d - levelDamageReduction);
        DirectDamageComponentSpec spec = new DirectDamageComponentSpec(componentName, source,
                BigDecimal.valueOf(skillDamagePercent), 1, null, true, true, null);
        DirectDamageResult directDamage = DIRECT_DAMAGE_CALCULATOR.calculate(context, List.of(spec));
        DamageComponentBreakdown component = toLegacyComponent(directDamage.components().getFirst());

        return new DamageBreakdown(
                directDamage.components().getFirst().baseDamage(),
                directDamage.rawDamage(),
                directDamage.finalDamage(),
                directDamage.rawCriticalDamage(),
                directDamage.criticalDamage(),
                weaponMultiplier,
                mainStat,
                mainStatMultiplier,
                additivePercent,
                additiveMultiplier,
                vulnerableMultiplier,
                1.0d,
                1.0d,
                itemMultiplicativeMultiplier,
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

    private static DamageComponentBreakdown toLegacyComponent(DirectDamageComponentResult result) {
        DirectDamageComponentSpec spec = result.spec();
        return new DamageComponentBreakdown(spec.name(), spec.source(), spec.damagePercent().longValueExact(),
                spec.hitCount(), spec.conditionLabel(), spec.active(), spec.includedInSingleTarget(), spec.exclusionReason(),
                result.rawDamage(), result.finalDamage(), result.rawCriticalDamage(), result.criticalDamage());
    }

    private static double resolveLevelDamageReduction(int heroLevel) {
        if (heroLevel == 70) {
            return 0.80d;
        }
        return Math.min(85.0d, heroLevel + 25.0d) / 100.0d;
    }

    private static double resolveVerathielBasicSkillMultiplier(HeroBuildSnapshot snapshot, SkillId skillId) {
        if (!snapshot.hasActiveAspect(VERATHIEL_SHARD_ASPECT_ID) || !isBasicSkill(skillId)) {
            return 1.0d;
        }
        return VERATHIEL_BASIC_SKILL_MULTIPLIER;
    }

    private static boolean isBasicSkill(SkillId skillId) {
        var key = LegacyPaladinRuntimeSkillKeyAdapter.toRuntimeKey(skillId);
        return PaladinSkillTreeRegistry.requireSkill(key.skillId()).hasSkillCategory(SkillCategory.PODSTAWOWE);
    }

}

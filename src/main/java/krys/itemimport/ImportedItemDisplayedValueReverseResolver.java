package krys.itemimport;

import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkingResolvedItemValueResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Odwraca widoczną wartość z tooltipa do bazowego referenceValue, gdy model doskonalenia jest jednoznaczny. */
final class ImportedItemDisplayedValueReverseResolver {
    private static final double EPSILON = 0.0001d;
    private final MasterworkingResolvedItemValueResolver resolver;

    ImportedItemDisplayedValueReverseResolver() {
        this(new MasterworkingResolvedItemValueResolver());
    }

    ImportedItemDisplayedValueReverseResolver(MasterworkingResolvedItemValueResolver resolver) {
        this.resolver = resolver;
    }

    Optional<ReverseResolvedReferenceValue> resolveReferenceValue(ImportedItemAffixType type,
                                                                  double displayedValue,
                                                                  boolean greaterAffix,
                                                                  ItemMasterworking masterworking) {
        if (type == null || masterworking == null || masterworking.getQualityCurrent() <= 0) {
            return Optional.empty();
        }
        if (!supportsReverseResolve(type)) {
            return Optional.empty();
        }
        List<Double> matches = new ArrayList<>();
        for (double candidate : candidatesFor(type, displayedValue)) {
            ImportedItemAffix affix = new ImportedItemAffix(
                    type,
                    candidate,
                    defaultUnit(type),
                    greaterAffix,
                    0,
                    "",
                    ImportedItemAffixSource.OCR
            );
            if (sameValue(resolver.resolveAffixValue(affix, masterworking), displayedValue)) {
                matches.add(candidate);
            }
        }
        if (matches.size() != 1) {
            return Optional.empty();
        }
        return Optional.of(new ReverseResolvedReferenceValue(
                matches.getFirst(),
                "Jednoznaczne odwrócenie displayed/current value przez model Doskonalenia."
        ));
    }

    private static boolean supportsReverseResolve(ImportedItemAffixType type) {
        return switch (type) {
            case STRENGTH, INTELLIGENCE, THORNS, BLOCK_CHANCE, RETRIBUTION_CHANCE, CRITICAL_STRIKE_CHANCE,
                 LUCKY_HIT_CHANCE, WEAPON_DAMAGE_FLAT, MAXIMUM_LIFE, LIFE_ON_HIT, LUCKY_HIT_PRIMARY_RESOURCE,
                 COOLDOWN_REDUCTION, MOVEMENT_SPEED, DODGE_CHANCE, CORE_SKILL_RANKS -> true;
            case ALL_RESISTANCE, FIRE_RESISTANCE, DAMAGE_REDUCTION, LIFE_ON_KILL, ALL_DAMAGE_MULTIPLIER,
                    DAMAGE_OVER_TIME_MULTIPLIER -> false;
        };
    }

    private static List<Double> candidatesFor(ImportedItemAffixType type, double displayedValue) {
        if (integerLikeAffix(type)) {
            int max = Math.max(20, (int) Math.ceil(displayedValue * 2.0d + 20.0d));
            List<Double> candidates = new ArrayList<>();
            for (int value = 0; value <= max; value++) {
                candidates.add((double) value);
            }
            return candidates;
        }
        int maxTenths = Math.max(1000, (int) Math.ceil(displayedValue * 20.0d + 100.0d));
        List<Double> candidates = new ArrayList<>();
        for (int tenths = 0; tenths <= maxTenths; tenths++) {
            candidates.add(tenths / 10.0d);
        }
        return candidates;
    }

    private static boolean integerLikeAffix(ImportedItemAffixType type) {
        return switch (type) {
            case STRENGTH, INTELLIGENCE, THORNS, ALL_RESISTANCE, FIRE_RESISTANCE, WEAPON_DAMAGE_FLAT,
                 MAXIMUM_LIFE, LIFE_ON_HIT, LIFE_ON_KILL, LUCKY_HIT_PRIMARY_RESOURCE, CORE_SKILL_RANKS -> true;
            case DAMAGE_REDUCTION, BLOCK_CHANCE, RETRIBUTION_CHANCE, CRITICAL_STRIKE_CHANCE, LUCKY_HIT_CHANCE,
                 COOLDOWN_REDUCTION, MOVEMENT_SPEED, DODGE_CHANCE, ALL_DAMAGE_MULTIPLIER, DAMAGE_OVER_TIME_MULTIPLIER -> false;
        };
    }

    private static String defaultUnit(ImportedItemAffixType type) {
        return switch (type) {
            case BLOCK_CHANCE, RETRIBUTION_CHANCE, CRITICAL_STRIKE_CHANCE, LUCKY_HIT_CHANCE, COOLDOWN_REDUCTION,
                 MOVEMENT_SPEED, DODGE_CHANCE, DAMAGE_REDUCTION, ALL_DAMAGE_MULTIPLIER, DAMAGE_OVER_TIME_MULTIPLIER -> "%";
            case STRENGTH, INTELLIGENCE, THORNS, ALL_RESISTANCE, FIRE_RESISTANCE, WEAPON_DAMAGE_FLAT, MAXIMUM_LIFE,
                 LIFE_ON_HIT, LIFE_ON_KILL, LUCKY_HIT_PRIMARY_RESOURCE, CORE_SKILL_RANKS -> "";
        };
    }

    private static boolean sameValue(double left, double right) {
        return Math.abs(left - right) < EPSILON;
    }

    record ReverseResolvedReferenceValue(double referenceValue, String reason) {
    }
}

package krys.itemimport.image;

import krys.itemimport.ImportedItemAffix;
import krys.masterworking.ItemMasterworking;
import krys.masterworking.MasterworkedAffixSelection;
import krys.masterworking.MasterworkingResolvedItemValueResolver;
import krys.tempering.TemperingAffixDefinition;
import krys.tempering.ItemTemperingAffix;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.Set;

/** Odwraca wartości DISPLAYED z tooltipa do SOURCE values wymaganych przez formularz importu. */
public final class DisplayedItemValueSourceNormalizer {
    private static final double EPSILON = 0.0001d;

    private final MasterworkingResolvedItemValueResolver resolver;

    public DisplayedItemValueSourceNormalizer() {
        this(new MasterworkingResolvedItemValueResolver());
    }

    public DisplayedItemValueSourceNormalizer(MasterworkingResolvedItemValueResolver resolver) {
        this.resolver = resolver;
    }

    public OptionalLong reverseArmor(long displayedArmor, ItemMasterworking masterworking) {
        if (displayedArmor <= 0L) {
            return OptionalLong.empty();
        }
        int quality = quality(masterworking);
        if (quality <= 0) {
            return OptionalLong.of(displayedArmor);
        }
        double multiplier = 1.0d + quality / 100.0d;
        long center = (long) Math.ceil(displayedArmor / multiplier);
        List<Long> matches = new ArrayList<>();
        for (long candidate = Math.max(0L, center - 3L); candidate <= center + 3L; candidate++) {
            if (resolver.resolveArmor(candidate, masterworking) == displayedArmor) {
                matches.add(candidate);
            }
        }
        return matches.size() == 1 ? OptionalLong.of(matches.getFirst()) : OptionalLong.empty();
    }

    public OptionalDouble reverseAffix(ImportedItemAffix displayedAffix, double displayedValue, ItemMasterworking masterworking) {
        if (displayedAffix == null || displayedValue < 0.0d) {
            return OptionalDouble.empty();
        }
        if (displayedAffix.getReferenceValue() != null) {
            return OptionalDouble.of(displayedValue);
        }
        if (quality(masterworking) <= 0 || !resolver.supportsAffix(displayedAffix)) {
            return OptionalDouble.of(displayedValue);
        }

        Set<Double> candidates = new LinkedHashSet<>();
        inverseAffixCandidate(displayedAffix, displayedValue, masterworking).ifPresent(candidates::add);
        if (displayedAffix.isGreaterAffix()) {
            inverseGreaterAffixCandidate(displayedAffix, displayedValue, masterworking).ifPresent(candidates::add);
        }

        List<Double> matches = new ArrayList<>();
        for (Double candidate : candidates) {
            if (candidate == null || candidate < 0.0d) {
                continue;
            }
            ImportedItemAffix sourceAffix = new ImportedItemAffix(
                    displayedAffix.getType(),
                    normalizePrecision(candidate),
                    displayedAffix.getUnit(),
                    displayedAffix.isGreaterAffix(),
                    displayedAffix.getDisplayOrder(),
                    displayedAffix.getRawOcrLine(),
                    displayedAffix.getSource(),
                    displayedAffix.getAffixDefinitionId(),
                    displayedAffix.getRollRangeMin(),
                    displayedAffix.getRollRangeMax(),
                    displayedAffix.getReferenceValue(),
                    displayedAffix.getDisplayValue()
            );
            if (sameValue(resolver.resolveAffixValue(sourceAffix, masterworking), displayedValue)) {
                matches.add(sourceAffix.getValue());
            }
        }
        return matches.size() == 1 ? OptionalDouble.of(matches.getFirst()) : OptionalDouble.empty();
    }

    public Optional<ItemTemperingAffix> reverseTempering(ItemTemperingAffix displayedAffix,
                                                        double displayedValue,
                                                        ItemMasterworking masterworking,
                                                        TemperingAffixDefinition definition) {
        if (displayedAffix == null || definition == null || displayedValue < 0.0d) {
            return Optional.empty();
        }
        if (!resolver.supportsTempering(displayedAffix)) {
            return Optional.of(displayedAffix);
        }
        double sourceValue = displayedAffix.isGreaterAffix()
                ? definition.greaterAffixValue()
                : displayedAffix.getValue();
        ItemTemperingAffix sourceAffix = new ItemTemperingAffix(
                displayedAffix.getDefinitionId(),
                displayedAffix.getCategory(),
                sourceValue,
                displayedAffix.getDisplayText(),
                displayedAffix.getSourceLine(),
                displayedAffix.getRuntimeStatus(),
                displayedAffix.isGreaterAffix()
        );
        List<ItemMasterworking> candidates = new ArrayList<>();
        candidates.add(ItemMasterworking.quality(quality(masterworking)));
        candidates.add(new ItemMasterworking(
                quality(masterworking),
                masterworking == null ? ItemMasterworking.DEFAULT_QUALITY_MAX : masterworking.getQualityMax(),
                MasterworkedAffixSelection.temperingAffix(displayedAffix.getDefinitionId())
        ));
        for (ItemMasterworking candidateMasterworking : candidates) {
            if (sameValue(resolver.resolveTemperingValue(sourceAffix, candidateMasterworking), displayedValue)) {
                return Optional.of(sourceAffix);
            }
        }
        return Optional.empty();
    }

    public Optional<MasterworkedAffixSelection> detectPerfectedAffix(List<DisplayedAffix> affixes,
                                                                     List<DisplayedTempering> temperingAffixes,
                                                                     ItemMasterworking masterworking) {
        if (masterworking == null || masterworking.getQualityCurrent() != ItemMasterworking.DEFAULT_QUALITY_MAX) {
            return Optional.empty();
        }
        List<MasterworkedAffixSelection> matches = new ArrayList<>();
        ItemMasterworking baseMasterworking = ItemMasterworking.quality(masterworking.getQualityCurrent());

        for (DisplayedAffix displayedAffix : affixes == null ? List.<DisplayedAffix>of() : affixes) {
            ImportedItemAffix affix = displayedAffix.affix();
            if (affix == null) {
                continue;
            }
            double baseValue = resolver.resolveAffixValue(affix, baseMasterworking);
            MasterworkedAffixSelection selection = MasterworkedAffixSelection.ordinaryAffix(affix.getType().name());
            double selectedValue = resolver.resolveAffixValue(affix,
                    new ItemMasterworking(masterworking.getQualityCurrent(), masterworking.getQualityMax(), selection));
            if (!sameValue(baseValue, displayedAffix.displayedValue())
                    && sameValue(selectedValue, displayedAffix.displayedValue())) {
                matches.add(selection);
            }
        }

        for (DisplayedTempering displayedTempering : temperingAffixes == null ? List.<DisplayedTempering>of() : temperingAffixes) {
            ItemTemperingAffix affix = displayedTempering.affix();
            if (affix == null || !resolver.supportsTempering(affix)) {
                continue;
            }
            double baseValue = resolver.resolveTemperingValue(affix, baseMasterworking);
            MasterworkedAffixSelection selection = MasterworkedAffixSelection.temperingAffix(affix.getDefinitionId());
            double selectedValue = resolver.resolveTemperingValue(affix,
                    new ItemMasterworking(masterworking.getQualityCurrent(), masterworking.getQualityMax(), selection));
            if (!sameValue(baseValue, displayedTempering.displayedValue())
                    && sameValue(selectedValue, displayedTempering.displayedValue())) {
                matches.add(selection);
            }
        }

        return matches.size() == 1 ? Optional.of(matches.getFirst()) : Optional.empty();
    }

    public boolean hasAmbiguousPerfectedAffix(List<DisplayedAffix> affixes,
                                              List<DisplayedTempering> temperingAffixes,
                                              ItemMasterworking masterworking) {
        if (masterworking == null || masterworking.getQualityCurrent() != ItemMasterworking.DEFAULT_QUALITY_MAX) {
            return false;
        }
        List<MasterworkedAffixSelection> matches = new ArrayList<>();
        detectPerfectedCandidates(affixes, temperingAffixes, masterworking, matches);
        return matches.size() > 1;
    }

    private void detectPerfectedCandidates(List<DisplayedAffix> affixes,
                                           List<DisplayedTempering> temperingAffixes,
                                           ItemMasterworking masterworking,
                                           List<MasterworkedAffixSelection> matches) {
        ItemMasterworking baseMasterworking = ItemMasterworking.quality(masterworking.getQualityCurrent());
        for (DisplayedAffix displayedAffix : affixes == null ? List.<DisplayedAffix>of() : affixes) {
            ImportedItemAffix affix = displayedAffix.affix();
            if (affix == null) {
                continue;
            }
            double baseValue = resolver.resolveAffixValue(affix, baseMasterworking);
            MasterworkedAffixSelection selection = MasterworkedAffixSelection.ordinaryAffix(affix.getType().name());
            double selectedValue = resolver.resolveAffixValue(affix,
                    new ItemMasterworking(masterworking.getQualityCurrent(), masterworking.getQualityMax(), selection));
            if (!sameValue(baseValue, displayedAffix.displayedValue())
                    && sameValue(selectedValue, displayedAffix.displayedValue())) {
                matches.add(selection);
            }
        }
        for (DisplayedTempering displayedTempering : temperingAffixes == null ? List.<DisplayedTempering>of() : temperingAffixes) {
            ItemTemperingAffix affix = displayedTempering.affix();
            if (affix == null || !resolver.supportsTempering(affix)) {
                continue;
            }
            double baseValue = resolver.resolveTemperingValue(affix, baseMasterworking);
            MasterworkedAffixSelection selection = MasterworkedAffixSelection.temperingAffix(affix.getDefinitionId());
            double selectedValue = resolver.resolveTemperingValue(affix,
                    new ItemMasterworking(masterworking.getQualityCurrent(), masterworking.getQualityMax(), selection));
            if (!sameValue(baseValue, displayedTempering.displayedValue())
                    && sameValue(selectedValue, displayedTempering.displayedValue())) {
                matches.add(selection);
            }
        }
    }

    private static Optional<Double> inverseAffixCandidate(ImportedItemAffix affix,
                                                          double displayedValue,
                                                          ItemMasterworking masterworking) {
        if (affix.isGreaterAffix()) {
            return Optional.empty();
        }
        double multiplier = 1.0d + quality(masterworking) / 100.0d;
        return Optional.of(displayedValue / multiplier);
    }

    private static Optional<Double> inverseGreaterAffixCandidate(ImportedItemAffix affix,
                                                                 double displayedValue,
                                                                 ItemMasterworking masterworking) {
        double multiplier = (1.25d + quality(masterworking) / 100.0d) / 1.25d;
        return Optional.of(displayedValue / multiplier);
    }

    private static int quality(ItemMasterworking masterworking) {
        return masterworking == null ? ItemMasterworking.DEFAULT_QUALITY_CURRENT : masterworking.getQualityCurrent();
    }

    private static double normalizePrecision(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP).stripTrailingZeros().doubleValue();
    }

    private static boolean sameValue(double left, double right) {
        return Math.abs(left - right) < EPSILON;
    }

    public record DisplayedAffix(ImportedItemAffix affix, double displayedValue) {
    }

    public record DisplayedTempering(ItemTemperingAffix affix, double displayedValue) {
    }
}

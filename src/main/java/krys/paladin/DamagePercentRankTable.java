package krys.paladin;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Niemutowalna tabela bazowych procentow obrazen per ranga umiejetnosci. */
public final class DamagePercentRankTable {
    public static final int MIN_RANK = 1;
    public static final int MAX_RANK = 15;

    private static final DamagePercentRankTable EMPTY = new DamagePercentRankTable(Map.of());

    private final Map<Integer, Integer> damagePercentsByRank;

    private DamagePercentRankTable(Map<Integer, Integer> damagePercentsByRank) {
        this.damagePercentsByRank = Map.copyOf(damagePercentsByRank);
    }

    public static DamagePercentRankTable empty() {
        return EMPTY;
    }

    public static DamagePercentRankTable of(Map<Integer, Integer> damagePercentsByRank) {
        Objects.requireNonNull(damagePercentsByRank, "damagePercentsByRank");
        if (damagePercentsByRank.isEmpty()) {
            return empty();
        }

        Map<Integer, Integer> validated = new TreeMap<>();
        for (Map.Entry<Integer, Integer> entry : damagePercentsByRank.entrySet()) {
            Integer rank = Objects.requireNonNull(entry.getKey(), "rank");
            Integer damagePercent = Objects.requireNonNull(entry.getValue(), "damagePercent");
            validateRank(rank);
            validated.put(rank, damagePercent);
        }
        return new DamagePercentRankTable(validated);
    }

    public Integer damagePercentAtRank(int rank) {
        validateRank(rank);
        return damagePercentsByRank.get(rank);
    }

    public Integer damagePercentAtRank1() {
        return damagePercentAtRank(MIN_RANK);
    }

    public Integer damagePercentAtTreeMaxRank(int treeMaxRank) {
        return damagePercentAtRank(treeMaxRank);
    }

    public boolean isEmpty() {
        return damagePercentsByRank.isEmpty();
    }

    public Map<Integer, Integer> asMap() {
        return new LinkedHashMap<>(damagePercentsByRank);
    }

    private static void validateRank(int rank) {
        if (rank < MIN_RANK || rank > MAX_RANK) {
            throw new IllegalArgumentException("Ranga musi byc w zakresie 1..15: " + rank);
        }
    }
}

package krys.hero;

import java.util.Objects;

/** Progresja core stats; nie określa mnożnika obrażeń, życia ani wytrzymałości. */
public record HeroClassCoreStatProgression(HeroCoreStats levelOne, int increasePerLevel) {
    public HeroClassCoreStatProgression {
        Objects.requireNonNull(levelOne, "levelOne");
        if (increasePerLevel < 0) { throw new IllegalArgumentException("Przyrost statystyk musi być nieujemny."); }
    }

    public HeroCoreStats atLevel(int level) {
        if (level < 1) { throw new IllegalArgumentException("Poziom musi być dodatni."); }
        int increase = Math.multiplyExact(level - 1, increasePerLevel);
        return new HeroCoreStats(Math.addExact(levelOne.strength(), increase), Math.addExact(levelOne.intelligence(), increase),
                Math.addExact(levelOne.willpower(), increase), Math.addExact(levelOne.dexterity(), increase));
    }
}

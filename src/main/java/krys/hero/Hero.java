package krys.hero;

/** Minimalny model bohatera potrzebny do foundation silnika obrażeń. */
public final class Hero {
    private final int id;
    private final String name;
    private final int level;
    private final HeroClass heroClass;

    public Hero(int id, String name, int level, HeroClass heroClass) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.heroClass = heroClass;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public HeroClass getHeroClass() {
        return heroClass;
    }
}

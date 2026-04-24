package krys.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Model widoku SSR modułu zarządzania bohaterami. */
public final class HeroesPageModel {
    private final List<HeroProfile> heroes;
    private final HeroProfile activeHero;
    private final List<String> messages;
    private final List<String> errors;

    public HeroesPageModel(List<HeroProfile> heroes,
                           HeroProfile activeHero,
                           List<String> messages,
                           List<String> errors) {
        this.heroes = Collections.unmodifiableList(new ArrayList<>(heroes));
        this.activeHero = activeHero;
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
        this.errors = Collections.unmodifiableList(new ArrayList<>(errors));
    }

    public List<HeroProfile> getHeroes() {
        return heroes;
    }

    public HeroProfile getActiveHero() {
        return activeHero;
    }

    public List<String> getMessages() {
        return messages;
    }

    public List<String> getErrors() {
        return errors;
    }

    public boolean hasHeroes() {
        return !heroes.isEmpty();
    }
}

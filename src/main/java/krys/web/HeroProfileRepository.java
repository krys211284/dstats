package krys.web;

import java.util.List;
import java.util.Optional;

/** Trwałe repozytorium bohaterów aplikacji i wskaźnika aktywnego bohatera. */
public interface HeroProfileRepository {
    HeroProfile save(HeroProfile heroProfile);

    List<HeroProfile> findAll();

    Optional<HeroProfile> findById(long heroId);

    void delete(long heroId);

    Optional<Long> loadActiveHeroId();

    void saveActiveHeroId(Long heroId);
}

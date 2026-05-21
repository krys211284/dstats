package krys.tempering;

import java.util.List;
import java.util.Optional;

/** Katalog znanych affixów hartowania. */
public interface TemperingAffixRegistry {
    List<TemperingAffixDefinition> all();

    List<TemperingAffixDefinition> byCategory(TemperingCategory category);

    Optional<TemperingAffixDefinition> findById(String id);
}

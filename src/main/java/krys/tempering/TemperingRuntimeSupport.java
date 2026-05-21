package krys.tempering;

/** Jawne mostkowanie pojedynczych hartowań, które są już aktywne w runtime. */
public final class TemperingRuntimeSupport {
    public static final String DEFENSE_MAX_ANIMUS_DEFINITION_ID = "defense_max_animus";

    private TemperingRuntimeSupport() {
    }

    public static boolean affectsMaximumAnimus(ItemTemperingAffix affix) {
        return affix != null
                && affix.getCategory() == TemperingCategory.DEFENSE
                && DEFENSE_MAX_ANIMUS_DEFINITION_ID.equals(affix.getDefinitionId());
    }
}

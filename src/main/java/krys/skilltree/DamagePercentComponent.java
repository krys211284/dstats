package krys.skilltree;

/** Nazwany komponent procentow obrazen w tabelach rang skilli wielokomponentowych. */
public enum DamagePercentComponent {
    PRIMARY_DAMAGE,
    ADDITIONAL_STRIKE_DAMAGE,
    PASSIVE_DAMAGE,
    ACTIVE_DAMAGE,
    JUMP_DAMAGE,
    LANDING_DAMAGE,
    BURST_DAMAGE,
    DAMAGE_PER_SECOND,
    FIRST_STRIKE_DAMAGE,
    SECOND_STRIKE_DAMAGE,
    /** Łączne obrażenia za cały jawnie podany okres, a nie obrażenia na sekundę. */
    DAMAGE_OVER_DURATION,
    /** Dyskretne obrażenia jednego impulsu; interwał nie oznacza DPS ani sumy za cały okres. */
    PERIODIC_DAMAGE,
    /** Obrażenia pojedynczego zdarzenia źródłowego, bez założenia jego częstotliwości. */
    EVENT_TRIGGERED_DAMAGE
}

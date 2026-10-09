package krys.skilltree;

/** Częstotliwość źródłowego generowania zasobu, bez przeliczania na runtime. */
public enum SkillResourceGenerationFrequency {
    PER_USE,
    PER_SECOND,
    /** Ilość na jedno zdarzenie opisane w źródłowych metadanych skilla. */
    PER_EVENT
}

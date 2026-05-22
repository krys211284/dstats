package krys.masterworking;

/** Opcjonalny wybor aktualnego doskonalonego afiksu po osiagnieciu jakosci 25/25. */
public final class MasterworkedAffixSelection {
    private final MasterworkedAffixSource source;
    private final String rawSource;
    private final String key;

    public MasterworkedAffixSelection(MasterworkedAffixSource source, String key) {
        this(source, source == null ? "" : source.name(), key);
    }

    private MasterworkedAffixSelection(MasterworkedAffixSource source, String rawSource, String key) {
        this.source = source;
        this.rawSource = rawSource == null ? "" : rawSource;
        this.key = key == null ? "" : key;
    }

    public static MasterworkedAffixSelection ordinaryAffix(String key) {
        return new MasterworkedAffixSelection(MasterworkedAffixSource.ORDINARY_AFFIX, key);
    }

    public static MasterworkedAffixSelection temperingAffix(String key) {
        return new MasterworkedAffixSelection(MasterworkedAffixSource.TEMPERING_AFFIX, key);
    }

    public static MasterworkedAffixSelection unknown(String rawSource, String key) {
        return new MasterworkedAffixSelection(null, rawSource, key);
    }

    public MasterworkedAffixSource getSource() {
        return source;
    }

    public String getRawSource() {
        return rawSource;
    }

    public String getKey() {
        return key;
    }

    public boolean hasRecognizedSource() {
        return source != null;
    }
}

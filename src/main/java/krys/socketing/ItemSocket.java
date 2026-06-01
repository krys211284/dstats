package krys.socketing;

/** Pojedyncze gniazdo itemu. */
public final class ItemSocket {
    private final int index;
    private final SocketContentType contentType;
    private final String gemId;
    private final SocketGemRuneStat detectedStat;

    public ItemSocket(int index, SocketContentType contentType, String gemId) {
        this(index, contentType, gemId, null);
    }

    public ItemSocket(int index, SocketContentType contentType, String gemId, SocketGemRuneStat detectedStat) {
        if (index < 0) {
            throw new IllegalArgumentException("Indeks gniazda nie może być ujemny.");
        }
        this.index = index;
        this.contentType = contentType == null ? SocketContentType.EMPTY : contentType;
        this.gemId = gemId == null ? "" : gemId;
        this.detectedStat = detectedStat;
    }

    public static ItemSocket empty(int index) {
        return new ItemSocket(index, SocketContentType.EMPTY, "");
    }

    public static ItemSocket gem(int index, String gemId) {
        return new ItemSocket(index, SocketContentType.GEM, gemId);
    }

    public static ItemSocket detectedStat(int index, SocketGemRuneStat stat) {
        return new ItemSocket(index, SocketContentType.DETECTED_STAT, "", stat);
    }

    public int getIndex() {
        return index;
    }

    public SocketContentType getContentType() {
        return contentType;
    }

    public String getGemId() {
        return gemId;
    }

    public SocketGemRuneStat getDetectedStat() {
        return detectedStat;
    }

    public boolean isOccupied() {
        return contentType == SocketContentType.GEM || contentType == SocketContentType.DETECTED_STAT;
    }
}

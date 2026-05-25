package krys.socketing;

/** Pojedyncze gniazdo itemu. */
public final class ItemSocket {
    private final int index;
    private final SocketContentType contentType;
    private final String gemId;

    public ItemSocket(int index, SocketContentType contentType, String gemId) {
        if (index < 0) {
            throw new IllegalArgumentException("Indeks gniazda nie może być ujemny.");
        }
        this.index = index;
        this.contentType = contentType == null ? SocketContentType.EMPTY : contentType;
        this.gemId = gemId == null ? "" : gemId;
    }

    public static ItemSocket empty(int index) {
        return new ItemSocket(index, SocketContentType.EMPTY, "");
    }

    public static ItemSocket gem(int index, String gemId) {
        return new ItemSocket(index, SocketContentType.GEM, gemId);
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
}

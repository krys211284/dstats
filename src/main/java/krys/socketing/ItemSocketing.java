package krys.socketing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Stan gniazd itemu. */
public final class ItemSocketing {
    public static final int MIN_SOCKET_COUNT = 0;
    public static final int MAX_SOCKET_COUNT = 2;

    private final int socketCount;
    private final List<ItemSocket> sockets;

    public ItemSocketing(int socketCount, List<ItemSocket> sockets) {
        this.socketCount = socketCount;
        this.sockets = sockets == null
                ? List.of()
                : sockets.stream()
                .sorted(Comparator.comparingInt(ItemSocket::getIndex))
                .toList();
    }

    public static ItemSocketing empty() {
        return new ItemSocketing(0, List.of());
    }

    public static ItemSocketing emptySockets(int socketCount) {
        List<ItemSocket> sockets = new ArrayList<>();
        for (int index = 0; index < socketCount; index++) {
            sockets.add(ItemSocket.empty(index));
        }
        return new ItemSocketing(socketCount, sockets);
    }

    public int getSocketCount() {
        return socketCount;
    }

    public List<ItemSocket> getSockets() {
        return sockets;
    }

    public ItemSocket socketAt(int index) {
        return sockets.stream()
                .filter(socket -> socket.getIndex() == index)
                .findFirst()
                .orElseGet(() -> ItemSocket.empty(index));
    }
}

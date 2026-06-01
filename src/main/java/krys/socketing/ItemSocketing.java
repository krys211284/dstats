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

    public int getTotalSocketCount() {
        return socketCount;
    }

    public int getEmptySocketCount() {
        return (int) sockets.stream()
                .limit(Math.max(0, socketCount))
                .filter(socket -> socket.getContentType() == SocketContentType.EMPTY)
                .count();
    }

    public int getOccupiedSocketCount() {
        return (int) sockets.stream()
                .limit(Math.max(0, socketCount))
                .filter(ItemSocket::isOccupied)
                .count();
    }

    public List<SocketGemRuneStat> getDetectedStats() {
        return sockets.stream()
                .filter(socket -> socket.getContentType() == SocketContentType.DETECTED_STAT)
                .map(ItemSocket::getDetectedStat)
                .filter(stat -> stat != null && stat.hasDisplayText())
                .toList();
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

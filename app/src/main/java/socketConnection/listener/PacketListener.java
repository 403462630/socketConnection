package socketConnection.listener;

import com.baidao.data.Jsonable;
import com.baidao.socketConnection.network.Packet;
import com.baidao.socketConnection.network.SocketConnection;

/**
 * Created by Bruce on 1/4/15.
 */
public interface PacketListener<T extends Jsonable> {
    boolean shouldProcess(Packet<T> packet);
    void processPacket(Packet<T> packet, SocketConnection socketConnection);
    T getContent(String content);
}

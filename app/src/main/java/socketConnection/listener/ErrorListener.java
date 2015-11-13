package socketConnection.listener;

import com.baidao.socketConnection.model.ErrorResult;
import com.baidao.socketConnection.network.Packet;
import com.baidao.socketConnection.network.SocketConnection;

/**
 * Created by Bruce on 1/7/15.
 */
public abstract class ErrorListener implements PacketListener<ErrorResult> {
    @Override
    public boolean shouldProcess(Packet<ErrorResult> packet) {
        return packet.getHeader().statusCode != 200;
    }

    @Override
    public void processPacket(Packet<ErrorResult> packet, SocketConnection socketConnection) {
        ErrorResult t = getContent(packet.getTempContent());
        packet.setContent(t);
        onError(t);
    }

    @Override
    public ErrorResult getContent(String content) {
        return ErrorResult.fromJson(content);
    }

    public abstract void onError(ErrorResult errorResult);
}

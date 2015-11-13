package socketConnection.listener;

import android.content.Context;

import com.baidao.data.Jsonable;
import com.baidao.socketConnection.model.ErrorResult;
import com.baidao.socketConnection.network.Packet;
import com.baidao.socketConnection.network.SocketConnection;

/**
 * Created by rjhy on 15-3-2.
 */
public abstract class BasePacketListener<T extends Jsonable> implements PacketListener<T> {
    protected Context context;
    protected BasePacketListener(Context context){
        this.context = context;
    }

    @Override
    public void processPacket(Packet<T> packet, SocketConnection socketConnection) {
        if (packet.getHeader().statusCode == 200) {
            T t = getContent(packet.getTempContent());
            packet.setContent(t);
            onProcess(t, socketConnection);
        } else {
            onFailure(ErrorResult.fromJson(packet.getTempContent()), socketConnection);
        }
    }

    public abstract void onProcess(T result, SocketConnection socketConnection);

    public void onFailure(ErrorResult errorResult, SocketConnection socketConnection) {

    }
}

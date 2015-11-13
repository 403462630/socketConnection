package socketConnection.listener;


import com.baidao.socketConnection.network.SocketConnection;

/**
 * Created by Bruce on 12/29/14.
 */
public interface ConnectionListener {
    void connected(SocketConnection connection);

    void connectionClosed();

    void connectionClosedOnError(Exception exception);

    void reconnectingIn(int time);
}

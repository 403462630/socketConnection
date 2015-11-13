package com.baidao.socketconnection.listener;

import com.baidao.socketconnection.SocketConnection;

public interface ConnectionListener {
    void connected(SocketConnection connection);

    void connectionClosed();

    void connectionError(Exception exception);

    void reconnectingIn(int time);
}
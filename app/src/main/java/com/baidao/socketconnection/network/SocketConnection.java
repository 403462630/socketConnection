package com.baidao.socketconnection.network;

import android.util.Log;

import com.baidao.socketconnection.listener.ConnectionListener;
import com.baidao.socketconnection.listener.PacketListener;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * Created by rjhy on 15-11-12.
 */
public class SocketConnection {
    private static final String TAG = "SocketConnection";
    private static final int DEFAULT_CONNECT_TIMEOUT = 5000;
    private Socket socket;
    private String host;
    private int port;
    private BufferedSource reader;
    private BufferedSink writer;
    private PacketManager packetManager;
    protected final Collection<ConnectionListener> connectionListeners = new CopyOnWriteArrayList();
    protected final Collection<PacketListener> packetListeners = new CopyOnWriteArrayList<>();
    private ReconnectionManager reconnectionManager;
    private PacketFactory factory;

    public void setSendTimeout(int sendTimeout) {
        packetManager.setSendTimeout(sendTimeout);
    }

    public void setResendTimes(int times) {
        packetManager.setResendTimes(times);
    }

    public void setIsAutoReconnect(boolean isAutoReconnect) {
        this.isAutoReconnect = isAutoReconnect;
    }

    private boolean isAutoReconnect = true;

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private boolean authed = false;

    public void setIsAutoAuth(boolean isAutoAuth) {
        this.isAutoAuth = isAutoAuth;
    }

    private boolean isAutoAuth = false;

    PacketFactory getFactory() {
        return factory;
    }

    public void setFactory(PacketFactory factory) {
        this.factory = factory;
    }

    public SocketConnection(){
        this.packetManager = new PacketManager(this);
        this.reconnectionManager = new ReconnectionManager(this);
    }

    public SocketConnection(String host, int port) {
        this.host = host;
        this.port = port;
        this.packetManager = new PacketManager(this);
        this.reconnectionManager = new ReconnectionManager(this);
        if (isAutoReconnect) {
            reconnectionManager.start();
        } else {
            reconnectionManager.stop();
        }
    }

    public void addConnectionListener(ConnectionListener connectionListener) {
        if (connectionListeners.contains(connectionListener)) {
            return;
        }
        this.connectionListeners.add(connectionListener);
    }

    public void removeConnectionListener(ConnectionListener connectionListener) {
        this.connectionListeners.remove(connectionListener);
    }

    public void addPacketListener(PacketListener packetListener) {
        if (packetListeners.contains(packetListener)) {
            return;
        }
        packetListeners.add(packetListener);
    }

    public void removePacketListener(PacketListener packetListener) {
        packetListeners.remove(packetListener);
    }

    public void setServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void sendPacket(Packet packet) {
        if (!isConnected()) {
            Log.i(TAG, "socket is closed");
            handleFailedPacket(packet);
            return ;
        }
        packetManager.sendPacket(packet);
    }

    public Socket newSocket(String host, int port) throws Exception {
        Socket socket = new Socket();
        socket.setKeepAlive(true);
        socket.setTcpNoDelay(true);
        Log.i(TAG, "connecting...");
        socket.connect(new InetSocketAddress(host, port), connectTimeout);
        return socket;
    }

    private void initConnection() throws Exception {
        reader = Okio.buffer(Okio.source(socket.getInputStream()));
        writer = Okio.buffer(Okio.sink(socket.getOutputStream()));
        packetManager.start();
    }

    public final synchronized void connect(boolean isForce) {
        try {
            if (isForce) {
                disconnect();
                packetManager.clearCachePacket();
            }
            if (!isConnected()) {
                this.socket = newSocket(host, port);
                initConnection();
                notifyConnected();
                if (isAutoAuth) {
                    Packet packet = factory.getAuthPacket();
                    if (packet != null) {
                        sendPacket(packet);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "------connect exception..." + e.getClass().getName());
            notifyConnectError(e);
        }
    }

    public final void connect() {
        connect(false);
    }

    public final boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public boolean isAuthed() {
        return authed;
    }

    public void setAuthed(boolean authed) {
        this.authed = authed;
    }

    public final void disconnect() {
        authed = false;
        if (packetManager != null) {
            packetManager.stop();
            packetManager.clearPacketPool();
        }
        if (!isConnected()) {
            return ;
        }
        try {
            socket.shutdownOutput();
            socket.shutdownInput();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        notifyConnectClose();
        Log.i(TAG, "socket disconnected");
    }

    public final void release() {
        if (reconnectionManager != null) {
            reconnectionManager.stop();
        }
        connectionListeners.clear();
        packetListeners.clear();
        disconnect();
        Log.i(TAG, "--socketconnection is released");
    }

    public BufferedSink getWriter() {
        return writer;
    }

    public BufferedSource getReader() {
        return reader;
    }

    void notifyConnected() {
        for (ConnectionListener connectionListener : connectionListeners) {
            connectionListener.connected(this);
        }
    }

    void notifyConnectError(Exception e) {
        disconnect();
        for (ConnectionListener connectionListener : connectionListeners) {
            connectionListener.connectionError(e);
        }
    }

    void notifyConnectClose() {
        for (ConnectionListener connectionListener : connectionListeners) {
            connectionListener.connectionClosed();
        }
    }

    void handleReceiverException(Exception e) {
        if (e instanceof SocketException) {
            notifyConnectError(e);
        }
    }

    void handleWriteException(Exception e) {
        if (e instanceof SocketException) {
            notifyConnectError(e);
        }
    }

    void handleFailedPacket(Packet packet) {
        for (PacketListener packetListener : packetListeners) {
            if (packetListener.shouldProcess(packet)) {
                packetListener.processPacket(packet, this);
            }
        }
    }

    void handleReceiverPacket(Packet packet) {
        for (PacketListener packetListener : packetListeners) {
            if (packetListener.shouldProcess(packet)) {
                packetListener.processPacket(packet, this);
            }
        }
    }
}

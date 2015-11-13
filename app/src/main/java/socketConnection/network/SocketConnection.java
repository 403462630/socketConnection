package socketConnection.network;

import android.util.Log;

import com.baidao.socketConnection.listener.ConnectionListener;
import com.baidao.socketConnection.listener.PacketListener;
import com.baidao.socketConnection.model.Empty;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * Created by Bruce on 12/25/14.
 */
public class SocketConnection {
    private static final String TAG = "SocketConnection";
    private static final int HEART_BEAT_INTERVAL =  20_000;

    private Socket socket;
    private String host;
    private int port;
    private BufferedSource reader;
    private BufferedSink writer;
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);
    protected final Collection<ConnectionListener> connectionListeners = new CopyOnWriteArrayList();
    protected final Collection<PacketListener> packetListeners = new CopyOnWriteArrayList<>();
    protected int connectionCounterValue;
    private PacketWriter packetWriter;
    private PacketReader packetReader;
    private Timer timer;
    private boolean authed = false;

    public SocketConnection(String host, int port) {
        this.connectionCounterValue = connectionCounter.getAndIncrement();
        this.host = host;
        this.port = port;
        ReconnectionManager.getInstance().bind(this);
    }

    public boolean isAuthed() {
        return authed;
    }

    public void setAuthed(boolean authed) {
        this.authed = authed;
    }

    public boolean isSocketClosed() {
        return socket == null || socket.isClosed() || !socket.isConnected();
    }

    public BufferedSource getReader() {
        return reader;
    }

    public BufferedSink getWriter() {
        return writer;
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

    public void connect() {
        try {
            disconnect();
            Log.i(TAG, "------connect...");
            socket = new Socket(host, port);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
//            socket.setSoTimeout(1000);

            initConnection();

            for (ConnectionListener connectionListener : connectionListeners) {
                connectionListener.connected(this);
            }
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, "------connect..." + e.getClass().getName());
            notifyConnectException(e);
        }
    }

    private void notifyConnectException(Exception exception) {
        for (ConnectionListener connectionListener : connectionListeners) {
            connectionListener.connectionClosedOnError(exception);
        }
    }

    private void notifyConnectionClosed() {
        for (ConnectionListener connectionListener : connectionListeners) {
            connectionListener.connectionClosed();
        }
    }

    public void disconnect() {
        authed = false;
        if (isSocketClosed()) {
            return;
        }
        stopHeartBeat();
        packetReader.stop();
        packetWriter.stop();
        try {
            socket.shutdownInput();
            socket.shutdownOutput();
            socket.close();
            notifyConnectionClosed();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.v(TAG, "socket disconnected");
    }

    private void initReaderAndWriter() {
        try {
            reader = Okio.buffer(Okio.source(socket.getInputStream()));
            writer = Okio.buffer(Okio.sink(socket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initConnection() throws IOException {
        if (isSocketClosed()) {
            throw new SocketException();
        }
        boolean isFirstInitialization = packetReader == null || packetWriter == null;

        initReaderAndWriter();
        if (isFirstInitialization) {
            this.packetWriter = new PacketWriter(this);
            this.packetReader = new PacketReader(this);
        }
        this.packetWriter.start();
        startHeartBeat();
        this.packetReader.start();
    }

    public void sendPacket(Packet packet) {
        if (isSocketClosed()) {
            Log.v(TAG, "socket close on sendPacket");
            return;
        }
        packetWriter.sendPacket(packet);
    }

    private void startHeartBeat() {
        stopHeartBeat();
        timer = new Timer();
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.v(TAG, "heartbeat");
                sendPacket(new Packet.PacketBuilder().withCommand(Command.PUMP).withContent(new Empty()).build());
            }
        };
        timer.scheduleAtFixedRate(timerTask, 0, HEART_BEAT_INTERVAL);
    }

    private void stopHeartBeat() {
        if (null != timer) {
            timer.cancel();
        }
    }

    void handlerReceivedPacket(Packet packet) {
        for (PacketListener packetListener : packetListeners) {
            if (packetListener.shouldProcess(packet)) {
                packetListener.processPacket(packet, this);
            }
        }
    }

    void handleReadWriteError(Exception e) {
        if (e instanceof SocketException) {
            notifyConnectionError(e);
        }
    }

    void notifyConnectionError(Exception exception) {
        authed = false;
        stopHeartBeat();
        packetReader.stop();
        packetWriter.stop();
        for (ConnectionListener connectionListener : connectionListeners) {
            connectionListener.connectionClosedOnError(exception);
        }
    }
}

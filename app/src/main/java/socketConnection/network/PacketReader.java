package socketConnection.network;

import android.util.Log;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;

import hugo.weaving.DebugLog;
import okio.BufferedSource;

/**
 * Created by Bruce on 12/25/14.
 */
class PacketReader {
    private static final String TAG = "PacketReader";

    private Thread readerThread;
    private SocketConnection connection;
    private BufferedSource reader;
    private boolean isReading = false;
    private long index;

    protected PacketReader(SocketConnection connection) {
        this.connection = connection;
    }

    public void start() {
        index = 0;
        isReading = true;
        this.reader = connection.getReader();
        if (readerThread != null && readerThread.isAlive()) {
            Log.d(TAG, "readerThread: " + connection.connectionCounterValue + " is alive");
            return;
        }

        readerThread = new Thread() {
            public void run() {
                read();
            }
        };
        readerThread.setName("Packet Reader (" + connection.connectionCounterValue + ")");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    @DebugLog
    public void stop() {
        isReading = false;
    }

    private void read() {
        while (isReading) {
            Packet packet = null;
            try {
                packet = Packet.build(reader);
                if (packet != null) {
                    Log.v(TAG, "-------------------receive: cmd: " + packet.getHeader().cmd + ", seq: " + packet.getHeader().seg + "---statusCode: " + packet.getHeader().statusCode);
                    connection.handlerReceivedPacket(packet);
                }
                index = 0;
            } catch (EOFException e) {
                Log.v(TAG, "not receive anything from server");
                index ++;
                if (index >= 100) {
                    Log.v(TAG, "not receive anything from server; index=" + index);
                    index = 0;
                    connection.handleReadWriteError(new SocketException(e.getMessage()));
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.v(TAG, "----Exception: " + e.getClass().getName());
                connection.handleReadWriteError(e);
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

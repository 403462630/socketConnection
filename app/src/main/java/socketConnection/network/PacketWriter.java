package socketConnection.network;

import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import okio.BufferedSink;

/**
 * Created by Bruce on 12/25/14.
 */
public class PacketWriter {
    private static final String TAG = "PacketWriter";

    private Thread writerThread;
    private SocketConnection connection;
    private BufferedSink writer;
    private final BlockingQueue<Packet> queue = new ArrayBlockingQueue<>(500, true);
    private boolean done = false;
//    private short sequence = 0;

    public PacketWriter(SocketConnection connection) {
        this.connection = connection;
    }

//    private synchronized short nextSequence() {
//        if (sequence == Short.MAX_VALUE) {
//            sequence = 0;
//        }
//
//        return sequence++;
//    }

    public void sendPacket(Packet packet) {
        if (this.done) {
            return;
        }

        try {
//            packet.setSequence(nextSequence());
            queue.put(packet);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        synchronized (queue) {
            queue.notifyAll();
        }
    }

    public void start() {
        done = false;
//        sequence = 0;
        this.writer = connection.getWriter();
        if (null != writerThread && writerThread.isAlive()) {
            Log.v(TAG, "writerThread: " + writerThread.getName() + " isAlive");
            return;
        }

        writerThread = new Thread() {
            public void run() {
                PacketWriter.this.writePackets(this);
            }
        };
        writerThread.setName("Packet Writer (" + connection.connectionCounterValue + ")");
        writerThread.setDaemon(true);
        writerThread.start();
        Log.d(TAG, "start writerThread: " + writerThread.getName());
    }

    public void stop() {
        this.done = true;

        synchronized (queue) {
            queue.notifyAll();
        }
    }

    private @Nullable Packet nextPacket() {
        Packet packet = null;
        while (!this.done && (packet = queue.poll()) == null) {
            try {
                synchronized (queue) {
                    queue.wait();
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return packet;
    }

    private void writePackets(Thread thread) {
        while (!this.done && this.writerThread == thread) {
            Packet packet = nextPacket();
            if (packet != null) {
                try {
                    writer.write(packet.toBytes());
                    writer.flush();
                    Log.v(TAG, "send packet: " + packet.getHeader().cmd + ", seq: " + packet.getHeader().seg);
                    if (packet.getContent() != null) {
                        Log.v(TAG, "send content: " + packet.getContent().toJson());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.v(TAG, "thread name: " + Thread.currentThread().getName());
                    notifyWriteError(e);
                }
            }
        }
    }

    private void notifyWriteError(Exception e) {
        connection.handleReadWriteError(e);
    }
}

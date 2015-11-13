package com.baidao.socketconnection;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import okio.BufferedSink;

/**
 * Created by rjhy on 15-11-11.
 */
public class PacketWriter {
    private static final String TAG = "PacketWriter";
    private BufferedSink writer;
    private PacketManager packetManager;
    private BlockingQueue<Packet> packetQueue = new ArrayBlockingQueue<Packet>(500, true);
    private static AtomicInteger integer = new AtomicInteger();

    private Thread writerThread;
    private boolean isStop = false;

    public PacketWriter(PacketManager packetManager) {
        this.packetManager = packetManager;
    }

    public void stop() {
        isStop = true;
        synchronized (packetQueue) {
            packetQueue.notifyAll();
        }
    }

    public void start() {
        writer = packetManager.getWriter();
        isStop = false;
        if (writerThread != null && writerThread.isAlive()) {
            Log.i(TAG, "WriterThread: " + writerThread.getName() + " is alive");
            return ;
        }

        writerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                write();
            }
        });
        writerThread.setName("Packet write thread_" + integer.incrementAndGet());
        writerThread.setDaemon(true);
        writerThread.start();
        Log.i(TAG, "start writerThread: " + writerThread.getName());
    }

    private Packet nextPacket() {
        Packet packet = null;
        while (!isStop && (packet = packetQueue.poll()) == null) {
            try {
                packetQueue.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return packet;
    }

    public void writerPacket(Packet packet) {
        try {
            if (packet != null) {
                packetQueue.put(packet);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Log.i(TAG, "writerPacket InterruptedException");
            packetManager.handleFailedPacket(packet);
        }
        synchronized (packetQueue) {
            packetQueue.notifyAll();
        }
    }

    public void clear() {
        Packet packet = null;
        while ((packet = packetQueue.poll()) != null) {
            packetManager.handleClearPacket(packet);
        }
    }

    private void write() {
        while (!isStop) {
            Packet packet = nextPacket();
            if (packet != null && !packet.isExpread()) {
                try {
                    packetManager.handleWrite(packet);
                    if (!packet.isExpread()) {
                        writer.write(packet.toBytes());
                        writer.flush();
                        Log.i(TAG, "send packet content: " + packet.toString());
                    } else {
                        Log.i(TAG, "send packet isExpread true, packetId = " + packet.getPacketId());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    packetManager.handleWriteException(e);
                    packetManager.handleFailedPacket(packet);
                }
            }
        }
    }
}

package com.baidao.socketconnection.network;

import android.util.Log;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicInteger;

import okio.BufferedSource;

/**
 * Created by rjhy on 15-11-11.
 */
public class PacketReader {
    private static final String TAG = "PacketReader";
    private BufferedSource reader;
    private Thread readerThread;
    private PacketManager packetManager;
    private static AtomicInteger integer = new AtomicInteger();
    private boolean isStop = false;
    private PacketPool pool;
    private long index;

    public PacketReader(PacketManager packetManager) {
        this.packetManager = packetManager;
        pool = new PacketPool();
    }

    void clearPacketPool() {
        if (pool != null) {
            pool.clear();
        }
    }

    void stop() {
        this.isStop = true;
    }

    void start() {
        reader = packetManager.getReader();
        isStop = false;
        index = 0;
        if (readerThread != null && readerThread.isAlive()) {
            Log.i(TAG, "ReaderThread: " + readerThread.getName() + " is alive");
            return ;
        }
        readerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                read();
            }
        });
        readerThread.setName("Packet write thread_" + integer.incrementAndGet());
        readerThread.setDaemon(true);
        readerThread.start();
        Log.i(TAG, "start readerThread: " + readerThread.getName());
    }

    private void read() {
        while (!isStop) {
            Packet packet = null;
            try {
                packet = packetManager.getPacketFactory().buildPacket(reader);
                if (packet != null) {
                    if (packet.hasNextSubPacket()) {
                        pool.add(packet);
                    } else {
                        packet = pool.get(packet);
                        packetManager.handleReceiverPacket(packet);
                        Log.v(TAG, "---------receive packet content: " + packet.toString());
                    }
                }
                index = 0;
            } catch (EOFException e) {
                Log.v(TAG, "not receive anything from server");
                index ++;
                if (index > 10) {
                    Log.v(TAG, "not receive anything from server; index=" + index);
                    index = 0;
                    packetManager.handleReceiverException(new SocketException(e.getMessage()));
                }
            } catch (IOException e) {
                e.printStackTrace();
                packetManager.handleReceiverException(e);
            }
//            try {
//                Thread.sleep(100);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
        }
    }
}

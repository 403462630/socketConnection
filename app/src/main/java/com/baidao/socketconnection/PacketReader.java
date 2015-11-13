package com.baidao.socketconnection;

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

    public PacketReader(PacketManager packetManager) {
        this.packetManager = packetManager;
        pool = new PacketPool();
    }

    public void stop() {
        this.isStop = true;
    }

    public void start() {
        reader = packetManager.getReader();
        isStop = false;
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
                packet = Packet.build(reader);
                if (packet != null) {
                    if (packet.header.hasNextSubPacket()) {
                        pool.add(packet);
                    } else {
                        packet = pool.get(packet);
                        packetManager.handleReceiverPacket(packet);
                        Log.v(TAG, "---------receive packet content: " + packet.toString());
                    }
                }
            } catch (EOFException e) {
                Log.v(TAG, "not receive anything from server");
            } catch (IOException e) {
                e.printStackTrace();
                packetManager.handleReceiverException(e);
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}

package com.baidao.socketconnection.network;

import android.os.SystemClock;
import android.util.Log;

import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import okio.BufferedSink;
import okio.BufferedSource;

/**
 * Created by rjhy on 15-11-11.
 */
public class PacketManager {
    private static final String TAG = "PacketManager";
    private static final int FLAG_RECEIVER = 1;
    private static final int FLAG_FAILED = -1;
    private static final int DEFAULT_RE_SEND_TIMES = 3;
    private static final long DEAULT_TIME_OUT = 20 * 1000;
    private SocketConnection socketConnection;
    private PacketWriter packetWriter;
    private PacketReader packetReader;
    private BlockingQueue<Packet> packetQueue = new ArrayBlockingQueue<Packet>(1000, true);
    private static AtomicInteger integer = new AtomicInteger();
    private HashMap<String, PacketTask> map = new HashMap<>();
    private Thread managerThread;
    private boolean isStop;
    private HeartBeat heartBeat;

    public void setSendTimeout(long sendTimeout) {
        this.sendTimeout = sendTimeout;
    }

    private long sendTimeout = DEAULT_TIME_OUT;

    public void setResendTimes(int resendTimes) {
        this.resendTimes = resendTimes;
    }

    public boolean isAuthed() {
        return socketConnection.isAuthed();
    }

    private int resendTimes = DEFAULT_RE_SEND_TIMES;

    public PacketManager(SocketConnection socketConnection) {
        this.socketConnection = socketConnection;
        packetWriter = new PacketWriter(this);
        packetReader = new PacketReader(this);
        heartBeat = new HeartBeat(socketConnection);
    }

    void start() {
        if (managerThread != null && managerThread.isAlive()) {
            Log.i(TAG, "ManagerThread: " + managerThread.getName() + " is alive");
        } else {
            managerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    handle();
                }
            });
            managerThread.setName("Packet Manager thread_" + integer.incrementAndGet());
            managerThread.setDaemon(true);
            managerThread.start();
            Log.i(TAG, "start ManagerThread: " + managerThread.getName());
        }
        isStop = false;

        if (packetWriter == null) {
            packetWriter = new PacketWriter(this);
        }
        if (packetReader == null) {
            packetReader = new PacketReader(this);
        }
        packetReader.start();
        packetWriter.start();
        heartBeat.start();
    }

    void stop() {
        heartBeat.stop();
        if (packetWriter != null) {
            packetWriter.stop();
        }
        if (packetReader != null) {
            packetReader.stop();
        }
        isStop = true;
        synchronized (packetQueue) {
            packetQueue.notifyAll();
        }
    }

    PacketFactory getPacketFactory() {
        return socketConnection.getFactory();
    }

    BufferedSink getWriter() {
        return socketConnection.getWriter();
    }

    BufferedSource getReader() {
        return socketConnection.getReader();
    }

    void clearCachePacket() {
        packetWriter.clearCachePacket();
    }

    void clearPacketPool() {
        packetReader.clearPacketPool();
    }

    void sendPacket(Packet packet) {
        if (packetWriter == null) {
            Log.i(TAG, "packetWriter is null");
            handleFailedPacket(packet);
            return ;
        }
        if (!packet.isHeartBeatPacket()) {
            startPacketTask(packet);
        }
        packetWriter.writerPacket(packet);
    }

    private void startPacketTask(Packet packet) {
        PacketTask task = new PacketTask(this, packet);
        task.setTimeout(sendTimeout);
        task.execute();
        putPacketTask(task);
    }

    private void stopPacketTask(Packet packet) {
        PacketTask task = map.get(packet.getPacketId());
        if (task != null) {
            removePacketTast(task.getTaskId());
            task.cancel();
        }
    }

    void putPacketTask(PacketTask task) {
        String key = task.getTaskId();
        if (key != null) {
            map.put(key, task);
        }
    }

    void removePacketTast(String taskId) {
        map.remove(taskId);
    }

    void handleWrite(Packet packet) {
        PacketTask task = map.get(packet.getPacketId());
        if (task != null) {
            long time = SystemClock.elapsedRealtime();
            if (time - task.startTime < PacketTask.TIME_OUT) {
                stopPacketTask(packet);
                startPacketTask(packet);
                Log.i(TAG, "stop and restart packet task, packetId = " + packet.getPacketId());
            }
        }
    }

    void handleReceiverException(Exception e) {
        socketConnection.handleReceiverException(e);
    }

    void handleWriteException(Exception e) {
        socketConnection.handleWriteException(e);
    }

    void handleFailedPacket(Packet packet) {
        stopPacketTask(packet);
        if (!packet.isExpread() && packet.getReSendTimes() < resendTimes && !packet.isHeartBeatPacket()) {
            Log.i(TAG, "resend packet. packetId: " + packet.getPacketId());
            sendPacket(packet);
        } else {
            packet.setFlag(FLAG_FAILED);
            handlePacket(packet);
        }
    }

    void handleClearPacket(Packet packet) {
        stopPacketTask(packet);
        if (!packet.isExpread()) {
            packet.setFlag(FLAG_FAILED);
            handlePacket(packet);
        }
    }

    void handleReceiverPacket(Packet packet) {
        stopPacketTask(packet);
        packet.setFlag(FLAG_RECEIVER);
        handlePacket(packet);
    }

    void handleTimeOutPacket(Packet packet) {
        stopPacketTask(packet);
        packet.setFlag(FLAG_FAILED);
        handlePacket(packet);
    }

    private void handlePacket(Packet packet) {
        if (packet.isHeartBeatPacket()) {
            return ;
        }
        if (isStop) {
            handle(packet);
        } else {
            try {
                if (packet != null) {
                    packetQueue.put(packet);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                Log.i(TAG, "handlePacket InterruptedException");
                handle(packet);
            }
            synchronized (packetQueue) {
                packetQueue.notifyAll();
            }
        }
    }

    private Packet nextPacket() {
        Packet packet = null;
        while ((packet = packetQueue.poll()) == null) {
            try {
                if (!isStop) {
                    synchronized (packetQueue) {
                        packetQueue.wait();
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return packet;
    }

    private void handle(Packet packet) {
        if (packet.isHeartBeatPacket()) {
            return ;
        }
        if (packet != null) {
            switch (packet.getFlag()) {
                case FLAG_RECEIVER:
                    Log.i(TAG, "handle reverver packet. packetId: " + packet.getPacketId());
                    socketConnection.handleReceiverPacket(packet);
                    break;
                case FLAG_FAILED:
                    Log.i(TAG, "handle failed packet. packetId: " + packet.getPacketId());
                    socketConnection.handleFailedPacket(packet);
                    break;
            }
        }
    }

    private void handle() {
        Packet packet = null;
        while((packet = nextPacket()) != null || !isStop) {
            if (packet != null) {
                handle(packet);
            }
        }
        Log.i(TAG, "handle packet queue end");
    }
}

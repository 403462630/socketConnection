package com.baidao.socketconnection;

import android.os.SystemClock;
import android.util.Log;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by rjhy on 15-11-12.
 */
public class PacketTask {
    private static final String TAG = "PacketTask";
    private Packet packet;
    static final long TIME_OUT = 20 * 1000;
    private Timer timer;
    private PacketManager packetManager;
    long startTime;
    private long timeout = TIME_OUT;

    void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public PacketTask(PacketManager packetManager, Packet packet) {
        this.packetManager = packetManager;
        this.packet = packet;
    }

    public void execute() {
        if (packet == null) {
            return ;
        }
        startTime = SystemClock.elapsedRealtime();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Log.i(TAG, "send packet time out, packetId = " + packet.getPacketId());
                packet.setIsExpread(true);
                packetManager.handleTimeOutPacket(packet);
            }
        };
        timer.schedule(task, timeout);
    }

    public void cancel() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public String getTaskId() {
        return packet == null ? null : packet.getPacketId();
    }
}

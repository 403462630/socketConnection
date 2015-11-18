package com.baidao.socketconnection.network;

import java.util.Timer;
import java.util.TimerTask;

public class HeartBeat {
    private static final String TAG = "HeartBeat";
    private static final int HEART_BEAT_INTERVAL =  10_000;
    private SocketConnection socketConnection;
    private Timer timer;

    public HeartBeat(SocketConnection socketConnection) {
        this.socketConnection = socketConnection;
    }

    public void start() {
        stop();
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                PacketFactory factory = socketConnection.getFactory();
                if (factory != null) {
                    Packet packet = factory.getHeartBeat();
                    if (packet != null) {
                        socketConnection.sendPacket(packet);
                    }
                }
            }
        };
        timer.schedule(task, 0, HEART_BEAT_INTERVAL);
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
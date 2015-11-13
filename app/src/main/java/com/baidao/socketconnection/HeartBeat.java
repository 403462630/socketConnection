package com.baidao.socketconnection;

import java.util.Timer;
import java.util.TimerTask;

public class HeartBeat {
    private static final String TAG = "HeartBeat";
    private static final int HEART_BEAT_INTERVAL =  10_000;
    private SocketConnection socketConnection;
    private HeartBeatFactory factory;
    private Timer timer;

    public HeartBeat(SocketConnection socketConnection) {
        this.socketConnection = socketConnection;
        this.factory = socketConnection.getFactory();
    }

    public void start() {
        stop();
        timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Packet packet = factory.getHeartBeat();
                if (packet != null) {
                    socketConnection.sendPacket(packet);
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
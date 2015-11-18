package com.baidao.socketconnection.network;

import android.util.Log;

import com.baidao.socketconnection.listener.ConnectionListener;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by rjhy on 15-11-13.
 */
public class ReconnectionManager {
    private static final String TAG = "ReconnectionManager";
    private ReconnectionThread connectThread;
    private static AtomicInteger integer = new AtomicInteger();
    private static int RANDOM_BASE = 5;
    private boolean isStop;
    private boolean isConnected;
    private SocketConnection socketConnection;

    ReconnectionManager(SocketConnection socketConnection) {
        this.socketConnection = socketConnection;
        socketConnection.addConnectionListener(connectionListener);
    }

    private ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void connected(SocketConnection connection) {
            Log.i(TAG, "connected");
            isConnected = true;
            if (null != connectThread) {
                connectThread.resetAttempts();
            }
        }

        @Override
        public void connectionClosed() {
            Log.i(TAG, "connectionClosed");
            isConnected = false;
            if (null != connectThread) {
                connectThread.resetAttempts();
            }
        }

        @Override
        public void connectionError(Exception exception) {
            Log.i(TAG, "connectionError");
            isConnected = false;
            if (!isReconnectionAllowed()) {
                return;
            }
            reconnect();
        }

        @Override
        public void reconnectingIn(int time) {
            Log.i(TAG, "reconnectingIn " + time);
        }
    };

    private boolean isReconnectionAllowed() {
        return !isConnected && !this.isStop;
    }

    void stop() {
        isStop = true;
    }

    void start() {
        isStop = false;
    }

    private void reconnect() {
        if (isReconnectionAllowed()) {
            if (connectThread != null && connectThread.isAlive()) {
                Log.i(TAG, "ReconnectThread: " + connectThread.getName() + " is alive");
                return ;
            }
            connectThread = new ReconnectionThread();
            connectThread.setName("Reconnection thread_" + integer.incrementAndGet());
            connectThread.setDaemon(true);
            connectThread.start();
            Log.i(TAG, "start Reconnection thread: " + connectThread.getName());
        } else {
            Log.i(TAG, "isReconnectionAllowed: false");
        }
    }

    class ReconnectionThread extends Thread {
        private int attempts = 0;

        ReconnectionThread() {
        }

        public void resetAttempts() {
            this.attempts = 0;
        }

        private int timeDelay() {
            ++this.attempts;
            return this.attempts > 9 ? ReconnectionManager.this.RANDOM_BASE * 3
                    : ReconnectionManager.this.RANDOM_BASE;
        }

        public void run() {
            while (ReconnectionManager.this.isReconnectionAllowed()) {
                int timeDelay = this.timeDelay();

                while (ReconnectionManager.this.isReconnectionAllowed() && timeDelay > 0) {
                    try {
                        Thread.sleep(1000L);
                        --timeDelay;
                        for (ConnectionListener listener : socketConnection.connectionListeners) {
                            listener.reconnectingIn(timeDelay);
                        }
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }

                if (socketConnection.isConnected()) {
                    Log.d(TAG, "socket is already connected");
                    isConnected = true;
                } else {
                    if (isReconnectionAllowed()) {
                        socketConnection.connect();
                    }
                }
            }
        }
    }
}

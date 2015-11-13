package socketConnection.network;

import android.util.Log;

import com.baidao.socketConnection.listener.ConnectionListener;

/**
 * Created by Bruce on 12/29/14.
 */
public class ReconnectionManager {
    private static String TAG = "ReconnectionManager";
    private static int RANDOM_BASE = 5;

    private SocketConnection connection;
    private ReconnectionManager.ReconnectionThread reconnectionThread;
    private boolean done = false;
    private static ReconnectionManager instance = new ReconnectionManager();

    private ReconnectionManager() {}

    public static ReconnectionManager getInstance() {
        return instance;
    }

    public void bind(SocketConnection connection) {
        if (this.connection != null) {
            this.connection.removeConnectionListener(connectionListener);
        }
        this.connection = connection;
        this.connection.addConnectionListener(connectionListener);
    }

    public boolean isReconnectionAllowed() {
        return !this.done;
    }

    public synchronized void reconnect() {
        if(this.isReconnectionAllowed()) {
            if(this.reconnectionThread != null && this.reconnectionThread.isAlive()) {
                Log.d(TAG, "reconnect");
                return;
            }

            this.reconnectionThread = new ReconnectionManager.ReconnectionThread();
            this.reconnectionThread.setName("Reconnection Manager");
            this.reconnectionThread.setDaemon(true);
            this.reconnectionThread.start();
            Log.d(TAG, "reconnect");
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
            while(ReconnectionManager.this.isReconnectionAllowed()) {
                int timeDelay = this.timeDelay();

                while(ReconnectionManager.this.isReconnectionAllowed() && timeDelay > 0) {
                    try {
                        Thread.sleep(1000L);
                        --timeDelay;
                        for (ConnectionListener listener : connection.connectionListeners) {
                            listener.reconnectingIn(timeDelay);
                        }
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                }

                try {
                    if(isReconnectionAllowed()) {
                        connection.connect();
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                    connection.notifyConnectionError(exception);
                }
            }

        }
    }

    private ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void connected(SocketConnection connection) {
            Log.d(TAG, "connected");
            done = true;
            if (null != reconnectionThread) {
                reconnectionThread.resetAttempts();
            }
        }

        @Override
        public void connectionClosed() {
            Log.d(TAG, "connectionClosed");
            done = true;
            if (null != reconnectionThread) {
                reconnectionThread.resetAttempts();
            }
        }

        @Override
        public void connectionClosedOnError(Exception exception) {
            Log.d(TAG, "connectionClosedOnError");
            done = false;
            if (!isReconnectionAllowed()) {
                return;
            }

            reconnect();
        }

        @Override
        public void reconnectingIn(int time) {
            Log.d(TAG, "reconnectingIn: " + time);
        }
    };
}

package com.baidao.socketconnection.listener;

import com.baidao.socketconnection.Packet;
import com.baidao.socketconnection.SocketConnection;

public interface PacketListener {
    boolean shouldProcess(Packet packet);
    void processPacket(Packet packet, SocketConnection socketConnection);
}
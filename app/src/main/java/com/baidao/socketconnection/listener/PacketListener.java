package com.baidao.socketconnection.listener;


import com.baidao.socketconnection.network.Packet;
import com.baidao.socketconnection.network.SocketConnection;

public interface PacketListener<T> {
    boolean shouldProcess(Packet<T> packet);
    void processPacket(Packet<T> packet, SocketConnection socketConnection);
    T getContent(String content);
}
package com.baidao.socketconnection;

import java.util.HashMap;

/**
 * Created by rjhy on 15-11-12.
 */
public class PacketPool {

    private HashMap<String, Packet> packetPool = new HashMap<>();

    public void add(Packet packet) {
        String id = packet.getPacketId();
        Packet tempPacket = packetPool.get(id);
        if (tempPacket != null) {
            packet.body = tempPacket.body + packet.body;
        }
        packetPool.put(id, packet);
    }

    public Packet get(Packet packet) {
        String id = packet.getPacketId();
        if (packetPool.containsKey(id)) {
            Packet p =  packetPool.get(id);
            packetPool.remove(id);
            return p;
        }
        return packet;
    }
}

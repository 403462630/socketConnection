package com.baidao.socketconnection.network;

import java.io.IOException;

import okio.BufferedSource;

/**
 * Created by rjhy on 15-11-16.
 */
public interface PacketFactory {
    public Packet getHeartBeat();
    public Packet buildPacket(BufferedSource source) throws IOException;
    public Packet getAuthPacket();
}

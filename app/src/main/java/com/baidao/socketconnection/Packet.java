package com.baidao.socketconnection;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicInteger;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;

/**
 * Created by rjhy on 15-11-11.
 */
public class Packet {

    private AtomicInteger reSendTimes = new AtomicInteger();
    protected Header header;
    protected String body;
    int flag;

    private boolean isExpread = false;
    private long sendTime;

    public Packet(Header header, String body) {
        this.header = header;
        this.body = body;
    }

    public byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedSink sink = null;
        try {
            sink = Okio.buffer(Okio.sink(out));
            if (header != null) {
                header.write(sink);
            }
            sink.write(ByteString.encodeUtf8(body));
            sink.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (sink != null) {
                try {
                    sink.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return out.toByteArray();
    }

    public int getReSendTimes() {
        return reSendTimes.getAndIncrement();
    }

    final static Packet build(BufferedSource source) throws IOException {
        Header header = Header.build(source);
        if (header.isValid()) {
            String body = source.readString(header.length(), Charset.forName("utf-8"));
            return new Packet(header, body);
        }
        return null;
    }

    public String getPacketId() {
        return null;
    }

    public String toString() {
        return "";
    }

    final synchronized boolean isExpread() {
        return isExpread;
    }

    final synchronized void setIsExpread(boolean isExpread) {
        this.isExpread = isExpread;
    }

    final long getSendTime() {
        return sendTime;
    }

    final void setSendTime(long sendTime) {
        this.sendTime = sendTime;
    }

    final int getFlag() {
        return flag;
    }

    final void setFlag(int flag) {
        this.flag = flag;
    }
}

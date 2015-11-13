package com.baidao.socketconnection;

import okio.BufferedSink;
import okio.BufferedSource;

/**
 * Created by rjhy on 15-11-11.
 */
public class Header {

    public void read(BufferedSource source) {

    }

    public void write(BufferedSink bufferedSink) {

    }

    public boolean isValid() {
        return true;
    }

    public boolean hasNextSubPacket() {
        return false;
    }

    public int length() {
        return 0;
    }

    final static Header build(BufferedSource source) {
        Header header = new Header();
        header.read(source);
        return header;
    }


    public String toString() {
        return "";
    }
}

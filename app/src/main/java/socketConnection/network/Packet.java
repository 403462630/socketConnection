package socketConnection.network;

import com.baidao.data.Jsonable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.ByteString;
import okio.Okio;

/**
 * Created by Bruce on 12/3/14.
 */
public class Packet<T extends Jsonable> {
    private static final String TAG = "Packet";
    private static final byte START_FLAG = (byte) 0xff;
    private Header header;
    private T content;
    private String tempContent;
    private static short sequence = 0;

    public String getPacketId() {
        return "_" + header.seg;
    }

    private static synchronized short nextSequence() {
        if (sequence == Short.MAX_VALUE) {
            sequence = 0;
        }

        return sequence++;
    }

    public static class PacketBuilder<T extends Jsonable> {
        private Packet packet = new Packet();

        public PacketBuilder withHeader(Header header) {
            packet.header = header;
            packet.header.seg = Packet.nextSequence();
            return this;
        }

        public PacketBuilder withCommand(Command command) {
            if (packet.header == null) {
                packet.header = new Header();
                packet.header.seg = Packet.nextSequence();
            }

            packet.header.cmd = command;
            return this;
        }

        public PacketBuilder withContent(T content) {
            if (packet.getHeader() == null) {
                throw new RuntimeException("Header must be set before content");
            }
            packet.content = content;

            packet.getHeader().length
                    = (short) ByteString.encodeUtf8(content.toJson()).size();
            return this;
        }

        public Packet build() {
            return packet;
        }
    }

    private Packet() {}

    public byte[] toBytes() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedSink bufferedSink = Okio.buffer(Okio.sink(out));
        try {
            writeHeader(bufferedSink);
            if (content != null) {
                ByteString byteString = ByteString.encodeUtf8(content.toJson());
                bufferedSink.write(byteString);
            }
            bufferedSink.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private void writeHeader(BufferedSink bufferedSink) throws IOException {
        bufferedSink.writeByte(START_FLAG);
        bufferedSink.writeByte(header.version);
        bufferedSink.writeByte(header.isEncrypt);
        bufferedSink.writeByte(header.isFrag);
        bufferedSink.writeShort(header.length);
        bufferedSink.writeShort(header.cmd.getValue());
        bufferedSink.writeShort(header.seg);
        bufferedSink.writeShort(header.statusCode);
        bufferedSink.writeInt(header.sid);
        bufferedSink.writeShort(header.fragCount);
        bufferedSink.writeShort(header.fragSeq);
    }

    public static Packet build(BufferedSource source) throws IOException {
        Header header = buildHeader(source);
        if (header == null) {
            return null;
        }

        Packet packet = new Packet();
        packet.header = header;

        String jsonContent = source.readString(header.length, Charset.forName("utf-8"));

        packet.setTempContent(jsonContent);
        return packet;
    }

    private static Header buildHeader(BufferedSource source) throws IOException {
        byte startFlag = source.readByte();
        if (startFlag != START_FLAG) {
            return null;
        }

        Header header = new Header();
        header.version = source.readByte();
        header.isEncrypt = source.readByte();
        header.isFrag = source.readByte();
        header.length = source.readShort();
        header.cmd = Command.fromValue(source.readShort());
        header.seg = source.readShort();
        header.statusCode = source.readShort();
        header.sid = source.readInt();
        header.fragCount = source.readShort();
        header.fragSeq = source.readShort();

        return header;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    public String getTempContent() {
        return tempContent;
    }

    public void setTempContent(String tempContent) {
        this.tempContent = tempContent;
    }

    public Header getHeader() {
        return header;
    }

    public void setSequence(short sequence) {
        if (header == null ) {
            throw new RuntimeException("packet header must not null when call setSequence");
        }
        header.seg = sequence;
    }

    public static class Header {
//      1 byte startFlag should be skipped
        public byte version;
        public byte isEncrypt;
        public byte isFrag;
        public short length;
        public Command cmd;
        public short seg;
        public short statusCode;
        public int sid;
        public short fragCount;
        public short fragSeq;
    }
}
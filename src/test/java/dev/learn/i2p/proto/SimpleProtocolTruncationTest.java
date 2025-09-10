package dev.learn.i2p.proto;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class SimpleProtocolTruncationTest {

    // Простейший «фрейм»: [len(4 bytes BE)] [payload...]
    static ByteBuffer makeFrame(int payloadLen) {
        byte[] p = new byte[payloadLen];
        for (int i = 0; i < payloadLen; i++) p[i] = (byte) (i & 0xFF);
        ByteBuffer buf = ByteBuffer.allocate(4 + payloadLen);
        buf.putInt(payloadLen).put(p).flip();
        return buf;
    }

    static boolean isTruncated(ByteBuffer buf) {
        if (buf.remaining() < 4) return true;
        buf.mark();
        int len = buf.getInt();
        boolean truncated = buf.remaining() < len;
        buf.reset();
        return truncated;
    }

    @Test
    void full_frame_is_not_truncated() {
        ByteBuffer full = makeFrame(1024);
        assertFalse(isTruncated(full));
    }

    @Test
    void header_only_is_truncated() {
        ByteBuffer full = makeFrame(32);
        // Обрезаем до 4 байт заголовка
        ByteBuffer cut = ByteBuffer.allocate(4);
        cut.putInt(32).flip();
        assertTrue(isTruncated(cut));
    }

    @Test
    void partially_cut_payload_is_truncated() {
        ByteBuffer full = makeFrame(128);
        // Оставим 4 + 100 байт
        ByteBuffer cut = ByteBuffer.allocate(4 + 100);
        cut.putInt(128);
        cut.put(new byte[100]);
        cut.flip();
        assertTrue(isTruncated(cut));
    }
}

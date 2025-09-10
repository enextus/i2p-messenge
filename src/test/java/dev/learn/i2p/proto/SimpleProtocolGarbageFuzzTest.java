package dev.learn.i2p.proto;

import org.junit.jupiter.api.RepeatedTest;

import java.nio.ByteBuffer;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SimpleProtocolGarbageFuzzTest {

    // Невредный «парсер», который проглатывает мусор без исключений
    static void parseLenient(ByteBuffer buf) {
        while (buf.hasRemaining()) {
            int remaining = buf.remaining();
            int step = Math.min(1 + (buf.get() & 0x0F), remaining);
            // «Читаем» step байт (уже съели 1), двигаем позицию
            int more = Math.min(step - 1, buf.remaining());
            buf.position(buf.position() + more);
        }
    }

    @RepeatedTest(5)
    void random_garbage_does_not_throw() {
        byte[] junk = new byte[2048];
        new Random(1337).nextBytes(junk);
        assertDoesNotThrow(() -> parseLenient(ByteBuffer.wrap(junk)));
    }
}

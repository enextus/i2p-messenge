package dev.learn.i2p.proto;

import dev.learn.i2p.core.profile.support.ProtocolTestKit;
import dev.learn.i2p.core.profile.support.ProtocolTestKit.ParserAdapter;
import org.junit.jupiter.api.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@Tag("security")
class SimpleProtocolTruncationTest {

    static ParserAdapter P;

    @BeforeAll
    static void loadParser() { P = ParserAdapter.load("dev.learn.i2p.proto.SimpleProtocol"); }

    @Test
    void truncatedHeaderOnly() {
        byte[] valid = ProtocolTestKit.makeFrameWithLenPrefix(ProtocolTestKit.utf8("hello"));
        byte[] cut = ProtocolTestKit.truncated(valid, 2); // меньше 4 байт заголовка
        assertThrows(RuntimeException.class, () -> ProtocolTestKit.withTimeout(() -> P.parse(cut)));
    }

    @Test
    void truncatedAfterHeaderBeforePayloadEnd() {
        byte[] valid = ProtocolTestKit.makeFrameWithLenPrefix(ProtocolTestKit.utf8("world"));
        byte[] cut = ProtocolTestKit.truncated(valid, 4 + 2); // часть полезной нагрузки
        assertThrows(RuntimeException.class, () -> ProtocolTestKit.withTimeout(() -> P.parse(cut)));
    }

    @Test
    void garbageTailDoesNotHang() {
        byte[] valid = ProtocolTestKit.makeFrameWithLenPrefix(ProtocolTestKit.utf8("ok"));
        byte[] noisy = new byte[valid.length + 128];
        System.arraycopy(valid, 0, noisy, 0, valid.length);
        Arrays.fill(noisy, valid.length, noisy.length, (byte) 0xFF);
        ProtocolTestKit.withTimeout(() -> P.parse(noisy)); // допустимо: игнор/ошибка — но не зависание
    }

}

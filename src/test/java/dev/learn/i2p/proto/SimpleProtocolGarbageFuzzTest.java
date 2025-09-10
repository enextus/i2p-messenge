package dev.learn.i2p.proto;

import dev.learn.i2p.core.profile.support.ProtocolTestKit;
import dev.learn.i2p.core.profile.support.ProtocolTestKit.ParserAdapter;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@Tag("security")
class SimpleProtocolGarbageFuzzTest {

    static ParserAdapter P;

    @BeforeAll
    static void loadParser() { P = ParserAdapter.load("dev.learn.i2p.proto.SimpleProtocol"); }

    @RepeatedTest(10)
    void randomGarbageDoesNotHangOrExplode(RepetitionInfo ri) {
        int size = 1 + (ri.getCurrentRepetition() * 257);
        byte[] junk = ProtocolTestKit.randomBytes(size, 0xC0FFEE + ri.getCurrentRepetition());
        try { ProtocolTestKit.withTimeout(() -> P.parse(junk)); }
        catch (RuntimeException e) {
            // ожидаем «предсказуемую» ошибку, но не OOME/SOE и не вечный цикл
            assertFalse(e.getCause() instanceof OutOfMemoryError, "OOM недопустим");
        }
    }

}

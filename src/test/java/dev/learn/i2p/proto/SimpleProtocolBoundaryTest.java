package dev.learn.i2p.proto;

import dev.learn.i2p.core.profile.support.ProtocolTestKit;
import dev.learn.i2p.core.profile.support.ProtocolTestKit.ParserAdapter;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@Tag("security")
class SimpleProtocolBoundaryTest {

    static ParserAdapter P;

    @BeforeAll
    static void loadParser() { P = ParserAdapter.load("dev.learn.i2p.proto.SimpleProtocol"); }

    @Test
    void acceptsMaxBoundary() {
        int n = ProtocolTestKit.DEFAULT_MAX_PAYLOAD; // подстрой, если у вас другой лимит
        byte[] frame = ProtocolTestKit.makeFrameWithLen(n, (byte) 'A');
        ProtocolTestKit.withTimeout(() -> P.parse(frame));
    }

    @Test
    void rejectsOversize() {
        int n = ProtocolTestKit.DEFAULT_MAX_PAYLOAD + 1;
        byte[] frame = ProtocolTestKit.makeFrameWithLen(n, (byte) 'B');
        assertThrows(RuntimeException.class, () -> ProtocolTestKit.withTimeout(() -> P.parse(frame)));
    }

    @Test
    void rejectsNegativeLengthPrefix() {
        byte[] frame = ProtocolTestKit.makeFrameWithLen(-1, (byte) 0x00);
        assertThrows(RuntimeException.class, () -> ProtocolTestKit.withTimeout(() -> P.parse(frame)));
    }

    @Test
    void zeroLengthEitherRejectedOrHandled() {
        byte[] frame = ProtocolTestKit.makeFrameWithLen(0, (byte) 0x00);
        try { ProtocolTestKit.withTimeout(() -> P.parse(frame)); }
        catch (RuntimeException expected) { /* допустим строгое поведение */ }
    }

}

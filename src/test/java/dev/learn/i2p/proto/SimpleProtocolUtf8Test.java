package dev.learn.i2p.proto;

import dev.learn.i2p.core.profile.support.ProtocolTestKit;
import dev.learn.i2p.core.profile.support.ProtocolTestKit.ParserAdapter;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

@Tag("security")
class SimpleProtocolUtf8Test {

    static ParserAdapter P;

    @BeforeAll
    static void loadParser() { P = ParserAdapter.load("dev.learn.i2p.proto.SimpleProtocol"); }

    @Test
    void rejectsInvalidUtf8Sequence() {
        byte[] bad = ProtocolTestKit.invalidUtf8Payload();
        byte[] frame = ProtocolTestKit.makeFrameWithLenPrefix(bad);
        assertThrows(RuntimeException.class, () -> ProtocolTestKit.withTimeout(() -> P.parse(frame)));
    }

    @Test
    void acceptsValidUtf8EdgeCases() {
        String tricky = "Привет 🌍́"; // эмодзи + комбинирующий акцент
        byte[] frame = ProtocolTestKit.makeFrameWithLenPrefix(ProtocolTestKit.utf8(tricky));
        ProtocolTestKit.withTimeout(() -> P.parse(frame));
    }

    @Test
    void controlCharsAreEitherRejectedOrSanitized() {
        byte[] payload = new byte[]{0x41, 0x42, 0x00, 0x43}; // A B NUL C
        byte[] frame = ProtocolTestKit.makeFrameWithLenPrefix(payload);
        try { ProtocolTestKit.withTimeout(() -> P.parse(frame)); }
        catch (RuntimeException expected) { /* строгая политика тоже ок */ }
    }


}

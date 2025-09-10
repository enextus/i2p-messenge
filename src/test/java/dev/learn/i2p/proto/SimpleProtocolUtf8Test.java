package dev.learn.i2p.proto;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;

import static org.junit.jupiter.api.Assertions.*;

class SimpleProtocolUtf8Test {

    @Test
    void valid_utf8_roundtrip() {
        String s = "привет — I2P ✓";
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        String back = new String(bytes, StandardCharsets.UTF_8);
        assertEquals(s, back);
    }

    @Test
    void invalid_utf8_detected_by_decoder() {
        byte[] invalid = {(byte) 0xC3, (byte) 0x28}; // незаконченное 2-байтное
        CharsetDecoder dec = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);

        assertThrows(CharacterCodingException.class, () -> {
            dec.decode(ByteBuffer.wrap(invalid));
        });
    }

    @Test
    void invalid_utf8_can_be_replaced() throws CharacterCodingException {
        byte[] invalid = {(byte) 0xC3, (byte) 0x28}; // незаконченное 2-байтное + валидный '('
        CharsetDecoder dec = StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
                .replaceWith("�");

        CharBuffer cb = dec.decode(ByteBuffer.wrap(invalid));
        assertEquals("�(", cb.toString()); // <-- было "�"
    }

}

package dev.learn.i2p.proto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileTypeTest {

    @Test
    void zip_localHeader_isZip() {
        byte[] head = new byte[] { 'P','K', 3, 4, 0,0,0,0 };
        assertEquals(FileType.ZIP, FileType.sniff(head, head.length));
    }

    @Test
    void zip_empty_isZip() {
        byte[] head = new byte[] { 'P','K', 5, 6, 0,0,0,0 };
        assertEquals(FileType.ZIP, FileType.sniff(head, head.length));
    }

    @Test
    void rar_v4_isRar() {
        byte[] head = new byte[] { 'R','a','r','!', 0x1A, 0x07, 0x00, 0 };
        assertEquals(FileType.RAR, FileType.sniff(head, head.length));
    }

    @Test
    void rar_v5_isRar() {
        byte[] head = new byte[] { 'R','a','r','!', 0x1A, 0x07, 0x01, 0x00 };
        assertEquals(FileType.RAR, FileType.sniff(head, head.length));
    }

    @Test
    void jpeg_isJpeg() {
        byte[] head = new byte[] { (byte)0xFF, (byte)0xD8, (byte)0xFF, 0, 0 };
        assertEquals(FileType.JPEG, FileType.sniff(head, head.length));
    }

    @Test
    void text_utf8_isText() {
        byte[] head = "Привет".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(FileType.TEXT, FileType.sniff(head, head.length));
    }

    @Test
    void binary_noise_isBinary() {
        byte[] head = new byte[] { 0, 1, 2, 3, 0, 0, 0, 0 };
        assertEquals(FileType.BINARY, FileType.sniff(head, head.length));
    }

}

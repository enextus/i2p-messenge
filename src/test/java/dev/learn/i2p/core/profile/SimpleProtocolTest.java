package dev.learn.i2p.core.profile;

import dev.learn.i2p.proto.SimpleProtocol;
import net.i2p.client.streaming.I2PSocket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.Files.list;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SimpleProtocolTest {

    @Test
    void handle_creates_file_in_inbox(@TempDir Path tmp) throws Exception {
        Path inbox = tmp.resolve("inbox");
        SimpleProtocol p = new SimpleProtocol(inbox);

        byte[] payload = "hello over i2p".getBytes(StandardCharsets.UTF_8);
        I2PSocket sock = Mockito.mock(I2PSocket.class);
        when(sock.getInputStream()).thenReturn(new ByteArrayInputStream(payload));

        p.handle(sock);

        assertTrue(Files.exists(inbox), "inbox dir must exist");
        long files = list(inbox).count();
        assertTrue(files >= 1, "expected at least one file in inbox");
    }

    @Test
    void sendText_writes_utf8(@TempDir Path tmp) throws Exception {
        SimpleProtocol p = new SimpleProtocol(tmp.resolve("inbox"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        I2PSocket sock = Mockito.mock(I2PSocket.class);
        when(sock.getOutputStream()).thenReturn(baos);

        String msg = "привет из I2P";
        p.sendText(sock, msg);

        assertEquals(msg, baos.toString(StandardCharsets.UTF_8));
    }

    @Test
    void sendImage_writes_binary(@TempDir Path tmp) throws Exception {
        SimpleProtocol p = new SimpleProtocol(tmp.resolve("inbox"));

        // создаём "картинку" (любой бинарь)
        byte[] bytes = new byte[] {0x01, 0x02, (byte)0xFF, 0x00, 0x7F};
        Path img = tmp.resolve("img.bin");
        Files.write(img, bytes);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        I2PSocket sock = Mockito.mock(I2PSocket.class);
        when(sock.getOutputStream()).thenReturn(baos);

        p.sendImage(sock, img);

        assertArrayEquals(bytes, baos.toByteArray());
    }

}

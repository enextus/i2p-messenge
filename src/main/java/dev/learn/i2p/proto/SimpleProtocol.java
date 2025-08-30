package dev.learn.i2p.proto;

import net.i2p.client.streaming.I2PSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class SimpleProtocol implements MessengerProtocol {
    private final Path inbox;

    public SimpleProtocol(Path inbox) {
        this.inbox = inbox;
    }

    @Override
    public void handle(I2PSocket socket) throws IOException {
        Files.createDirectories(inbox);
        byte[] data;
        try (InputStream in = socket.getInputStream()) {
            data = in.readAllBytes(); // простой one-shot протокол
        }
        if (data.length == 0) return;
        Path out = inbox.resolve("msg-" + System.currentTimeMillis() + ".bin");
        Files.write(out, data);
    }

    @Override
    public void sendText(I2PSocket socket, String text) throws IOException {
        if (text == null) text = "";
        try (OutputStream out = socket.getOutputStream()) {
            out.write(text.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    @Override
    public void sendImage(I2PSocket socket, Path img) throws IOException {
        try (OutputStream out = socket.getOutputStream()) {
            Files.copy(img, out);
            out.flush();
        }
    }
}
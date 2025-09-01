package dev.learn.i2p.proto;

import net.i2p.client.streaming.I2PSocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import dev.learn.i2p.core.InboundSaver;
import java.io.InputStream;
import java.nio.file.Path;
import net.i2p.client.streaming.I2PSocket;

public class SimpleProtocol implements MessengerProtocol {
    private final Path inbox;

    public SimpleProtocol(Path inbox) {
        this.inbox = inbox;
    }

    @Override
    public void handle(I2PSocket socket) throws java.io.IOException {
        String senderB32 = null;
        try {
            var peer = socket.getPeerDestination();
            if (peer != null) senderB32 = net.i2p.data.Base32.encode(peer.calculateHash().getData()) + ".b32.i2p";
        } catch (Throwable ignore) {
            // best-effort: адрес отправителя не обязателен
        }

        try (InputStream in = socket.getInputStream()) {
            Path saved = InboundSaver.saveSmart(inbox, senderB32, in);
            // Лог — информативный: mime/ext/size пишет InboundSaver; тут можно кратко
            org.slf4j.LoggerFactory.getLogger(SimpleProtocol.class)
                    .info("Inbound stored as {}", saved.getFileName());
        }
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
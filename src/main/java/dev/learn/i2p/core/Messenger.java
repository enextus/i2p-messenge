package dev.learn.i2p.core;

import dev.learn.i2p.net.I2PTransport;
import dev.learn.i2p.proto.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

public record Messenger(I2PTransport transport, Protocol protocol) implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(Messenger.class);

    public static Messenger createDefault() throws Exception {
        Path keyFile = Path.of("messenger-keys.dat");
        Path inbox = Path.of("inbox");
        log.info("Messenger.createDefault() keyFile={}, inbox={}", keyFile.toAbsolutePath(), inbox.toAbsolutePath());
        var t = I2PTransport.connectDefault(keyFile);
        var p = new Protocol(inbox);
        log.info("Messenger created. Local address: {}", t.myB32());
        return new Messenger(t, p);
    }

    public String myB32() {
        return transport.myB32();
    }

    public void listen() {
        log.info("Starting listener loop...");
        transport.acceptLoop(socket -> {
            try {
                var dest = socket.getPeerDestination();
                if (dest != null) {
                    log.debug("Handling inbound from {}", dev.learn.i2p.net.I2PTransport.class
                            .getDeclaredMethod("toB32", net.i2p.data.Destination.class) // reflect-safe call avoided; keep simple log
                            .toString());
                }
                protocol.handle(socket);
            } catch (IOException e) {
                System.err.println("Client error: " + e.getMessage());
                log.warn("Client error: {}", e.getMessage(), e);

            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void sendText(String destB32, String text) throws IOException {
        log.info("sendText -> {} ({} bytes)", destB32, text == null ? 0 : text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length);

        transport.withConnection(destB32, s -> protocol.sendText(s, text));
    }

    public void sendImage(String destB32, Path img) throws IOException {
        log.info("sendImage -> {} (path: {})", destB32, img);
        transport.withConnection(destB32, s -> protocol.sendImage(s, img));
    }

    @Override
    public void close() {
        transport.close();
        log.info("Closing Messenger...");
        transport.close();
    }

}

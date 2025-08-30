package dev.learn.i2p.core;

import dev.learn.i2p.proto.MessengerProtocol;
import dev.learn.i2p.proto.SimpleProtocol;
import dev.learn.i2p.net.I2PTransport;
import net.i2p.data.Base32;
import net.i2p.data.Destination;
import net.i2p.data.Hash;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public record Messenger(I2PTransport transport, MessengerProtocol protocol, Path inbox) implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(Messenger.class);

    public static Messenger createDefault() throws Exception {
        Path base = resolvePathDir(
                "i2p.messenger.home",
                Paths.get(System.getProperty("user.home"), ".i2p-messenger")
        );
        Path keyFile = resolvePath("i2p.messenger.keyfile", base.resolve("messenger-keys.dat"));
        Path inboxDir = resolvePathDir("i2p.messenger.inbox", base.resolve("inbox"));

        Files.createDirectories(keyFile.getParent());
        Files.createDirectories(inboxDir);

        log.info("Messenger.createDefault() keyFile={}, inbox={}", keyFile, inboxDir);
        var transport = I2PTransport.connectDefault(keyFile);
        var protocol = new SimpleProtocol(inboxDir);
        return new Messenger(transport, protocol, inboxDir);
    }

    private static Path resolvePath(String sysProp, Path defVal) {
        String v = System.getProperty(sysProp);
        if (v == null || v.isBlank()) {
            String env = System.getenv(sysProp.toUpperCase().replace('.', '_'));
            if (env != null && !env.isBlank()) v = env;
        }
        return (v != null && !v.isBlank()) ? Paths.get(v) : defVal;
    }
    private static Path resolvePathDir(String sysProp, Path defVal) { return resolvePath(sysProp, defVal); }

    private static String b32Of(Destination dest) {
        if (dest == null) return "<unknown>";
        Hash h = dest.calculateHash();
        return Base32.encode(h.getData()) + ".b32.i2p";
    }

    public String myB32() { return transport.myB32(); }

    public void listen() {
        log.info("Starting listener loop...");
        transport.acceptLoop(socket -> {
            try {
                String peer = b32Of(socket.getPeerDestination());
                log.debug("Inbound from {}", peer);
                protocol.handle(socket);
            } catch (IOException e) {
                log.warn("Client error: {}", e.toString(), e);
            }
        });
    }

    public void sendText(String destB32, String text) throws IOException {
        int len = (text == null) ? 0 : text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        log.info("sendText -> {} ({} bytes)", destB32, len);
        transport.withConnection(destB32, s -> protocol.sendText(s, text));
    }

    public void sendImage(String destB32, Path img) throws IOException {
        log.info("sendImage -> {} (path: {})", destB32, img);
        transport.withConnection(destB32, s -> protocol.sendImage(s, img));
    }

    @Override
    public void close() {
        log.info("Closing Messenger...");
        transport.close();
    }
}
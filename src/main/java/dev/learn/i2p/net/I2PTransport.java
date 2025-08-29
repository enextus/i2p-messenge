/*
 * Start of file src/main/java/dev/learn/i2p/net/I2PTransport.java
 */
package dev.learn.i2p.net;

import net.i2p.I2PAppContext;
import net.i2p.I2PException;
import net.i2p.client.streaming.I2PServerSocket;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.data.Base32;
import net.i2p.data.Destination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;

public record I2PTransport(I2PSocketManager mgr) implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(I2PTransport.class);

    // компактный конструктор record — защищаемся от null
    public I2PTransport {
        if (mgr == null) {
            throw new IllegalStateException(
                    "I2PSocketManager is null. Cannot connect to I2P router via I2CP.");
        }

        try {
            Destination d = mgr.getSession().getMyDestination();
            log.info("I2PSocketManager initialized. Local address: {}", toB32(d));
        } catch (Throwable t) {
            log.warn("I2PSocketManager initialized, but failed to read local destination: {}", t.toString());
        }

    }

    public static I2PTransport connectDefault(Path keyFile) throws Exception {
        I2PIdentity.ensure(keyFile);
        String host = System.getProperty(
                "i2p.i2cp.host",
                System.getenv().getOrDefault("I2P_I2CP_HOST", "127.0.0.1")
        );
        int port = Integer.parseInt(System.getProperty(
                "i2p.i2cp.port",
                System.getenv().getOrDefault("I2P_I2CP_PORT", "7654")
        ));

        log.info("Connecting to I2P router via I2CP {}:{}", host, port);
        log.debug("Using key file: {}", keyFile.toAbsolutePath());

        try (InputStream in = Files.newInputStream(keyFile)) {
            Properties p = new Properties();

            log.debug("Creating I2PSocketManager...");

            I2PSocketManager m = I2PSocketManagerFactory.createManager(in, host, port, p);
            if (m == null) {
                throw new IOException("Cannot connect to I2P router via I2CP at "
                        + host + ":" + port + " (I2PSocketManagerFactory returned null). "
                        + "Is the router running and I2CP enabled?");
            }

            log.info("I2PSocketManager created successfully.");

            return new I2PTransport(m);
        }
    }

    /**
     * Возвращает собственный .b32.i2p адрес.
     */
    public String myB32() {
        Destination d = mgr.getSession().getMyDestination();
        return (Base32.encode(d.calculateHash().getData()) + ".b32.i2p").toLowerCase();
    }

    /**
     * Блокирующий цикл accept. Обрабатываем I2PException, чтобы компилировалось и не падало из-за сетевых сбоев.
     * Blocking accept loop with safe I2PException handling.
     */
    public void acceptLoop(Consumer<I2PSocket> handler) {
        I2PServerSocket server = mgr.getServerSocket();
        System.out.println("Listening on: " + myB32());

        log.info("Listening on {}", myB32());

        while (!Thread.currentThread().isInterrupted()) {
            try {
                log.debug("Waiting for inbound connection...");
                I2PSocket socket = server.accept(); // may throw I2PException
                Destination peer = socket.getPeerDestination();
                String peerB32 = peer != null ? toB32(peer) : "<unknown>";
                MDC.put("peer", "[" + peerB32 + "]");
                log.info("Inbound connection accepted from {}", peerB32);

                handler.accept(socket);

                log.debug("Inbound connection from {} handled and closed.", peerB32);
                MDC.remove("peer");

            } catch (I2PException | IOException e) {
                // логируем и продолжаем; можно добавить backoff/счетчик
                System.err.println("Accept failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());

                log.warn("Accept failed: {}: {}", e.getClass().getSimpleName(), e.getMessage(), e);
                MDC.remove("peer");
            }
        }
    }

    /**
     * Устанавливает соединение по .b32 и выполняет операцию, корректно оборачивая I2PException в IOException.
     * Resolves .b32 to Destination and wraps I2PException as IOException.
     */
    public void withConnection(String destB32, IOConsumer<I2PSocket> op) throws IOException {
        log.info("Resolving destination: {}", destB32);
        Destination dest = I2PAppContext.getGlobalContext().namingService().lookup(destB32);
        if (dest == null) {

            log.error("Unknown I2P host: {}", destB32);
            throw new IOException("Unknown I2P host: " + destB32);
        }
        String peerB32 = toB32(dest);
        MDC.put("peer", "[" + peerB32 + "]");
        log.info("Connecting to {}", peerB32);
        try (I2PSocket s = mgr.connect(dest)) {
            log.debug("Connected to {}. Executing operation...", peerB32);
            op.accept(s);
            log.debug("Operation on {} completed. Closing socket.", peerB32);

        } catch (I2PException e) {
            log.error("I2P connect failed: {}", e.getMessage(), e);
            throw new IOException("I2P connect failed: " + e.getMessage(), e);
        } finally {
            MDC.remove("peer");
        }
    }

    @FunctionalInterface
    public interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }

    @Override
    public void close() {
        try {
            mgr.destroySocketManager();
            log.info("I2PSocketManager destroyed.");
        } catch (Throwable ignore) {
            log.debug("Ignoring error on destroySocketManager: {}", ignore.toString());

        }
    }

    private static String toB32(Destination d) {
        return (Base32.encode(d.calculateHash().getData()) + ".b32.i2p").toLowerCase();
    }
}

/*
 * End of file src/main/java/dev/learn/i2p/net/I2PTransport.java
 */

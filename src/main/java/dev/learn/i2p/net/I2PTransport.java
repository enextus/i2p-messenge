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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.Consumer;

public record I2PTransport(I2PSocketManager mgr) implements Closeable {

    // компактный конструктор record — защищаемся от null
    public I2PTransport {
        if (mgr == null) {
            throw new IllegalStateException(
                    "I2PSocketManager is null. Cannot connect to I2P router via I2CP.");
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
        try (InputStream in = Files.newInputStream(keyFile)) {
            Properties p = new Properties();
            I2PSocketManager m = I2PSocketManagerFactory.createManager(in, host, port, p);
            if (m == null) {
                throw new IOException("Cannot connect to I2P router via I2CP at "
                        + host + ":" + port + " (I2PSocketManagerFactory returned null). "
                        + "Is the router running and I2CP enabled?");
            }
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
        while (!Thread.currentThread().isInterrupted()) {
            try {
                I2PSocket socket = server.accept(); // may throw I2PException
                handler.accept(socket);
            } catch (I2PException | IOException e) {
                // логируем и продолжаем; можно добавить backoff/счетчик
                System.err.println("Accept failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Устанавливает соединение по .b32 и выполняет операцию, корректно оборачивая I2PException в IOException.
     * Resolves .b32 to Destination and wraps I2PException as IOException.
     */
    public void withConnection(String destB32, IOConsumer<I2PSocket> op) throws IOException {
        Destination dest = I2PAppContext.getGlobalContext().namingService().lookup(destB32);
        if (dest == null) {
            throw new IOException("Unknown I2P host: " + destB32);
        }
        try (I2PSocket s = mgr.connect(dest)) {
            op.accept(s);
        } catch (I2PException e) {
            throw new IOException("I2P connect failed: " + e.getMessage(), e);
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
        } catch (Throwable ignore) {
        }
    }

}

/*
 * End of file src/main/java/dev/learn/i2p/net/I2PTransport.java
 */

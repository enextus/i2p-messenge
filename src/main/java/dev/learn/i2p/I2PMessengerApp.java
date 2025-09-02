/*
 * Start of file src/main/java/dev/learn/i2p/I2PMessengerApp.java
 */
package dev.learn.i2p;

import dev.learn.i2p.core.FileTypes;
import dev.learn.i2p.core.Constants; // ← добавлен импорт

import net.i2p.I2PAppContext;
import net.i2p.I2PException;
import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import net.i2p.client.streaming.I2PServerSocket;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.data.Base32;
import net.i2p.data.Destination;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.Properties;

public class I2PMessengerApp {
    // Где держим ключи (eepPriv.dat совместимый формат)
    private static final Path KEY_FILE = Path.of("messenger-keys.dat");
    // Куда сохраняем входящие файлы
    private static final Path INBOX_DIR = Path.of("inbox");

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }

        // Гарантируем наличие ключей и менеджера сокетов
        ensureKeys();

        I2PSocketManager mgr = createManager();
        // Чистое завершение
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                mgr.destroySocketManager();
            } catch (Throwable ignore) {
            }
        }, "i2p-shutdown"));

        try {
            switch (args[0]) {
                case "address" -> System.out.println("Your I2P address: " + myB32(mgr));
                case "listen" -> listenLoop(mgr);
                case "send-text" -> {
                    if (args.length < 3) {
                        usage();
                        return;
                    }
                    String destB32 = args[1];
                    String msg = String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length));
                    sendText(mgr, destB32, msg);
                }
                case "send-image" -> {
                    if (args.length != 3) {
                        usage();
                        return;
                    }
                    String destB32 = args[1];
                    Path img = Path.of(args[2]);
                    sendImage(mgr, destB32, img);
                }
                default -> usage();
            }
        } finally {
            if (mgr != null) {
                mgr.destroySocketManager();
            }
        }
    }

    private static void usage() {
        System.out.println("""
                Usage:
                  java -jar i2p-messenger.jar address
                  java -jar i2p-messenger.jar listen
                  java -jar i2p-messenger.jar send-text <peer.b32.i2p> <message>
                  java -jar i2p-messenger.jar send-image <peer.b32.i2p> <path/to/image>
                """);
    }

    /** Создаёт eepPriv.dat-совместимый файл ключей, если его нет. */
    private static void ensureKeys() throws Exception {
        if (Files.exists(KEY_FILE)) return;
        Path parent = KEY_FILE.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        I2PClient client = I2PClientFactory.createClient();
        try (OutputStream out = Files.newOutputStream(KEY_FILE)) {
            client.createDestination(out); // пишет полные ключи в формат файла
        }
        System.out.println("New I2P identity created: " + KEY_FILE.toAbsolutePath());
    }

    /** Создаёт менеджер I2P-сокетов, подключаясь к локальному роутеру по I2CP (127.0.0.1:7654). */
    private static I2PSocketManager createManager() throws Exception {
        Properties opts = new Properties(); // при желании сюда: inbound/outbound.length и т.п.
        try (InputStream in = Files.newInputStream(KEY_FILE)) {
            return I2PSocketManagerFactory.createManager(in, "127.0.0.1", 7654, opts);
        }
    }

    /** Возвращает свой .b32.i2p адрес. */
    private static String myB32(I2PSocketManager mgr) {
        Destination dest = mgr.getSession().getMyDestination();
        String b32 = Base32.encode(dest.calculateHash().getData()) + ".b32.i2p";
        return b32.toLowerCase();
    }

    /** Запускает серверный цикл приёма сообщений. */
    private static void listenLoop(I2PSocketManager mgr) throws IOException {
        Files.createDirectories(INBOX_DIR);
        I2PServerSocket server = mgr.getServerSocket();
        System.out.println("Listening on: " + myB32(mgr));
        while (!Thread.currentThread().isInterrupted()) {
            try {
                I2PSocket socket = server.accept(); // может кидать I2PException

                // ← сразу задаём таймаут чтения для входящего соединения
                socket.setReadTimeout(Constants.READ_TIMEOUT_MS);

                Thread t = new Thread(() -> handleClient(socket), "i2p-client-" + System.nanoTime());
                t.setDaemon(true);
                t.start();
            } catch (I2PException e) {
                System.err.println("Accept failed: " + e.getMessage());
            }
        }
    }

    /** Обработка входящего соединения. */
    private static void handleClient(I2PSocket socket) {
        try (socket;
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            try {
                // Таймаут уже задан в listenLoop(...) через setReadTimeout(Constants.READ_TIMEOUT_MS)

                final int MAX_IMAGE = 32 * 1024 * 1024; // 32 MB safety cap
                int type = in.readUnsignedByte(); // 1=text, 2=image
                if (type == 1) {
                    int len = in.readInt();
                    if (len < 0 || len > 1_000_000) { // 1MB cap для текста
                        out.writeUTF("ERR: text too large");
                        out.flush();
                        return;
                    }
                    byte[] buf = new byte[len];
                    in.readFully(buf); // гарантируем весь пакет
                    String text = new String(buf, java.nio.charset.StandardCharsets.UTF_8);
                    System.out.println("[TEXT] " + text);
                    out.writeUTF("OK");
                } else if (type == 2) {
                    String fileName = in.readUTF();    // <= 64k
                    String contentType = in.readUTF(); // MIME
                    int size = in.readInt();
                    if (size < 0 || size > MAX_IMAGE) {
                        out.writeUTF("ERR: image too large");
                        out.flush();
                        return;
                    }
                    byte[] data = new byte[size];
                    in.readFully(data); // читаем ровно size

                    String safe = sanitize(fileName);
                    if (safe.isBlank()) safe = "image";
                    String ext = FileTypes.toExtension(contentType, safe);
                    Path outPath = INBOX_DIR.resolve(safe + "_" + Instant.now().toEpochMilli() + ext).normalize();

                    Files.write(outPath, data);
                    System.out.println("[IMAGE] saved: " + outPath + " (" + contentType + ", " + size + " bytes)");
                    out.writeUTF("OK");
                } else {
                    out.writeUTF("ERR: unknown type");
                }
                out.flush();
            } catch (InterruptedIOException e) {
                System.err.println("Read timed out after " + Constants.READ_TIMEOUT_MS + " ms: " + e.getMessage());
            } catch (IOException e) {
                System.err.println("Client error: " + e.getMessage());
            }
        } catch (IOException ignore) {
        }
    }

    /** Отправка текстового сообщения. */
    private static void sendText(I2PSocketManager mgr, String destB32, String message) {
        try (I2PSocket s = connect(mgr, destB32);
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
             DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()))) {

            byte[] payload = message.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            out.writeByte(1);
            out.writeInt(payload.length);
            out.write(payload);
            out.flush();
            System.out.println("Server replied: " + in.readUTF());
        } catch (IOException e) {
            System.err.println("sendText failed: " + e.getMessage());
        }
    }

    /** Отправка изображения. */
    private static void sendImage(I2PSocketManager mgr, String destB32, Path imgPath) {
        try {
            Objects.requireNonNull(imgPath, "Image path required");
            byte[] data = Files.readAllBytes(imgPath);
            String ct = Files.probeContentType(imgPath);
            if (ct == null) ct = "application/octet-stream";
            String name = imgPath.getFileName().toString();

            try (I2PSocket s = connect(mgr, destB32);
                 DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
                 DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()))) {

                out.writeByte(2);
                out.writeUTF(name);
                out.writeUTF(ct);
                out.writeInt(data.length);
                out.write(data);
                out.flush();
                System.out.println("Server replied: " + in.readUTF());
            }
        } catch (IOException e) {
            System.err.println("sendImage failed: " + e.getMessage());
        }
    }

    /** Подключение к пиру по .b32 адресу. */
    private static I2PSocket connect(I2PSocketManager mgr, String hostB32) throws IOException {
        Destination dest = resolveDestination(hostB32);
        try {
            return mgr.connect(dest);
        } catch (I2PException e) {
            throw new IOException("I2P connect failed: " + e.getMessage(), e);
        }
    }

    private static Destination resolveDestination(String host) throws IOException {
        // Резолвим .b32/.i2p имя через локальный NamingService
        Destination d = I2PAppContext.getGlobalContext().namingService().lookup(host);
        if (d == null) throw new IOException("Unknown I2P host: " + host);
        return d;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
// End of I2PMessengerApp.java

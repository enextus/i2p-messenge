package dev.learn.i2p.proto;

import dev.learn.i2p.core.FileTypes;
import dev.learn.i2p.core.MessageType;
import net.i2p.client.streaming.I2PSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static dev.learn.i2p.core.Constants.*;

public final class Protocol {
    private final Path inboxDir;

    private static final Logger log = LoggerFactory.getLogger(Protocol.class);

    public Protocol(Path inboxDir) throws IOException {
        this.inboxDir = inboxDir;
        Files.createDirectories(inboxDir);
    }

    /**
     * Обработка входящего сообщения по простому бинарному протоколу.
     */
    public void handle(I2PSocket socket) throws IOException {
        try (socket;
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream(), BUFFER_SIZE));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream(), BUFFER_SIZE))) {

            int type = in.readUnsignedByte();

            if (type == MessageType.TEXT.code) {
                int len = in.readInt();
                if (len < 0 || len > MAX_TEXT_BYTES) {
                    out.writeUTF("ERR: text too large");
                    out.flush();
                    return;
                }
                byte[] buf = in.readNBytes(len);
                if (buf.length != len) {
                    throw new IOException("Incomplete text payload: expected " + len + " bytes, got " + buf.length);
                }
                String text = new String(buf, StandardCharsets.UTF_8);
                log.info("[TEXT] {}", text);
                out.writeUTF("OK");
                out.flush();
                return;
            }

            if (type == MessageType.IMAGE.code) {
                String fileName = in.readUTF();
                if (fileName.length() > MAX_FILENAME_LENGTH) {
                    out.writeUTF("ERR: filename too long");
                    out.flush();
                    return;
                }
                String contentType = in.readUTF();
                int size = in.readInt();
                if (size < 0 || size > MAX_IMAGE_BYTES) {
                    out.writeUTF("ERR: image too large");
                    out.flush();
                    return;
                }

                // Санитизация имени и выбор расширения
                String safe = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                if (safe.isBlank()) safe = "image";
                String ext = FileTypes.toExtension(contentType, safe);

                // Убираем исходное расширение, чтобы не получить двойное
                int dot = safe.lastIndexOf('.');
                String baseName = (dot > 0) ? safe.substring(0, dot) : safe;

                Path candidate = inboxDir.resolve(baseName + "_" + System.currentTimeMillis() + ext)
                        .normalize().toAbsolutePath();
                Path baseDir = inboxDir.toAbsolutePath();
                if (!candidate.startsWith(baseDir)) {
                    throw new IOException("Path traversal detected");
                }

                // Читаем ровно size байт и сохраняем
                try (OutputStream fileOut = Files.newOutputStream(candidate)) {
                    byte[] chunk = new byte[BUFFER_SIZE];
                    int remaining = size;
                    while (remaining > 0) {
                        int read = in.read(chunk, 0, Math.min(chunk.length, remaining));
                        if (read == -1) break;
                        fileOut.write(chunk, 0, read);
                        remaining -= read;
                    }
                    if (remaining != 0) {
                        throw new IOException("Incomplete image data: " + remaining + " bytes missing");
                    }
                }

                log.info("[IMAGE] saved: {} ({} , {} bytes)", candidate, contentType, size);
                out.writeUTF("OK");
                out.flush();
                return;
            }

            // Неизвестный тип
            out.writeUTF("ERR: unknown type");
            out.flush();
        }
    }

    // ===== Исходящие сообщения (клиент) ======================================

    @FunctionalInterface
    private interface IOConsumer<T> {
        void accept(T t) throws IOException;
    }

    /**
     * Шаблон: открыть потоки, записать запрос, дождаться ответа, залогировать.
     */
    private void withStreams(I2PSocket s, IOConsumer<DataOutputStream> writer) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream(), BUFFER_SIZE));
             DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream(), BUFFER_SIZE))) {

            writer.accept(out);
            out.flush();

            String reply = in.readUTF();
            log.info("Server replied: {}", reply);
        }
    }

    public void sendText(I2PSocket s, String text) throws IOException {
        withStreams(s, out -> {
            byte[] payload = text.getBytes(StandardCharsets.UTF_8);
            out.writeByte(MessageType.TEXT.code);
            out.writeInt(payload.length);
            out.write(payload);
        });
    }

    public void sendImage(I2PSocket s, Path imgPath) throws IOException {
        // вычисляем финальные значения заранее
        String ct0 = Files.probeContentType(imgPath);
        final String contentType = (ct0 != null) ? ct0 : "application/octet-stream";
        final String fileName    = imgPath.getFileName().toString();
        final long sizeLong      = Files.size(imgPath);
        if (sizeLong > MAX_IMAGE_BYTES) {
            throw new IOException("Image too large: " + sizeLong + " bytes");
        }
        final int intSize = (int) sizeLong; // протокол у нас с int(size)

        withStreams(s, out -> {
            out.writeByte(MessageType.IMAGE.code);
            out.writeUTF(fileName);
            out.writeUTF(contentType);
            out.writeInt(intSize);

            try (InputStream fileIn = Files.newInputStream(imgPath)) {
                byte[] buf = new byte[BUFFER_SIZE];
                int r;
                while ((r = fileIn.read(buf)) != -1) {
                    out.write(buf, 0, r);
                }
            }
        });
    }

}

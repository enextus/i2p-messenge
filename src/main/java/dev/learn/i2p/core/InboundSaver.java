// start src/main/java/dev/learn/i2p/core/InboundSaver.java
package dev.learn.i2p.core;

import dev.learn.i2p.proto.FileType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class InboundSaver {
    private static final Logger log = LoggerFactory.getLogger(InboundSaver.class);
    private static final int SNIFF_N = 16384; // до 16К на определение
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private InboundSaver() {}

    public static Path saveSmart(Path inboxDir, String senderB32, InputStream rawIn) throws IOException {
        Files.createDirectories(inboxDir);

        try (BufferedInputStream in = new BufferedInputStream(rawIn)) {
            in.mark(SNIFF_N);
            byte[] head = in.readNBytes(SNIFF_N);
            in.reset();

            // 1) Определяем тип через единую точку — FileType.sniff(...)
            FileType ft = FileType.sniff(head, head.length);
            String ext = ft.ext;
            String mime = ft.mime;

            // 2) Для справки: что верит JDK (в логах поможет разбирать спорные случаи)
            String jdkGuess = null;
            try {
                jdkGuess = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(head));
            } catch (IOException ignored) {
                // ничего: это только для логов
            }

            String base = TS.format(LocalDateTime.now()) + "_from-" + shortSender(senderB32);
            Path target = uniquified(inboxDir, base, ext);

            try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                in.transferTo(out);
            }
            log.info("Saved inbound (sniffed mime={}, ext={}, jdkGuess={}) {} bytes -> {}",
                    mime, ext, jdkGuess, Files.size(target), target.getFileName());

            return target;
        }
    }

    private static String shortSender(String b32) {
        if (b32 == null) return "unknown";
        int cut = b32.indexOf(".b32.i2p");
        String raw = cut > 0 ? b32.substring(0, cut) : b32;
        return raw.length() > 8 ? raw.substring(0, 8) : raw;
    }

    private static Path uniquified(Path dir, String base, String ext) throws IOException {
        for (int i = 0; i < 10000; i++) {
            String suffix = (i == 0) ? "" : ("_" + i);
            Path p = dir.resolve(base + suffix + ext);
            if (!Files.exists(p)) return p;
        }
        throw new IOException("Failed to create unique filename in " + dir);
    }
}

// stop src/main/java/dev/learn/i2p/core/InboundSaver.java

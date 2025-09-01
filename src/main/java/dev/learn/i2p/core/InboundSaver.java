// src/main/java/dev/learn/i2p/core/InboundSaver.java
package dev.learn.i2p.core;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.*;
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

             // 1) Пробуем JDK-определение MIME
                     String mime = URLConnection.guessContentTypeFromStream(new ByteArrayInputStream(head));
             String ext = FileTypes.toExtension(mime, null); // ".png/.jpg/..." или ".bin"

                     // 2) Если JDK не распознал — пробуем «магию» сигнатур
                            if (".bin".equals(ext)) {
                    String magic = magicExt(head, head.length); // ".png/.jpg/..." или null
                   if (magic != null) ext = magic;
                 }

                     // 3) Если всё ещё неизвестно — проверим «похоже ли на текст»
                             if (".bin".equals(ext) && looksLikeUtf8Text(head, head.length)) {
                     ext = ".txt";
                 }

                    String base = TS.format(LocalDateTime.now()) + "_from-" + shortSender(senderB32);
             Path target = uniquified(inboxDir, base, ext);

            try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                in.transferTo(out);
            }
            log.info("Saved inbound (mime={}, ext={}) {} bytes -> {}",
                    mime, ext, Files.size(target), target.getFileName());

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



    private static String magicExt(byte[] h, int len) {
        if (len >= 8
                && (h[0] & 0xFF) == 0x89 && h[1] == 0x50 && h[2] == 0x4E && h[3] == 0x47
                && h[4] == 0x0D && h[5] == 0x0A && h[6] == 0x1A && h[7] == 0x0A) return ".png";
        if (len >= 3
                && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) return ".jpg";
        if (len >= 6 && h[0]=='G' && h[1]=='I' && h[2]=='F' && h[3]=='8' && (h[4]=='7'||h[4]=='9') && h[5]=='a') return ".gif";
        if (len >= 12 && h[0]=='R' && h[1]=='I' && h[2]=='F' && h[3]=='F' && h[8]=='W' && h[9]=='E' && h[10]=='B' && h[11]=='P') return ".webp";
        if (len >= 2 && h[0]=='B' && h[1]=='M') return ".bmp";
        if (len >= 4 && h[0]==0x25 && h[1]==0x50 && h[2]==0x44 && h[3]==0x46) return ".pdf"; // "%PDF"
        return null;
    }

    private static boolean looksLikeUtf8Text(byte[] b, int len) {
        // Разрешаем TAB/CR/LF и печатные ASCII; грубо валидируем UTF-8 последовательности.
        int i = 0;
        while (i < len) {
            int c = b[i] & 0xFF;
            if (c == 0x09 || c == 0x0A || c == 0x0D || (c >= 0x20 && c <= 0x7E)) { i++; continue; }
            int n = 0;
            if      ((c & 0xE0) == 0xC0) n = 1;
            else if ((c & 0xF0) == 0xE0) n = 2;
            else if ((c & 0xF8) == 0xF0) n = 3;
            else return false; // не похоже на текст
            if (i + n >= len) break; // обрезанный хвост — не считаем бинарём
            for (int j=1;j<=n;j++) {
                int cc = b[i+j] & 0xFF;
                if ((cc & 0xC0) != 0x80) return false;
            }
            i += n + 1;
        }
        // «Нулевых» байтов слишком много — почти наверняка бинарь
        int zeros = 0;
        for (int k=0;k<len;k++) if (b[k]==0) zeros++;
        return zeros < Math.max(1, len/50); // <2% нулей
    }



}

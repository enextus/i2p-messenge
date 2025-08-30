/*
 * Start of file src/main/java/dev/learn/i2p/core/FileTypes.java
 */

package dev.learn.i2p.core;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class FileTypes {
    private static final Map<String, String> MIME_TO_EXT = new HashMap<>();

    static {
        // Базовые картинки
        MIME_TO_EXT.put("image/png", ".png");
        MIME_TO_EXT.put("image/jpeg", ".jpg");
        MIME_TO_EXT.put("image/jpg", ".jpg");
        MIME_TO_EXT.put("image/gif", ".gif");
        // Полезные доп-типы
        MIME_TO_EXT.put("image/webp", ".webp");
        MIME_TO_EXT.put("image/bmp", ".bmp");
        MIME_TO_EXT.put("image/svg+xml", ".svg");
        MIME_TO_EXT.put("image/tiff", ".tiff");   // иногда .tif
        MIME_TO_EXT.put("image/x-icon", ".ico");
        MIME_TO_EXT.put("image/vnd.microsoft.icon", ".ico");
        MIME_TO_EXT.put("image/heic", ".heic");
        MIME_TO_EXT.put("image/heif", ".heif");
        MIME_TO_EXT.put("image/avif", ".avif");
    }

    /**
     * Возвращает расширение файла (c точкой) по MIME-типу; если неизвестен —
     * пытается взять из имени файла; если и это не удаётся — ".bin".
     * Метод безопасно обрабатывает null/пустые значения и MIME с параметрами.
     */
    public static String toExtension(String contentType, String fallbackName) {
        String ext = null;

        // 1) Пытаемся по MIME
        if (contentType != null && !contentType.isBlank()) {
            String mt = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            ext = MIME_TO_EXT.get(mt);
            if (ext == null) {
                // эвристики по подстрокам MIME
                if (mt.contains("png")) ext = ".png";
                else if (mt.contains("jpeg") || mt.contains("jpg")) ext = ".jpg";
                else if (mt.contains("gif")) ext = ".gif";
                else if (mt.contains("webp")) ext = ".webp";
                else if (mt.contains("svg")) ext = ".svg";
                else if (mt.contains("tif")) ext = ".tiff"; // унифицируем на .tiff
                else if (mt.contains("ico")) ext = ".ico";
                else if (mt.contains("heic")) ext = ".heic";
                else if (mt.contains("heif")) ext = ".heif";
                else if (mt.contains("avif")) ext = ".avif";
                else if (mt.contains("bmp")) ext = ".bmp";
            }
        }

        // 2) Фолбэк по имени файла
        if (ext == null && fallbackName != null && !fallbackName.isBlank()) {
            String s = fallbackName;
            int q = s.indexOf('?');
            if (q >= 0) s = s.substring(0, q);
            int h = s.indexOf('#');
            if (h >= 0) s = s.substring(0, h);
            int dot = s.lastIndexOf('.');
            if (dot > 0 && dot < s.length() - 1) {
                String e = s.substring(dot).toLowerCase(Locale.ROOT);
                if (e.length() >= 2 && e.length() <= 6 && e.matches("\\.[a-z0-9]+")) {
                    ext = e;
                }
            }
        }

        // 3) Финальный дефолт
        return ext != null ? ext : ".bin";
    }

}

/*
 * End of file src/main/java/dev/learn/i2p/core/FileTypes.java
 */

package dev.learn.i2p.core;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class FileTypes {
    private static final Map<String, String> MIME_TO_EXT = new HashMap<>();
    static {
        // Базовые картинки
        MIME_TO_EXT.put("image/png",        ".png");
        MIME_TO_EXT.put("image/jpeg",       ".jpg");
        MIME_TO_EXT.put("image/jpg",        ".jpg");
        MIME_TO_EXT.put("image/gif",        ".gif");
        // Полезные доп-типы
        MIME_TO_EXT.put("image/webp",       ".webp");
        MIME_TO_EXT.put("image/bmp",        ".bmp");
        MIME_TO_EXT.put("image/svg+xml",    ".svg");
        MIME_TO_EXT.put("image/tiff",       ".tiff");   // иногда .tif
        MIME_TO_EXT.put("image/x-icon",     ".ico");
        MIME_TO_EXT.put("image/vnd.microsoft.icon", ".ico");
        MIME_TO_EXT.put("image/heic",       ".heic");
        MIME_TO_EXT.put("image/heif",       ".heif");
        MIME_TO_EXT.put("image/avif",       ".avif");
    }

    /**
     * Возвращает расширение файла (c точкой) по MIME-типу; если неизвестен —
     * пытается взять из имени файла; если и это не удаётся — ".bin".
     * Метод безопасно обрабатывает null/пустые значения и MIME с параметрами.
     */
    public static String toExtension(String contentType, String fallbackName) {
        // 1) Пробуем по MIME
        if (contentType != null && !contentType.isBlank()) {
            String mt = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
            String mapped = MIME_TO_EXT.get(mt);
            if (mapped != null) return mapped;

            // Лояльные эвристики, если прислали что-то вроде "image/jpg; charset=binary" или экзотику
            if (mt.contains("png"))                 return ".png";
            if (mt.contains("jpeg") || mt.contains("jpg")) return ".jpg";
            if (mt.contains("gif"))                 return ".gif";
            if (mt.contains("webp"))                return ".webp";
            if (mt.contains("svg"))                 return ".svg";
            if (mt.contains("tif"))                 return ".tif";
            if (mt.contains("ico"))                 return ".ico";
            if (mt.contains("heic"))                return ".heic";
            if (mt.contains("heif"))                return ".heif";
            if (mt.contains("avif"))                return ".avif";
            if (mt.contains("bmp"))                 return ".bmp";
        }

        // 2) Фолбэк по имени файла
        if (fallbackName != null && !fallbackName.isBlank()) {
            // уберём возможные «хвосты» от URL-подобных имён (на всякий случай)
            String name = fallbackName;
            int q = name.indexOf('?'); if (q >= 0) name = name.substring(0, q);
            int h = name.indexOf('#'); if (h >= 0) name = name.substring(0, h);

            int dot = name.lastIndexOf('.');
            if (dot > 0 && dot < name.length() - 1) {
                String ext = name.substring(dot).toLowerCase(Locale.ROOT);

                // лёгкая валидация расширения (длина 2..6, только [a-z0-9])
                if (ext.length() >= 2 && ext.length() <= 6 && ext.matches("\\.[a-z0-9]+")) {
                    return ext;
                }
            }
        }

        // 3) Совсем ничего не распознали
        return ".bin";
    }

    private FileTypes() {}

}

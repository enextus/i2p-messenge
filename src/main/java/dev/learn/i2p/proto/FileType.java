// start src/main/java/dev/learn/i2p/proto/FileType.java
package dev.learn.i2p.proto;

public enum FileType {
    PNG(".png", "image/png"),
    JPEG(".jpg", "image/jpeg"),
    GIF(".gif", "image/gif"),
    WEBP(".webp", "image/webp"),
    BMP(".bmp", "image/bmp"),
    PDF(".pdf", "application/pdf"),
    TEXT(".txt", "text/plain"),
    BINARY(".bin", "application/octet-stream");

    public final String ext;
    public final String mime;

    FileType(String ext, String mime) { this.ext = ext; this.mime = mime; }

    public static FileType sniff(byte[] head, int len) {
        if (len >= 8
                && (head[0] & 0xFF) == 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47
                && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A) return PNG;
        if (len >= 3
                && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) return JPEG;
        if (len >= 6 && head[0]=='G' && head[1]=='I' && head[2]=='F' && head[3]=='8' && (head[4]=='7'||head[4]=='9') && head[5]=='a') return GIF;
        if (len >= 12 && head[0]=='R' && head[1]=='I' && head[2]=='F' && head[3]=='F' && head[8]=='W' && head[9]=='E' && head[10]=='B' && head[11]=='P') return WEBP;
        if (len >= 2 && head[0]=='B' && head[1]=='M') return BMP;
        if (len >= 4 && head[0]==0x25 && head[1]==0x50 && head[2]==0x44 && head[3]==0x46) return PDF;
        if (looksLikeUtf8Text(head, len)) return TEXT;
        return BINARY;
    }

    private static boolean looksLikeUtf8Text(byte[] b, int len) {
        // Разрешаем таб/CR/LF и печатные ASCII; грубо валидируем UTF-8 последовательности
        int i = 0, controlCount = 0;
        while (i < len) {
            int c = b[i] & 0xFF;
            if (c == 0x09 || c == 0x0A || c == 0x0D || (c >= 0x20 && c <= 0x7E)) { i++; continue; }
            // UTF-8 multi-byte
            int n = 0;
            if      ((c & 0xE0) == 0xC0) n = 1;
            else if ((c & 0xF0) == 0xE0) n = 2;
            else if ((c & 0xF8) == 0xF0) n = 3;
            else return false;
            if (i + n >= len) break; // обрежем: недостаточно данных — не считаем бинарником
            for (int j=1;j<=n;j++) {
                int cc = b[i+j] & 0xFF;
                if ((cc & 0xC0) != 0x80) return false;
            }
            i += n + 1;
            controlCount++;
        }
        // Если «бинарных» нулей много — не текст.
        long zeros = 0;
        for (int k=0;k<len;k++) if (b[k]==0) zeros++;
        return zeros*1.0/Math.max(1,len) < 0.02; // <2% нулей
    }

}

// stop src/main/java/dev/learn/i2p/proto/FileType.java

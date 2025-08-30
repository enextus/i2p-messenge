package dev.learn.i2p.core;

public final class Constants {
    private Constants() {}

    public static final int EXIT_USAGE = 64;
    public static final int EXIT_FAILURE = 2;

    public static final int READ_TIMEOUT_MS = 120_000;      // 2 минуты
    public static final int MAX_TEXT_BYTES   = 1_000_000;   // 1 MB
    public static final int MAX_IMAGE_BYTES  = 32 * 1024 * 1024; // 32 MB
    public static final int BUFFER_SIZE      = 8192;        // буфер для потоков
    public static final int MAX_FILENAME_LENGTH = 255;      // ограничение имени файла
}

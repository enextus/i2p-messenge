package dev.learn.i2p.core.profile;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class ProfileManager {
    private ProfileManager() {}

    /** Главный вход: читаем sysprops/env и резолвим профиль. */
    public static ResolvedProfile resolveFromSystemProps() throws IOException {
        Path base  = getPathProp("i2p.messenger.home",
                Paths.get(System.getProperty("user.home"), ".i2p-messenger"));
        Path key   = getPathProp("i2p.messenger.keyfile", base.resolve("messenger-keys.dat"));
        Path inbox = getPathProp("i2p.messenger.inbox",  base.resolve("inbox"));

        boolean allowEphemeral = !"false".equalsIgnoreCase(System.getProperty("i2p.messenger.allowEphemeral","true"));
        boolean disableLock    =  "true".equalsIgnoreCase(System.getProperty("i2p.messenger.disableLock","false"));
        return resolve(base, key, inbox, allowEphemeral, disableLock);
    }

    /** Пытаемся захватить lock. Если занят и allowEphemeral=true — создаём auto-профиль. */
    public static ResolvedProfile resolve(Path base, Path keyFile, Path inboxDir,
                                          boolean allowEphemeral, boolean disableLock) throws IOException {
        Files.createDirectories(base);
        if (disableLock) {
            Files.createDirectories(keyFile.getParent());
            Files.createDirectories(inboxDir);
            return new ResolvedProfile(base, keyFile, inboxDir, false, null, null);
        }

        ResolvedProfile locked = tryLockProfile(base, keyFile, inboxDir, false);
        if (locked != null) return locked;

        if (!allowEphemeral)
            throw new IOException("Profile is already in use (key locked): " + keyFile);

        for (int i = 0; i < 10; i++) {
            Path ephBase  = base.resolve("profiles").resolve("auto-" + stamp() + "-" + pid() + "-" + rand8());
            Path ephKey   = ephBase.resolve("messenger-keys.dat");
            Path ephInbox = ephBase.resolve("inbox");
            ResolvedProfile eph = tryLockProfile(ephBase, ephKey, ephInbox, true);
            if (eph != null) return eph;
        }
        throw new IOException("Failed to allocate ephemeral profile (exhausted attempts).");
    }

    private static ResolvedProfile tryLockProfile(Path home, Path keyFile, Path inboxDir, boolean ephemeral) throws IOException {
        Files.createDirectories(home);
        Files.createDirectories(keyFile.getParent());
        Files.createDirectories(inboxDir);

        Path lockPath = keyFile.resolveSibling(keyFile.getFileName().toString() + ".lock");
        Files.createDirectories(lockPath.getParent());

        RandomAccessFile raf = new RandomAccessFile(lockPath.toFile(), "rw");
        FileChannel ch = raf.getChannel();
        FileLock lock = null;
        try {
            lock = ch.tryLock(); // non-blocking; если занято/не поддерживается — будет null/exception
        } catch (Exception ignored) {}
        if (lock == null) {
            ch.close();
            return null;
        }
        return new ResolvedProfile(home, keyFile, inboxDir, ephemeral, ch, lock);
    }

    private static Path getPathProp(String name, Path defVal) {
        String v = System.getProperty(name);
        if (v == null || v.isBlank()) {
            String env = System.getenv(name.toUpperCase(Locale.ROOT).replace('.', '_'));
            if (env != null && !env.isBlank()) v = env;
        }
        return (v != null && !v.isBlank()) ? Paths.get(v) : defVal;
    }

    private static String stamp() { return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")); }
    private static long   pid()   { return ProcessHandle.current().pid(); }
    private static String rand8() {
        long n = Math.abs(ThreadLocalRandom.current().nextLong());
        String s = Long.toString(n, 36);
        return s.length() >= 8 ? s.substring(0,8) : String.format("%08s", s).replace(' ', '0');
    }

}

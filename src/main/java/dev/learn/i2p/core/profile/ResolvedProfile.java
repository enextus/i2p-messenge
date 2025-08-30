package dev.learn.i2p.core.profile;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;

/** Результат резолва профиля. Держит файл-лок, чтобы профайл был эксклюзивным. */
public final class ResolvedProfile implements Closeable {
    public final Path home;
    public final Path keyFile;
    public final Path inboxDir;
    public final boolean ephemeral;

    final FileChannel lockChannel; // держим открытым, чтобы не потерять lock
    final FileLock    lock;

    ResolvedProfile(Path home, Path keyFile, Path inboxDir, boolean ephemeral,
                    FileChannel lockChannel, FileLock lock) {
        this.home = home;
        this.keyFile = keyFile;
        this.inboxDir = inboxDir;
        this.ephemeral = ephemeral;
        this.lockChannel = lockChannel;
        this.lock = lock;
    }

    @Override
    public void close() throws IOException {
        try { if (lock != null && lock.isValid()) lock.release(); }
        finally { if (lockChannel != null && lockChannel.isOpen()) lockChannel.close(); }
    }
}

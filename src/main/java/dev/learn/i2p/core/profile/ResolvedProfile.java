// src/main/java/dev/learn/i2p/core/profile/ResolvedProfile.java
package dev.learn.i2p.core.profile;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;

/** Результат резолва профиля. Держит файл-лок, чтобы профайл был эксклюзивным. */
public record ResolvedProfile(
        Path home,
        Path keyFile,
        Path inboxDir,
        boolean ephemeral,
        FileChannel lockChannel, // держим открытым, чтобы не потерять lock
        FileLock lock
) implements Closeable {

    // Компактный конструктор: базовая валидация
    public ResolvedProfile {
        if (home == null || keyFile == null || inboxDir == null) {
            throw new IllegalArgumentException("home, keyFile, inboxDir must not be null");
        }
        // lockChannel/lock могут быть null, когда lock отключён
    }

    @Override
    public void close() throws IOException {
        try {
            if (lock != null && lock.isValid()) lock.release();
        } finally {
            if (lockChannel != null && lockChannel.isOpen()) lockChannel.close();
        }
    }

    // (необязательно) Чтобы не светить объекты lock* в логах:
    @Override
    public String toString() {
        return "ResolvedProfile[" +
                "home=" + home +
                ", keyFile=" + keyFile +
                ", inboxDir=" + inboxDir +
                ", ephemeral=" + ephemeral +
                ']';
    }

}

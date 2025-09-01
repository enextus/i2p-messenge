package dev.learn.i2p.core.profile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ProfileManagerTest {

    @AfterEach
    void clearProps() {
        System.clearProperty("i2p.messenger.home");
        System.clearProperty("i2p.messenger.keyfile");
        System.clearProperty("i2p.messenger.inbox");
        System.clearProperty("i2p.messenger.allowEphemeral");
        System.clearProperty("i2p.messenger.disableLock");
    }

    @Test
    void resolve_default_creates_dirs(@TempDir Path tmp) throws Exception {
        System.setProperty("i2p.messenger.home", tmp.resolve(".i2p-messenger").toString());
        ResolvedProfile rp = ProfileManager.resolveFromSystemProps();
        assertTrue(Files.exists(rp.keyFile.getParent()));
        assertTrue(Files.exists(rp.inboxDir));
        assertFalse(rp.ephemeral);
        rp.close();
    }

    @Test
    void resolve_honors_explicit_keyfile(@TempDir Path tmp) throws Exception {
        Path base = tmp.resolve("base");
        Path key  = tmp.resolve("custom").resolve("keys.dat");
        System.setProperty("i2p.messenger.home", base.toString());
        System.setProperty("i2p.messenger.keyfile", key.toString());
        ResolvedProfile rp = ProfileManager.resolveFromSystemProps();
        assertEquals(key.normalize(), rp.keyFile.normalize());
        rp.close();
    }

    @Test
    void lock_conflict_creates_ephemeral(@TempDir Path tmp) throws Exception {
        Path base = tmp.resolve(".i2p-messenger");
        Path key  = base.resolve("messenger-keys.dat");
        Files.createDirectories(key.getParent());
        Path lockPath = key.resolveSibling("messenger-keys.dat.lock");
        Files.createDirectories(lockPath.getParent());
        // эмулируем чужой процесс: держим эксклюзивную блокировку
        try (RandomAccessFile raf = new RandomAccessFile(lockPath.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             FileLock lock = ch.tryLock()) {
            assertNotNull(lock); // у этого теста блок есть

            // теперь наш резолвер должен уйти в ephemeral
            ResolvedProfile rp = ProfileManager.resolve(base, key, base.resolve("inbox"), true, false);
            assertTrue(rp.ephemeral, "expected an ephemeral profile if lock is busy");
            assertTrue(rp.home.toString().contains("profiles\\") || rp.home.toString().contains("profiles/"));
            rp.close();
        }
    }

    @Test
    void lock_conflict_fail_when_ephemeral_disabled(@TempDir Path tmp) throws Exception {
        Path base = tmp.resolve(".i2p-messenger");
        Path key  = base.resolve("messenger-keys.dat");
        Files.createDirectories(key.getParent());
        Path lockPath = key.resolveSibling("messenger-keys.dat.lock");
        Files.createDirectories(lockPath.getParent());
        try (RandomAccessFile raf = new RandomAccessFile(lockPath.toFile(), "rw");
             FileChannel ch = raf.getChannel();
             FileLock lock = ch.tryLock()) {
            assertNotNull(lock);
            assertThrows(Exception.class,
                    () -> ProfileManager.resolve(base, key, base.resolve("inbox"), false, false));
        }
    }

}

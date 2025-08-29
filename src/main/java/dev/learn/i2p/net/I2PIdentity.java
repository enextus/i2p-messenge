package dev.learn.i2p.net;

import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;

import java.io.OutputStream;
import java.nio.file.*;

public final class I2PIdentity {
    public static void ensure(Path file) throws Exception {
        if (Files.exists(file)) return;
        Files.createDirectories(file.toAbsolutePath().getParent());
        I2PClient c = I2PClientFactory.createClient();
        try (OutputStream out = Files.newOutputStream(file)) { c.createDestination(out); }
        System.out.println("New I2P identity created: " + file.toAbsolutePath());
    }

}

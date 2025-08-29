/*
 * Start of file
 * */


package dev.learn.i2p.net;

import dev.learn.i2p.core.Messenger;
import net.i2p.client.I2PClient;
import net.i2p.client.I2PClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class I2PIdentity {
    private static final Logger log = LoggerFactory.getLogger(I2PIdentity.class);

    public static void ensure(Path file) throws Exception {
        if (Files.exists(file)) return;

        if (Files.exists(file)) {
            log.debug("I2P identity exists at {}", file.toAbsolutePath());
            return;
        }
        log.info("I2P identity not found. Generating at {}", file.toAbsolutePath());

        Files.createDirectories(file.toAbsolutePath().getParent());
        I2PClient c = I2PClientFactory.createClient();

        try (OutputStream out = Files.newOutputStream(file)) {
            c.createDestination(out);
        }

        System.out.println("New I2P identity created: " + file.toAbsolutePath());
        log.info("New I2P identity created: {}", file.toAbsolutePath());
    }

}

/*
 * End of file s
 * */


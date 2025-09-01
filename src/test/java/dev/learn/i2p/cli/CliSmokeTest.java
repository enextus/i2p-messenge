// Start src/test/java/dev/learn/i2p/cli/CliSmokeTest.java
package dev.learn.i2p.cli;

import dev.learn.i2p.net.I2PTransport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CliSmokeTest {

    private final PrintStream origOut = System.out;

    @AfterEach
    void tearDown() {
        System.setOut(origOut);
        System.clearProperty("i2p.messenger.home");
        System.clearProperty("i2p.messenger.keyfile");
        System.clearProperty("i2p.messenger.inbox");
    }

    @Test
    void address_prints_b32(@TempDir Path tmp) throws Exception {
        System.setProperty("i2p.messenger.home", tmp.resolve(".i2p-messenger").toString());

        I2PTransport transport = mock(I2PTransport.class);
        when(transport.myB32()).thenReturn("testaddr.b32.i2p");

        try (MockedStatic<I2PTransport> st = Mockito.mockStatic(I2PTransport.class)) {
            st.when(() -> I2PTransport.connectDefault(any())).thenReturn(transport);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            System.setOut(new PrintStream(baos, true, StandardCharsets.UTF_8));

            Main.main(new String[]{"address"});

            String out = baos.toString(StandardCharsets.UTF_8);
            assertTrue(out.contains("testaddr.b32.i2p"));
            verify(transport, atLeastOnce()).close();
        }
    }

}

package dev.learn.i2p.core;

import dev.learn.i2p.net.I2PTransport;
import dev.learn.i2p.proto.Protocol;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;

public record Messenger(I2PTransport transport, Protocol protocol) implements Closeable {

    public static Messenger createDefault() throws Exception {
        var t = I2PTransport.connectDefault(Path.of("messenger-keys.dat"));
        var p = new Protocol(Path.of("inbox"));
        return new Messenger(t, p);
    }

    public String myB32() {
        return transport.myB32();
    }

    public void listen() {
        transport.acceptLoop(socket -> {
            try {
                protocol.handle(socket);
            } catch (IOException e) {
                System.err.println("Client error: " + e.getMessage());
            }
        });
    }

    public void sendText(String destB32, String text) throws IOException {
        transport.withConnection(destB32, s -> protocol.sendText(s, text));
    }

    public void sendImage(String destB32, Path img) throws IOException {
        transport.withConnection(destB32, s -> protocol.sendImage(s, img));
    }

    @Override
    public void close() {
        transport.close();
    }

}

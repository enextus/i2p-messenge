package dev.learn.i2p.proto;

import net.i2p.client.streaming.I2PSocket;

import java.io.IOException;
import java.nio.file.Path;

public interface MessengerProtocol {
    void handle(I2PSocket socket) throws IOException;

    void sendText(I2PSocket socket, String text) throws IOException;

    void sendImage(I2PSocket socket, Path img) throws IOException;
}


package dev.learn.i2p.net;

import net.i2p.client.streaming.I2PSocket;

import java.io.IOException;

/**
 * Функциональный интерфейс-обработчик I2P-сокета.
 */
@FunctionalInterface
public interface SocketConsumer {
    void accept(I2PSocket socket) throws IOException;
}

package dev.learn.i2p.proto;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SimpleProtocolBoundaryTest {

    // Допустим, протокол ограничивает полезную нагрузку
    static final int MIN_PAYLOAD = 0;
    static final int MAX_PAYLOAD = 64 * 1024; // 64 KiB

    private static boolean isPayloadSizeValid(int size) {
        return size >= MIN_PAYLOAD && size <= MAX_PAYLOAD;
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 1024, 64 * 1024})
    void valid_sizes_pass(int size) {
        assertTrue(isPayloadSizeValid(size), "Ожидали валидный размер: " + size);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, -100, 64 * 1024 + 1, Integer.MAX_VALUE})
    void invalid_sizes_fail(int size) {
        assertFalse(isPayloadSizeValid(size), "Ожидали НЕвалидный размер: " + size);
    }
}

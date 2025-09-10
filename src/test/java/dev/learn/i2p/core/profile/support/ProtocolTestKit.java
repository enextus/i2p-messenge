package dev.learn.i2p.core.profile.support;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.function.ThrowingSupplier;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

/**
 * Вспомогалки для тестов протокола.
 * - ParserAdapter: находит статический метод парсера по reflection (parse/decode/fromBytes).
 * - makeFrame*: генераторы фреймов (по умолчанию length-prefix 4B BE + payload; подгони при необходимости).
 * - invalid UTF-8 генераторы и т.п.
 */
public final class ProtocolTestKit {
    private ProtocolTestKit() {}

    // === Настройки по умолчанию (подгони под свой протокол) ===
    public static final int DEFAULT_MAX_PAYLOAD = 64 * 1024; // если у тебя другой лимит — поменяй
    public static final Duration PARSE_TIMEOUT = Duration.ofMillis(250);

    // === Парсер через reflection ===
    public static final class ParserAdapter {
        private final Method m;

        private ParserAdapter(Method m) { this.m = m; }

        public static ParserAdapter load(String fqnClass) {
            try {
                Class<?> cls = Class.forName(fqnClass);
                // Пробуем распространённые имена API
                for (String name : new String[]{"parse", "decode", "fromBytes"}) {
                    try {
                        Method cand = cls.getDeclaredMethod(name, byte[].class);
                        if ((cand.getModifiers() & java.lang.reflect.Modifier.STATIC) != 0) {
                            cand.setAccessible(true);
                            return new ParserAdapter(cand);
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
                Assumptions.abort("Не найден подходящий статический метод парсера в " + fqnClass +
                        " (ищу parse/decode/fromBytes(byte[]))");
                return null;
            } catch (ClassNotFoundException e) {
                Assumptions.abort("Класс парсера не найден: " + fqnClass);
                return null;
            }
        }

        @SuppressWarnings("unchecked")
        public <T> T parse(byte[] frame) throws Throwable {
            try {
                return (T) m.invoke(null, frame);
            } catch (InvocationTargetException ite) {
                throw ite.getTargetException();
            }
        }
    }

    // === Генераторы фреймов (по умолчанию: [len (4B BE)] + payload) ===
    public static byte[] makeFrameWithLenPrefix(byte[] payload) {
        ByteBuffer bb = ByteBuffer.allocate(4 + payload.length);
        bb.putInt(payload.length);
        bb.put(payload);
        return bb.array();
    }

    public static byte[] makeFrameWithLen(int length, byte fill) {
        if (length < 0) {
            // отрицательная длина — положим как есть (two's complement) в префикс
            ByteBuffer bb = ByteBuffer.allocate(4);
            bb.putInt(length);
            byte[] prefix = bb.array();
            // без payload
            return prefix;
        } else {
            byte[] payload = new byte[length];
            Arrays.fill(payload, fill);
            return makeFrameWithLenPrefix(payload);
        }
    }

    public static byte[] truncated(byte[] full, int leave) {
        if (leave < 0) leave = 0;
        if (leave > full.length) leave = full.length;
        return Arrays.copyOf(full, leave);
    }

    // Невалидная UTF-8: одиночные продолжения, незакрытые многобайтовые
    public static byte[] invalidUtf8Payload() {
        return new byte[]{(byte) 0xC3, /* ожидает continuation */ (byte) 0x28};
    }

    public static byte[] randomBytes(int n, long seed) {
        byte[] a = new byte[n];
        new Random(seed).nextBytes(a);
        return a;
    }

    public static byte[] utf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    // Защита от зависаний (DoS): выполняем парс с таймаутом
    public static <T> T withTimeout(ThrowingSupplier<T> action) {
        return assertTimeoutPreemptively(PARSE_TIMEOUT, () -> {
            try {
                return action.get();
            } catch (RuntimeException e) {
                throw e;
            } catch (Throwable t) {
                // Заворачиваем checked в RTE, чтобы не тянуть сигнатуры
                throw new RuntimeException(t);
            }
        });
    }
}

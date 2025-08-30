/*
 * Start of file src/main/java/dev/learn/i2p/cli/Main.java
 */
package dev.learn.i2p.cli;

import dev.learn.i2p.core.Messenger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Arrays;

import static dev.learn.i2p.core.Constants.EXIT_FAILURE;
import static dev.learn.i2p.core.Constants.EXIT_USAGE;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("CLI args: {}", Arrays.toString(args));

        if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            usage();
            System.exit(EXIT_USAGE);
        }

        try (Messenger m = Messenger.createDefault()) {
            switch (args[0]) {
                case "address" -> System.out.println(m.myB32()); // ← без лишних {}

                case "listen" -> {
                    // Корректно закрыть транспорт/пулы при Ctrl+C / SIGTERM
                    Runtime.getRuntime().addShutdownHook(new Thread(m::close));
                    m.listen(); // блокирующий цикл; при прерывании сработает hook
                }

                case "send-text" -> {
                    if (args.length < 3) {
                        log.error("send-text requires: <peer.b32.i2p> <message...>");
                        usage();
                        System.exit(EXIT_USAGE);
                    }
                    String dest = args[1];
                    String msg = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    log.info("send-text -> {} ({} bytes)", dest, msg.getBytes(StandardCharsets.UTF_8).length);
                    m.sendText(dest, msg);
                }

                case "send-image" -> {
                    if (args.length != 3) {
                        log.error("send-image requires: <peer.b32.i2p> <path/to/image>");
                        usage();
                        System.exit(EXIT_USAGE);
                    }
                    String dest = args[1];
                    Path img = Path.of(args[2]);
                    log.info("send-image -> {} (path: {})", dest, img.toAbsolutePath());
                    m.sendImage(dest, img);
                }

                default -> {
                    log.error("Unknown command: {}", args[0]);
                    usage();
                    System.exit(EXIT_USAGE);
                }
            }
        } catch (Throwable t) {
            // В CLI оставим полный стектрейс — полезно для диагностики
            log.error("CLI failed: {}", t.getMessage(), t);
            System.exit(EXIT_FAILURE);
        }
    }

    private static void usage() {
        System.out.println("""
                Usage:
                  i2p-messenger address
                  i2p-messenger listen
                  i2p-messenger send-text <peer.b32.i2p> <message>
                  i2p-messenger send-image <peer.b32.i2p> <path/to/image>

                Examples:
                  i2p-messenger address
                  i2p-messenger listen
                  i2p-messenger send-text uz6d...f2dq.b32.i2p "привет из I2P"
                  i2p-messenger send-image uz6d...f2dq.b32.i2p ./cat.png
                """);
    }
}

/*
 * End of file src/main/java/dev/learn/i2p/cli/Main.java
 */

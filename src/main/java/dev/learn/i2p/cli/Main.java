/*
 * Start of file src/cli/Main.java
 * */

package dev.learn.i2p.cli;

import dev.learn.i2p.core.Messenger;

public class Main {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            return;
        }
        try (Messenger m = Messenger.createDefault()) {
            switch (args[0]) {
                case "address" -> System.out.println(m.myB32());
                case "listen" -> m.listen();
                case "send-text" -> m.sendText(args[1], String.join(" ",
                        java.util.Arrays.copyOfRange(args, 2, args.length)));
                case "send-image" -> m.sendImage(args[1], java.nio.file.Path.of(args[2]));
                default -> usage();
            }
        }
    }

    private static void usage() {
        System.out.println("usage ...");
    }

}

/*
 * End of file src/cli/Main.java
 * */

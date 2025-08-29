package dev.learn.i2p.proto;

import dev.learn.i2p.core.MessageType;
import net.i2p.client.streaming.I2PSocket;

import java.io.*;
import java.nio.file.*;

public final class Protocol {
    private final Path inboxDir;
    public Protocol(Path inboxDir) throws IOException {
        this.inboxDir = inboxDir; Files.createDirectories(inboxDir);
    }

    public void handle(I2PSocket socket) throws IOException {
        try (socket;
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            int type = in.readUnsignedByte();
            if (type == MessageType.TEXT.code) {
                int len = in.readInt();
                byte[] buf = in.readNBytes(len);
                System.out.println("[TEXT] " + new String(buf, java.nio.charset.StandardCharsets.UTF_8));
                out.writeUTF("OK");
            } else if (type == MessageType.IMAGE.code) {
                String fileName = in.readUTF();
                String contentType = in.readUTF();
                int size = in.readInt();
                byte[] data = in.readNBytes(size);
                String safe = fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
                if (safe.isBlank()) safe = "image";
                String ext = guessExt(contentType, safe);
                Path outPath = inboxDir.resolve(safe + "_" + System.currentTimeMillis() + ext);
                Files.write(outPath, data);
                System.out.println("[IMAGE] saved: " + outPath + " (" + contentType + ", " + size + " bytes)");
                out.writeUTF("OK");
            } else {
                out.writeUTF("ERR: unknown type");
            }
            out.flush();
        }
    }

    public void sendText(I2PSocket s, String text) throws IOException {
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
             DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()))) {
            byte[] payload = text.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            out.writeByte(MessageType.TEXT.code);
            out.writeInt(payload.length);
            out.write(payload);
            out.flush();
            System.out.println("Server replied: " + in.readUTF());
        }
    }

    public void sendImage(I2PSocket s, Path imgPath) throws IOException {
        byte[] data = Files.readAllBytes(imgPath);
        String ct = Files.probeContentType(imgPath);
        if (ct == null) ct = "application/octet-stream";
        String name = imgPath.getFileName().toString();

        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
             DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()))) {
            out.writeByte(MessageType.IMAGE.code);
            out.writeUTF(name);
            out.writeUTF(ct);
            out.writeInt(data.length);
            out.write(data);
            out.flush();
            System.out.println("Server replied: " + in.readUTF());
        }
    }

    private static String guessExt(String contentType, String fallbackName) {
        String lower = contentType.toLowerCase();
        if (lower.contains("png")) return ".png";
        if (lower.contains("jpeg") || lower.contains("jpg")) return ".jpg";
        if (lower.contains("gif")) return ".gif";
        int dot = fallbackName.lastIndexOf('.');
        return dot > 0 ? fallbackName.substring(dot) : ".bin";
    }

}

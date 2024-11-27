package com.duynguyen;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Session {
    private static final byte[] KEY = "MySecretKey12345".getBytes();
    private final Socket socket;
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private volatile boolean running;
    private final ExecutorService executor;

    public static byte[] encrypt(byte[] data) {
        byte[] result = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            result[i] = (byte) (data[i] ^ KEY[i % KEY.length]);
        }
        return result;
    }
    public static byte[] decrypt(byte[] data) {
        return encrypt(data);
    }


    public Session(Socket socket) throws IOException {
        this.socket = socket;
        this.socket.setKeepAlive(true);
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
        this.running = true;
        this.executor = Executors.newFixedThreadPool(2);
    }

    public void start() {
        executor.submit(this::receiveMessages);

        executor.submit(this::consoleInput);
    }

    private void receiveMessages() {
        try {
            while (running) {
                byte[] message = readMessage();
                if (message == null) break;

                byte[] decryptedMessage = decrypt(message);
                System.out.println("Received: " + new String(decryptedMessage, StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            System.out.println("Receive error: " + e.getMessage());
        } finally {
            close();
        }
    }

    private void consoleInput() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
                String input = scanner.nextLine();
                if ("exit".equalsIgnoreCase(input)) {
                    close();
                    break;
                }
                sendMessage(input.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            System.out.println("Input error: " + e.getMessage());
        }
    }

    public void sendMessage(byte[] message) {
        try {
            byte[] encryptedMessage = encrypt(message);

            dos.writeInt(encryptedMessage.length);

            int offset = 0;
            while (offset < encryptedMessage.length) {
                int remainingBytes = encryptedMessage.length - offset;
                int chunkSize = Math.min(remainingBytes, 1024);

                dos.write(encryptedMessage, offset, chunkSize);
                dos.flush();

                offset += chunkSize;
            }
        } catch (IOException e) {
            System.out.println("Send error: " + e.getMessage());
            close();
        }
    }

    private byte[] readMessage() {
        try {
            int messageLength = dis.readInt();

            byte[] fullMessage = new byte[messageLength];

            int bytesRead = 0;
            while (bytesRead < messageLength) {
                int remaining = messageLength - bytesRead;
                int chunkSize = Math.min(remaining, 1024);

                int n = dis.read(fullMessage, bytesRead, chunkSize);
                if (n == -1) break;

                bytesRead += n;
            }

            if (bytesRead < messageLength) {
                System.out.println("Incomplete message");
                return null;
            }

            return fullMessage;
        } catch (EOFException e) {
            return null;
        } catch (IOException e) {
            System.out.println("Read error: " + e.getMessage());
            return null;
        }
    }

    public void close() {
        running = false;
        try {
            if (socket != null) socket.close();
            if (dis != null) dis.close();
            if (dos != null) dos.close();
            if (executor != null) executor.shutdown();
        } catch (IOException e) {
            System.out.println("Close error: " + e.getMessage());
        }
    }
}
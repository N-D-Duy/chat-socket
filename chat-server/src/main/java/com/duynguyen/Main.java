package com.duynguyen;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        SocketIO.init();
        try (ServerSocket serverSocket = new ServerSocket(1609)) {
            System.out.println("Server is listening on port 1609");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());

                Session session = new Session(clientSocket);
                session.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
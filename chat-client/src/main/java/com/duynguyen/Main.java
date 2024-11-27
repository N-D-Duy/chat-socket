package com.duynguyen;

import java.io.IOException;
import java.net.Socket;

public class Main {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 1609);
            Session session = new Session(socket);
            session.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
package com.asia;

import com.asia.manager.WebSocketManager;

import java.util.Scanner;

public class App2 {
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            WebSocketManager.getInstance().disconnect();
        }));

        WebSocketManager.getInstance().connect();

        // 保持主线程运行
        new Scanner(System.in).nextLine();
    }
}

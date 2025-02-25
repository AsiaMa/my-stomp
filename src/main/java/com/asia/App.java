package com.asia;

import com.asia.config.WebSocketConfig;
import com.asia.manager.WebSocketManager;

import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> WebSocketManager.getInstance(WebSocketConfig.USE_STANDARD_WEB_SOCKET).disconnect()));

        WebSocketManager.getInstance(WebSocketConfig.USE_STANDARD_WEB_SOCKET).connect();

        // 保持主线程运行
        new Scanner(System.in).nextLine();
    }
}

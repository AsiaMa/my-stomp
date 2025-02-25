package com.asia.config;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class WebSocketConfig {
    public static final int RETRY_DELAY = 5; // 重连延迟
    public static final String WEBSOCKET_URL = "ws://localhost:12356/ws"; // WebSocket URL
    public static final ScheduledExecutorService HEARTBEAT_SCHEDULER = Executors.newSingleThreadScheduledExecutor(); // 心跳检测线程池
    public static final ScheduledExecutorService RECONNECT_SCHEDULER = Executors.newSingleThreadScheduledExecutor(); // 重连线程池
}

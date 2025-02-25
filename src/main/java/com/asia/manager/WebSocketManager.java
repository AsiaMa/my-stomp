package com.asia.manager;

import com.asia.config.WebSocketConfig;
import com.asia.handler.MessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WebSocketManager {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketManager.class);
    private final ScheduledExecutorService reconnectScheduler = Executors.newSingleThreadScheduledExecutor();
    private static final ScheduledExecutorService HEARTBEAT_SCHEDULER = Executors.newSingleThreadScheduledExecutor(); // 心跳检测线程池
    private static WebSocketManager instance;
    private WebSocketStompClient stompClient;
    private final MessageHandler messageHandler = new MessageHandler();

    private WebSocketManager() {
        initializeClient();
    }

    public static synchronized WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }

    private void initializeClient() {
        List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        sockJsClient.setMessageCodec(new Jackson2SockJsMessageCodec(new ObjectMapper()));

        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());
        stompClient.setTaskScheduler(new ConcurrentTaskScheduler(HEARTBEAT_SCHEDULER));
    }

    public void connect() {
        logger.info("尝试连接 WebSocket...");
        CompletableFuture<StompSession> feature = stompClient.connectAsync(WebSocketConfig.WEBSOCKET_URL, messageHandler);

        feature.thenApply(session -> {
            logger.info("WebSocket 连接成功: Session ={}", session);
            return session;
        }).exceptionally(ex -> {
            logger.error("WebSocket 连接失败: {}", ex.getMessage());
            return null;
        });
    }

    public void scheduleReconnect() {
        logger.info("{} 秒后尝试重连...", WebSocketConfig.RETRY_DELAY);
        reconnectScheduler.schedule(this::connect, WebSocketConfig.RETRY_DELAY, TimeUnit.SECONDS);
    }

    public void disconnect() {
        logger.info("关闭 WebSocket 连接...");

        stompClient.stop();
        HEARTBEAT_SCHEDULER.shutdown();
        reconnectScheduler.shutdown();
    }
}

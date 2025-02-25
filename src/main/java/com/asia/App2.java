package com.asia;

import com.asia.handler.MessageHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;

import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class App2 {
    private static final Logger logger = LoggerFactory.getLogger(App2.class);

    private static final Integer RETRY_DELAY = 5;
    // 用于心跳检测的线程池
    private static final ScheduledExecutorService HEARTBEAT_SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    // 用于重连的线程池（单独管理重连任务）
    private static final ScheduledExecutorService RECONNECT_SCHEDULER = Executors.newSingleThreadScheduledExecutor();
    private static WebSocketStompClient stompClient;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("正在优雅关闭 WebSocket 连接...");
            HEARTBEAT_SCHEDULER.shutdown();
            RECONNECT_SCHEDULER.shutdown();
            if (stompClient != null) {
                stompClient.stop();
            }
        }));

        connect();

        // 保持主线程运行
        new Scanner(System.in).nextLine();
    }

    private static void connect() {
        // 1. 创建 WebSocket 传输方式
        List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));

        // 2. 创建 SockJsClient
        SockJsClient sockJsClient = new SockJsClient(transports);

        // 3. 设置 Jackson2SockJsMessageCodec 解决 No SockJsMessageCodec set 问题
        sockJsClient.setMessageCodec(new Jackson2SockJsMessageCodec(new ObjectMapper()));

        // 4. 创建 WebSocketStompClient
        stompClient = new WebSocketStompClient(sockJsClient);

        // 5. 使用 MappingJackson2MessageConverter 替换 StringMessageConverter
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompClient.setTaskScheduler(new ConcurrentTaskScheduler(HEARTBEAT_SCHEDULER));

        // 6. 连接服务器
        String wsUrl = "ws://localhost:12356/ws";
        StompSessionHandler sessionHandler = new MessageHandler();
        stompClient.connectAsync(wsUrl, sessionHandler);
    }

    public static void scheduleReconnect() {
        logger.info("{}秒后重新连接...", RETRY_DELAY);

        RECONNECT_SCHEDULER.schedule(() -> {
            System.out.println("正在进行重连...");
            connect();
        }, RETRY_DELAY, TimeUnit.SECONDS);
    }
}

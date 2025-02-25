package com.asia;

import com.asia.config.WebSocketConfig;
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
import java.util.concurrent.TimeUnit;

public class App2 {
    private static final Logger logger = LoggerFactory.getLogger(App2.class);
    private static WebSocketStompClient stompClient;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("正在优雅关闭 WebSocket 连接...");
            WebSocketConfig.HEARTBEAT_SCHEDULER.shutdown();
            WebSocketConfig.RECONNECT_SCHEDULER.shutdown();
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

        stompClient.setTaskScheduler(new ConcurrentTaskScheduler(WebSocketConfig.HEARTBEAT_SCHEDULER));

        // 6. 连接服务器
        StompSessionHandler sessionHandler = new MessageHandler();
        stompClient.connectAsync(WebSocketConfig.WEBSOCKET_URL, sessionHandler);
    }

    public static void scheduleReconnect() {
        logger.info("{}秒后重新连接...", WebSocketConfig.RETRY_DELAY);

        WebSocketConfig.RECONNECT_SCHEDULER.schedule(() -> {
            System.out.println("正在进行重连...");
            connect();
        }, WebSocketConfig.RETRY_DELAY, TimeUnit.SECONDS);
    }
}

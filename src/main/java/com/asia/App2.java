package com.asia;

import com.asia.domain.Student;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.springframework.web.socket.sockjs.frame.Jackson2SockJsMessageCodec;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class App2 {
    private static final Integer retryDelay = 5000;
    // private static final AtomicInteger retryCount = new AtomicInteger(0);
    private static final AtomicBoolean reconnecting = new AtomicBoolean(false);
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static WebSocketStompClient stompClient;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("资源释放");
            scheduler.shutdown();
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

        stompClient.setTaskScheduler(new ConcurrentTaskScheduler(scheduler));

        // 6. 连接服务器
        String wsUrl = "ws://localhost:12356/ws";
        StompSessionHandler sessionHandler = new MyStompSessionHandler();
        stompClient.connectAsync(wsUrl, sessionHandler);
    }

    static class MyStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, @NonNull StompHeaders connectedHeaders) {
            System.out.println("连接到ws服务");

            // 订阅接收消息的目的地（根据服务端要求修改）
            session.subscribe("/topic/messages", new StompFrameHandler() {
                @Override
                @NonNull
                public Type getPayloadType(@NonNull StompHeaders headers) {
                    System.out.println("getPayloadType方法,headers:" + headers);
                    return Student.class;
                }

                @Override
                public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                    System.out.println("Received message: " + payload);
                }
            });

            // 发送测试消息（可选）
            session.send("/app/hello", "嘿嘿哈哈哈");
        }

        @Override
        public void handleException(@NonNull StompSession session, StompCommand command,
                                    @NonNull StompHeaders headers, @NonNull byte[] payload, Throwable exception) {
            System.err.println("Communication error: " + exception.getMessage());
        }

        @Override
        public void handleTransportError(@NonNull StompSession session, Throwable exception) {
            System.err.println("Transport error: " + exception.getMessage());

            // 连接断开时，尝试重新连接
            scheduleReconnect();
        }

        private static void scheduleReconnect() {
            if (!reconnecting.compareAndSet(false, true)) {
                System.out.println("已经在进行重连,return此次重连");
                return;  // 如果已经有重连任务在执行，则直接返回
            }

            System.out.printf("%d秒后重新连接...\n", retryDelay);

            scheduler.schedule(() -> {
                System.out.println("马上进行重连...");
                connect();
                reconnecting.set(false);
            }, 5000, TimeUnit.SECONDS);
        }
    }
}

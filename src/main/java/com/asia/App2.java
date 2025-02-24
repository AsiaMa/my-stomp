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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class App2 {
    private static final int MAX_RETRY = 10;
    private static final long INITIAL_DELAY = 5;
    private static final AtomicInteger retryCount = new AtomicInteger(0);
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
        CompletableFuture<StompSession> future = stompClient.connectAsync(wsUrl, sessionHandler);

        // 添加异步处理
        future.whenComplete((session, ex) -> {
            if (ex != null) {
                System.err.println("ws连接失败: " + ex.getMessage());
                MyStompSessionHandler.scheduleReconnect();
            } else {
                System.out.println("已连接上ws");
                retryCount.set(0);
            }
        });
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

        private static synchronized void scheduleReconnect() {
            if (retryCount.getAndIncrement() >= MAX_RETRY) {
                System.out.println("达到了最大重连次数。");
                return;
            }

            long delay = (long) (INITIAL_DELAY * Math.pow(2, retryCount.get()));
            System.out.printf("正在重新连接 #%d in %ds...\n", retryCount.get(), delay);

            scheduler.schedule(() -> {
                System.out.println("重连中...");
                connect();
            }, delay, TimeUnit.SECONDS);
        }
    }
}

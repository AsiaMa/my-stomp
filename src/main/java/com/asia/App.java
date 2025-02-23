package com.asia;

import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Hello world!
 */
public class App {
    public static void main(String[] args) {
        // 1. 创建 WebSocket 客户端
//        List<Transport> transports = Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()));
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
//        WebSocketStompClient stompClient = new WebSocketStompClient(new SockJsClient(transports));

        // 2. 配置消息转换器
        stompClient.setMessageConverter(new StringMessageConverter());

        // 3. 构建连接地址（带 WS_TOKEN 参数）
        String wsUrl = "ws://localhost:22222/ws?WS_TOKEN=e12826e23b9946f5893dac8ca15be45e";

        // 4. 连接服务器
        StompSessionHandler sessionHandler = new MyStompSessionHandler();
        stompClient.connectAsync(wsUrl, sessionHandler);

        // 保持主线程运行
        new Scanner(System.in).nextLine();
    }

    static class MyStompSessionHandler extends StompSessionHandlerAdapter {
        @Override
        public void afterConnected(StompSession session, @NonNull StompHeaders connectedHeaders) {
            System.out.println("Connected to server!");

            // 订阅接收消息的目的地（根据服务端要求修改）
            session.subscribe("/topic/messages", new StompFrameHandler() {
                @Override
                @NonNull
                public Type getPayloadType(@NonNull StompHeaders headers) {
                    return String.class;
                }

                @Override
                public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                    System.out.println("Received message: " + payload);
                }
            });

            // 发送测试消息（可选）
            session.send("/app/endpoint", "Hello from Java client!");
        }

        @Override
        public void handleException(@NonNull StompSession session, StompCommand command,
                                    @NonNull StompHeaders headers, @NonNull byte[] payload, Throwable exception) {
            System.err.println("Communication error: " + exception.getMessage());
        }

        @Override
        public void handleTransportError(@NonNull StompSession session, Throwable exception) {
            System.err.println("Transport error: " + exception.getMessage());
        }
    }
}

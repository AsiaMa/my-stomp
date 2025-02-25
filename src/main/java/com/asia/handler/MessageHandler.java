package com.asia.handler;

import com.asia.config.WebSocketConfig;
import com.asia.domain.Student;
import com.asia.manager.WebSocketManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.client.ResourceAccessException;

import java.lang.reflect.Type;

public class MessageHandler extends StompSessionHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    @Override
    public void afterConnected(StompSession session, @NonNull StompHeaders connectedHeaders) {
        // 订阅接收消息的目的地（根据服务端要求修改）
        session.subscribe("/topic/messages", new StompFrameHandler() {
            @Override
            @NonNull
            public Type getPayloadType(@NonNull StompHeaders headers) {
                logger.info("getPayloadType方法,headers:{}", headers);
                return Student.class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                logger.info("接收到消息:{}", payload);
            }
        });

        // 发送测试消息（可选）
        session.send("/app/hello", "嘿嘿哈哈哈");
    }

    @Override
    public void handleException(@NonNull StompSession session, StompCommand command,
                                @NonNull StompHeaders headers, @NonNull byte[] payload, Throwable exception) {
        logger.error("处理STOMP帧时出现异常: {}", exception.getMessage());
    }

    @Override
    public void handleTransportError(@NonNull StompSession session, Throwable exception) {
        logger.error("websocket传输错误: {}", exception.getMessage());

        // 连接断开时，尝试重新连接
        if (exception instanceof ResourceAccessException) {
            WebSocketManager.getInstance(WebSocketConfig.USE_STANDARD_WEB_SOCKET).scheduleReconnect();
        }
    }
}

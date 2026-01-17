package com.example.sunxu_mall.websocket;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 服务端
 * 接口路径: /ws/{userId}
 */
@Slf4j
@Component
@ServerEndpoint("/ws/{userId}")
public class WebSocketServer {

    /**
     * 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
     */
    private static int onlineCount = 0;

    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的MyWebSocket对象。
     */
    private static final ConcurrentHashMap<String, WebSocketServer> webSocketMap = new ConcurrentHashMap<>();

    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;

    /**
     * 接收 userId
     */
    private String userId = "";

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.session = session;
        this.userId = userId;
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            webSocketMap.put(userId, this);
        } else {
            webSocketMap.put(userId, this);
            addOnlineCount();
        }
        log.info("User connected: {}, current online count: {}", userId, getOnlineCount());
        try {
            sendMessage("连接成功");
        } catch (IOException e) {
            log.error("User: {}, network error", userId);
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.remove(userId);
            subOnlineCount();
        }
        log.info("User logged out: {}, current online count: {}", userId, getOnlineCount());
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     * @param session 会话
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        log.info("User message: {}, payload: {}", userId, message);
        // 可以根据需要处理客户端发送的消息
    }

    /**
     * @param session 会话
     * @param error   错误
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("User error: {}, reason: {}", this.userId, error.getMessage());
        error.printStackTrace();
    }

    /**
     * 实现服务器主动推送
     */
    public void sendMessage(String message) throws IOException {
        this.session.getBasicRemote().sendText(message);
    }

    /**
     * 发送自定义消息
     */
    public static void sendInfo(String userId, String message) {
        log.info("Sending message to: {}, payload: {}", userId, message);
        if (userId != null && webSocketMap.containsKey(userId)) {
            try {
                webSocketMap.get(userId).sendMessage(message);
            } catch (IOException e) {
                log.error("Failed to send message to: {}", userId);
                e.printStackTrace();
            }
        } else {
            log.warn("User: {} not online, message not sent", userId);
        }
    }

    /**
     * 发送对象消息 (JSON)
     */
    public static void sendObject(String userId, Object object) {
        if (object != null) {
            sendInfo(userId, JSONUtil.toJsonStr(object));
        }
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WebSocketServer.onlineCount--;
    }
}

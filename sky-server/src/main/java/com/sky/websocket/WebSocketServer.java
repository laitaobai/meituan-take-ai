package com.sky.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket服务
 */
@Component
@ServerEndpoint("/ws/{sid}")
@Slf4j
public class WebSocketServer {
    //存放会话对象 ，类似发送的信息
    private static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
       // System.out.println("客户端：" + sid + "建立连接");
        sessionMap.put(sid, session);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, @PathParam("sid") String sid) {
        System.out.println("收到来自客户端：" + sid + "的信息:" + message);
    }

    /**
     * 连接关闭调用的方法
     *
     * @param sid
     */
    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        System.out.println("连接断开:" + sid);
        sessionMap.remove(sid);
    }

    /**
     * 定向发送给指定用户。
     * 前端需以 userId 作为 sid 建立连接：ws://host/ws/{userId}
     * @param userId 用户id
     * @param message 消息内容
     */
    public void sendToUser(Long userId, String message) {
        if (userId == null) {
            return;
        }
        Session session = sessionMap.get(String.valueOf(userId));
        if (session == null || !session.isOpen()) {
            log.warn("用户未建立 WebSocket 连接，定向推送跳过, userId={}", userId);
            return;
        }
        try {
            synchronized (session) {
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            log.error("定向推送失败, userId={}", userId, e);
        }
    }

    /**
     * 群发
     *
     * @param message
     */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            try {
                //同一个 Session 不允许并发发送，需按会话串行化
                synchronized (session) {
                    if (session.isOpen()) {
                        session.getBasicRemote().sendText(message);
                    }
                }
            } catch (Exception e) {
                log.error("WebSocket 推送失败, sid={}", session.getId(), e);
            }
        }
    }
}

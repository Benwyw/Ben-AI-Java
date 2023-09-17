package com.benwyw.bot.config;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
@ServerEndpoint("/websocket/{userId}")
public class WebSocket implements WebSocketMessageBrokerConfigurer {
    private Session session;
    private Integer userId;
    private long sessionCreationTime;
    private static CopyOnWriteArraySet<WebSocket> webSockets = new CopyOnWriteArraySet<>();
    private static ConcurrentHashMap<Integer, Session> sessionPool = new ConcurrentHashMap<Integer, Session>();
    private static final long EXPIRATION_DURATION_IN_MILLISECONDS = 1 * 60 * 60 * 1000; // 1 hour in milliseconds
    private static final Integer DEFAULT_USER_ID = 1;
    private static final Integer SERVER_USER_ID = 0;

    private Integer getUserId() {
        return this.userId;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam(value="userId") Integer userId) {
        try {
            this.session = session;
            this.userId = userId;
            this.sessionCreationTime = System.currentTimeMillis();
            webSockets.add(this);
            sessionPool.put(userId, session);

            String formattedString = String.format("User %s connected, total of %s online people", userId, webSockets.size());
            log.info(formattedString);
            sendAllMessage(SERVER_USER_ID, formattedString);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @OnClose
    public void onClose() {
        try {
            webSockets.remove(this);
            sessionPool.remove(this.userId);

            String formattedString = String.format("User %s disconnected, total of %s online people", userId, webSockets.size());
//            log.info(formattedString);
            sendAllMessage(SERVER_USER_ID, formattedString);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @OnMessage
    public void onMessage(String message) {
//        log.info("[WebSocket] User {}, sent {}", userId, message);
        sendAllMessage(userId, message);
    }

    @OnError
    public void onError(Session session, Throwable error) {
//        log.info("[WebSocket] User error, reason {}", error.getMessage());
        error.printStackTrace();
    }

    /**
     * Session expire in 1 hour
     */
    @Scheduled(fixedDelay = EXPIRATION_DURATION_IN_MILLISECONDS)
    public void checkSessionExpiration() {
        long currentTime = System.currentTimeMillis();
        for (WebSocket webSocket : webSockets) {
            if (currentTime - webSocket.sessionCreationTime >= EXPIRATION_DURATION_IN_MILLISECONDS) {
                sendAllMessage(SERVER_USER_ID, "Your session has expired.");
                webSocket.closeSession();
            }
        }
    }

    /**
     * Close session
     */
    private void closeSession() {
        try {
            this.session.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Auto handled
//    @PreDestroy
//    public void onExit() {
//        closeWebSockets();
//    }
//
//    private void closeWebSockets() {
//        for (Session session : sessionPool.values()) {
//            if (session.isOpen()) {
//                try {
//                    session.close();
//                } catch (IOException e) {
//                    log.error("Error closing WebSocket session: " + e.getMessage());
//                }
//            }
//        }
//    }

    public static Integer generateUserId() {
        Optional<Integer> maxUserId = webSockets.stream()
                .map(WebSocket::getUserId)
                .max(Comparator.naturalOrder());

        int nextUserId;
        if (maxUserId.isPresent()) {
            int max = maxUserId.get();
            nextUserId = max + 1;

            for (int i = 1; i <= max; i++) {
                final int currentId = i;
                if (!webSockets.stream().map(WebSocket::getUserId).anyMatch(id -> id.equals(currentId))) {
                    nextUserId = currentId;
                    break;
                }
            }
        } else {
            nextUserId = DEFAULT_USER_ID;
        }

        return nextUserId;
    }

    public static Integer getOnlineUserCount() {
        return webSockets.size();
    }

    /**
     * Format message with userId
     * @param userId Integer
     * @param message String
     * @return String
     */
    private String getFormattedMessage(Integer userId, String message) {
        String formattedMessage = String.format("[Server]: %s", message);
        if (!Objects.equals(userId, SERVER_USER_ID)) {
            formattedMessage = String.format("[User %s]: %s", userId, message);
        }
        return formattedMessage;
    }

    public void sendAllMessage(Integer userId, String message) {
        for (WebSocket webSocket : webSockets) {
            try {
                if (webSocket.session.isOpen()) {
                    webSocket.session.getAsyncRemote().sendText(getFormattedMessage(userId, message));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // In development, restricted Local only
    public void sendOneMessage(Integer userId, String message) {
        Session session = sessionPool.get(userId);
        if (session != null && session.isOpen()) {
            try {
                log.info("[WebSocket] Single message: {}", message);
                session.getAsyncRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // In development, restricted Local only
    public void sendMoreMessage(Integer[] userIds, String message) {
        for (Integer userId : userIds) {
            Session session = sessionPool.get(userId);
            if (session != null && session.isOpen()) {
                try {
                    log.info("[WebSocket] Single message: {}", message);
                    session.getAsyncRemote().sendText(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // In development, restricted Local only
    private void sendMessageToUser(Integer userId, String message) {
        Session session = sessionPool.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("Error sending message to User {}: {}", userId, e.getMessage());
            }
        } else {
            log.warn("User {} is not connected or session is closed.", userId);
        }
    }

}
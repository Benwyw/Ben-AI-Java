package com.benwyw.bot.config;

import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Component
@ServerEndpoint("/websocket/{userId}")
public class WebSocket implements WebSocketMessageBrokerConfigurer {
    private Session session;
    private String userId;
    private long sessionCreationTime;
    private static final CopyOnWriteArraySet<WebSocket> webSockets = new CopyOnWriteArraySet<>();
    private static final ConcurrentHashMap<String, Session> sessionPool = new ConcurrentHashMap<>();
    // Map userId -> WebSocket instance for targeted sends
    private static final ConcurrentHashMap<String, WebSocket> socketByUserId = new ConcurrentHashMap<>();
    private static final long EXPIRATION_DURATION_IN_MILLISECONDS = 24 * 60 * 60 * 1000; // 24 hour in milliseconds
    private static final String DEFAULT_USER_ID = "1";
    private static final String SERVER_USER_ID = "0";
    // Per-session send queue & state to serialize writes and avoid TEXT_FULL_WRITING
    private final Queue<String> outboundQueue = new ConcurrentLinkedQueue<>();
    private final AtomicBoolean sending = new AtomicBoolean(false);
    // Backoff scheduler (shared) for retrying failed sends due to transient state
    private static final ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "ws-send-retry");
        t.setDaemon(true);
        return t;
    });
    // Max retry attempts per message send
    private static final int MAX_RETRY = 3;

    private String getUserId() {
        return this.userId;
    }

    @OnOpen
    public void onOpen(Session session, @PathParam(value="userId") String userId) {
        try {
            this.session = session;
            this.userId = userId;
            this.sessionCreationTime = System.currentTimeMillis();
            webSockets.add(this);
            sessionPool.put(userId, session);
            socketByUserId.put(userId, this);

            // Configure async remote timeout to avoid indefinite TEXT_FULL_WRITING if client stalls
            try {
                session.getAsyncRemote().setSendTimeout(15000); // 15s safety
            } catch (Throwable ignored) {}

            String formattedString = String.format("User %s connected, total of %s online people", userId, webSockets.size());
            log.info(formattedString);
            sendAllMessage(getFormattedMessage(SERVER_USER_ID, formattedString));
            sendOnlineCountUpdate();
        } catch(Exception e) {
            log.error("Error during WebSocket onOpen", e);
        }
    }

    @OnClose
    public void onClose() {
        try {
            if (this.userId == null) {
                log.warn("WebSocket onClose before userId init; skipping keyed cleanup.");
                webSockets.remove(this);
                return;
            }
            webSockets.remove(this);
            sessionPool.remove(this.userId);
            socketByUserId.remove(this.userId);

            String formattedString = String.format("User %s disconnected, total of %s online people", userId, webSockets.size());
//            log.info(formattedString);
            sendAllMessage(getFormattedMessage(SERVER_USER_ID, formattedString));
            sendOnlineCountUpdate();
        } catch(Exception e) {
            log.error("Error in onClose cleanup", e);
        }
    }

    @OnMessage
    public void onMessage(String message) {
//        log.info("[WebSocket] User {}, sent {}", userId, message);
        sendAllMessage(getFormattedMessage(userId, message), userId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket error for user {}: {}", this.userId, error.getMessage(), error);
    }

    /**
     * Session expire in 24 hour
     */
    @Scheduled(fixedDelay = EXPIRATION_DURATION_IN_MILLISECONDS)
    public void checkSessionExpiration() {
        long currentTime = System.currentTimeMillis();
        for (WebSocket webSocket : webSockets) {
            if (currentTime - webSocket.sessionCreationTime >= EXPIRATION_DURATION_IN_MILLISECONDS) {
                webSocket.enqueueAndSend(getFormattedMessage(SERVER_USER_ID, "Your session has expired."));
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
            log.error("Error closing session", e);
        }
    }

    private static Integer tryParseInt(String s) {
        if (s == null) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
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

    public static String generateUserId() {
        Set<Integer> used = webSockets.stream()
                .map(WebSocket::getUserId)
                .map(WebSocket::tryParseInt)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        int next = 1;
        while (used.contains(next)) {
            next++;
        }
        return String.valueOf(next);
    }

    public static Integer getOnlineUserCount() {
        return webSockets.size();
    }

    /**
     * Format message with userId as JSON
     * @param userId Integer
     * @param message String
     * @return String JSON formatted message
     */
    public String getFormattedMessage(String userId, String message) {
        String formattedMessage = String.format("[Server]: %s", message);
        if (!Objects.equals(userId, SERVER_USER_ID)) {
            formattedMessage = String.format("[User %s]: %s", userId, message);
        }
        String safe = escapeJson(formattedMessage);
        return String.format("{\"type\":\"chatMessage\",\"content\":\"%s\"}", safe);
    }

    /**
     * Enqueue message and trigger drain
     */
    private void enqueueAndSend(String message) {
        Session s = this.session;
        if (s == null || !s.isOpen()) return;
        outboundQueue.add(message);
        drainQueue();
    }

    /**
     * Drain the outbound queue serially to avoid TEXT_FULL_WRITING issues
     */
    private void drainQueue() {
        if (!sending.compareAndSet(false, true)) {
            return; // send already in progress
        }
        String next = outboundQueue.poll();
        if (next == null) {
            sending.set(false);
            return;
        }
        Session s = this.session;
        if (s == null || !s.isOpen()) {
            sending.set(false);
            return;
        }
        try {
            s.getAsyncRemote().sendText(next, result -> {
                if (!result.isOK()) {
                    Throwable t = result.getException();
                    log.error("Async send failed for user {}: {}", this.userId, t != null ? t.getMessage() : "unknown", t);
                }
                sending.set(false);
                if (!outboundQueue.isEmpty()) {
                    drainQueue();
                }
            });
        } catch (IllegalStateException ise) {
            // Container still in TEXT_FULL_WRITING; requeue and retry with small backoff
            log.warn("Send state conflict for user {}: {}. Will retry.", this.userId, ise.getMessage());
            // Put message back at head (preserve order)
            outboundQueue.add(next);
            sending.set(false);
            scheduleRetry(1); // first retry
        } catch (Exception ex) {
            log.error("Unexpected send exception for user {}: {}", this.userId, ex.getMessage(), ex);
            sending.set(false);
            if (!outboundQueue.isEmpty()) {
                drainQueue();
            }
        }
    }

    /**
     * Schedule a retry with backoff
     */
    private void scheduleRetry(int attempt) {
        if (attempt > MAX_RETRY) {
            log.error("Exceeded max send retries for user {}; dropping {} pending messages", this.userId, outboundQueue.size());
            outboundQueue.clear();
            return;
        }
        retryScheduler.schedule(() -> {
            if (!outboundQueue.isEmpty()) {
                drainQueue();
            }
        }, attempt * 50L, TimeUnit.MILLISECONDS); // incremental backoff 50ms,100ms,150ms
    }

    public void sendAllMessage(String message) {
        for (WebSocket webSocket : new ArrayList<>(webSockets)) {
            try {
                if (webSocket.session != null && webSocket.session.isOpen()) {
                    webSocket.enqueueAndSend(message);
                }
            } catch (Exception e) {
                log.error("Error sending message to user: {}", webSocket.userId, e);
            }
        }
    }

    public void sendAllMessage(String message, String senderId) {
        for (WebSocket webSocket : new ArrayList<>(webSockets)) {
            try {
                // Skip sending message back to the sender (except for server messages)
                if (webSocket.session != null && webSocket.session.isOpen() &&
                    (Objects.equals(senderId, SERVER_USER_ID) || !Objects.equals(webSocket.userId, senderId))) {
                    webSocket.enqueueAndSend(message);
                }
            } catch (Exception e) {
                log.error("Error sending message to user: {}", webSocket.userId, e);
            }
        }
    }

    // In development, restricted Local only
    public void sendOneMessage(String userId, String message) {
        WebSocket socket = socketByUserId.get(userId);
        if (socket != null && socket.session != null && socket.session.isOpen()) {
            try {
                log.info("[WebSocket] Single message: {}", message);
                socket.enqueueAndSend(message);
            } catch (Exception e) {
                log.error("Error queueing message for user {}: {}", userId, e.getMessage(), e);
            }
        } else {
            log.warn("User {} not found or session closed; message dropped.", userId);
        }
    }

    // In development, restricted Local only
    public void sendMoreMessage(String[] userIds, String message) {
        for (String uid : userIds) {
            WebSocket socket = socketByUserId.get(uid);
            if (socket != null && socket.session != null && socket.session.isOpen()) {
                try {
                    log.info("[WebSocket] Single message: {}", message);
                    socket.enqueueAndSend(message);
                } catch (Exception e) {
                    log.error("Error queueing message for user {}: {}", uid, e.getMessage(), e);
                }
            } else {
                log.warn("User {} not found or session closed; message dropped.", uid);
            }
        }
    }

    // In development, restricted Local only
    private void sendMessageToUser(String userId, String message) {
        WebSocket socket = socketByUserId.get(userId);
        if (socket != null && socket.session != null && socket.session.isOpen()) {
            socket.enqueueAndSend(message);
        } else {
            log.warn("User {} is not connected or session is closed.", userId);
        }
    }

    private void sendOnlineCountUpdate() {
        String message = String.format("{\"type\":\"onlineCountUpdate\",\"count\":%d}", webSockets.size());
        for (WebSocket webSocket : new ArrayList<>(webSockets)) {
            try {
                if (webSocket.session != null && webSocket.session.isOpen()) {
                    webSocket.enqueueAndSend(message);
                }
            } catch (Exception e) {
                log.error("Error sending online count update to user: {}", webSocket.userId, e);
            }
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

}
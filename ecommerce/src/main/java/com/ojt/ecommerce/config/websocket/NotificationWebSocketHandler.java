package com.ojt.ecommerce.config.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private final Set<WebSocketSession> sessions = new CopyOnWriteArraySet<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
        log.info("WebSocket connected: session id={}, total active={}", session.getId(), sessions.size());
        try {
            session.sendMessage(new TextMessage("{\"type\":\"CONNECTED\",\"message\":\"Connected to admin notification stream\"}"));
        } catch (IOException e) {
            log.warn("Failed to send welcome message to session {}", session.getId(), e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload)) {
            try {
                session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
            } catch (IOException e) {
                log.warn("Failed to respond to ping for session {}", session.getId(), e);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("WebSocket closed: session id={}, status={}, total active={}", session.getId(), status, sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        sessions.remove(session);
    }

    /**
     * Broadcasts a JSON message to all currently connected admin sessions.
     */
    public void broadcast(String jsonPayload) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(jsonPayload));
                    }
                } catch (Exception e) {
                    log.error("Failed to deliver notification to session {}", session.getId(), e);
                    sessions.remove(session);
                }
            } else {
                sessions.remove(session);
            }
        }
    }

    public int getActiveSessionCount() {
        return sessions.size();
    }
}

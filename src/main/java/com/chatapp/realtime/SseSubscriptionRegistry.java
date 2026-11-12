package com.chatapp.realtime;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Keeps the open Server-Sent-Event connections per room. Redis Pub/Sub messages are
 * pushed to these connections, which lets a client (curl / browser / Postman SSE) see
 * messages in real time without any UI in this project.
 */
@Component
public class SseSubscriptionRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseSubscriptionRegistry.class);

    private final Map<String, Set<SseEmitter>> emittersByRoom = new ConcurrentHashMap<>();

    public SseEmitter subscribe(String roomId, long timeoutMs) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        emittersByRoom.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(emitter);

        Runnable cleanup = () -> remove(roomId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(t -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data("Subscribed to chatroom '" + roomId + "'"));
        } catch (IOException | IllegalStateException e) {
            cleanup.run();
        }
        return emitter;
    }

    public void broadcast(String roomId, String messageJson) {
        Set<SseEmitter> emitters = emittersByRoom.get(roomId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("message").data(messageJson, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException e) {
                log.debug("Dropping dead SSE connection for room {}", roomId);
                remove(roomId, emitter);
            }
        }
    }

    public void closeRoom(String roomId) {
        Set<SseEmitter> emitters = emittersByRoom.remove(roomId);
        if (emitters == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("room_deleted").data("Chatroom '" + roomId + "' was deleted"));
            } catch (IOException | IllegalStateException ignored) {
                // connection already gone
            }
            emitter.complete();
        }
    }

    public int subscriberCount(String roomId) {
        Set<SseEmitter> emitters = emittersByRoom.get(roomId);
        return emitters == null ? 0 : emitters.size();
    }

    private void remove(String roomId, SseEmitter emitter) {
        emittersByRoom.computeIfPresent(roomId, (k, set) -> {
            set.remove(emitter);
            return set.isEmpty() ? null : set;
        });
    }
}

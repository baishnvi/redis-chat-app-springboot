package com.chatapp.controller;

import com.chatapp.config.ChatProperties;
import com.chatapp.realtime.SseSubscriptionRegistry;
import com.chatapp.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chatapp/chatrooms/{roomId}/stream")
@Tag(name = "Real-time", description = "Live message stream backed by Redis Pub/Sub")
public class RealtimeController {

    private final ChatRoomService rooms;
    private final SseSubscriptionRegistry registry;
    private final ChatProperties properties;

    public RealtimeController(ChatRoomService rooms, SseSubscriptionRegistry registry, ChatProperties properties) {
        this.rooms = rooms;
        this.registry = registry;
        this.properties = properties;
    }

    @Operation(summary = "Subscribe to live messages (Server-Sent Events)",
            description = "Keeps the connection open and pushes every message published on the room's "
                    + "Redis Pub/Sub channel. Try it with: curl -N <url>")
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String roomId) {
        String id = rooms.requireRoom(roomId);
        return registry.subscribe(id, properties.sseTimeoutMs());
    }
}

package com.chatapp.service;

import static com.chatapp.util.RoomIdNormalizer.normalize;

import com.chatapp.config.ChatProperties;
import com.chatapp.dto.ApiResponse;
import com.chatapp.dto.MessagesResponse;
import com.chatapp.dto.SendMessageRequest;
import com.chatapp.exception.InvalidRequestException;
import com.chatapp.exception.NotAParticipantException;
import com.chatapp.exception.RoomNotFoundException;
import com.chatapp.model.ChatMessage;
import com.chatapp.realtime.MessageBroadcaster;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final ChatRoomRepository rooms;
    private final MessageRepository messages;
    private final MessageBroadcaster broadcaster;
    private final ObjectMapper objectMapper;
    private final ChatProperties properties;

    public MessageService(ChatRoomRepository rooms, MessageRepository messages, MessageBroadcaster broadcaster,
                          ObjectMapper objectMapper, ChatProperties properties) {
        this.rooms = rooms;
        this.messages = messages;
        this.broadcaster = broadcaster;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public ApiResponse sendMessage(String roomId, SendMessageRequest request) {
        String id = normalize(roomId);
        if (id.isEmpty() || !rooms.exists(id)) {
            throw new RoomNotFoundException(id);
        }
        String participant = request.participant().trim();
        if (properties.enforceMembership() && !rooms.isParticipant(id, participant)) {
            throw new NotAParticipantException(participant, id);
        }

        ChatMessage chatMessage = new ChatMessage(participant, request.message(),
                Instant.now().truncatedTo(ChronoUnit.MILLIS).toString());
        String json = toJson(chatMessage);

        messages.append(id, json, properties.maxMessagesPerRoom());   // 1) persist in Redis List
        try {
            broadcaster.publishMessage(id, json);                      // 2) real-time fan-out via Pub/Sub
        } catch (RuntimeException e) {
            log.warn("Message stored but Pub/Sub publish failed for room {}", id, e);
        }
        return ApiResponse.success("Message sent successfully.");
    }

    public MessagesResponse getMessages(String roomId, Integer limit) {
        String id = normalize(roomId);
        if (id.isEmpty() || !rooms.exists(id)) {
            throw new RoomNotFoundException(id);
        }
        int n = limit == null ? properties.defaultHistoryLimit() : limit;
        if (n < 1 || n > properties.maxHistoryLimit()) {
            throw new InvalidRequestException(
                    "limit must be between 1 and " + properties.maxHistoryLimit() + ".");
        }
        List<ChatMessage> result = new ArrayList<>();
        for (String raw : messages.lastN(id, n)) {
            result.add(fromJson(raw));
        }
        return new MessagesResponse(result);
    }

    private String toJson(ChatMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize message", e);
        }
    }

    private ChatMessage fromJson(String json) {
        try {
            return objectMapper.readValue(json, ChatMessage.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Corrupt message stored in Redis", e);
        }
    }
}

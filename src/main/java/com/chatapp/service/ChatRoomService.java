package com.chatapp.service;

import static com.chatapp.util.RoomIdNormalizer.normalize;

import com.chatapp.dto.ApiResponse;
import com.chatapp.dto.CreateRoomRequest;
import com.chatapp.dto.CreateRoomResponse;
import com.chatapp.dto.JoinRoomRequest;
import com.chatapp.dto.RoomDetails;
import com.chatapp.dto.RoomSummary;
import com.chatapp.exception.RoomAlreadyExistsException;
import com.chatapp.exception.RoomNotFoundException;
import com.chatapp.realtime.MessageBroadcaster;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ChatRoomService {

    private static final Logger log = LoggerFactory.getLogger(ChatRoomService.class);

    private final ChatRoomRepository rooms;
    private final MessageRepository messages;
    private final MessageBroadcaster broadcaster;

    public ChatRoomService(ChatRoomRepository rooms, MessageRepository messages, MessageBroadcaster broadcaster) {
        this.rooms = rooms;
        this.messages = messages;
        this.broadcaster = broadcaster;
    }

    public CreateRoomResponse createRoom(CreateRoomRequest request) {
        String roomName = request.roomName().trim();
        String roomId = normalize(roomName);
        boolean created = rooms.createIfAbsent(roomId, roomName, Instant.now().truncatedTo(ChronoUnit.SECONDS));
        if (!created) {
            throw new RoomAlreadyExistsException(roomName);
        }
        return new CreateRoomResponse("Chatroom '" + roomName + "' created successfully.", roomId, "success");
    }

    public ApiResponse joinRoom(String roomId, JoinRoomRequest request) {
        String id = requireRoom(roomId);
        String participant = request.participant().trim();
        boolean added = rooms.addParticipant(id, participant);
        String message = added
                ? "User '" + participant + "' joined chatroom '" + id + "'."
                : "User '" + participant + "' is already a member of chatroom '" + id + "'.";
        return ApiResponse.success(message);
    }

    public List<RoomSummary> listRooms() {
        return rooms.findAll();
    }

    public RoomDetails getRoom(String roomId) {
        String id = requireRoom(roomId);
        RoomSummary summary = rooms.findSummary(id).orElseThrow(() -> new RoomNotFoundException(id));
        return new RoomDetails(id, summary.roomName(), summary.createdAt(),
                rooms.participants(id), messages.count(id));
    }

    public ApiResponse deleteRoom(String roomId) {
        String id = normalize(roomId);
        if (!rooms.delete(id)) {
            throw new RoomNotFoundException(id);
        }
        try {
            broadcaster.publishRoomDeleted(id);
        } catch (RuntimeException e) {
            log.warn("Room {} deleted but could not notify subscribers", id, e);
        }
        return ApiResponse.success("Chatroom '" + id + "' deleted successfully.");
    }

    /** Validates the room exists and returns its normalized id. */
    public String requireRoom(String roomId) {
        String id = normalize(roomId);
        if (id.isEmpty() || !rooms.exists(id)) {
            throw new RoomNotFoundException(id);
        }
        return id;
    }
}

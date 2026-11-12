package com.chatapp.controller;

import com.chatapp.dto.ApiResponse;
import com.chatapp.dto.CreateRoomRequest;
import com.chatapp.dto.CreateRoomResponse;
import com.chatapp.dto.JoinRoomRequest;
import com.chatapp.dto.RoomDetails;
import com.chatapp.dto.RoomSummary;
import com.chatapp.service.ChatRoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatapp/chatrooms")
@Tag(name = "Chat Rooms", description = "Create, join, inspect and delete chat rooms")
public class ChatRoomController {

    private final ChatRoomService service;

    public ChatRoomController(ChatRoomService service) {
        this.service = service;
    }

    @Operation(summary = "Create a chat room (unique name)",
            description = "Returns 409 if a room with the same name already exists.")
    @PostMapping
    public ResponseEntity<CreateRoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRoom(request));
    }

    @Operation(summary = "Join an existing chat room", description = "Returns 404 if the room does not exist.")
    @PostMapping("/{roomId}/join")
    public ApiResponse joinRoom(@PathVariable String roomId, @Valid @RequestBody JoinRoomRequest request) {
        return service.joinRoom(roomId, request);
    }

    @Operation(summary = "List all chat rooms")
    @GetMapping
    public List<RoomSummary> listRooms() {
        return service.listRooms();
    }

    @Operation(summary = "Get room details (metadata, participants, message count)")
    @GetMapping("/{roomId}")
    public RoomDetails getRoom(@PathVariable String roomId) {
        return service.getRoom(roomId);
    }

    @Operation(summary = "Delete a chat room (optional)",
            description = "Removes the room's Hash, Set and List keys from Redis and closes live subscriptions.")
    @DeleteMapping("/{roomId}")
    public ApiResponse deleteRoom(@PathVariable String roomId) {
        return service.deleteRoom(roomId);
    }
}

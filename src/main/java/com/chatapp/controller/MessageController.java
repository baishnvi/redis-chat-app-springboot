package com.chatapp.controller;

import com.chatapp.dto.ApiResponse;
import com.chatapp.dto.MessagesResponse;
import com.chatapp.dto.SendMessageRequest;
import com.chatapp.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatapp/chatrooms/{roomId}/messages")
@Tag(name = "Messages", description = "Send messages and read chat history")
public class MessageController {

    private final MessageService service;

    public MessageController(MessageService service) {
        this.service = service;
    }

    @Operation(summary = "Send a message",
            description = "Stores the message in a Redis List and publishes it on the room's Pub/Sub channel. "
                    + "404 if the room does not exist, 403 if the participant has not joined.")
    @PostMapping
    public ResponseEntity<ApiResponse> sendMessage(@PathVariable String roomId,
                                                   @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.sendMessage(roomId, request));
    }

    @Operation(summary = "Retrieve the last N messages (chronological order)")
    @GetMapping
    public MessagesResponse getMessages(
            @PathVariable String roomId,
            @Parameter(description = "Number of latest messages to return (default 10, max 100)")
            @RequestParam(required = false) Integer limit) {
        return service.getMessages(roomId, limit);
    }
}

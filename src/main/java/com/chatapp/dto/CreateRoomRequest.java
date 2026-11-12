package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
        @NotBlank(message = "roomName must not be blank")
        @Size(max = 50, message = "roomName must be at most 50 characters")
        @Pattern(regexp = "^[A-Za-z0-9_-]*$",
                message = "roomName may only contain letters, digits, '_' and '-'")
        String roomName) {
}

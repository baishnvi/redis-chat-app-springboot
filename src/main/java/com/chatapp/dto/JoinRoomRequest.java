package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRoomRequest(
        @NotBlank(message = "participant must not be blank")
        @Size(max = 50, message = "participant must be at most 50 characters")
        String participant) {
}

package com.chatapp.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
        @NotBlank(message = "participant must not be blank")
        @Size(max = 50, message = "participant must be at most 50 characters")
        String participant,
        @NotBlank(message = "message must not be blank")
        @Size(max = 2000, message = "message must be at most 2000 characters")
        String message) {
}

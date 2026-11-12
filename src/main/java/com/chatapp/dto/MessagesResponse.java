package com.chatapp.dto;

import com.chatapp.model.ChatMessage;
import java.util.List;

public record MessagesResponse(List<ChatMessage> messages) {
}

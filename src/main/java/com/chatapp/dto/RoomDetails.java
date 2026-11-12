package com.chatapp.dto;

import java.util.Set;

public record RoomDetails(String roomId, String roomName, String createdAt,
                          Set<String> participants, long messageCount) {
}

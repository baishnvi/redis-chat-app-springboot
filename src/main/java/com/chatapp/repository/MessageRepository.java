package com.chatapp.repository;

import java.util.List;

/** Storage abstraction for chat history (Redis List of JSON strings). */
public interface MessageRepository {

    /** Appends a JSON message to the room's list, trimming old messages beyond maxMessages (0 = unlimited). */
    void append(String roomId, String messageJson, int maxMessages);

    /** Returns the last {@code n} messages in chronological order (oldest first). */
    List<String> lastN(String roomId, int n);

    long count(String roomId);
}

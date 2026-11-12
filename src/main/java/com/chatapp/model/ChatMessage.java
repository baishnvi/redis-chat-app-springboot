package com.chatapp.model;

/** Stored in the Redis List as a JSON string: {"participant","message","timestamp"}. */
public record ChatMessage(String participant, String message, String timestamp) {
}

package com.chatapp.exception;

public class RoomNotFoundException extends RuntimeException {
    public RoomNotFoundException(String roomId) {
        super("Chatroom '" + roomId + "' does not exist.");
    }
}

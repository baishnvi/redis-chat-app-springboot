package com.chatapp.exception;

public class RoomAlreadyExistsException extends RuntimeException {
    public RoomAlreadyExistsException(String roomName) {
        super("Chatroom '" + roomName + "' already exists.");
    }
}

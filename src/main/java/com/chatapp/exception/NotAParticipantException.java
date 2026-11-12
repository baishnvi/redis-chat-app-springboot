package com.chatapp.exception;

public class NotAParticipantException extends RuntimeException {
    public NotAParticipantException(String participant, String roomId) {
        super("User '" + participant + "' has not joined chatroom '" + roomId + "'. Join the room first.");
    }
}

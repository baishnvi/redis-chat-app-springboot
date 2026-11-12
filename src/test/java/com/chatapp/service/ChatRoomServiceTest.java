package com.chatapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chatapp.dto.ApiResponse;
import com.chatapp.dto.CreateRoomRequest;
import com.chatapp.dto.CreateRoomResponse;
import com.chatapp.dto.JoinRoomRequest;
import com.chatapp.exception.RoomAlreadyExistsException;
import com.chatapp.exception.RoomNotFoundException;
import com.chatapp.realtime.MessageBroadcaster;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatRoomServiceTest {

    @Mock ChatRoomRepository rooms;
    @Mock MessageRepository messages;
    @Mock MessageBroadcaster broadcaster;
    @InjectMocks ChatRoomService service;

    @Test
    void createRoom_success() {
        when(rooms.createIfAbsent(eq("general"), eq("general"), any(Instant.class))).thenReturn(true);

        CreateRoomResponse response = service.createRoom(new CreateRoomRequest("general"));

        assertEquals("general", response.roomId());
        assertEquals("success", response.status());
        assertEquals("Chatroom 'general' created successfully.", response.message());
    }

    @Test
    void createRoom_isCaseInsensitiveForIds() {
        when(rooms.createIfAbsent(eq("general"), eq("General"), any(Instant.class))).thenReturn(true);

        assertEquals("general", service.createRoom(new CreateRoomRequest("General")).roomId());
    }

    @Test
    void createRoom_duplicateName_throwsConflict() {
        when(rooms.createIfAbsent(eq("general"), eq("general"), any(Instant.class))).thenReturn(false);

        assertThrows(RoomAlreadyExistsException.class,
                () -> service.createRoom(new CreateRoomRequest("general")));
    }

    @Test
    void joinRoom_success() {
        when(rooms.exists("general")).thenReturn(true);
        when(rooms.addParticipant("general", "guest_user")).thenReturn(true);

        ApiResponse response = service.joinRoom("general", new JoinRoomRequest("guest_user"));

        assertEquals("success", response.status());
        assertEquals("User 'guest_user' joined chatroom 'general'.", response.message());
    }

    @Test
    void joinRoom_alreadyMember_isIdempotent() {
        when(rooms.exists("general")).thenReturn(true);
        when(rooms.addParticipant("general", "guest_user")).thenReturn(false);

        ApiResponse response = service.joinRoom("general", new JoinRoomRequest("guest_user"));

        assertEquals("success", response.status());
        assertTrue(response.message().contains("already a member"));
    }

    @Test
    void joinRoom_nonExistentRoom_throwsNotFound() {
        when(rooms.exists("ghost")).thenReturn(false);

        assertThrows(RoomNotFoundException.class,
                () -> service.joinRoom("ghost", new JoinRoomRequest("guest_user")));
        verify(rooms, never()).addParticipant(any(), any());
    }

    @Test
    void deleteRoom_success_notifiesSubscribers() {
        when(rooms.delete("general")).thenReturn(true);

        ApiResponse response = service.deleteRoom("general");

        assertEquals("success", response.status());
        verify(broadcaster).publishRoomDeleted("general");
    }

    @Test
    void deleteRoom_nonExistent_throwsNotFound() {
        when(rooms.delete("ghost")).thenReturn(false);

        assertThrows(RoomNotFoundException.class, () -> service.deleteRoom("ghost"));
        verify(broadcaster, never()).publishRoomDeleted(any());
    }
}

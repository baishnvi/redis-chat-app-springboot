package com.chatapp.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chatapp.config.ChatProperties;
import com.chatapp.dto.ApiResponse;
import com.chatapp.dto.MessagesResponse;
import com.chatapp.dto.SendMessageRequest;
import com.chatapp.exception.InvalidRequestException;
import com.chatapp.exception.NotAParticipantException;
import com.chatapp.exception.RoomNotFoundException;
import com.chatapp.model.ChatMessage;
import com.chatapp.realtime.MessageBroadcaster;
import com.chatapp.repository.ChatRoomRepository;
import com.chatapp.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MessageServiceTest {

    private ChatRoomRepository rooms;
    private MessageRepository messages;
    private MessageBroadcaster broadcaster;
    private MessageService service;
    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        rooms = mock(ChatRoomRepository.class);
        messages = mock(MessageRepository.class);
        broadcaster = mock(MessageBroadcaster.class);
        ChatProperties props = new ChatProperties(1000, 10, 100, true, 1_800_000L);
        service = new MessageService(rooms, messages, broadcaster, mapper, props);
    }

    @Test
    void sendMessage_storesInListAndPublishesSameJson() throws Exception {
        when(rooms.exists("general")).thenReturn(true);
        when(rooms.isParticipant("general", "guest_user")).thenReturn(true);

        ApiResponse response = service.sendMessage("general", new SendMessageRequest("guest_user", "Hello, everyone!"));

        assertEquals("Message sent successfully.", response.message());
        assertEquals("success", response.status());

        ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> published = ArgumentCaptor.forClass(String.class);
        verify(messages).append(eq("general"), stored.capture(), eq(1000));
        verify(broadcaster).publishMessage(eq("general"), published.capture());

        assertEquals(stored.getValue(), published.getValue());
        ChatMessage saved = mapper.readValue(stored.getValue(), ChatMessage.class);
        assertEquals("guest_user", saved.participant());
        assertEquals("Hello, everyone!", saved.message());
        assertEquals(true, saved.timestamp().endsWith("Z"));
    }

    @Test
    void sendMessage_nonExistentRoom_throwsNotFound() {
        when(rooms.exists("ghost")).thenReturn(false);

        assertThrows(RoomNotFoundException.class,
                () -> service.sendMessage("ghost", new SendMessageRequest("guest_user", "hi")));
        verify(messages, never()).append(anyString(), anyString(), anyInt());
        verify(broadcaster, never()).publishMessage(anyString(), anyString());
    }

    @Test
    void sendMessage_participantNotJoined_throwsForbidden() {
        when(rooms.exists("general")).thenReturn(true);
        when(rooms.isParticipant("general", "stranger")).thenReturn(false);

        assertThrows(NotAParticipantException.class,
                () -> service.sendMessage("general", new SendMessageRequest("stranger", "hi")));
        verify(messages, never()).append(anyString(), anyString(), anyInt());
    }

    @Test
    void sendMessage_membershipEnforcementOff_allowsAnyone() {
        ChatProperties relaxed = new ChatProperties(1000, 10, 100, false, 1_800_000L);
        MessageService relaxedService = new MessageService(rooms, messages, broadcaster, mapper, relaxed);
        when(rooms.exists("general")).thenReturn(true);

        relaxedService.sendMessage("general", new SendMessageRequest("stranger", "hi"));

        verify(messages).append(eq("general"), anyString(), eq(1000));
    }

    @Test
    void sendMessage_publishFailure_doesNotFailRequest() {
        when(rooms.exists("general")).thenReturn(true);
        when(rooms.isParticipant("general", "guest_user")).thenReturn(true);
        doThrow(new IllegalStateException("redis pubsub down")).when(broadcaster).publishMessage(anyString(), any());

        ApiResponse response = service.sendMessage("general", new SendMessageRequest("guest_user", "hi"));

        assertEquals("success", response.status());
        verify(messages).append(eq("general"), anyString(), eq(1000));
    }

    @Test
    void getMessages_returnsChronologicalOrderAsStored() {
        when(rooms.exists("general")).thenReturn(true);
        when(messages.lastN("general", 10)).thenReturn(List.of(
                "{\"participant\":\"guest_user\",\"message\":\"Hello, everyone!\",\"timestamp\":\"2024-01-01T10:00:00Z\"}",
                "{\"participant\":\"another_user\",\"message\":\"Hi, guest_user!\",\"timestamp\":\"2024-01-01T10:01:00Z\"}"));

        MessagesResponse response = service.getMessages("general", null);

        assertEquals(2, response.messages().size());
        assertEquals("guest_user", response.messages().get(0).participant());
        assertEquals("another_user", response.messages().get(1).participant());
    }

    @Test
    void getMessages_usesRequestedLimit() {
        when(rooms.exists("general")).thenReturn(true);
        when(messages.lastN("general", 1)).thenReturn(List.of(
                "{\"participant\":\"a\",\"message\":\"last\",\"timestamp\":\"2024-01-01T10:01:00Z\"}"));

        MessagesResponse response = service.getMessages("general", 1);

        assertEquals(1, response.messages().size());
        assertEquals("last", response.messages().get(0).message());
    }

    @Test
    void getMessages_invalidLimit_throwsBadRequest() {
        when(rooms.exists("general")).thenReturn(true);

        assertThrows(InvalidRequestException.class, () -> service.getMessages("general", 0));
        assertThrows(InvalidRequestException.class, () -> service.getMessages("general", 101));
        assertThrows(InvalidRequestException.class, () -> service.getMessages("general", -5));
    }

    @Test
    void getMessages_nonExistentRoom_throwsNotFound() {
        when(rooms.exists("ghost")).thenReturn(false);

        assertThrows(RoomNotFoundException.class, () -> service.getMessages("ghost", 5));
    }
}

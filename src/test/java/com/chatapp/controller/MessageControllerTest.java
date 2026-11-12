package com.chatapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chatapp.dto.ApiResponse;
import com.chatapp.dto.MessagesResponse;
import com.chatapp.dto.SendMessageRequest;
import com.chatapp.exception.InvalidRequestException;
import com.chatapp.exception.NotAParticipantException;
import com.chatapp.exception.RoomNotFoundException;
import com.chatapp.model.ChatMessage;
import com.chatapp.service.MessageService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired MockMvc mvc;
    @MockBean MessageService service;

    private static final String URL = "/api/chatapp/chatrooms/general/messages";
    private static final String BODY = "{\"participant\":\"guest_user\",\"message\":\"Hello, everyone!\"}";

    @Test
    void sendMessage_returns201() throws Exception {
        when(service.sendMessage(eq("general"), any(SendMessageRequest.class)))
                .thenReturn(ApiResponse.success("Message sent successfully."));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Message sent successfully."))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void sendMessage_nonExistentRoom_returns404() throws Exception {
        when(service.sendMessage(eq("general"), any(SendMessageRequest.class)))
                .thenThrow(new RoomNotFoundException("general"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void sendMessage_notJoined_returns403() throws Exception {
        when(service.sendMessage(eq("general"), any(SendMessageRequest.class)))
                .thenThrow(new NotAParticipantException("guest_user", "general"));

        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    void sendMessage_blankMessage_returns400() throws Exception {
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participant\":\"guest_user\",\"message\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void getMessages_returnsList() throws Exception {
        when(service.getMessages("general", 10)).thenReturn(new MessagesResponse(List.of(
                new ChatMessage("guest_user", "Hello, everyone!", "2024-01-01T10:00:00Z"),
                new ChatMessage("another_user", "Hi, guest_user!", "2024-01-01T10:01:00Z"))));

        mvc.perform(get(URL).param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2))
                .andExpect(jsonPath("$.messages[0].participant").value("guest_user"))
                .andExpect(jsonPath("$.messages[1].message").value("Hi, guest_user!"));
    }

    @Test
    void getMessages_invalidLimit_returns400() throws Exception {
        when(service.getMessages("general", 0)).thenThrow(new InvalidRequestException("limit must be between 1 and 100."));

        mvc.perform(get(URL).param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMessages_nonNumericLimit_returns400() throws Exception {
        mvc.perform(get(URL).param("limit", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }
}

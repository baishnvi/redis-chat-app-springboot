package com.chatapp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chatapp.dto.ApiResponse;
import com.chatapp.dto.CreateRoomRequest;
import com.chatapp.dto.CreateRoomResponse;
import com.chatapp.dto.JoinRoomRequest;
import com.chatapp.exception.RoomAlreadyExistsException;
import com.chatapp.exception.RoomNotFoundException;
import com.chatapp.service.ChatRoomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ChatRoomController.class)
class ChatRoomControllerTest {

    @Autowired MockMvc mvc;
    @MockBean ChatRoomService service;

    private static final String BASE = "/api/chatapp/chatrooms";

    @Test
    void createRoom_returns201WithRoomId() throws Exception {
        when(service.createRoom(any(CreateRoomRequest.class))).thenReturn(
                new CreateRoomResponse("Chatroom 'general' created successfully.", "general", "success"));

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"roomName\":\"general\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value("general"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Chatroom 'general' created successfully."));
    }

    @Test
    void createRoom_duplicate_returns409() throws Exception {
        when(service.createRoom(any(CreateRoomRequest.class))).thenThrow(new RoomAlreadyExistsException("general"));

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"roomName\":\"general\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value("Chatroom 'general' already exists."));
    }

    @Test
    void createRoom_blankName_returns400() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"roomName\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void createRoom_missingBody_returns400() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void joinRoom_returns200() throws Exception {
        when(service.joinRoom(eq("general"), any(JoinRoomRequest.class)))
                .thenReturn(ApiResponse.success("User 'guest_user' joined chatroom 'general'."));

        mvc.perform(post(BASE + "/general/join").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participant\":\"guest_user\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User 'guest_user' joined chatroom 'general'."))
                .andExpect(jsonPath("$.status").value("success"));
    }

    @Test
    void joinRoom_nonExistentRoom_returns404() throws Exception {
        when(service.joinRoom(eq("ghost"), any(JoinRoomRequest.class))).thenThrow(new RoomNotFoundException("ghost"));

        mvc.perform(post(BASE + "/ghost/join").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"participant\":\"guest_user\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void deleteRoom_returns200() throws Exception {
        when(service.deleteRoom("general")).thenReturn(ApiResponse.success("Chatroom 'general' deleted successfully."));

        mvc.perform(delete(BASE + "/general"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
    }
}

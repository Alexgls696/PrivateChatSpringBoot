package com.alexgls.springboot.messagestorageservice.controller;

import com.alexgls.springboot.messagestorageservice.client.AuthWebClient;
import com.alexgls.springboot.messagestorageservice.dto.ChatDto;
import com.alexgls.springboot.messagestorageservice.entity.Message;
import com.alexgls.springboot.messagestorageservice.service.ChatsService;
import com.alexgls.springboot.messagestorageservice.service.ParticipantsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Timestamp;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ChatsControllerTest {

    @Mock
    private ChatsService chatsService;

    @Mock
    private AuthWebClient authWebClient;

    @Mock
    private ParticipantsService participantsService;

    @InjectMocks
    private ChatsController chatsController;

    @Test
    @DisplayName("getChatById должен вернуть ChatDto по переданному id чата")
    void handleGetChatById() {
        //given

        //when

        //then
    }
}
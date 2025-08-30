package com.alexgls.springboot.messagestorageservice.controller;

import com.alexgls.springboot.messagestorageservice.client.AuthWebClient;
import com.alexgls.springboot.messagestorageservice.dto.ChatDto;
import com.alexgls.springboot.messagestorageservice.dto.MessageDto;
import com.alexgls.springboot.messagestorageservice.entity.MessageType;
import com.alexgls.springboot.messagestorageservice.service.ChatsService;
import com.alexgls.springboot.messagestorageservice.service.ParticipantsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.sql.Timestamp;
import java.time.Instant;
import static org.mockito.Mockito.*;


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
        int chatId = 1;
        MessageDto lastMessage = new MessageDto(1L,chatId,1,2,"content",Timestamp.from(Instant.now().minusSeconds(1000)),null,false,null, MessageType.TEXT,null,"test");
        ChatDto chatDto = new ChatDto(chatId,null,false,"PRIVATE",Timestamp.from(Instant.now()),null, lastMessage);
        when(chatsService.findById(chatId)).thenReturn(Mono.just(chatDto));
        //when
        var result = chatsController.getChatById(chatId);
        //then
        StepVerifier
                .create(result)
                .expectNext(chatDto)
                .verifyComplete();
        verify(chatsService).findById(chatId);
    }

    @Test
    @DisplayName("findUserChatsById, когда все входные параметры верны")
    void handleFindUserChatsById() {
        //given

        //when

        //then
    }
}
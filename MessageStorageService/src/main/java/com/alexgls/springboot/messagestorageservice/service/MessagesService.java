package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.dto.CreateMessagePayload;
import com.alexgls.springboot.messagestorageservice.dto.CreatedMessageDto;
import com.alexgls.springboot.messagestorageservice.entity.Chat;
import com.alexgls.springboot.messagestorageservice.entity.Message;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchRecipientException;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchUsersChatException;
import com.alexgls.springboot.messagestorageservice.repository.MessagesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MessagesService {
    private final MessagesRepository messagesRepository;
    private final ChatsService chatsService;

    public Flux<Message> getMessagesByChatId(int chatId, int page, int pageSize) {
        return messagesRepository.findAllMessagesByChatId(chatId, page, pageSize);
    }

    private CreatedMessageDto createMessageDto(Message message) {
        CreatedMessageDto createdMessageDto = new CreatedMessageDto();
        createdMessageDto.setCreatedAt(message.getCreatedAt());
        createdMessageDto.setId(message.getId());
        createdMessageDto.setChatId(message.getChatId());
        createdMessageDto.setSenderId(message.getSenderId());
        createdMessageDto.setRecipientId(message.getRecipientId());
        createdMessageDto.setContent(message.getContent());
        createdMessageDto.setRead(message.isRead());
        createdMessageDto.setReadAt(message.getReadAt());
        return createdMessageDto;
    }


    //ДОДЕЛАТЬ ЭТОТ МЕТОД ДЛЯ РАССЫЛКИ ВСЕМ ПОЛЬЗОВАТЕЛЯМ
    @Transactional
    public Mono<CreatedMessageDto> save(CreateMessagePayload createMessagePayload) {
        return chatsService.existsById(createMessagePayload.chatId())
                .flatMap(isExists -> {
                    if (isExists) {
                        Message message = new Message();
                        message.setCreatedAt(Timestamp.from(Instant.now()));
                        message.setContent(createMessagePayload.content());
                        message.setSenderId(createMessagePayload.senderId());
                        message.setChatId(createMessagePayload.chatId());
                        Mono<Message> createdMessageMono = messagesRepository.save(message);
                        Mono<Integer> recipientMono = chatsService.findRecipientIdByChatId(createMessagePayload.chatId(), createMessagePayload.senderId());
                        return Mono.zip(createdMessageMono, recipientMono)
                                .flatMap(tuple -> {
                                    Message createdMessage = tuple.getT1();
                                    Integer recipientId = tuple.getT2();
                                    createdMessage.setRecipientId(recipientId);
                                    return Mono.just(createMessageDto(createdMessage));
                                }).switchIfEmpty(Mono.defer(() -> Mono.error(new NoSuchRecipientException("Recipients not found"))));
                    }
                    return Mono.error(() -> new NoSuchUsersChatException("Chat with id " + createMessagePayload.chatId() + " not found"));
                });

    }

    private Mono<Boolean> chatIsGroup(int chatId) {
        return chatsService.findById(chatId)
                .map(Chat::isGroup);
    }

    public Mono<Void> deleteById(long id) {
        return messagesRepository.deleteById(id);
    }
}

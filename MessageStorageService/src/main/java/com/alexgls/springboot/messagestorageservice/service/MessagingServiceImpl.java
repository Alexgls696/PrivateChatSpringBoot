package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.dto.CreateMessagePayload;
import com.alexgls.springboot.messagestorageservice.dto.UpdateMessagePayload;
import com.alexgls.springboot.messagestorageservice.entity.Chat;
import com.alexgls.springboot.messagestorageservice.entity.Message;
import com.alexgls.springboot.messagestorageservice.entity.Participants;
import com.alexgls.springboot.messagestorageservice.repository.ChatsRepository;
import com.alexgls.springboot.messagestorageservice.repository.MessagesRepository;
import com.alexgls.springboot.messagestorageservice.repository.ParticipantsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagingServiceImpl implements MessagingService {
    private final MessagesRepository messagesRepository;
    private final ChatsRepository chatsRepository;
    private final ParticipantsRepository participantsRepository;

    @Transactional
    public Mono<Integer> checkChatBeforeCreateMessage(CreateMessagePayload createMessagePayload) {
        if (Objects.isNull(createMessagePayload.chatId())) {
            Chat chat = new Chat();
            chat.setCreatedAt(Timestamp.from(Instant.now()));
            chat.setType("Private");

            return chatsRepository.save(chat)
                    .flatMap(savedChat->createParticipantsForChat(savedChat, createMessagePayload));
        } else {
            return Mono.just(createMessagePayload.chatId());
        }
    }

    private Mono<Integer> createParticipantsForChat(Chat createdChat, CreateMessagePayload createMessagePayload) {
        Participants senderParticipant = new Participants();
        senderParticipant.setChatId(createdChat.getChatId());
        senderParticipant.setUserId(createMessagePayload.senderId());
        senderParticipant.setJoinedAt(Timestamp.from(Instant.now()));

        Participants receiverParticipant = new Participants();
        receiverParticipant.setChatId(createdChat.getChatId());
        receiverParticipant.setUserId(createMessagePayload.recipientId());

        return participantsRepository.save(senderParticipant)
                .then(participantsRepository.save(receiverParticipant))
                .thenReturn(createdChat.getChatId());
    }


    @Override
    public Flux<Message> findAllMessagesByChatId(int chatId) {
        return messagesRepository.findAllByChatId(chatId);
    }

    @Override
    public Mono<Message> save(CreateMessagePayload createMessagePayload) {
        return checkChatBeforeCreateMessage(createMessagePayload)
                .flatMap(chatId -> {
                    Message message = new Message();
                    message.setChatId(chatId);
                    message.setSenderId(createMessagePayload.senderId());
                    message.setContent(createMessagePayload.content());
                    message.setCreatedAt(Timestamp.from(Instant.now()));
                    return messagesRepository.save(message);
                });
    }

    @Override
    public Mono<Message> update(UpdateMessagePayload updateMessagePayload) {
        return messagesRepository.findByChatIdAndId(updateMessagePayload.chatId(), updateMessagePayload.id())
                .flatMap(message -> {
                    message.setContent(updateMessagePayload.content());
                    message.setUpdatedAt(Timestamp.from(Instant.now()));
                    return messagesRepository.save(message);
                });
    }

    @Override
    public Mono<Void> deleteById(long id) {
        return messagesRepository.deleteById(id);
    }

}

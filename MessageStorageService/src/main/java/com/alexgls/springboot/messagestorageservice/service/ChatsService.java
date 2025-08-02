package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.controller.ChatsController;
import com.alexgls.springboot.messagestorageservice.dto.ChatDto;
import com.alexgls.springboot.messagestorageservice.entity.Chat;
import com.alexgls.springboot.messagestorageservice.entity.Message;
import com.alexgls.springboot.messagestorageservice.entity.Participants;
import com.alexgls.springboot.messagestorageservice.repository.ChatsRepository;
import com.alexgls.springboot.messagestorageservice.repository.MessagesRepository;
import com.alexgls.springboot.messagestorageservice.repository.ParticipantsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatsService {

    private final ChatsRepository chatsRepository;
    private final ParticipantsRepository participantsRepository;
    private final MessagesRepository messagesRepository;

    public Flux<ChatDto> findAllChatsByUserId(int userId, Pageable pageable) {
        int limit = pageable.getPageSize();
        long offset = pageable.getOffset();
        return chatsRepository.findChatsByUserId(userId, limit, offset)
                .flatMap(chat -> {
                    log.info("Chat {}", chat);
                    ChatDto chatDto = convertToDto(chat);
                    Mono<Message> lastMessageInChat = messagesRepository.findLastMessageByChatId(chat.getChatId());
                    return Mono.zip(Mono.just(chatDto), lastMessageInChat)
                            .flatMap(tuple -> {
                                ChatDto chatdto = tuple.getT1();
                                Message lastMessage = tuple.getT2();
                                chatdto.setLastMessage(lastMessage);
                                return Mono.just(chatdto);
                            });
                });
    }

    @Transactional
    public Mono<ChatDto> findOrCreatePrivateChat(int senderId, int receiverId) {
        return chatsRepository.findChatIdByParticipantsIdForPrivateChats(senderId, receiverId)
                .flatMap(existingChatId -> {
                    Mono<Chat> chatMono = chatsRepository.findById(existingChatId);
                    return chatMono.map(this::convertToDto);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    Chat newChat = new Chat();
                    newChat.setType("PRIVATE");
                    return chatsRepository.save(newChat)
                            .flatMap(savedChat -> {
                                Participants p1 = new Participants();
                                p1.setUserId(senderId);
                                p1.setChatId(savedChat.getChatId());
                                p1.setJoinedAt(Timestamp.from(Instant.now()));

                                Participants p2 = new Participants();
                                p2.setUserId(receiverId);
                                p2.setChatId(savedChat.getChatId());
                                p2.setJoinedAt(Timestamp.from(Instant.now()));

                                return participantsRepository.saveAll(List.of(p1, p2))
                                        .then(Mono.just(convertToDto(savedChat)));
                            });
                }));
    }

    private ChatDto convertToDto(Chat chat) {
        ChatDto chatDto = new ChatDto();
        chatDto.setName(chat.getName());
        chatDto.setType(chat.getType());
        chatDto.setGroup(chat.isGroup());
        chatDto.setCreatedAt(chat.getCreatedAt());
        chatDto.setUpdatedAt(chat.getUpdatedAt());
        chatDto.setChatId(chat.getChatId());
        return chatDto;
    }

    public Mono<Boolean> existsById(Integer id) {
        return chatsRepository.existsById(id);
    }

    public Mono<Chat> findById(int id) {
        return chatsRepository.findById(id);
    }

    public Mono<Integer> findRecipientIdByChatId(int chatId, int senderId) {
        return chatsRepository.findRecipientIdByChatId(chatId, senderId);
    }

    public Flux<Integer> findRecipientIdsByChatId(int chatId, int senderId) {
        return chatsRepository.findRecipientIdsByChatId(chatId, senderId);
    }

}

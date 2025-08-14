package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.dto.CreateAttachmentPayload;
import com.alexgls.springboot.messagestorageservice.dto.CreateMessagePayload;
import com.alexgls.springboot.messagestorageservice.dto.CreatedMessageDto;
import com.alexgls.springboot.messagestorageservice.dto.ReadMessagePayload;
import com.alexgls.springboot.messagestorageservice.entity.Attachment;
import com.alexgls.springboot.messagestorageservice.entity.Chat;
import com.alexgls.springboot.messagestorageservice.entity.Message;
import com.alexgls.springboot.messagestorageservice.entity.MessageType;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchRecipientException;
import com.alexgls.springboot.messagestorageservice.exceptions.NoSuchUsersChatException;
import com.alexgls.springboot.messagestorageservice.repository.AttachmentRepository;
import com.alexgls.springboot.messagestorageservice.repository.MessagesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagesService {

    private final MessagesRepository messagesRepository;
    private final ChatsService chatsService;
    private final AttachmentRepository attachmentRepository;

    public Flux<Message> getMessagesByChatId(int chatId, int page, int pageSize) {
        return messagesRepository.findAllMessagesByChatId(chatId, page, pageSize);
    }


    public Mono<Long> readMessagesByList(List<ReadMessagePayload> messages) {
        return Flux.fromIterable(messages)
                .flatMap(message -> messagesRepository.readMessagesByList(message.messageId()))
                .count();
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

    @Transactional
    public Mono<CreatedMessageDto> save(CreateMessagePayload createMessagePayload) {
        return chatsService.existsById(createMessagePayload.chatId())
                .flatMap(isExists -> {
                    if (!isExists) {
                        return Mono.error(new NoSuchUsersChatException("Chat with id " + createMessagePayload.chatId() + " not found"));
                    }
                    Message message = new Message();
                    message.setCreatedAt(Timestamp.from(Instant.now()));
                    message.setContent(createMessagePayload.content());
                    message.setType(
                            (createMessagePayload.attachments() == null || createMessagePayload.attachments().isEmpty())
                                    ? MessageType.TEXT
                                    : MessageType.FILE
                    );
                    message.setSenderId(createMessagePayload.senderId());
                    message.setChatId(createMessagePayload.chatId());

                    Mono<Message> savedMessageMono = messagesRepository.save(message);
                    Mono<Integer> recipientIdMono = chatsService.findRecipientIdByChatId(
                            createMessagePayload.chatId(),
                            createMessagePayload.senderId()
                    );

                    return Mono.zip(savedMessageMono, recipientIdMono)
                            .flatMap(tuple -> {
                                Message savedMessage = tuple.getT1();
                                Integer recipientId = tuple.getT2();
                                savedMessage.setRecipientId(recipientId);

                                CreatedMessageDto createdMessageDto = createMessageDto(savedMessage);

                                if (createMessagePayload.attachments() == null || createMessagePayload.attachments().isEmpty()) {
                                    return Mono.just(createdMessageDto);
                                } else {
                                    log.info("Saving attachments to database for messageId: {}", savedMessage.getId());
                                    return saveAttachmentsPayloadsToDatabase(createMessagePayload.attachments(), savedMessage.getId())
                                            .map(savedAttachments -> {
                                                createdMessageDto.setAttachments(savedAttachments);
                                                return createdMessageDto;
                                            });
                                }
                            })
                            .switchIfEmpty(Mono.error(new NoSuchRecipientException("Recipient not found for chat " + createMessagePayload.chatId())));
                });
    }

    private Mono<List<Attachment>> saveAttachmentsPayloadsToDatabase(List<CreateAttachmentPayload> createAttachmentPayloads, long messageId) {
        List<Attachment> attachments = createAttachmentPayloads
                .stream()
                .map(creatingAttachmentPayload -> {
                    Attachment attachment = new Attachment();
                    attachment.setMessageId(messageId);
                    attachment.setUrl(creatingAttachmentPayload.url());
                    attachment.setMimeType(creatingAttachmentPayload.mimeType());
                    return attachment;
                }).toList();
        return attachmentRepository
                .saveAll(attachments)
                .collectList();
    }

    private Mono<Boolean> chatIsGroup(int chatId) {
        return chatsService.findById(chatId)
                .map(Chat::isGroup);
    }

    public Mono<Void> deleteById(long id) {
        return messagesRepository.deleteById(id);
    }
}

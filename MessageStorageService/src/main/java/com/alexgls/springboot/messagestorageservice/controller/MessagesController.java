package com.alexgls.springboot.messagestorageservice.controller;

import com.alexgls.springboot.messagestorageservice.dto.ReadMessagePayload;
import com.alexgls.springboot.messagestorageservice.entity.Message;
import com.alexgls.springboot.messagestorageservice.service.MessagesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.kafka.common.protocol.types.Field;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@Slf4j
public class MessagesController {

    private final MessagesService messagesService;

    private final KafkaTemplate<String, ReadMessagePayload> readMessageTemplate;

    @GetMapping
    public Flux<Message> findMessagesByChatId(
            @RequestParam("chatId") int chatId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int pageSize) {

        log.info("findMessagesByChatId chatId={}, page={}, size={}", chatId, page, pageSize);
        return messagesService.getMessagesByChatId(chatId, page * pageSize, pageSize);
    }

    @PostMapping("/read-messages")
    public Mono<Long> readMessagesList(@RequestBody List<ReadMessagePayload> messages, Authentication authentication) {
        int currentUserId = getCurrentUserId(authentication);
        final List<ReadMessagePayload> filteredMessages = messages.stream().filter(message -> message.senderId() != currentUserId).toList();
        return Mono.just(filteredMessages)
                .flatMap(messagesList -> {
                    log.info("Read messages from payload... {}", messagesList);
                    return messagesService.readMessagesByList(messagesList);
                }).flatMap(count -> {
                    sendMessagesToKafka(filteredMessages, count);
                    return Mono.just(count);
                });
    }

    private void sendMessagesToKafka(List<ReadMessagePayload> messages, long count) {
        log.info("Count of updated messages {}", count);
        messages
                .forEach(message -> {
                    CompletableFuture<SendResult<String, ReadMessagePayload>> futureResult = readMessageTemplate.send("read-message-topic", message).toCompletableFuture();
                    futureResult.whenComplete((result, throwable) -> {
                        if (throwable != null) {
                            log.error("Error when sending via kafka", throwable.getMessage());
                        } else {
                            log.info("Successfully sent via kafka");
                        }
                    });
                });

    }

    private Integer getCurrentUserId(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return Integer.parseInt(jwt.getClaim("userId").toString());
    }

}

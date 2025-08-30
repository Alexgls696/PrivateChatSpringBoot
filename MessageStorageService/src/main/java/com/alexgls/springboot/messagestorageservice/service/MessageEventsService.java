package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.dto.MessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageEventsService {

    private final KafkaTemplate<String, MessageDto> kafkaTemplate;

    public void sendMessage(MessageDto createdMessageDto) {
        log.info("Try to sending message to kafka: {}", createdMessageDto);
        CompletableFuture<SendResult<String, MessageDto>> future = kafkaTemplate
                .send("events-message-created", createdMessageDto).toCompletableFuture();
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error(throwable.getMessage(), throwable);
            }else{
                log.info("Message sent: {}", createdMessageDto);
            }
        });
    }
}

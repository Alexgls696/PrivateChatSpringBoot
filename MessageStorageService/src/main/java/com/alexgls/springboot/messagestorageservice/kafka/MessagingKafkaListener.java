package com.alexgls.springboot.messagestorageservice.kafka;

import com.alexgls.springboot.messagestorageservice.dto.CreateMessagePayload;
import com.alexgls.springboot.messagestorageservice.dto.UpdateMessagePayload;
import com.alexgls.springboot.messagestorageservice.service.MessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class MessagingKafkaListener {

    private final MessagingService messagingService;

    @KafkaListener(topics = "messaging-topic", groupId = "messaging-group", containerFactory = "kafkaCreateMessageListenerContainerFactory")
    public void listen(CreateMessagePayload createMessagePayload) {
        log.info("Message received: {}", createMessagePayload);
        log.info("Starting message saving process in database...");
        messagingService.save(createMessagePayload)
                .doOnNext(savedMessage -> {
                    log.info("Message has been successfully saved: {}", savedMessage);
                })
                .subscribe(
                        result -> log.info("Reactive stream completed successfully for payload: {}", createMessagePayload),
                        error -> log.error("!!! FAILED to process message payload: " + createMessagePayload, error)
                );
    }

    @KafkaListener(topics = "update-messaging-topic", groupId = "messaging-group", containerFactory = "kafkaUpdateMessageListenerContainerFactory")
    public void listen(UpdateMessagePayload updateMessagePayload) {
        log.info("Message received: {}", updateMessagePayload);
        log.info("Saving message in database...");
        messagingService.update(updateMessagePayload)
                .doOnNext(savedMessage -> {
                    log.info("Message updated: {}", savedMessage);
                });
    }

}

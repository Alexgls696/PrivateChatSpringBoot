package ru.alexgls.springboot.usersmessagingservice.service;

import ru.alexgls.springboot.usersmessagingservice.dto.ChatMessage;
import ru.alexgls.springboot.usersmessagingservice.dto.CreateMessagePayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessagingService {
    
    private final KafkaTemplate<String, CreateMessagePayload> createMessageKafkaTemplate;

    public void sendMessage(ChatMessage message, Principal principal) {
        CreateMessagePayload payload = new CreateMessagePayload(Integer.parseInt(message.getChatId()), Integer.parseInt(principal.getName()), message.getContent());
        CompletableFuture<SendResult<String, CreateMessagePayload>> futureResult = createMessageKafkaTemplate.send("messaging-topic", payload).toCompletableFuture();
        futureResult.whenComplete((result, throwable) -> {
            if (throwable == null) {
                log.info("Message sent to kafka: {}", result);
            } else {
                log.error("Error sending message {}", throwable.getMessage());
            }
        });
    }
}

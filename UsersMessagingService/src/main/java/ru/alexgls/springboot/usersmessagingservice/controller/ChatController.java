package ru.alexgls.springboot.usersmessagingservice.controller;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import ru.alexgls.springboot.usersmessagingservice.client.AuthServiceClient;
import ru.alexgls.springboot.usersmessagingservice.dto.CreatedMessageDto;
import ru.alexgls.springboot.usersmessagingservice.messaging.MessagingService;
import ru.alexgls.springboot.usersmessagingservice.messaging.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;


@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final MessagingService messagingService;
    private final SimpMessagingTemplate messagingTemplate;

    private final AuthServiceClient authServiceClient;

    @MessageMapping("/chat.send")
    public void sendChatMessage(@Payload ChatMessage chatMessage, Principal principal) {
        log.info("Try to send message to kafka: {}", chatMessage);
        messagingService.sendMessage(chatMessage, principal);
    }

    @KafkaListener(topics = "events-message-created", groupId = "event-message-group", containerFactory = "kafkaMessageListenerContainerFactory")
    public void listen(CreatedMessageDto createdMessageDto) {
        log.info("Received message, which was saved to database: {}", createdMessageDto);
        log.info("Try to send message to client: {}", createdMessageDto);
        messagingTemplate.convertAndSendToUser(String.valueOf(createdMessageDto.getReceiverId()), "/queue/messages", createdMessageDto);
        messagingTemplate.convertAndSendToUser(String.valueOf(createdMessageDto.getSenderId()), "/queue/messages", createdMessageDto);
    }

}

package ru.alexgls.springboot.usersmessagingservice.controller;

import ru.alexgls.springboot.usersmessagingservice.messaging.MessagingService;
import ru.alexgls.springboot.usersmessagingservice.messaging.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;


@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatController {
    private final MessagingService messagingService;

    @MessageMapping("/chat.send")
    public void sendChatMessage(@Payload ChatMessage chatMessage, Principal principal) {
        messagingService.sendMessage(chatMessage, principal);
    }

}

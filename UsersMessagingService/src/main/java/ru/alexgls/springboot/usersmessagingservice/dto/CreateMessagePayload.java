package ru.alexgls.springboot.usersmessagingservice.dto;

public record CreateMessagePayload(
        Integer chatId,
        int senderId,
        int recipientId,
        String content
) {
}
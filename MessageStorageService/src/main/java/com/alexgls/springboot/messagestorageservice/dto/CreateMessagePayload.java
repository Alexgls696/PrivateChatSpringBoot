package com.alexgls.springboot.messagestorageservice.dto;

public record CreateMessagePayload(
        Integer chatId,
        int senderId,
        int recipientId,
        String content
) {
}

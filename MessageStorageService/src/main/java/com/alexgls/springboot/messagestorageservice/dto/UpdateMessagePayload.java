package com.alexgls.springboot.messagestorageservice.dto;

public record UpdateMessagePayload(
        long id,
        int chatId,
        String content
) {
}

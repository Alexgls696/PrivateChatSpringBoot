package com.alexgls.springboot.messagestorageservice.dto;

public record ReadMessagePayload(
        int messageId,
        int senderId,
        int chatId
) {

}

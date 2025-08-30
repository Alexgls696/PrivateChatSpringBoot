package com.alexgls.springboot.messagestorageservice.mapper;

import com.alexgls.springboot.messagestorageservice.dto.CreateMessagePayload;
import com.alexgls.springboot.messagestorageservice.dto.MessageDto;
import com.alexgls.springboot.messagestorageservice.entity.Message;

public class MessageMapper {

    public static MessageDto toMessageDto(Message message) {
        MessageDto messageDto = new MessageDto();
        messageDto.setId(message.getId());
        messageDto.setType(message.getType());
        messageDto.setCreatedAt(message.getCreatedAt());
        messageDto.setUpdatedAt(message.getUpdatedAt());
        messageDto.setAttachments(message.getAttachments());
        messageDto.setContent(message.getContent());
        messageDto.setRead(message.isRead());
        messageDto.setSenderId(message.getSenderId());
        messageDto.setRecipientId(message.getRecipientId());
        messageDto.setReadAt(message.getReadAt());
        messageDto.setChatId(message.getChatId());
        return messageDto;
    }

    public static Message toMessageFromCreateMessagePayload(CreateMessagePayload createMessagePayload) {
        Message message = new Message();
        message.setChatId(createMessagePayload.chatId());
        message.setSenderId(createMessagePayload.senderId());
        message.setContent(createMessagePayload.content());
        return message;
    }
}

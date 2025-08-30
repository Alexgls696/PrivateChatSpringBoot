package com.alexgls.springboot.messagestorageservice.mapper;

import com.alexgls.springboot.messagestorageservice.dto.ChatDto;
import com.alexgls.springboot.messagestorageservice.entity.Chat;

public class ChatMapper {

    public static ChatDto toDto(Chat chat) {
        ChatDto chatDto = new ChatDto();
        chatDto.setName(chat.getName());
        chatDto.setType(chat.getType());
        chatDto.setGroup(chat.isGroup());
        chatDto.setCreatedAt(chat.getCreatedAt());
        chatDto.setUpdatedAt(chat.getUpdatedAt());
        chatDto.setChatId(chat.getChatId());
        return chatDto;
    }

    public static Chat toEntity(ChatDto chatDto) {
        Chat chat = new Chat();
        chat.setName(chatDto.getName());
        chat.setType(chatDto.getType());
        chat.setGroup(chatDto.isGroup());
        chat.setCreatedAt(chatDto.getCreatedAt());
        chat.setUpdatedAt(chatDto.getUpdatedAt());
        chat.setChatId(chatDto.getChatId());
        return chat;
    }
}

package com.alexgls.springboot.messagestorageservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ChatListElementDto {
    private int chatId;
    private String chatName;
    private String lastMessage;
    private int lastMessageSenderId;
}

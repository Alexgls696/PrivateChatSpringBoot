package ru.alexgls.springboot.usersmessagingservice.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ChatMessage {
    private String chatId;
    private String content;
}

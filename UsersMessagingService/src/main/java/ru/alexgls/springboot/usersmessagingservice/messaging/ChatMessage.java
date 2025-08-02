package ru.alexgls.springboot.usersmessagingservice.messaging;

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

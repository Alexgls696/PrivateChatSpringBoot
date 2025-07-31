package ru.alexgls.springboot.usersmessagingservice.messaging;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ChatMessage {
    private String toUserId;
    private String content;
    private long timestamp;
}

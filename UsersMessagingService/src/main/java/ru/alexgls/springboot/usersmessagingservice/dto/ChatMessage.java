package ru.alexgls.springboot.usersmessagingservice.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ChatMessage {
    private String chatId;
    private String content;
    private List<CreateAttachmentPayload> attachments;
}

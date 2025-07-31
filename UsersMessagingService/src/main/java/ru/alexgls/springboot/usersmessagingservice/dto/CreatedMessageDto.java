package ru.alexgls.springboot.usersmessagingservice.dto;

import lombok.*;

import java.sql.Timestamp;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreatedMessageDto {
    private long id;

    private int chatId;

    private int senderId;

    private int receiverId;

    private String content;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private boolean isRead;

    private Timestamp readAt;
}
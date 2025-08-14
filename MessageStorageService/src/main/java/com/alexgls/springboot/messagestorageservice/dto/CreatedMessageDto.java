package com.alexgls.springboot.messagestorageservice.dto;

import com.alexgls.springboot.messagestorageservice.entity.Attachment;
import lombok.*;

import java.sql.Timestamp;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreatedMessageDto {
    private long id;

    private int chatId;

    private int senderId;

    private int recipientId;

    private String content;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private boolean isRead;

    private Timestamp readAt;

    private List<Attachment> attachments;

}
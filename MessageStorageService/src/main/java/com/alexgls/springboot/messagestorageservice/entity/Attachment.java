package com.alexgls.springboot.messagestorageservice.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "attachments")
@Getter
@Setter
@NoArgsConstructor
public class Attachment {

    @Id
    @Column(value = "attachment_id")
    private Long id;

    @Column(value = "message_id")
    private Long messageId;

    @Column(value = "file_id")
    private Long fileId;

    @Column(value = "mime_type")
    private String mimeType;
}

package com.alexgls.springboot.messagestorageservice.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Embedded;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@Table(value = "participants")
@Getter
@Setter
@NoArgsConstructor
@ToString
public class Participants {

    @Id
    private int id;

    private int chatId;

    private int userId;

    @Column(value = "joined_at")
    private Timestamp joinedAt;


}

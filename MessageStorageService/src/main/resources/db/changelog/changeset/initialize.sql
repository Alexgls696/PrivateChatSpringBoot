--liquibase formatted sql
--changeset alexgls:initialize
--comment Создание схемы базы данных

create table chats
(
    chat_id    integer primary key generated always as identity,
    name       varchar(256),
    is_group   boolean default false,
    type       varchar(32),
    created_at timestamp,
    updated_at timestamp
);

create table participants
(
    id integer primary key generated always as identity,
    user_id   integer,
    chat_id   integer references chats (chat_id),
    joined_at timestamp
);

create table messages
(
    message_id bigint primary key generated always as identity,
    chat_id    integer references chats (chat_id),
    sender_id  integer,
    content    text,
    created_at timestamp,
    updated_at timestamp,
    is_read    boolean,
    read_at    timestamp
);


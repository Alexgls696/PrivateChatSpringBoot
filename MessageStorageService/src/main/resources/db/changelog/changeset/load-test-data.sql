--liquibase formatted sql
--changeset alexgls:load_test_data
--comment Загрузка тестовых данных
--liquibase formatted sql
--changeset alexgls:load_clean_test_data
--comment Загрузка очищенных тестовых данных для пользователей 1, 2, 3

-- Чат 1: Приватный чат между пользователями 1 и 2
INSERT INTO chats (name, is_group, type, created_at, updated_at)
VALUES (NULL, false, 'private', '2023-01-01 10:00:00', '2023-01-05 15:30:00');

INSERT INTO participants (user_id, chat_id, joined_at)
VALUES
    (1, 1, '2023-01-01 10:00:00'),
    (2, 1, '2023-01-01 10:00:00');

INSERT INTO messages (chat_id, sender_id, content, created_at, updated_at, is_read, read_at)
VALUES
    (1, 1, 'Привет! Как дела?', '2023-01-01 10:05:00', '2023-01-01 10:05:00', true, '2023-01-01 10:10:00'),
    (1, 2, 'Привет! Все отлично, спасибо!', '2023-01-01 10:12:00', '2023-01-01 10:12:00', true, '2023-01-01 10:15:00'),
    (1, 1, 'Что планируешь на выходные?', '2023-01-03 14:20:00', '2023-01-03 14:20:00', true, '2023-01-03 14:25:00'),
    (1, 2, 'Пока не решил, может встретимся?', '2023-01-03 14:30:00', '2023-01-03 14:30:00', true, '2023-01-03 14:35:00');

UPDATE chats SET last_message_id = 4 WHERE chat_id = 1;


-- Чат 2: Приватный чат между пользователями 1 и 3
INSERT INTO chats (name, is_group, type, created_at, updated_at)
VALUES (NULL, false, 'private', '2023-01-02 11:00:00', '2023-01-10 09:15:00');

INSERT INTO participants (user_id, chat_id, joined_at)
VALUES
    (1, 2, '2023-01-02 11:00:00'),
    (3, 2, '2023-01-02 11:00:00');

INSERT INTO messages (chat_id, sender_id, content, created_at, updated_at, is_read, read_at)
VALUES
    (2, 1, 'Здравствуй!', '2023-01-02 11:05:00', '2023-01-02 11:05:00', true, '2023-01-02 11:10:00'),
    (2, 3, 'Привет! Как жизнь?', '2023-01-02 11:15:00', '2023-01-02 11:15:00', true, '2023-01-02 11:20:00'),
    (2, 1, 'Все хорошо, работаю над проектом', '2023-01-05 16:45:00', '2023-01-05 16:45:00', true, '2023-01-05 16:50:00'),
    (2, 3, 'Понятно, удачи!', '2023-01-05 16:55:00', '2023-01-05 16:55:00', true, '2023-01-05 17:00:00');

UPDATE chats SET last_message_id = 8 WHERE chat_id = 2;


-- Чат 3: Приватный чат между пользователями 2 и 3
INSERT INTO chats (name, is_group, type, created_at, updated_at)
VALUES (NULL, false, 'private', '2023-01-03 12:00:00', '2023-01-15 18:20:00');

INSERT INTO participants (user_id, chat_id, joined_at)
VALUES
    (2, 3, '2023-01-03 12:00:00'),
    (3, 3, '2023-01-03 12:00:00');

INSERT INTO messages (chat_id, sender_id, content, created_at, updated_at, is_read, read_at)
VALUES
    (3, 2, 'Привет, коллега!', '2023-01-03 12:05:00', '2023-01-03 12:05:00', true, '2023-01-03 12:10:00'),
    (3, 3, 'Здорово! Готов к совещанию?', '2023-01-03 12:15:00', '2023-01-03 12:15:00', true, '2023-01-03 12:20:00'),
    (3, 2, 'Да, уже все подготовил', '2023-01-03 12:25:00', '2023-01-03 12:25:00', true, '2023-01-03 12:30:00'),
    (3, 3, 'Отлично, тогда до встречи!', '2023-01-03 12:35:00', '2023-01-03 12:35:00', true, '2023-01-03 12:40:00');

UPDATE chats SET last_message_id = 12 WHERE chat_id = 3;


-- Чат 4: Групповой чат с пользователями 1, 2, 3
INSERT INTO chats (name, is_group, type, created_at, updated_at)
VALUES ('Тестовая группа', true, 'group', '2023-01-10 09:00:00', '2023-01-20 17:30:00');

INSERT INTO participants (user_id, chat_id, joined_at)
VALUES
    (1, 4, '2023-01-10 09:00:00'),
    (2, 4, '2023-01-10 09:00:00'),
    (3, 4, '2023-01-10 09:00:00');

INSERT INTO messages (chat_id, sender_id, content, created_at, updated_at, is_read, read_at)
VALUES
    (4, 1, 'Всем привет! Это наш рабочий чат', '2023-01-10 09:05:00', '2023-01-10 09:05:00', true, '2023-01-10 09:10:00'),
    (4, 2, 'Отлично, будем здесь обсуждать проекты', '2023-01-10 09:15:00', '2023-01-10 09:15:00', true, '2023-01-10 09:20:00'),
    (4, 3, 'Какие ближайшие задачи?', '2023-01-10 09:25:00', '2023-01-10 09:25:00', true, '2023-01-10 09:30:00'),
    (4, 1, 'Нужно подготовить отчет к пятнице', '2023-01-10 09:35:00', '2023-01-10 09:35:00', true, '2023-01-10 09:40:00');

UPDATE chats SET last_message_id = 16 WHERE chat_id = 4;
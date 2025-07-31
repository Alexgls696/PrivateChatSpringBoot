package com.alexgls.springboot.messagestorageservice.repository;

import com.alexgls.springboot.messagestorageservice.entity.Chat;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ChatsRepository extends ReactiveCrudRepository<Chat, Integer> {

    @Query(value = "select c.* from chats c " +
            "join participants p on p.chat_id = c.chat_id where user_id = :userId")
    Flux<Chat> findChatsByUserId(@Param("userId") int userId);
}

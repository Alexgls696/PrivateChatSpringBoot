package com.alexgls.springboot.messagestorageservice.repository;

import com.alexgls.springboot.messagestorageservice.entity.Message;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface

MessagesRepository extends ReactiveCrudRepository<Message, Long> {
    Flux<Message> findAllByChatId(int chatId);

    Mono<Message> findByChatIdAndId(int chatId, long id);

    @Query(value = "select m.* from messages m where chat_id = :chatId order by created_at desc limit 1")
    Mono<Message> findLastMessageByChatId(int chatId);

    @Query("select * from messages m where chat_id = :chatId order by created_at desc limit :size offset :page")
    Flux<Message> findAllMessagesByChatId(@Param("chatId") int chatId,
                                          @Param("page") int page,
                                          @Param("size") int size);
}

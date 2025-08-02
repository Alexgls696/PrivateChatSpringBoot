package com.alexgls.springboot.messagestorageservice.repository;

import com.alexgls.springboot.messagestorageservice.entity.Participants;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface ParticipantsRepository extends ReactiveCrudRepository<Participants, Integer> {
    @Query(value = "select user_id from participants where chat_id = :chatId")
    Flux<Integer>findUserIdsByChatId(@Param("chatId") Integer chatId);
}

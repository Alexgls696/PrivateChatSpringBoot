package com.alexgls.springboot.messagestorageservice.repository;

import com.alexgls.springboot.messagestorageservice.entity.Message;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface MessagesRepository extends ReactiveCrudRepository<Message, Long> {
    Flux<Message> findAllByChatId(int chatId);

    Mono<Message> findByChatIdAndId(int chatId, long id);
}

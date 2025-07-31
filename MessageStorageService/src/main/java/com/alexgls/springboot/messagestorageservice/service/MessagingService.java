package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.dto.CreateMessagePayload;
import com.alexgls.springboot.messagestorageservice.dto.UpdateMessagePayload;
import com.alexgls.springboot.messagestorageservice.entity.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface MessagingService {


    Flux<Message> findAllMessagesByChatId(int chatId);

    Mono<Message> save(CreateMessagePayload createMessagePayload);

    Mono<Message> update(UpdateMessagePayload updateMessagePayload);

    Mono<Void> deleteById(long id);
}

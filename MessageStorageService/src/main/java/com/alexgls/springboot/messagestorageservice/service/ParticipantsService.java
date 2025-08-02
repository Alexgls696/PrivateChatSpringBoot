package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.repository.ParticipantsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ParticipantsService {
    private final ParticipantsRepository participantsRepository;

    public Flux<Integer>findUserIdsByChatId(int chatId) {
        return participantsRepository.findUserIdsByChatId(chatId);
    }

}

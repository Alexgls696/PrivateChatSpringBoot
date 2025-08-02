package com.alexgls.springboot.messagestorageservice.client;

import com.alexgls.springboot.messagestorageservice.dto.GetUserDto;
import reactor.core.publisher.Mono;

public interface AuthWebClient {
    Mono<GetUserDto> findUserById(int id, String token);
}

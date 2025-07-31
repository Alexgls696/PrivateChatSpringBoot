package ru.alexgls.springboot.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.alexgls.springboot.entity.RefreshToken;

@Repository
public interface RefreshTokenRepository extends ReactiveCrudRepository<RefreshToken, Long> {

    Mono<RefreshToken>findByToken(String token);
    Mono<Void>deleteByUserId(Integer userId);
    Mono<Void>deleteByToken(String token);
}

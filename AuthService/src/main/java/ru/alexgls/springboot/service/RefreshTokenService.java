package ru.alexgls.springboot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.alexgls.springboot.entity.RefreshToken;
import ru.alexgls.springboot.repository.RefreshTokenRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {


    @Value("${jwt.refresh.expirationMs}")
    private Long refreshExpirationMs;


    private final RefreshTokenRepository refreshTokenRepository;

    public Mono<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    public Mono<Void> deleteTokenByUserId(int id) {
        return refreshTokenRepository.deleteByUserId(id);
    }

    public Mono<Void> deleteByToken(String token) {
        return refreshTokenRepository.deleteByToken(token);
    }

    public Mono<RefreshToken> createRefreshToken(Integer userId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setExpiresDate(Instant.now().plusMillis(refreshExpirationMs));
        refreshToken.setToken(UUID.randomUUID().toString());
        return refreshTokenRepository.save(refreshToken);
    }

    public Mono<RefreshToken> verifyExpiration(RefreshToken refreshToken) {
        if (refreshToken.getExpiresDate()
                .compareTo(Instant.now()) < 0) {
            refreshTokenRepository
                    .delete(refreshToken)
                    .then(Mono.error(new RuntimeException("\"Refresh token was expired." +
                            "Please make a new signin request.\"")));
        }
        return Mono.just(refreshToken);
    }
}

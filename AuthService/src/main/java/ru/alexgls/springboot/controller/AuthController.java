package ru.alexgls.springboot.controller;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.alexgls.springboot.config.JwtUtil;
import ru.alexgls.springboot.dto.JwtValidationRequest;
import ru.alexgls.springboot.dto.JwtValidationResponse;
import ru.alexgls.springboot.dto.RefreshTokenRequest;
import ru.alexgls.springboot.dto.UserRegisterDto;
import ru.alexgls.springboot.entity.RefreshToken;
import ru.alexgls.springboot.entity.User;
import ru.alexgls.springboot.service.RefreshTokenService;
import ru.alexgls.springboot.service.UsersService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    private final UsersService usersService;

    private final RefreshTokenService refreshTokenService;

    public record LoginRequest(String username, String password) {
    }


    @Getter
    @Setter
    @AllArgsConstructor
    public class JwtResponse {
        private String accessToken;
        private String refreshToken;
    }


    @PostMapping("/login")
    public Mono<ResponseEntity<?>> login(@RequestBody LoginRequest loginRequest) {
        log.info("Login request: username={}", loginRequest.username);
        Mono<Boolean> checkCredentialsResult = usersService.checkCredentials(loginRequest.username, loginRequest.password);
        Mono<User> userMono = usersService.getUser(loginRequest.username);
        Mono<List<String>> monoRoles = userMono
                .flatMap(user -> usersService
                        .getUserRoles(user.getId())
                        .collect(Collectors.toList()));
        return Mono.zip(checkCredentialsResult, userMono, monoRoles)
                .flatMap(tuple -> {
                    boolean result = tuple.getT1();
                    User user = tuple.getT2();
                    List<String> roles = tuple.getT3();
                    if (result) {
                        String accessToken = jwtUtil.generateToken(loginRequest.username, user.getId(), roles);
                        return refreshTokenService.createRefreshToken(user.getId())
                                .map(refreshToken -> {
                                    JwtResponse jwtResponse = new JwtResponse(accessToken, refreshToken.getToken());
                                    return ResponseEntity.ok(jwtResponse);
                                });
                    } else {
                        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(Map.of("error", "Invalid username or password")));
                    }
                });
    }

    @Transactional
    @PostMapping("/refresh")
    public Mono<ResponseEntity<?>> refreshToken(@RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();
        return refreshTokenService.findByToken(requestRefreshToken)
                .switchIfEmpty(Mono.error(new RuntimeException("Refresh token не найден в базе данных.")))
                .flatMap(refreshTokenService::verifyExpiration) // verifiedToken - это старый токен
                .flatMap(verifiedToken -> {
                    Mono<RefreshToken> newRefreshTokenMono = refreshTokenService.createRefreshToken(verifiedToken.getUserId());

                    Mono<Void> deleteOldTokenMono = refreshTokenService.deleteByToken(verifiedToken.getToken());
                    return deleteOldTokenMono.then(newRefreshTokenMono);
                })
                .flatMap(newRefreshToken -> usersService.findUserById(newRefreshToken.getUserId())
                        .flatMap(user -> usersService.getUserRoles(user.getId()).collectList()
                                .map(roles -> {
                                    String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getId(), roles);
                                    return ResponseEntity.ok(new JwtResponse(newAccessToken, newRefreshToken.getToken()));
                                })
                        )
                );
    }

    @PostMapping("/register")
    public Mono<ResponseEntity<Void>> register(@RequestBody UserRegisterDto userRegisterDto) {
        log.info("Register user: username={} email={}", userRegisterDto.username(), userRegisterDto.email());
        return usersService.saveUser(userRegisterDto)
                .thenReturn(ResponseEntity.ok().build());
    }

    @PostMapping("/validate")
    public ResponseEntity<JwtValidationResponse> validateJwtToken(@RequestBody JwtValidationRequest tokenRequest) {
        log.info("Try to validate token: {}", tokenRequest);
        return ResponseEntity
                .ok(jwtUtil.validateTokenAndGetJwtValidationResponse(tokenRequest.getToken()));
    }

}

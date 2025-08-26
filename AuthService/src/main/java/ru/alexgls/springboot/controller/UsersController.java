package ru.alexgls.springboot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.alexgls.springboot.dto.GetUserDto;
import ru.alexgls.springboot.exceptions.InvalidJwtException;
import ru.alexgls.springboot.exceptions.NoSuchAuthException;
import ru.alexgls.springboot.service.UsersService;

import java.util.Objects;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
@Slf4j
public class UsersController {

    private final UsersService usersService;

    @GetMapping("initials/{id}")
    public Mono<String> findUserInitialsById(@PathVariable("id") int id) {
        log.info("Find user initials by id {}", id);
        return usersService.findUserInitialsById(id);
    }

    @GetMapping
    public Flux<GetUserDto> findUsers() {
        log.info("Find users");
        return usersService.findAllUsers();
    }

    @GetMapping("/{id}")
    public Mono<GetUserDto> findUserById(@PathVariable("id") int id) {
        log.info("Find user by id {}", id);
        return usersService.findUserDtoById(id);
    }

    @GetMapping("/me")
    public Mono<GetUserDto> getCurrentUser(Authentication authentication) {
        log.info("Try to get current user");
        if (Objects.isNull(authentication) || Objects.isNull(authentication.getPrincipal())) {
            return Mono.error(() -> new NoSuchAuthException("User is not authenticated"));
        }
        Jwt jwt = (Jwt) authentication.getPrincipal();
        if (jwt.getClaims().containsKey("userId")) {
            int userId = Integer.parseInt(jwt.getClaim("userId").toString());
            log.info("Get current user with id {}", userId);
            return usersService.findUserDtoById(userId);
        } else {
            return Mono.error(() -> new InvalidJwtException("Jwt is invalid"));
        }
    }

}

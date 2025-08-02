package ru.alexgls.springboot.controller;

import lombok.Getter;
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
import ru.alexgls.springboot.service.UsersService;

@RestController
@RequestMapping("api/users")
@RequiredArgsConstructor
@Slf4j
public class UsersController {

    private final UsersService usersService;

    @GetMapping("initials/{id}")
    public Mono<String> findUserInitialsById(@PathVariable("id") int id, Authentication authentication) {
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
        Jwt jwt = (Jwt) authentication.getPrincipal();
        Integer userId = Integer.parseInt(jwt.getClaim("userId").toString());
        log.info("Get current user");
        return usersService.findUserDtoById(userId);
    }

}

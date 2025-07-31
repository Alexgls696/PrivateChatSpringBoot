package ru.alexgls.springboot.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.alexgls.springboot.service.UsersService;

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
}

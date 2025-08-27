package ru.alexgls.springboot.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.alexgls.springboot.dto.GetUserDto;
import ru.alexgls.springboot.exceptions.InvalidJwtException;
import ru.alexgls.springboot.exceptions.NoSuchAuthException;
import ru.alexgls.springboot.service.UsersService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersControllerTest {

    @Mock
    private UsersService usersService;

    @InjectMocks
    private UsersController usersController;

    @Test
    void handleFindUsers_ReturnsGetUserDtoList() {
        //given
        List<GetUserDto> getUsersDtoList = List.of(new GetUserDto(1, "Александр", "Глущенко", "alexgls"),
                new GetUserDto(2, "Марина", "Низовская", "marina"),
                new GetUserDto(3, "Сергей", "Глущенко", "serioga"));

        Flux<GetUserDto> getUserDtoFlux = Flux.fromIterable(getUsersDtoList);
        when(usersService.findAllUsers()).thenReturn(getUserDtoFlux);

        //when
        Flux<GetUserDto> result = usersController.findUsers();

        //then
        StepVerifier.create(result)
                .expectNextSequence(getUsersDtoList)
                .verifyComplete();
    }

    @Test
    void handleFindUserInitialsById_ReturnsInitialsString() {
        //given
        int id = 1;
        String initials = "Александр Глущенко";
        when(usersService.findUserInitialsById(id)).thenReturn(Mono.just(initials));
        //when
        var result = usersController.findUserInitialsById(id);
        //then
        StepVerifier.create(result)
                .expectNext(initials)
                .verifyComplete();
    }

    @Test
    void handleFindUserById_ReturnsMonoGetUserDto() {
        //given
        int id = 1;
        GetUserDto getUserDto = new GetUserDto(id, "Александр", "Глущенко", "alexgls");
        when(usersService.findUserDtoById(id)).thenReturn(Mono.just(getUserDto));
        //when
        var result = usersController.findUserById(id);
        //then
        StepVerifier.create(result)
                .expectNext(getUserDto)
                .verifyComplete();
        verify(usersService).findUserDtoById(id);
    }

    @Test
    @DisplayName("getCurrentUser должен вернуть конкретного пользователя, когда верный jwt")
    void getCurrentUser_WithValidJwt_ReturnsUserDto() {
        //given
        int id = 1;
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaims()).thenReturn(Map.of("userId",id));
        when(jwt.getClaim("userId")).thenReturn(id);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        GetUserDto getUserDto = new GetUserDto(id, "Александр", "Глущенко", "alexgls");
        when(usersService.findUserDtoById(id)).thenReturn(Mono.just(getUserDto));
        //when
        var result = usersController.getCurrentUser(authentication);
        //then
        StepVerifier.create(result)
                .expectNext(getUserDto)
                .verifyComplete();
        verify(usersService).findUserDtoById(id);
        verify(jwt).getClaim("userId");
    }

    @Test
    @DisplayName("getCurrentUser должен вернуть ошибку NoSuchAuthException, когда Authentication неверный")
    void getCurrentUser_WhenAuthenticationIsInvalid_ReturnsMonoErrorWithNoSuchAuthException() {
        //given
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(null);
        //when
        var result = usersController.getCurrentUser(authentication);
        //then
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof NoSuchAuthException && e.getMessage().equals("User is not authenticated"))
                .verify();
        verify(authentication).getPrincipal();
    }

    @Test
    @DisplayName("getCurrentUser должен вернуть ошибку NoSuchAuthException, когда Authentication отсутсвует")
    void getCurrentUser_WhenAuthenticationIsNull_ReturnsMonoErrorWithNoSuchAuthException() {
        //when
        var result = usersController.getCurrentUser(null);
        //then
        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof NoSuchAuthException && e.getMessage().equals("User is not authenticated"))
                .verify();
    }

    @Test
    @DisplayName("getCurrentUser должен вернуть ошибку InvalidJwtClaimException, когда jwt некорректный")
    void getCurrentUser_WhenInvalidJwt_ReturnsMonoErrorWithInvalidJwt() {
        //given
        Jwt jwt = mock(Jwt.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(jwt);
        //when
        StepVerifier.create(usersController.getCurrentUser(authentication))
                //then
                .expectErrorMatches(e -> e instanceof InvalidJwtException && e.getMessage().equals("Jwt is invalid"))
                .verify();
    }

}
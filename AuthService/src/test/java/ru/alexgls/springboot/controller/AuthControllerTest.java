package ru.alexgls.springboot.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.alexgls.springboot.config.JwtUtil;
import ru.alexgls.springboot.dto.GetUserDto;
import ru.alexgls.springboot.dto.JwtValidationRequest;
import ru.alexgls.springboot.dto.RefreshTokenRequest;
import ru.alexgls.springboot.dto.UserRegisterDto;
import ru.alexgls.springboot.entity.RefreshToken;
import ru.alexgls.springboot.entity.User;
import ru.alexgls.springboot.exceptions.RefreshTokenNotFoundException;
import ru.alexgls.springboot.service.RefreshTokenService;
import ru.alexgls.springboot.service.UsersService;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UsersService usersService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("login, когда данные пользователя верные, возвращает jwt и resfesh токены")
    public void handleLoginRequestWhenUserCredentialsIsCurrent_ReturnsMonoJwtResponse() {
        //given
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest("alexgls", "12345678");
        User user = new User(1, "Александр", "Глущенко", "alexgls", "encodedPassword", "glualex066@gmail.com");

        Mono<User> existingUserMono = Mono.just(user);
        List<String> roles = List.of("ROLE_USER");
        Flux<String> userRolesFlux = Flux.fromIterable(roles);

        when(usersService.getUserByUsername(loginRequest.username())).thenReturn(existingUserMono);
        when(usersService.checkCredentials(loginRequest.username(), loginRequest.password())).thenReturn(Mono.just(true));
        when(usersService.getUserRoles(user.getId())).thenReturn(userRolesFlux);
        when(jwtUtil.generateToken(user.getUsername(), user.getId(), roles)).thenReturn("jwt_access_token");
        when(refreshTokenService.createRefreshToken(user.getId())).thenReturn(Mono.just(new RefreshToken(1L, user.getId(), "refresh_token", Instant.now())));
        //when
        var result = authController.login(loginRequest);

        //then

        var response = new AuthController.JwtResponse("jwt_access_token", "refresh_token");
        StepVerifier
                .create(result)
                .expectNext(ResponseEntity
                        .ok(response))
                .verifyComplete();

        verify(usersService).getUserByUsername(loginRequest.username());
        verify(usersService).checkCredentials(loginRequest.username(), loginRequest.password());
        verify(usersService).getUserRoles(user.getId());
        verify(jwtUtil).generateToken(user.getUsername(), user.getId(), roles);
        verify(refreshTokenService).createRefreshToken(user.getId());
    }

    @Test
    @DisplayName("login, когда данные пользователя неверные, возвращает 401 и ошибку")
    public void handleLoginRequestWhenUserCredentialsIsInvalid_ReturnsMonoResponseEntityWithErrorDescription() {
        //given
        AuthController.LoginRequest loginRequest = new AuthController.LoginRequest("alexgls", "12345678");
        User user = new User(1, "Александр", "Глущенко", "alexgls", "encodedPassword", "glualex066@gmail.com");

        Mono<User> existingUserMono = Mono.just(user);
        List<String> roles = List.of("ROLE_USER");
        Flux<String> userRolesFlux = Flux.fromIterable(roles);

        when(usersService.checkCredentials(loginRequest.username(), loginRequest.password())).thenReturn(Mono.just(false));
        //when

        var result = authController.login(loginRequest);
        //then
        var responseEntityResult = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Неверное имя пользователя или пароль"));
        StepVerifier
                .create(result)
                .expectNext(responseEntityResult)
                .verifyComplete();
        verify(usersService).checkCredentials(loginRequest.username(), loginRequest.password());
    }

    @Test
    void handleRefreshTokenRequest_WhenRefreshTokenIsCorrect() {
        //given
        RefreshTokenRequest request = new RefreshTokenRequest("refresh_token");
        RefreshToken refreshToken = new RefreshToken(1L, 1, "refresh_token", Instant.now());
        RefreshToken newRefreshToken = new RefreshToken(2L, 1, "newRefreshToken", Instant.now());
        when(refreshTokenService.findByToken(request.getRefreshToken())).thenReturn(Mono.just(refreshToken));
        when(refreshTokenService.verifyExpiration(refreshToken)).thenReturn(Mono.just(refreshToken));
        when(refreshTokenService.createRefreshToken(refreshToken.getUserId())).thenReturn(Mono.just(newRefreshToken));
        when(refreshTokenService.deleteByToken("refresh_token")).thenReturn(Mono.empty());

        User user = new User(1, "Александр", "Глущенко", "alexgls", "encodedPassword", "glualex066@gmail.com");
        Mono<User> existingUserMono = Mono.just(user);
        List<String> roles = List.of("ROLE_USER");
        Flux<String> userRolesFlux = Flux.fromIterable(roles);

        when(usersService.findUserById(newRefreshToken.getUserId())).thenReturn(existingUserMono);
        when(usersService.getUserRoles(user.getId())).thenReturn(userRolesFlux);

        String newAccessToken = "newAccessToken";
        when(jwtUtil.generateToken(user.getUsername(), user.getId(), roles)).thenReturn(newAccessToken);
        //when
        var result = authController.refreshToken(request);
        //then
        AuthController.JwtResponse jwtResponse = new AuthController.JwtResponse(newAccessToken, newRefreshToken.getToken());
        ResponseEntity<AuthController.JwtResponse> responseEntity = ResponseEntity.ok(jwtResponse);
        StepVerifier
                .create(result)
                .expectNext(responseEntity)
                .verifyComplete();

        verify(refreshTokenService).findByToken(request.getRefreshToken());
        verify(refreshTokenService).verifyExpiration(refreshToken);
        verify(refreshTokenService).createRefreshToken(refreshToken.getUserId());
        verify(refreshTokenService).deleteByToken("refresh_token");

        verify(usersService).findUserById(newRefreshToken.getUserId());
        verify(usersService).getUserRoles(user.getId());
        verify(jwtUtil).generateToken(user.getUsername(), user.getId(), roles);
    }

    @Test
    void handleRefreshTokenRequest_WhenRefreshTokenIsIncorrect() {
        //given
        RefreshTokenRequest request = new RefreshTokenRequest("refresh_token");
        when(refreshTokenService.findByToken(request.getRefreshToken())).thenReturn(Mono.empty());
        //when
        var result = authController.refreshToken(request);
        //then
        StepVerifier
                .create(result)
                .expectErrorMatches((error) -> error instanceof RefreshTokenNotFoundException && error.getMessage().equals("Refresh token не найден в базе данных."))
                .verify();

        verify(refreshTokenService).findByToken(request.getRefreshToken());
    }

    @Test
    void handleRegisterRequest_WhenDataIsCorrect_ReturnsGetUserDtoMono() {
        //given
        UserRegisterDto userRegisterDto = new UserRegisterDto("Александр", "Глущенко", "alexgls", "encodedPassword", "glualex066@gmail.com");
        GetUserDto getUserDto = new GetUserDto(1, "Александр", "Глущенко", "alexgls");
        User user = new User(1, "Александр", "Глущенко", "alexgls", "encodedPassword", "glualex066@gmail.com");
        List<String>roles = List.of("ROLE_USER");
        String accessToken = "access_token";
        RefreshToken refreshToken = new RefreshToken(1L, 1, "refresh_token", Instant.now());
        when(usersService.saveUser(userRegisterDto)).thenReturn(Mono.just(getUserDto));
        when(usersService.getUserByUsername(user.getUsername())).thenReturn(Mono.just(user));
        when(usersService.getUserRoles(user.getId())).thenReturn(Flux.fromIterable(roles));
        when(jwtUtil.generateToken(user.getUsername(), user.getId(), roles)).thenReturn(accessToken);
        when(refreshTokenService.createRefreshToken(refreshToken.getUserId())).thenReturn(Mono.just(refreshToken));
        //when
        var result = authController.register(userRegisterDto);
        //then
        AuthController.JwtResponse jwtResponse = new AuthController.JwtResponse("access_token", "refresh_token");
        ResponseEntity<AuthController.JwtResponse> responseEntity = ResponseEntity
                .ok(jwtResponse);
        StepVerifier
                .create(result)
                .expectNext(responseEntity)
                .verifyComplete();

        verify(usersService).saveUser(userRegisterDto);
        verify(usersService).getUserRoles(user.getId());
        verify(jwtUtil).generateToken(user.getUsername(), user.getId(), roles);
        verify(refreshTokenService).createRefreshToken(refreshToken.getUserId());
        verify(usersService).getUserByUsername(user.getUsername());

    }


}
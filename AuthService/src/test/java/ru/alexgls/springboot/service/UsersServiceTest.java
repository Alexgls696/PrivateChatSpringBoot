package ru.alexgls.springboot.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.alexgls.springboot.dto.GetUserDto;
import ru.alexgls.springboot.dto.UserRegisterDto;
import ru.alexgls.springboot.entity.Role;
import ru.alexgls.springboot.entity.User;
import ru.alexgls.springboot.exceptions.NoSuchUserException;
import ru.alexgls.springboot.exceptions.NoSuchUserRoleException;
import ru.alexgls.springboot.mapper.UserMapper;
import ru.alexgls.springboot.repository.UserRolesRepository;
import ru.alexgls.springboot.repository.UsersRepository;

import java.util.List;

import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    UsersRepository usersRepository;

    @Mock
    UserRolesRepository userRolesRepository;

    @InjectMocks
    UsersService usersService;

    @Test
    @DisplayName("checkCredentials должен возвращать Mono true в случае верных данных")
    public void handleCheckCredentialsSuccess_ReturnsMonoTrue() {
        //given
        String username = "alex";
        String password = "password";
        String encodedPassword = "encodedPassword";

        User user = new User(1, "Александр", "Глущенко", "alex", encodedPassword, "glualex066@gmail.com");
        Mockito.when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);
        Mockito.when(usersRepository.findByUsername(username)).thenReturn(Mono.just(user));

        //when

        var result = usersService.checkCredentials(username, password);
        //then
        StepVerifier
                .create(result)
                .expectNext(true)
                .verifyComplete();

        verify(usersRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, encodedPassword);
    }

    @Test
    @DisplayName("checkCredentials должен возвращать Mono false в случае неверных данных")
    public void handleCheckCredentialsFailed_ReturnsMonoFalse() {
        //given
        String username = "alex";
        String password = "password";
        String encodedPassword = "encodedPassword";

        User user = new User(1, "Александр", "Глущенко", "alex", encodedPassword, "glualex066@gmail.com");
        Mockito.when(passwordEncoder.matches(password, encodedPassword)).thenReturn(false);
        Mockito.when(usersRepository.findByUsername(username)).thenReturn(Mono.just(user));

        //when

        var result = usersService.checkCredentials(username, password);
        //then
        StepVerifier
                .create(result)
                .expectNext(false)
                .verifyComplete();

        verify(usersRepository).findByUsername(username);
        verify(passwordEncoder).matches(password, encodedPassword);
    }

    @Test
    @DisplayName("getUser by username, когда пользователь с ззаданным ником существует")
    void handleGetUserByUsername_WhenUserWithCorrectUsernameIsExists_ReturnsMonoUser() {
        //given
        String username = "alexgls";
        User user = new User(1, "Алекксандр", "Глущенко", "alexgls", "password", "glualex066@gmail.com");
        when(usersRepository.findByUsername(username)).thenReturn(Mono.just(user));
        //when
        var result = usersService.getUserByUsername(username);
        //then
        StepVerifier
                .create(result)
                .expectNext(user)
                .verifyComplete();
        verify(usersRepository).findByUsername(username);
    }

    @Test
    @DisplayName("getUser by username, когда пользователь с заданным ником не существует")
    void handleGetUserByUsername_WhenUserWithIncorrectUsernameIsExists_ReturnsMonoErrorWithUsernameNotFoundException() {
        //given
        String username = "alexgls";
        when(usersRepository.findByUsername(username)).thenReturn(Mono.empty());
        //when
        var result = usersService.getUserByUsername(username);
        //then
        StepVerifier
                .create(result)
                .expectErrorMatches((error) -> error instanceof UsernameNotFoundException && error.getMessage().equals("User with username %s not found".formatted(username)))
                .verify();
        verify(usersRepository).findByUsername(username);
    }

    @Test
    @DisplayName("findUserById, когда пользователь с заданным id существует")
    void handleFindUserByIdWhenUserIsExists_returnsMonoUser() {
        //given
        int id = 1;
        User user = new User(id, "Алекксандр", "Глущенко", "alexgls", "password", "glualex066@gmail.com");
        when(usersRepository.findById(id)).thenReturn(Mono.just(user));
        //when
        var result = usersService.findUserById(id);
        //then
        StepVerifier
                .create(result)
                .expectNext(user)
                .verifyComplete();
        verify(usersRepository).findById(id);
    }

    @Test
    @DisplayName("findUserById, когда пользователь с заданным id не существует, то возвращается Mono.error")
    void handleFindUserByIdWhenUserIsNotExists_returnsMonoErrorWithNoSuchUserException() {
        //given
        int id = 1;
        when(usersRepository.findById(id)).thenReturn(Mono.error(new NoSuchUserException("User with id %d not found".formatted(id))));
        //when
        var result = usersService.findUserById(id);
        //then
        StepVerifier
                .create(result)
                .expectErrorMatches((error) -> error instanceof NoSuchUserException && error.getMessage().equals("User with id %d not found".formatted(id)))
                .verify();
        verify(usersRepository).findById(id);
    }

    @Test
    void handleFindUserInitialsById_ReturnsMonoStringInitials() {
        //given
        int id = 1;
        User user = new User(id, "Александр", "Глущенко", "alexgls", "password", "glualex066@gmail.com");
        when(usersRepository.findById(id)).thenReturn(Mono.just(user));
        //when
        var result = usersService.findUserInitialsById(id);
        //then
        StepVerifier
                .create(result)
                .expectNext("Александр Глущенко")
                .verifyComplete();

        verify(usersRepository).findById(id);
    }

    @Test
    void handleFindUserInitialsById_WhenUserNotFound_ReturnsMonoErrorWithNoSuchUserException() {
        //given
        int id = 1;
        when(usersRepository.findById(id)).thenReturn(Mono.empty());
        //when
        var result = usersService.findUserInitialsById(id);
        //then
        StepVerifier
                .create(result)
                .expectErrorMatches((error) -> error instanceof NoSuchUserException && error.getMessage().equals("User with id %d not found".formatted(id)))
                .verify();

        verify(usersRepository).findById(id);
    }

    @Test
    void handleFindUserDtoByIdWhenUserIsExists_ReturnsMonoUserDto() {
        //given
        int id = 1;
        User user = new User(id, "Александр", "Глущенко", "alexgls", "password", "glualex066@gmail.com");
        when(usersRepository.findById(id)).thenReturn(Mono.just(user));
        //when
        var result = usersService.findUserDtoById(id);
        //then
        StepVerifier
                .create(result)
                .expectNext(new GetUserDto(1, user.getName(), user.getSurname(), user.getUsername()))
                .verifyComplete();

        verify(usersRepository).findById(id);
    }

    @Test
    void handleFindUserDtoByIdWhenUserNotFound_ReturnsMonoErrorWithNoSuchUserException() {
        //given
        int id = 1;
        when(usersRepository.findById(id)).thenReturn(Mono.empty());
        //when
        var result = usersService.findUserDtoById(id);
        //then
        StepVerifier
                .create(result)
                .expectErrorMatches((error) -> error instanceof NoSuchUserException && error.getMessage().equals("User with id %d not found".formatted(id)));
        verify(usersRepository).findById(id);
    }

    @Test
    void handleGetUserRolesWhenUserRolesIsExists_ReturnsFluxStringUserRoles() {
        //given
        int id = 1;
        List<Role> roles = List.of(new Role(1, "ROLE_USER"), new Role(2, "ROLE_ADMIN"));
        when(userRolesRepository.findByUserId(id)).thenReturn(Flux.fromIterable(roles));
        //when
        var result = usersService.getUserRoles(id);
        //then
        StepVerifier
                .create(result)
                .expectNextSequence(roles.stream().map(role -> role.getName()).toList())
                .verifyComplete();

        verify(userRolesRepository).findByUserId(id);
    }

    @Test
    void handleGetUserRolesWhenUserRolesListIsEmpty_ReturnsMonoErrorWithNoSuchUserRoleException() {
        //given
        int id = 1;
        when(userRolesRepository.findByUserId(id)).thenReturn(Flux.empty());
        //when
        var result = usersService.getUserRoles(id);
        //then
        StepVerifier
                .create(result)
                .expectErrorMatches((error) -> error instanceof NoSuchUserRoleException && error.getMessage().equals("Roles for user with id %d not found".formatted(id)))
                .verify();

        verify(userRolesRepository).findByUserId(id);
    }

    @Test
    void handleSaveUserWhenUserRegisterDtoIsCurrent_ReturnsMonoUserDto() {
        //given
        UserRegisterDto userRegisterDto = new UserRegisterDto("Александр", "Глущенко", "alexgls", "password", "glualex066@gmail.com");
        User user = UserMapper.fromUserRegisterDto(userRegisterDto, passwordEncoder);
        User savedUser = UserMapper.fromUserRegisterDto(userRegisterDto, passwordEncoder);
        savedUser.setId(1);
        when(passwordEncoder.encode("password")).thenReturn("encodedPassword");
        when(userRolesRepository.findRoleByName("ROLE_USER")).thenReturn(Mono.just(new Role(1, "ROLE_USER")));
        doReturn(Mono.just(savedUser)).when(usersRepository).save(any(User.class));
        when(userRolesRepository.insertIntoUserRoles(savedUser.getId(), 1)).thenReturn(Mono.empty());
        //when
        var result = usersService.saveUser(userRegisterDto);
        //then
        StepVerifier
                .create(result)
                .expectNext(new GetUserDto(1, user.getName(), user.getSurname(), user.getUsername()))
                .verifyComplete();

        verify(userRolesRepository).findRoleByName("ROLE_USER");
        verify(usersRepository).save(any(User.class));
        verify(userRolesRepository).insertIntoUserRoles(1, 1);
    }

}
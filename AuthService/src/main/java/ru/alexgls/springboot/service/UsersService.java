package ru.alexgls.springboot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.alexgls.springboot.dto.UserRegisterDto;
import ru.alexgls.springboot.entity.Role;
import ru.alexgls.springboot.entity.User;
import ru.alexgls.springboot.repository.UserRolesRepository;
import ru.alexgls.springboot.repository.UsersRepository;

@Service
@RequiredArgsConstructor
public class UsersService {

    private final PasswordEncoder passwordEncoder;

    private final UsersRepository usersRepository;
    private final UserRolesRepository userRolesRepository;

    public Mono<Boolean> checkCredentials(String username, String password) {
        return usersRepository.findByUsername(username)
                .map(user -> passwordEncoder.matches(password, user.getPassword()))
                .switchIfEmpty(Mono.just(false));
    }

    public Mono<User> getUser(String username) {
        return usersRepository.findByUsername(username);
    }

    public Mono<User> findUserById(int id) {
        return usersRepository.findById(id);
    }


    public Flux<String> getUserRoles(int id) {
        return userRolesRepository.findByUserId(id)
                .map(Role::getName);
    }


    @Transactional
    public Mono<Void> saveUser(UserRegisterDto userRegisterDto) {
        User user = new User();
        user.setEmail(userRegisterDto.email());
        user.setPassword(passwordEncoder.encode(userRegisterDto.password()));
        user.setUsername(userRegisterDto.username());
        user.setUsersServiceId(userRegisterDto.studentId());
        Mono<Role> monoUserRole = userRolesRepository.findRoleByName("ROLE_USER");
        Mono<User> monoSavedUser = usersRepository.save(user);
        return Mono.zip(monoSavedUser, monoUserRole)
                .flatMap(tuple -> {
                    User savedUser = tuple.getT1();
                    Role savedRole = tuple.getT2();
                    return userRolesRepository.insertIntoUserRoles(savedUser.getId(), savedRole.getId());
                });
    }

    @Transactional
    public Mono<Void> removeUserByUserServiceId(int userServiceId) {
        return usersRepository.findUserIdByUsersServiceId(userServiceId)
                .flatMap(securityServiceUserId -> userRolesRepository.removeAllByUserId(securityServiceUserId)
                        .then(usersRepository.removeUserByUsersServiceId(userServiceId)));
    }
}
package ru.alexgls.springboot.repository;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import ru.alexgls.springboot.entity.User;

@Repository
public interface UsersRepository extends ReactiveCrudRepository<User, Integer> {
    Mono<User> findByUsername(String username);

    Mono<Void> removeUserByUsersServiceId(int id);

    Mono<Integer>findUserIdByUsersServiceId(int id);
}

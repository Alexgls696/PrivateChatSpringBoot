package ru.alexgls.springboot.repository;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.alexgls.springboot.entity.Role;


@Repository
public interface UserRolesRepository extends ReactiveCrudRepository<Role, Integer> {
    @Query(value = "select r.* from roles r join users_roles ur on r.id = ur.role_id where ur.user_id = :userId")
    Flux<Role> findByUserId(@Param("userId") int userId);

    @Modifying
    @Query("insert into users_roles (role_id, user_id) VALUES (:roleId,:userId)")
    Mono<Void> insertIntoUserRoles(@Param("userId") int userId, @Param("roleId") int roleId);

    @Modifying
    @Query("delete from users_roles ur where user_id = :userId")
    Mono<Void> removeAllByUserId(@Param("userId") int userId);

    Mono<Role> findRoleByName(String name);
}

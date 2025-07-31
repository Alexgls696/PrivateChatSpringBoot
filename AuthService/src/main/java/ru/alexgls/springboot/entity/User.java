package ru.alexgls.springboot.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    private int id;
    private String name;
    private String surname;
    private String username;
    private String password;
    private String email;
}

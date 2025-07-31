package ru.alexgls.springboot.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
public class Role {
    @Id
    private int id;
    private String name;
}

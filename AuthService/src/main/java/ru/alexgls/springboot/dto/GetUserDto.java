package ru.alexgls.springboot.dto;

import java.util.List;

public record GetUserDto(
        String username,
        String email,
        List<String> roles
) {
}

package ru.alexgls.springboot.dto;

public record UserRegisterDto(
        String username,
        String password,
        String email,
        int studentId
) {
}

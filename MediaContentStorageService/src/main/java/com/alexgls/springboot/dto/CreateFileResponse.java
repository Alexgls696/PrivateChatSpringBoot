package com.alexgls.springboot.dto;

import java.sql.Timestamp;

public record CreateFileResponse(
        Integer id,
        String path,
        Timestamp createdAt
) {
}

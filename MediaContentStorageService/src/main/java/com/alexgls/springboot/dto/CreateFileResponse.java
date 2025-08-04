package com.alexgls.springboot.dto;

import java.sql.Timestamp;

public record CreateFileResponse(
        String path,
        Timestamp createdAt
) {
}

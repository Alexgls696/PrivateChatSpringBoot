package com.alexgls.springboot.dto;

public record CreateFileMetadataRequest(
        String path,
        String filename
) {
}

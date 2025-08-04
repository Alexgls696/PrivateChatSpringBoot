package com.alexgls.springboot.client.response;

public record DownloadFileResponse(
        String method,
        String href,
        boolean templated
) {
}

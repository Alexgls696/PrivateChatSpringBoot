package com.alexgls.springboot.messagestorageservice.dto;


public record CreateAttachmentPayload(
        String url,

        String mimeType
) {
}
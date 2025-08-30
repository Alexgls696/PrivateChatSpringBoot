package com.alexgls.springboot.messagestorageservice.service;

import com.alexgls.springboot.messagestorageservice.entity.Attachment;
import reactor.core.publisher.Flux;

public interface AttachmentService {
    Flux<Attachment> findAllByMediaTypeAndChatId(String mediaType, int chatId, int currentUserId);
}

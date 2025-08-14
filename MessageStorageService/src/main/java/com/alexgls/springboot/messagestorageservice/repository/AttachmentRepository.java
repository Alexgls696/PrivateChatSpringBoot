package com.alexgls.springboot.messagestorageservice.repository;

import com.alexgls.springboot.messagestorageservice.entity.Attachment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AttachmentRepository extends ReactiveCrudRepository<Attachment, Long> {
    Flux<Attachment> findAllByMessageId(Long messageId);
}

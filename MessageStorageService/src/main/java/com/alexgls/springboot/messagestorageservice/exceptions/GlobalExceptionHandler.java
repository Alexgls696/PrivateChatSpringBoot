package com.alexgls.springboot.messagestorageservice.exceptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.util.Locale;

@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    @ExceptionHandler(AccessDeniedException.class)
    public Mono<ResponseEntity<ProblemDetail>> handleAccessDeniedException(AccessDeniedException exception, Locale locale) {
        log.warn("handleAccessDeniedException: {}", exception.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, messageSource
                .getMessage("errors.access_denied", new Object[0], "errors.access_denied", locale));
        problemDetail.setProperty("error", exception.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(problemDetail));
    }
}

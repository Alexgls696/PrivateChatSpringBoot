package com.alexgls.springboot.indatabasecontentstorageservice.controller;

import com.alexgls.springboot.indatabasecontentstorageservice.entity.ChatImage;
import com.alexgls.springboot.indatabasecontentstorageservice.service.ImagesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
@Slf4j
public class ImagesController {

    private final ImagesService imagesService;

    @GetMapping("/{id}")
    public ChatImage findChatImageById(@PathVariable("id") int id) {
        log.info("Find chat image by id: {}", id);
        return imagesService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ChatImage> saveChatImage(@RequestBody String path, UriComponentsBuilder uriBuilder) {
        log.info("Save chat image: {}", path);
        ChatImage created = imagesService.save(path);
        return ResponseEntity
                .created(uriBuilder.replacePath("/api/images/{id}")
                        .build(Map.of("id", created.getId())))
                .body(created);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChatImage(@PathVariable("id") int id) {
        log.info("Delete chat image by id: {}", id);
        imagesService.deleteById(id);
        return ResponseEntity
                .noContent()
                .build();
    }

}

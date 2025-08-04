package com.alexgls.springboot.controller;

import com.alexgls.springboot.dto.CreateFileResponse;
import com.alexgls.springboot.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<CreateFileResponse> createImage(@RequestParam("file") MultipartFile file) {
        log.info("Uploading image to storage");
        return ResponseEntity
                .ok()
                .body(storageService.uploadImage(file));
    }

    @GetMapping("/download")
    public ResponseEntity<String> getDownLoadFilePath(@RequestParam("path") String path) {
        log.info("Downloading image from storage: {}", path);
        return ResponseEntity
                .ok()
                .body(storageService.getDownLoadFilePath(path));
    }

}

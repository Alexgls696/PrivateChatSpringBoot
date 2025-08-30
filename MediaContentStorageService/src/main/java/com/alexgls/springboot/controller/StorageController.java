package com.alexgls.springboot.controller;

import com.alexgls.springboot.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
@Slf4j
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/upload")
    public ResponseEntity<?> saveFile(@RequestParam("file") MultipartFile file) {
        log.info("Uploading image to storage");
        if (Objects.isNull(file)) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "File is null", "code", HttpStatus.BAD_REQUEST.value()));
        }
        return ResponseEntity
                .ok()
                .body(storageService.uploadImage(file));
    }

    @GetMapping("/download/by-path")
    public ResponseEntity<Map<String,String>> getDownloadLinkByPath(@RequestParam("path") String path) {
        log.info("Downloading image from storage: {}", path);
        return ResponseEntity
                .ok()
                .body(Map.of("href",storageService.getDownLoadFilePath(path)));
    }

    @GetMapping("/download/by-id")
    public ResponseEntity<Map<String,String>> getDownloadLinkById(@RequestParam("id") Integer id) {
        if(Objects.isNull(id)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
        log.info("Downloading image from storage: {}", id);
        return ResponseEntity
                .ok()
                .body(Map.of("href",storageService.getDownloadPathById(id)));
    }

}

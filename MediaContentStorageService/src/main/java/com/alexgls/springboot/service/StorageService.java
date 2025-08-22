package com.alexgls.springboot.service;

import com.alexgls.springboot.dto.CreateFileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {
    CreateFileResponse uploadImage(MultipartFile file);

    String getDownLoadFilePath(String path);

    String getDownloadPathById(int id);
}


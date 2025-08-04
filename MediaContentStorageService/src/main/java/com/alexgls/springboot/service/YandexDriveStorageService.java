package com.alexgls.springboot.service;

import com.alexgls.springboot.client.InDatabaseStorageServiceRestClient;
import com.alexgls.springboot.client.YandexDriveStorageRestClient;
import com.alexgls.springboot.client.response.DownloadFileResponse;
import com.alexgls.springboot.client.response.UploadFilePathResponse;
import com.alexgls.springboot.dto.CreateFileResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class YandexDriveStorageService implements StorageService {

    private final YandexDriveStorageRestClient yandexDriveRestClient;

    private final InDatabaseStorageServiceRestClient inDatabaseStorageServiceRestClient;

    @Value("${yandex.baseUrl}")
    private String baseUrl;

    @Value("${yandex.application-path}")
    private String applicationPath;

    @Override
    public CreateFileResponse uploadImage(MultipartFile file) {
        StringBuilder pathBuilder = new StringBuilder();
        String filepathToDatabase = getFilePath(file.getOriginalFilename());
        pathBuilder.append(baseUrl).append("/upload?path=").append(filepathToDatabase);
        UploadFilePathResponse uploadPathResponse = yandexDriveRestClient.getUploadFilePathUrl(pathBuilder.toString());
        log.info("upload path from yandex {}", uploadPathResponse);
        saveFileToYandexDrive(file, uploadPathResponse);
        log.info("Saving path to database... {}", filepathToDatabase);
        inDatabaseStorageServiceRestClient.saveChatImage(filepathToDatabase);
        return new CreateFileResponse(filepathToDatabase, Timestamp.from(Instant.now()));
    }

    @Override
    public String getDownLoadFilePath(String path) {
        path = baseUrl + "/download?path=" + path;
        DownloadFileResponse downloadFileResponse = yandexDriveRestClient.getDownloadFileUrl(path);
        log.info("download file url from yandex {}", downloadFileResponse);
        return downloadFileResponse.href();
    }

    private String getFilePath(String fileName) {
        int lastPointIndex = fileName.lastIndexOf(".");
        String format = fileName.substring(lastPointIndex);
        String randomFileName = UUID.randomUUID() + format;
        return applicationPath + "/images/" + randomFileName;
    }

    private String getFullFilePath(String fileName) {
        StringBuilder pathBuilder = new StringBuilder();
        int lastPointIndex = fileName.lastIndexOf(".");
        String format = fileName.substring(lastPointIndex);
        String randomFileName = UUID.randomUUID() + format;
        String fullPath = applicationPath + "/images/" + randomFileName;
        pathBuilder.append(baseUrl).append("/upload?path=").append(fullPath);
        log.info("result path {}", pathBuilder);
        return pathBuilder.toString();
    }

    private void saveFileToYandexDrive(MultipartFile file, UploadFilePathResponse response) {
        try {
            byte[] fileBytes = file.getBytes();
            ResponseEntity<Void> fileUploadResult = yandexDriveRestClient.uploadFile(response.getHref(), fileBytes);
            if (fileUploadResult.getStatusCode().is2xxSuccessful()) {
                log.info("Upload file success");
            } else {
                log.warn("Upload file failed, code: {}, href: {}", fileUploadResult.getStatusCode(), response.getHref());
            }
        } catch (IOException ioException) {
            log.info("Error with work fileBytes {}", ioException.getMessage());
        }
    }


}

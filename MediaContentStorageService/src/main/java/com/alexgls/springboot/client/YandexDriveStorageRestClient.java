package com.alexgls.springboot.client;

import com.alexgls.springboot.client.response.DownloadFileResponse;
import com.alexgls.springboot.client.response.UploadFilePathResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RequiredArgsConstructor
public class YandexDriveStorageRestClient {

    private final RestClient restClient;

    private void exceptionMessage(Exception exception) {
        log.warn("При работе с yandex client возникло исключение {}", exception.getMessage());
    }

    public UploadFilePathResponse getUploadFilePathUrl(String path) {
        try {
            return restClient
                    .get()
                    .uri(path)
                    .retrieve()
                    .body(UploadFilePathResponse.class);
        } catch (Exception exception) {
            exceptionMessage(exception);
            throw exception;
        }
    }

    public ResponseEntity<Void> uploadFile(String path, byte[] bytes) {
        try {
            return restClient
                    .put()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(bytes)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception exception) {
            exceptionMessage(exception);
            throw exception;
        }
    }

    public DownloadFileResponse getDownloadFileUrl(String path) {
        try {
            return restClient
                    .get()
                    .uri(path)
                    .retrieve()
                    .body(DownloadFileResponse.class);
        } catch (Exception exception) {
            exceptionMessage(exception);
            throw exception;
        }
    }

}

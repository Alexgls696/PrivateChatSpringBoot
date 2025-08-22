package com.alexgls.springboot.client;

import com.alexgls.springboot.dto.ChatImage;
import com.alexgls.springboot.dto.CreateFileMetadataRequest;
import com.alexgls.springboot.exception.InDatabaseServiceException;
import com.alexgls.springboot.exception.NoSuchImageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
@Slf4j
public class InDatabaseStorageServiceRestClientImpl implements InDatabaseStorageServiceRestClient {

    private final RestClient restClient;

    @Override
    public ChatImage findChatImageById(int id) {
        try {
            return restClient
                    .get()
                    .uri("/api/images/{id}", id)
                    .retrieve()
                    .body(ChatImage.class);
        } catch (HttpClientErrorException.NotFound exception) {
            ProblemDetail problemDetail = exception.getResponseBodyAs(ProblemDetail.class);
            String error = (String) problemDetail.getProperties().get("error");
            log.warn("Rest client exception: {}", error);
            throw new NoSuchImageException(error);
        } catch (HttpClientErrorException exception) {
            log.warn("In database service client exception: {}", exception.getResponseBodyAsString());
            throw new InDatabaseServiceException(exception.getResponseBodyAsString());
        }
    }

    @Override
    public ChatImage saveChatImage(CreateFileMetadataRequest createFileMetadataRequest) {
        try {
            return restClient
                    .post()
                    .uri("/api/images")
                    .body(createFileMetadataRequest)
                    .retrieve()
                    .body(ChatImage.class);
        } catch (HttpClientErrorException exception) {
            log.warn("In database service client exception: {}", exception.getResponseBodyAsString());
            throw new InDatabaseServiceException(exception.getResponseBodyAsString());
        }
    }
}

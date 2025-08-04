package com.alexgls.springboot.config;

import com.alexgls.springboot.client.InDatabaseStorageServiceRestClient;
import com.alexgls.springboot.client.InDatabaseStorageServiceRestClientImpl;
import com.alexgls.springboot.client.YandexDriveStorageRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientConfig {

    @Value("${yandex.oauth-token}")
    private String oauthToken;

    @Bean
    public YandexDriveStorageRestClient yandexDriveRestClient() {
        return new YandexDriveStorageRestClient(RestClient
                .builder()
                .defaultHeader("Authorization", oauthToken)
                .build());
    }

    @Bean
    public InDatabaseStorageServiceRestClient inDatabaseStorageServiceRestClient(@Value("${services.in-database-image-service}") String serviceUrl) {
        return new InDatabaseStorageServiceRestClientImpl(RestClient
                .builder()
                .baseUrl(serviceUrl)
                .build());
    }
}

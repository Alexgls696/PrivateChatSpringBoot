package ru.alexgls.springboot.usersmessagingservice.config;

import ru.alexgls.springboot.usersmessagingservice.client.AuthServiceClient;
import ru.alexgls.springboot.usersmessagingservice.client.AuthServiceClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;


@Configuration
public class RestClientConfig {

    @Bean
    public AuthServiceClient authServiceClient(@Value("${services.auth-service}") String authServiceUrl) {
        return new AuthServiceClientImpl(RestClient
                .builder()
                .baseUrl(authServiceUrl)
                .build());
    }
}

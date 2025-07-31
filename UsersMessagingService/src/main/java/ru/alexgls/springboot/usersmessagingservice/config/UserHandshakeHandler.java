package ru.alexgls.springboot.usersmessagingservice.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

public class UserHandshakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {
        String userId = (String) attributes.get("userId");
        if (userId == null) {
            // fallback, например, сгенерировать UUID или вернуть null
            return null;
        }

        // Возвращаем Principal с именем userId
        return new Principal() {
            @Override
            public String getName() {
                return userId;
            }
        };
    }
}

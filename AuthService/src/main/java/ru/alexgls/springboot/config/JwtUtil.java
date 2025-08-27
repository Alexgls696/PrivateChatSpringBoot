package ru.alexgls.springboot.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.alexgls.springboot.dto.JwtValidationResponse;

import java.security.Key;
import java.security.interfaces.RSAPublicKey;
import java.util.*;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    @Value("${jwt.expiration}")
    private Long expiration;

    private final KeyPairProvider keyProvider;

    public Claims getAllClaimsFromToken(String token) {
        RSAPublicKey publicKey = keyProvider.getPublicKey();
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsernameFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    public Date getExpirationDateFromToken(String token) {
        return getAllClaimsFromToken(token).getExpiration();
    }

    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }


    public String generateToken(String username, Integer userId, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", roles);
        claims.put("userId", String.valueOf(userId));
        long nowMillis = System.currentTimeMillis();
        Date now = new Date(nowMillis);
        long expMillis = nowMillis + expiration;
        Date exp = new Date(expMillis);
        return Jwts.builder()
                .setHeaderParam(JwsHeader.KEY_ID, keyProvider.getKeyId())
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(now)
                .setIssuer("http://localhost:8085")
                .setExpiration(exp)
                .signWith(keyProvider.getPrivateKey(), SignatureAlgorithm.RS256)
                .compact();
    }

    public Boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(keyProvider.getPublicKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public JwtValidationResponse validateTokenAndGetJwtValidationResponse(String token) {
        Claims claims = getAllClaimsFromToken(token);
        String userId = claims.get("userId", String.class);
        List<String> roles = (List<String>) claims.get("roles");
        return new JwtValidationResponse(true,
                "The token is successfully parsed", userId, roles);
    }

}

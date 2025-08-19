package streetwalker.userservice.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import streetwalker.userservice.models.User;

import javax.crypto.SecretKey;
import java.util.Date;
//import java.util.HashMap;

@Component
@Getter
public class JwtCore {

    @Value("${walker.secret}")
    private String secret;
    @Value("${walker.lifetime}")
    private int lifetime;
    private final long refreshTokenLifetime = 7 * 24 * 60 * 60 * 1000; // 7 дней

    public String generateToken(Authentication authentication) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        User userDetails = (User) authentication.getPrincipal();
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("email", userDetails.getEmail())
                .claim("role", userDetails.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + lifetime))
                .signWith(key)
                .compact();
    }
    public String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + lifetime))
                .signWith(key)
                .compact();
    }

    public String generateRefreshToken(String username) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + refreshTokenLifetime))
                .signWith(key)
                .compact();
    }
    public String getUserNameFromJwt(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
//        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody().getSubject();
    }
//    public HashMap<String, String> getClaimsFromJwt(String token) {
//        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes());
//        Claims claims = Jwts.parserBuilder()
//                .setSigningKey(key)
//                .build()
//                .parseClaimsJws(token)
//                .getBody();
//        String username = claims.getSubject(); // Извлекаем subject (username)
//        String phone = claims.get("phone", String.class); // Извлекаем телефон
//        String email = claims.get("email", String.class); // Извлекаем email
//        String role = claims.get("role", String.class); // Извлекаем роль
//        HashMap<String, String> map = new HashMap<>();
//        map.put("username", username);
//        map.put("phone", phone);
//        map.put("email", email);
//        map.put("role", role);
//        return map;
//    }
}

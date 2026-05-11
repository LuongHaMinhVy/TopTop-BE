package com.back.auth.security.jwt;

import com.back.auth.repo.IBlacklistedTokenRepo;
import com.back.common.utils.exception.AppException;
import com.back.common.utils.exception.ErrorCode;
import com.back.user.model.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final IBlacklistedTokenRepo blacklistedTokenRepo;

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.access-token-expiration}")
    private long ACCESS_TOKEN_EXP;

    @Value("${jwt.refresh-token-expiration}")
    private long REFRESH_TOKEN_EXP;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("roles", extractRoles(user))
                .claim("type", "access")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXP))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("roles", extractRoles(user))
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXP))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private List<String> extractRoles(User user) {
        return user.getRoles()
                .stream()
                .map(r -> r.getName().name())
                .toList();
    }

    public String extractFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }

        return authHeader.substring(7);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractTokenType(String token) {
        return extractClaim(token, c -> c.get("type", String.class));
    }

    public List extractRolesFromToken(String token) {
        return extractClaim(token, c -> c.get("roles", List.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = extractAllClaims(token);
        return resolver.apply(claims);
    }


    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

        } catch (JwtException | IllegalArgumentException e) {
            throw new AppException(ErrorCode.INVALID_TOKEN, "token");
        }
    }

    public boolean isTokenExpired(String token) {
        return !extractExpiration(token).before(new Date());
    }

    public boolean isTokenValid(String token) {
        String hash = DigestUtils.sha256Hex(token);
        return isTokenExpired(token)
                && blacklistedTokenRepo.existsByTokenHash(hash);
    }

    public boolean isTokenValid(String token, User user) {
        String hash = DigestUtils.sha256Hex(token);

        return user.getEmail().equals(extractUsername(token))
                && isTokenExpired(token)
                && blacklistedTokenRepo.existsByTokenHash(hash);
    }

    public boolean isAccessTokenValid(String token) {
        return "access".equals(extractTokenType(token))
                && isTokenValid(token);
    }

    public Instant getExpirationTime(String accessToken){
        return extractExpiration(accessToken).toInstant();
    }

    public String extractEmail(String refreshToken){
        return extractUsername(refreshToken);
    }
}
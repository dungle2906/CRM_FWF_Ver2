package com.example.BasicCRM_FWF.Service;

import com.example.BasicCRM_FWF.Model.User;
import com.example.BasicCRM_FWF.Repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JWTService {

    private final UserRepository userRepository;

    @Value("${application.security.jwt.secret-key}") // Secret key cho HMAC
    private String secretKey;

    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long jwtRefreshExpiration;

    public final JwtDecoder jwtDecoder; // Decoder để xác thực token RSA từ Google

    // 🛠 Xử lý token của hệ thống (HMAC - HS256)
    public String generateToken(UserDetails userDetails, String ipAddress) {
        return generateToken(new HashMap<>(), userDetails, ipAddress);
    }

    public String generateToken(Map<String, Object> extractClaims, UserDetails userDetails, String ipAddress) {
        var user = userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
        extractClaims.put("authorities", userDetails.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList()));
        extractClaims.put("userId", user.getId()); // Use "userId" instead of "username"
        extractClaims.put("ipAddress", ipAddress);
        return buildToken(extractClaims, userDetails, jwtExpiration);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, jwtRefreshExpiration);
    }

//    public String generateRefreshToken(User user) {
//        return Jwts.builder()
//                .setSubject(user.getUsername())
//                .setIssuedAt(new Date(System.currentTimeMillis()))
//                .setExpiration(new Date(System.currentTimeMillis() + jwtRefreshExpiration))
//                .signWith(getSignInKeyHMAC(), SignatureAlgorithm.HS256)
//                .compact();
//    }

    private String buildToken(Map<String, Object> extractClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .setClaims(extractClaims)
                .setSubject(userDetails.getUsername())
                .setIssuer("http://localhost:8080")
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKeyHMAC(), SignatureAlgorithm.HS256) // HMAC Token
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            return extractClaim(token, Claims::getSubject);
        } catch (Exception e) {
            return null; // Trả về null nếu token là RS256 (Google)
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSignInKeyHMAC()) // HMAC Key
                    .setAllowedClockSkewSeconds(300)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            return null;
        } catch (Exception e) {
            // Nếu giải mã thất bại, thử giải mã với Google Public Key (RS256)
            return decodeGoogleJwt(token);
        }
    }

    private Claims decodeGoogleJwt(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return (Claims) jwt.getClaims();
        } catch (JwtException e) {
            return null;
        }
    }

//    public Map<String, Object> decodeJwt(String token) throws Exception {
//        try {
//            // Parse the JWT and extract claims
//            Claims claims = Jwts.parserBuilder()
//                    .setSigningKey(getSignInKeyHMAC())
//                    .setAllowedClockSkewSeconds(300)
//                    .build()
//                    .parseClaimsJws(token)
//                    .getBody();
//
//            // Create a map to store extracted claims
//            Map<String, Object> result = new HashMap<>();
//
//            // Extract standard claims
//            result.put("sub", claims.getSubject());
//            result.put("iss", claims.getIssuer());
//            result.put("iat", claims.getIssuedAt() != null ? claims.getIssuedAt().getTime() / 1000 : null);
//            result.put("exp", claims.getExpiration() != null ? claims.getExpiration().getTime() / 1000 : null);
//
//            // Extract custom claims
//            result.put("ipAddress", claims.get("ipAddress", String.class));
//            result.put("userId", claims.get("userId", Integer.class));
//            result.put("authorities", claims.get("authorities", List.class));
//
//            return result;
//        } catch (ExpiredJwtException e) {
//            throw new Exception("Token has expired", e);
//        } catch (Exception e) {
//            throw new Exception("Invalid token", e);
//        }
//    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Key getSignInKeyHMAC() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public long getTokenRemainingTime(String token) {
        try {
            Date expirationDate = extractClaim(token, Claims::getExpiration);
            long now = System.currentTimeMillis();
            long remaining = expirationDate.getTime() - now;
            return remaining > 0 ? remaining : 0;
        } catch (Exception e) {
            return 0;
        }
    }

}

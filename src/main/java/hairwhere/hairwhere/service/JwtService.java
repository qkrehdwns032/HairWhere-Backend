package hairwhere.hairwhere.service;

import hairwhere.hairwhere.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-token-validity}")
    private long accessTokenValidity;

    /**
     * JWT 토큰에서 사용자 이름(subject)을 추출합니다.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Long extractKakaoId(String token) {
        return extractClaim(token, claims -> Long.parseLong(claims.get("kakaoId").toString()));
    }

    /**
     * JWT 토큰에서 특정 클레임을 추출합니다.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 사용자 정보를 기반으로 JWT 토큰을 생성합니다.
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", user.getId());
        claims.put("kakaoId", user.getKakaoId());
        claims.put("nickname", user.getNickName());

        return Jwts.builder()
            .setClaims(claims)
            .setSubject(String.valueOf(user.getKakaoId())) // 카카오 ID를 subject로 사용
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + accessTokenValidity * 1000L))
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * JWT 토큰이 유효한지 검증합니다.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * JWT 토큰의 유효기간이 만료되었는지 확인합니다.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * JWT 토큰에서 만료 시간을 추출합니다.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * JWT 토큰에서 모든 클레임을 추출합니다.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }

    /**
     * JWT 서명에 사용할 키를 생성합니다.
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }



}

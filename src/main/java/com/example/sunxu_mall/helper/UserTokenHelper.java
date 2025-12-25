package com.example.sunxu_mall.helper;

import com.example.sunxu_mall.exception.BusinessException;
import com.example.sunxu_mall.util.RedisUtil;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Objects;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/24 20:14
 * @description
 */


@Slf4j
@Component
public class UserTokenHelper {

    private static final String TOKEN_PREFIX = "token:";
    private static final String USER_PREFIX = "user:";

    @Getter
    @Value("${mall.mgt.tokenSecret}")
    private String tokenSecret;
    @Value("${mall.mgt.tokenExpireTimeInRecord}")
    private int tokenExpireTimeInRecord;

    protected final RedisUtil redisUtil;

    public UserTokenHelper(RedisUtil redisUtil) {
        this.redisUtil = redisUtil;
    }

    /**
     * 生成 token
     *
     * @param username 用户名
     * @param json     用户信息
     * @return 返回生成的 token
     */
    public String generateToken(String username, String json) {
        // 将字符串密钥转换为 Key对象
        Key key = Keys.hmacShaKeyFor(tokenSecret.getBytes());
        
        String token = Jwts.builder()
                .setSubject(username)
                .setExpiration(generateExpired())
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
        redisUtil.set(getTokenKey(username), token, tokenExpireTimeInRecord, java.util.concurrent.TimeUnit.SECONDS);
        redisUtil.set(getUserKey(username), json, tokenExpireTimeInRecord, java.util.concurrent.TimeUnit.SECONDS);
        return token;
    }


    public String getTokenKey(String username) {
        return getKey(TOKEN_PREFIX, username);
    }

    public String getUserKey(String username) {
        return getKey(USER_PREFIX, username);
    }


    /**
     * 从 token中解析出username
     *
     * @param token token
     * @return username
     */
    public String getUsernameFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (Objects.isNull(claims)) {
            return null;
        }
        return claims.getSubject();
    }

    /**
     * 获得 Claims
     *
     * @param token Token
     * @return Claims
     */

    public Claims getClaimsFromToken(String token) {
        Claims claims;
        try {
            // 将字符串密钥转换为 Key对象
            Key key = Keys.hmacShaKeyFor(tokenSecret.getBytes());
            
            claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.info("Failed to get Claims:", e);
            throw new BusinessException(HttpStatus.FORBIDDEN.value(), "Please login first");
        }
        return claims;
    }


    /**
     * 计算过期时间
     *
     * @return Date
     */
    protected Date generateExpired() {
        return new Date(System.currentTimeMillis() + tokenExpireTimeInRecord * 1000L);
    }


    protected String getKey(String prefix, String userName) {
        return String.format("%s%s", prefix, userName);
    }
}


package com.example.sunxu_mall.util;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt密码加密测试类
 * 用于测试BCryptPasswordEncoder对密码的加密功能
 * 
 * @author sunxu
 * @version 1.0
 * @date 2025/12/26
 */
public class BcryptEncoderTest {
    
    /**
     * 测试BCrypt密码加密
     * 对特定字符串进行加密并输出加密结果
     */
    @Test
    public void testBcryptEncode() {
        // 要加密的特定字符串
        String originalPassword = "sunshine";
        System.out.println("=== BCrypt Password Encryption Test ===");
        System.out.println("Original password: " + originalPassword);
        
        // Create BCryptPasswordEncoder instance
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        
        // Execute encryption
        String encryptedPassword = passwordEncoder.encode(originalPassword);
        
        // Output encryption result
        System.out.println("Encrypted password: " + encryptedPassword);
        System.out.println("Encrypted result length: " + encryptedPassword.length());
        System.out.println();
        
        // Test if multiple encryptions produce different results (BCrypt generates different salt each time)
        String encryptedPassword2 = passwordEncoder.encode(originalPassword);
        System.out.println("Second encryption result: " + encryptedPassword2);
        System.out.println("Are two encryption results the same: " + encryptedPassword.equals(encryptedPassword2));
        System.out.println();
        
        // Verify password matching
        boolean isMatch = passwordEncoder.matches(originalPassword, encryptedPassword);
        boolean isMatch2 = passwordEncoder.matches(originalPassword, encryptedPassword2);
        System.out.println("Password match verification 1: " + isMatch);
        System.out.println("Password match verification 2: " + isMatch2);
        System.out.println("======================");
    }
}
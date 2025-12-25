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
        System.out.println("=== BCrypt密码加密测试 ===");
        System.out.println("原始密码: " + originalPassword);
        
        // 创建BCryptPasswordEncoder实例
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        
        // 执行加密
        String encryptedPassword = passwordEncoder.encode(originalPassword);
        
        // 输出加密结果
        System.out.println("加密后密码: " + encryptedPassword);
        System.out.println("加密结果长度: " + encryptedPassword.length());
        System.out.println();
        
        // 测试多次加密结果是否不同（BCrypt每次加密会生成不同的salt）
        String encryptedPassword2 = passwordEncoder.encode(originalPassword);
        System.out.println("第二次加密结果: " + encryptedPassword2);
        System.out.println("两次加密结果是否相同: " + encryptedPassword.equals(encryptedPassword2));
        System.out.println();
        
        // 验证密码匹配
        boolean isMatch = passwordEncoder.matches(originalPassword, encryptedPassword);
        boolean isMatch2 = passwordEncoder.matches(originalPassword, encryptedPassword2);
        System.out.println("密码匹配验证1: " + isMatch);
        System.out.println("密码匹配验证2: " + isMatch2);
        System.out.println("======================");
    }
}
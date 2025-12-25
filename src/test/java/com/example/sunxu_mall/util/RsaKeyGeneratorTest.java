package com.example.sunxu_mall.util;

import cn.hutool.crypto.asymmetric.KeyType;
import cn.hutool.crypto.asymmetric.RSA;
import org.junit.jupiter.api.Test;

/**
 * RSA密钥生成测试类
 * 用于测试和生成RSA密钥对
 * 
 * @author sunxu
 * @version 1.0
 * @date 2025/12/25
 */
public class RsaKeyGeneratorTest {
    
    /**
     * 测试生成RSA密钥对
     */
    @Test
    public void testGenerateRsaKeys() {
        String[] keys = RsaKeyGenerator.generateRsaKeys();
        
        // 输出密钥对
        System.out.println("=== RSA密钥对生成结果 ===");
        System.out.println("私钥 (privateKey):");
        System.out.println(keys[0]);
        System.out.println();
        System.out.println("公钥 (publicKey):");
        System.out.println(keys[1]);
        System.out.println("=======================");
        
        // 输出配置文件格式的密钥
        System.out.println("\n=== 配置文件格式 ===");
        System.out.println("mall:");
        System.out.println("  mgt:");
        System.out.println("    password:");
        System.out.println("      privateKey: " + keys[0]);
        System.out.println("      publicKey: " + keys[1]);
        System.out.println("================");
        
        // 测试加密特定字符串
        testEncryptString(keys);
    }
    
    /**
     * 测试对特定字符串加密，并输出Base64编码的密文
     * @param keys RSA密钥对，index 0为私钥，index 1为公钥
     */
    private void testEncryptString(String[] keys) {
        // 特定字符串
        String originalStr = "sunshine";
        System.out.println("\n=== 特定字符串加密测试 ===");
        System.out.println("原始字符串: " + originalStr);
        
        try {
            // 使用公钥加密
            RSA rsa = new RSA(keys[0], keys[1]);
            
            // 获取Base64编码的密文
            String encryptedBase64 = rsa.encryptBase64(originalStr, KeyType.PublicKey);
            
            // 输出密文
            System.out.println("加密后密文 (Base64编码): ");
            System.out.println(encryptedBase64);
            System.out.println();
            
            // 验证解密 - 使用Base64密文直接解密
            String decryptedStr = rsa.decryptStr(encryptedBase64, KeyType.PrivateKey);
            System.out.println("解密后字符串: " + decryptedStr);
            System.out.println("加密解密一致性验证: " + originalStr.equals(decryptedStr));
        } catch (Exception e) {
            System.err.println("加密解密测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=======================");
    }
}
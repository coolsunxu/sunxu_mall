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
        
        // Output key pair
        System.out.println("=== RSA Key Pair Generation Result ===");
        System.out.println("Private Key (privateKey):");
        System.out.println(keys[0]);
        System.out.println();
        System.out.println("Public Key (publicKey):");
        System.out.println(keys[1]);
        System.out.println("=======================");
        
        // Output keys in configuration file format
        System.out.println("\n=== Configuration File Format ===");
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
     * Test encrypting a specific string and outputting Base64 encoded ciphertext
     * @param keys RSA key pair, index 0 is private key, index 1 is public key
     */
    private void testEncryptString(String[] keys) {
        // Specific string
        String originalStr = "sunshine";
        System.out.println("\n=== Specific String Encryption Test ===");
        System.out.println("Original String: " + originalStr);
        
        try {
            // Encrypt using public key
            RSA rsa = new RSA(keys[0], keys[1]);
            
            // Get Base64 encoded ciphertext
            String encryptedBase64 = rsa.encryptBase64(originalStr, KeyType.PublicKey);
            
            // Output ciphertext
            System.out.println("Encrypted Ciphertext (Base64 encoded): ");
            System.out.println(encryptedBase64);
            System.out.println();
            
            // Verify decryption - decrypt directly using Base64 ciphertext
            String decryptedStr = rsa.decryptStr(encryptedBase64, KeyType.PrivateKey);
            System.out.println("Decrypted String: " + decryptedStr);
            System.out.println("Encryption-Decryption Consistency Verification: " + originalStr.equals(decryptedStr));
        } catch (Exception e) {
            System.err.println("Encryption-Decryption Test Failed: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("=======================");
    }
}
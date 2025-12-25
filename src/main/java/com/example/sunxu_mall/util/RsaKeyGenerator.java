package com.example.sunxu_mall.util;

import cn.hutool.crypto.asymmetric.RSA;

/**
 * RSA密钥生成器
 * 用于生成RSA公钥和私钥对
 * 
 * @author sunxu
 * @version 1.0
 * @date 2025/12/25
 */
public class RsaKeyGenerator {
    
    /**
     * 生成RSA密钥对
     * @return 密钥对数组，index 0为私钥，index 1为公钥
     */
    public static String[] generateRsaKeys() {
        // 生成RSA密钥对
        RSA rsa = new RSA();
        
        // 获取私钥，格式为Base64编码
        String privateKey = rsa.getPrivateKeyBase64();
        
        // 获取公钥，格式为Base64编码
        String publicKey = rsa.getPublicKeyBase64();
        
        return new String[] { privateKey, publicKey };
    }
    
    /**
     * 直接生成并打印密钥对，用于快速获取密钥
     */
    public static void main(String[] args) {
        String[] keys = generateRsaKeys();
        
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
    }
}
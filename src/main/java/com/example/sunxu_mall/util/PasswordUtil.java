package com.example.sunxu_mall.util;

import cn.hutool.crypto.asymmetric.KeyType;
import com.example.sunxu_mall.entity.auth.AuthUserEntity;
import com.example.sunxu_mall.errorcode.ErrorCode;
import com.example.sunxu_mall.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import cn.hutool.crypto.asymmetric.RSA;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/24 20:06
 * @description
 */

@Slf4j
@Component
public class PasswordUtil {

    @Value("${mall.mgt.password.privateKey}")
    private String privateKey;

    @Value("${mall.mgt.password.publicKey}")
    private String publicKey;

    private final PasswordEncoder passwordEncoder;

    public PasswordUtil(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }


    public String encode(String password) {
        return passwordEncoder.encode(password);
    }

    /**
     * 解密 RSA密码
     *
     * @param password 密文
     * @return 明文
     */
    public String decodeRsaPassword(String password) {
        try {
            RSA rsa = new RSA(privateKey, publicKey);
            byte[] encryptedBytes = cn.hutool.core.codec.Base64.decode(password);
            return new String(rsa.decrypt(encryptedBytes, KeyType.PrivateKey));
        } catch (Exception e) {
            log.warn("decode rsa password error", e);
            throw new BusinessException(ErrorCode.DECRYPTION_FAILED.getCode(), ErrorCode.DECRYPTION_FAILED.getMessage());
        }
    }

    /**
     * 解密 RSA密码
     *
     * @param authUserEntity 用户实体
     * @return 密码
     */
    public String decodeRsaPassword(AuthUserEntity authUserEntity) {
        return decodeRsaPassword(authUserEntity.getPassword());
    }
}

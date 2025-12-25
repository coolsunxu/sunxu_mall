package com.example.sunxu_mall.util;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/24 19:44
 * @description
 */
public class CaptchaKeyUtil {

    private static final String CAPTCHA_PREFIX = "captcha:";

    /**
     * 设置存 redis的前缀
     * @param uuid 唯一标识
     * @return 返回存 redis的前缀
     */
    public static String getCaptchaKey(String uuid) {
        return String.format("%s%s", CAPTCHA_PREFIX, uuid);
    }
}

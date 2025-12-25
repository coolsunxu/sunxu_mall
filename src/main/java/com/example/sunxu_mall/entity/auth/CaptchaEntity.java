package com.example.sunxu_mall.entity.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/25 21:40
 * @description
 */


@AllArgsConstructor
@NoArgsConstructor
@Data
public class CaptchaEntity {
    /**
     * 唯一标识
     */
    private String uuid;

    /**
     * 验证码图片
     */
    private String img;
}

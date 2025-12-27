package com.example.sunxu_mall.dto.sys;

import lombok.Data;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/27 19:31
 * @description
 */

@Data
public class MetaDTO {

    /**
     * icon
     */
    private String icon;

    /**
     * 是否不缓存
     */
    private Boolean noCache;

    /**
     * 菜单标题
     */
    private String title;
}

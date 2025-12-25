package com.example.sunxu_mall.util;

import org.springframework.util.CollectionUtils;

import java.util.HashSet;
import java.util.Set;

import static com.example.sunxu_mall.handler.GlobalApiResultHandler.URL_PREFIX;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/25 20:46
 * @description
 */

public abstract class NoLoginMap {
    private static final Set<String> NO_LOGIN_URL_SET = new HashSet<>();

    private NoLoginMap() {

    }

    /**
     * 获取不需要登录 url集合
     *
     * @return 不需要登录 url集合
     */
    public static Set<String> getNoLoginUrlSet() {
        return NO_LOGIN_URL_SET;
    }

    /**
     * 初始化 set
     *
     * @param noLoginUrls url 集合
     */
    public static void initSet(Set<String> noLoginUrls) {
        if (CollectionUtils.isEmpty(noLoginUrls)) {
            return;
        }
        NO_LOGIN_URL_SET.addAll(noLoginUrls);
    }

    /**
     * 不存在该 url
     *
     * @param url url地址
     * @return 是否不存在
     */
    public static boolean notExist(String url) {
        if (!url.startsWith(URL_PREFIX)) {
            return false;
        }
        return !NO_LOGIN_URL_SET.contains(url);
    }
}

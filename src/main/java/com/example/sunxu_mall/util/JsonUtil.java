package com.example.sunxu_mall.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.ResponseBody;

import java.io.IOException;

/**
 * @author sunxu
 * @version 1.0
 * @date 2025/12/31 18:58
 * @description
 */

public class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static <T> T fromJson(String json, Class<T> clazz) throws IOException {
        return MAPPER.readValue(json, clazz);
    }

    public static String toJson(Object object) throws IOException {
        return MAPPER.writeValueAsString(object);
    }

}

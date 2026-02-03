package com.alibaba.aletheia.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * JSON 工具类
 *
 * @author Aletheia Team
 */
public final class JsonUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtil.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 对象转 JSON 字符串
     *
     * @param obj 对象
     * @return JSON 字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to convert object to JSON", e);
            return null;
        }
    }

    /**
     * JSON 字符串转对象
     *
     * @param json JSON 字符串
     * @param clazz 目标类型
     * @param <T> 泛型
     * @return 对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            LOGGER.error("Failed to parse JSON to object", e);
            return null;
        }
    }

    /**
     * 私有构造函数，防止实例化
     */
    private JsonUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }
}

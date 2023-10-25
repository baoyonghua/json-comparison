package com.myhexin.autotest.jsoncomparison.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.TextNode;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @author baoyh
 * @since 2023/6/22
 */
@Slf4j
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final JacksonRuntime JACKSON_RUNTIME = new JacksonRuntime();


    private JsonUtils() {

    }

    /**
     * 根据一个字符串来获取
     *
     * @param str 字符串
     * @return
     */
    public static JsonNode getJsonNode(String str) {
        try {
            return OBJECT_MAPPER.readTree(str);
        } catch (IOException e) {
            log.error("解析Json失败, 可能并不是Json类型的字符串！");
            return TextNode.valueOf(str);
        }
    }

    /**
     * 根据JmesPath来获取Json字符串下的value
     *
     * @param expr jmesPath的表达式
     * @return
     */
    public static JsonNode getJsonNodeByJmesPath(String jsonStr, String expr) {
        Expression<JsonNode> expression = JACKSON_RUNTIME.compile(expr);
        return expression.search(getJsonNode(jsonStr));
    }

    public static <T extends JsonNode> T castJsonNode(
            JsonNode jsonNode, JsonNodeType type, Class<T> clz
    ) {
        if (!jsonNode.getNodeType().equals(type)) {
            throw new IllegalArgumentException(
                    "所传入的JsonNode并不是预期的类型！" +
                            "当前类型: " + jsonNode.getNodeType() +
                            ", 预期类型: " + type);
        }
        return clz.cast(jsonNode);
    }

    public static String toJsonString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("解析JSON失败, 当前错误信息：{}", e.getMessage());
            return "";
        }
    }
}

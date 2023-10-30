package com.myhexin.autotest.jsoncomparison.utils;

import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.myhexin.autotest.jsoncomparison.compare.factory.JsonComparator;
import com.myhexin.autotest.jsoncomparison.config.JsonCompareConfig;
import io.burt.jmespath.Expression;
import io.burt.jmespath.jackson.JacksonRuntime;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;

/**
 * @author baoyh
 * @since 2023/6/22
 */
@Slf4j
public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final JacksonRuntime JACKSON_RUNTIME = new JacksonRuntime();

    private static final JsonSchemaFactory DEFAULT_JSON_SCHEMA_FACTORY = JsonSchemaFactory.byDefault();

    private static final String JSON_SCHEMA_ARRAY_STR = "array";

    private static final String JSON_SCHEMA_ITEMS = "items";

    private static final String JSON_SCHEMA_TYPE_FIELD_NAME = "type";

    private static final String JSON_SCHEMA_PROPERTIES_FIELD_NAME = "properties";

    private static final String FIELD_ENABLE_CONTRAST = "enable";

    private static final String FIELD_ALLOW_TOLERANT = "allow_tolerant";

    private static final String ARRAY_FIELD_DISORDER = "disorder";

    private static final String FILED_ALLOWED_MAPPING = "field_mapping";

    private static final String EMPTY_STRING = "";

    private static final String SCHEMA_TYPE_FIELD_NAME = "type";

    private JsonUtils() {

    }

    /**
     * 校验是否是合法的json字符串
     *
     * @param content 需要校验的内容
     * @return
     */
    public static boolean isValidJson(String content) {
        try {
            OBJECT_MAPPER.readTree(content);
            return true;
        } catch (IOException ignore) {
            log.warn("解析文本为Json失败, 当前字符串可能并不是Json字符串！");
            return false;
        }
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

    /**
     * 将对象转换为Json字符串
     *
     * @param obj
     * @return
     */
    public static String toJsonString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("解析JSON失败, 当前错误信息：{}", e.getMessage());
            return EMPTY_STRING;
        }
    }

    /**
     * 根据输入的JsonSchema字符串获取JsonSchema对象
     *
     * @param jsonSchema 代表jsonSchema的字符串
     * @return
     */
    public static Optional<JsonSchema> getStandardJsonSchema(String jsonSchema) {
        JsonNode jsonSchemaNode = getJsonNode(jsonSchema);
        return getStandardJsonSchema(jsonSchemaNode);
    }

    /**
     * 根据输入的JsonSchema字符串获取JsonSchema对象
     *
     * @param jsonSchema 代表jsonSchema的字符串
     * @return
     */
    public static Optional<JsonSchema> getStandardJsonSchema(JsonNode jsonSchema) {
        try {
            if (jsonSchema.has(SCHEMA_TYPE_FIELD_NAME) && jsonSchema.has(JSON_SCHEMA_PROPERTIES_FIELD_NAME)) {
                JsonSchema schema = DEFAULT_JSON_SCHEMA_FACTORY.getJsonSchema(jsonSchema);
                return Optional.of(schema);
            }
            return Optional.empty();
        } catch (Exception e) {
            // ignore
            log.warn("无法获取到JsonSchema实例, 可能输入的jsonSchema字符串不是合法的Json");
        }
        return Optional.empty();
    }

    /**
     * 判断当前的jsonSchema对象是否是合法的jsonSchema
     *
     * @param jsonSchema
     * @return
     */
    public static boolean isStandardJsonSchema(JsonNode jsonSchema) {
        return getStandardJsonSchema(jsonSchema).isPresent();
    }

    /**
     * 判断当前的jsonSchema字符串是否是合法的jsonSchema
     *
     * @param jsonSchema
     * @return
     */
    public static boolean isStandardJsonSchema(String jsonSchema) {
        return getStandardJsonSchema(jsonSchema).isPresent();
    }

    /**
     * 校验一个Json是否符合预期给定的JsonSchema
     *
     * @param jsonStr
     * @param jsonSchema
     * @return
     */
    public static boolean verifyJsonByJsonSchema(String jsonStr, String jsonSchema) {
        Optional<Boolean> isPassed = getStandardJsonSchema(jsonSchema).map(schema -> {
            try {
                ProcessingReport report = schema.validate(getJsonNode(jsonStr));
                boolean isSuccess = report.isSuccess();
                if (!isSuccess) {
                    StringBuilder builder = new StringBuilder("预期Json不符合JsonSchema, 不符合的点如下: \n");
                    report.forEach(processingMessage -> {
                        if (processingMessage.getLogLevel().compareTo(LogLevel.ERROR) == 0) {
                            JsonNode jsonNode = processingMessage.asJson();
                            builder.append(jsonNode.toString()).append(", \n");
                        }
                    });
                    log.debug(builder.toString());
                }
                return isSuccess;
            } catch (ProcessingException e) {
                log.warn("当前所输入的Json不符合预期的JsonSchema");
            }
            return false;
        });
        return isPassed.orElse(false);
    }

    /**
     * 根据自定义的JsonSchema信息来生成Json对比规则
     * <b>解析规则：通过解析JsonSchema中每个层级的properties属性下的字段是否存在自定义属性来生成自定义规则</b>
     *
     * <br>
     *
     * @param customizedJsonSchema 代表jsonSchema的字符串
     * @return
     */
    public static JsonCompareConfig parseCustomizedJsonSchema(String customizedJsonSchema) {
        Assert.notBlank(customizedJsonSchema, "所输入的jsonSchema并不是合法的！");
        JsonNode customizedJsonSchemaNode = getJsonNode(customizedJsonSchema);
        Assert.isTrue(isStandardJsonSchema(customizedJsonSchemaNode), "所输入的jsonSchema并不是合法的！");
        JsonCompareConfig compareConfig = new JsonCompareConfig();
        if (customizedJsonSchemaNode.getNodeType() == JsonNodeType.OBJECT) {
            parseCustomizedJsonSchema((ObjectNode) customizedJsonSchemaNode, compareConfig, JsonComparator.ROOT_PATH);
        }
        return compareConfig;
    }

    /**
     * 为JsonSchema添加自定义的规则信息
     *
     * @param originCustomizedJsonSchema
     * @param jsonSchema
     * @return
     */
    public static String addCustomRulesToJsonSchema(String originCustomizedJsonSchema, String jsonSchema) {
        JsonCompareConfig jsonCompareConfig = parseCustomizedJsonSchema(originCustomizedJsonSchema);
        return addCustomRulesToJsonSchema(jsonSchema, jsonCompareConfig);
    }

    /**
     * 从自定义的JsonSchema中获取到自定义的规则信息
     *
     * @param customizedJsonSchemaNode
     * @param compareConfig
     * @param path
     */
    @SuppressWarnings("all")
    private static void parseCustomizedJsonSchema(
            ObjectNode customizedJsonSchemaNode, JsonCompareConfig compareConfig, String path) {
        // 获取当前JsonSchema的properties字段的值
        Optional<ObjectNode> propertiesObjectNodeOptional = getPropertiesObjectNode(customizedJsonSchemaNode);
        propertiesObjectNodeOptional.ifPresent(propertiesObjectNode -> {
            Iterator<String> fieldNames = propertiesObjectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                // 从properties中获取到指定的字段信息
                JsonNode jsonNode = propertiesObjectNode.get(fieldName);
                if (jsonNode.getNodeType() != JsonNodeType.OBJECT) {
                    continue;
                }
                // 生成当前的路径
                String fieldType = jsonNode.get(JSON_SCHEMA_TYPE_FIELD_NAME).asText();
                String currentPath = getCurrentPath(path, fieldName, fieldType);
                if (getCustomizedRules(compareConfig, currentPath, jsonNode)) {
                    parseCustomizedJsonSchema((ObjectNode) jsonNode, compareConfig, currentPath);
                }
            }
        });
        // 如果当前JsonSchema的properties字段为空则说明该字段不是对象, 直接取enable等字段即可
        if (!propertiesObjectNodeOptional.isPresent()) {
            getCustomizedRules(compareConfig, path, customizedJsonSchemaNode);
        }
    }

    /**
     * 获取自定义的规则信息
     *
     * @param compareConfig
     * @param currentPath
     * @param jsonNode
     * @return boolean 是否需要继续查找此对象下的子对象的对比规则信息
     */
    private static boolean getCustomizedRules(JsonCompareConfig compareConfig, String currentPath, JsonNode jsonNode) {
        // 判断当前路径是否需要忽略对比
        Optional<String> ignorePathOptional = isIgnorePath(jsonNode, currentPath);
        ignorePathOptional.ifPresent(ignorePath -> compareConfig.getIgnorePath().add(ignorePath));
        if (ignorePathOptional.isPresent()) {
            return false;
        }
        if (isArrayNodePath(currentPath)) {
            // 判断当前路径是否配置了忽略数组顺序
            getArrayWithDisorder(jsonNode, currentPath).ifPresent(
                    config -> compareConfig.getArrayWithDisorderPath().add(config)
            );
        } else {
            // 判断当前路径是否配置了允许容差
            getAllowTolerant(jsonNode, currentPath).ifPresent(tolerantConfig ->
                    compareConfig.getTolerantPath().add(tolerantConfig)
            );
            // 判断当前路径是否配置了允许字段映射
            getAllowMappingKey(jsonNode, currentPath).ifPresent(
                    mappingKey -> compareConfig.getFieldMappings().add(mappingKey)
            );
        }
        return hasPropertiesObjectNode(jsonNode);
    }

    /**
     * 向JsonSchema对象中添加代表自定义的规则的字段
     *
     * @param jsonSchema
     * @param jsonCompareConfig
     * @return
     */
    private static String addCustomRulesToJsonSchema(String jsonSchema, JsonCompareConfig jsonCompareConfig) {
        // 遍历jsonSchema的每个节点, 查找当前路径是否配置了自定义规则
        JsonNode jsonSchemaNode = getJsonNode(jsonSchema);
        if (jsonSchemaNode.isObject()) {
            return addCustomRulesToJsonSchema((ObjectNode) jsonSchemaNode, jsonCompareConfig, JsonComparator.ROOT_PATH);
        }
        return jsonSchema;
    }

    /**
     * 向JsonSchema对象中添加代表自定义的规则的字段
     *
     * @param jsonSchemaNode
     * @param jsonCompareConfig
     * @param path
     * @return
     */
    @SuppressWarnings("all")
    private static String addCustomRulesToJsonSchema(
            ObjectNode jsonSchemaNode, JsonCompareConfig jsonCompareConfig, String path) {
        Optional<ObjectNode> propertiesObjectNodeOptional = getPropertiesObjectNode(jsonSchemaNode);
        propertiesObjectNodeOptional.ifPresent(propertiesObjectNode -> {
            Iterator<String> fieldNames = propertiesObjectNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode jsonNode = propertiesObjectNode.get(fieldName);
                if (jsonNode.getNodeType() != JsonNodeType.OBJECT) {
                    continue;
                }
                String fieldType = jsonNode.get(JSON_SCHEMA_TYPE_FIELD_NAME).asText();
                String currentPath = getCurrentPath(path, fieldName, fieldType);
                ObjectNode objectNode = (ObjectNode) jsonNode;
                addCustomRules(jsonCompareConfig, currentPath, objectNode);
                addCustomRulesToJsonSchema(objectNode, jsonCompareConfig, currentPath);
            }
        });
        // 如果propertiesObjectNode为空则说明该字段不是对象, 直接设置enable等字段即可
        if (!propertiesObjectNodeOptional.isPresent()) {
            addCustomRules(jsonCompareConfig, path, jsonSchemaNode);
        }
        return jsonSchemaNode.toString();
    }

    /**
     * 为某一个节点添加自定义规则
     *
     * @param jsonCompareConfig
     * @param currentPath
     * @param objectNode
     */
    private static void addCustomRules(JsonCompareConfig jsonCompareConfig, String currentPath, ObjectNode objectNode) {
        if (isArrayNodePath(currentPath)) {
            currentPath = replaceArrayLastReplacement(currentPath);
        }
        objectNode.put(FIELD_ENABLE_CONTRAST, !jsonCompareConfig.isIgnorePath(currentPath));
        JsonCompareConfig.TolerantConfig tolerantConfig = jsonCompareConfig.getTolerantConfig(currentPath);
        if (Objects.nonNull(tolerantConfig)) {
            objectNode.put(FIELD_ALLOW_TOLERANT, tolerantConfig.getTolerant());
        }
        String uniqueKey = jsonCompareConfig.getArrayWithDisorderUniqueKey(currentPath);
        if (CharSequenceUtil.isNotBlank(uniqueKey)) {
            objectNode.put(ARRAY_FIELD_DISORDER, uniqueKey);
        }
        JsonCompareConfig.FieldMapping mappingConfig = jsonCompareConfig.getMappingConfig(currentPath);
        if (Objects.nonNull(mappingConfig)) {
            objectNode.put(FILED_ALLOWED_MAPPING, mappingConfig.getMappingKey());
        }
    }

    /**
     * 判断当前路径是否是需要忽略对比的路径
     *
     * @param jsonNode
     * @param currentPath
     * @return
     */
    private static Optional<String> isIgnorePath(JsonNode jsonNode, String currentPath) {
        JsonNode node = jsonNode.get(FIELD_ENABLE_CONTRAST);
        if (Objects.nonNull(node) && !node.asBoolean(true)) {
            if (isArrayNodePath(currentPath)) {
                return Optional.of(replaceArrayLastReplacement(currentPath));
            }
            return Optional.of(currentPath);
        }
        return Optional.empty();
    }

    /**
     * 将数组路径最后的[*]剔除
     *
     * @param finalPath
     * @return
     */
    private static String replaceArrayLastReplacement(String finalPath) {
        return CharSequenceUtil.subPre(
                finalPath,
                finalPath.length() - JsonComparator.ARRAY_REPLACEMENT.length()
        );
    }

    /**
     * 判断当前路径是否设置了允许容差
     *
     * @param jsonNode
     * @param currentPath
     * @return 如果设置了忽略数组顺序则返回配置对象, {@link JsonCompareConfig.TolerantConfig}
     */
    private static Optional<JsonCompareConfig.TolerantConfig> getAllowTolerant(JsonNode jsonNode, String currentPath) {
        JsonNode tolerant = jsonNode.get(FIELD_ALLOW_TOLERANT);
        if (Objects.nonNull(tolerant)) {
            JsonCompareConfig.TolerantConfig tolerantConfig = new JsonCompareConfig.TolerantConfig();
            tolerantConfig.setPath(currentPath);
            tolerantConfig.setTolerant(tolerant.asText());
            return Optional.of(tolerantConfig);
        }
        return Optional.empty();
    }

    /**
     * 判断当前路径是否设置了字段映射
     *
     * @param jsonNode
     * @param currentPath
     * @return 如果设置了忽略数组顺序则返回配置对象, {@link JsonCompareConfig.FieldMapping}
     */
    private static Optional<JsonCompareConfig.FieldMapping> getAllowMappingKey(JsonNode jsonNode, String currentPath) {
        JsonNode mapping = jsonNode.get(FILED_ALLOWED_MAPPING);
        if (Objects.nonNull(mapping) && !mapping.isNull() && CharSequenceUtil.isNotBlank(mapping.asText())) {
            JsonCompareConfig.FieldMapping fieldMapping = new JsonCompareConfig.FieldMapping();
            fieldMapping.setPath(currentPath);
            fieldMapping.setMappingKey(mapping.asText());
            return Optional.of(fieldMapping);
        }
        return Optional.empty();
    }

    /**
     * 判断当前路径是否设置了忽略数组顺序
     *
     * @param jsonNode
     * @param currentPath
     * @return 如果设置了忽略数组顺序则返回配置对象, {@link JsonCompareConfig.ArrayWithDisorderConfig}
     */
    private static Optional<JsonCompareConfig.ArrayWithDisorderConfig> getArrayWithDisorder(
            JsonNode jsonNode, String currentPath) {
        JsonNode disorder = jsonNode.get(ARRAY_FIELD_DISORDER);
        if (Objects.nonNull(disorder)) {
            JsonCompareConfig.ArrayWithDisorderConfig config = new JsonCompareConfig.ArrayWithDisorderConfig();
            config.setPath(replaceArrayLastReplacement(currentPath));
            if (!disorder.isNull() && CharSequenceUtil.isNotBlank(disorder.asText())) {
                config.setUniqueKey(disorder.asText());
            }
            return Optional.of(config);
        }
        return Optional.empty();
    }

    /**
     * 判断当前路径是否是数组类型的路径
     *
     * @param currentPath
     * @return
     */
    private static boolean isArrayNodePath(String currentPath) {
        return currentPath.endsWith(JsonComparator.ARRAY_REPLACEMENT);
    }

    /**
     * 根据字段类型以及pre路径生成当前路径
     *
     * @param path
     * @param fieldName
     * @param fieldType
     * @return
     */
    private static String getCurrentPath(String path, String fieldName, String fieldType) {
        String currentPath;
        if (JSON_SCHEMA_ARRAY_STR.equals(fieldType)) {
            currentPath = path + JsonComparator.SPLIT_POINT + fieldName + JsonComparator.ARRAY_REPLACEMENT;
        } else {
            currentPath = path + JsonComparator.SPLIT_POINT + fieldName;
        }
        return currentPath;
    }

    /**
     * 获取JsonSchema中的properties字段所对应的值
     *
     * @param jsonSchemaNode
     * @return
     */
    private static Optional<ObjectNode> getPropertiesObjectNode(ObjectNode jsonSchemaNode) {
        if (jsonSchemaNode == null) {
            return Optional.empty();
        }
        if (JSON_SCHEMA_ARRAY_STR.equals(jsonSchemaNode.get(JSON_SCHEMA_TYPE_FIELD_NAME).asText())) {
            // 如果是array的话则properties字段在items字段下
            JsonNode jsonNode = jsonSchemaNode.get(JSON_SCHEMA_ITEMS);
            if (jsonNode != null && jsonNode.isObject()) {
                jsonSchemaNode = (ObjectNode) jsonNode;
            }
        }
        JsonNode properties = jsonSchemaNode.get(JSON_SCHEMA_PROPERTIES_FIELD_NAME);
        if (properties == null || !properties.isObject()) {
            return Optional.empty();
        }
        return Optional.of(((ObjectNode) properties));
    }

    /**
     * 获取JsonSchema中的properties字段所对应的值
     *
     * @param jsonSchemaNode
     * @return
     */
    private static boolean hasPropertiesObjectNode(JsonNode jsonSchemaNode) {
        if (jsonSchemaNode == null || !jsonSchemaNode.isObject()) {
            return false;
        }
        if (JSON_SCHEMA_ARRAY_STR.equals(jsonSchemaNode.get(JSON_SCHEMA_TYPE_FIELD_NAME).asText())) {
            return true;
        }
        JsonNode properties = jsonSchemaNode.get(JSON_SCHEMA_PROPERTIES_FIELD_NAME);
        return properties != null && !properties.isNull() && properties.isObject();
    }
}

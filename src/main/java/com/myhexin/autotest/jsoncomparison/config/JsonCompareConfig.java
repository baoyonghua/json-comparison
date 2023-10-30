package com.myhexin.autotest.jsoncomparison.config;


import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.myhexin.autotest.jsoncomparison.compare.factory.JsonComparator;
import lombok.Data;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * json对比配置
 *
 * @author baoyh
 * @since 2023/6/26
 */
@Data
public class JsonCompareConfig implements Serializable {

    /**
     * 需要忽略对比的字段路径集合, 以实际的路径为准
     */
    @JsonProperty("ignore_path")
    private Set<String> ignorePath;

    /**
     * 需要忽略顺序的数组对比路径集合 --> 支持乱序
     */
    @JsonProperty("array_with_disorder_path")
    private Set<ArrayWithDisorderConfig> arrayWithDisorderPath;

    /**
     * 需要进行Json转义进行对比的路径集合
     */
    @JsonProperty("escaped_json")
    private Set<EscapedJson> escapedJsonPath;

    /**
     * 支持字段映射
     */
    @JsonProperty("field_mappings")
    private Set<FieldMapping> fieldMappings;

    /**
     * 需要忽略容差的路径集合
     */
    @JsonProperty("tolerant_path")
    private Set<TolerantConfig> tolerantPath;

    public JsonCompareConfig() {
        ignorePath = new HashSet<>();
        arrayWithDisorderPath = new HashSet<>();
        escapedJsonPath = new HashSet<>();
        fieldMappings = new HashSet<>();
        tolerantPath = new HashSet<>();
    }

    /**
     * 是否是需要忽略的path
     *
     * @param currentPath 当前path
     * @return
     */
    @SuppressWarnings("all")
    public boolean isIgnorePath(String currentPath) {
        boolean isIgnore = Objects.nonNull(ignorePath) && !ignorePath.isEmpty()
                && ignorePath.contains(currentPath);
        if (!isIgnore) {
            // employees[*].skills 和 employees[1].skills 是等同的
            String finalPath = currentPath.replaceAll(JsonComparator.REGEX, JsonComparator.ARRAY_REPLACEMENT);
            return ignorePath.contains(finalPath);
        }
        return true;
    }

    public boolean isWithDisorderPath(String path) {
        Set<String> arrayWithDisorderPaths = arrayWithDisorderPath.stream()
                .map(ArrayWithDisorderConfig::getPath)
                .collect(Collectors.toSet());
        // 如果这里包含则直接返回
        if (arrayWithDisorderPaths.contains(path)) {
            return true;
        }
        // employees[*].skills 和 employees[1].skills 是等同的
        String finalPath = path.replaceAll(JsonComparator.REGEX, JsonComparator.ARRAY_REPLACEMENT);
        return arrayWithDisorderPaths.contains(finalPath);
    }

    public EscapedJson getEscapedJson(String path) {
        Optional<EscapedJson> escapedJsonOptional = escapedJsonPath.stream()
                .filter(e -> e.getPath().equals(path))
                .findFirst();
        return escapedJsonOptional.orElseGet(() -> {
            // employees[*].skills 和 employees[1].skills 是等同的
            String finalPath = path.replaceAll(JsonComparator.REGEX, JsonComparator.ARRAY_REPLACEMENT);
            return escapedJsonPath.stream()
                    .filter(e -> e.getPath().equals(finalPath))
                    .findFirst().orElse(null);
        });
    }

    public TolerantConfig getTolerantConfig(String path) {
        Optional<TolerantConfig> tolerantConfigOptional = tolerantPath.stream()
                .filter(e -> e.getPath().equals(path))
                .findFirst();
        return tolerantConfigOptional.orElseGet(() -> {
            // employees[*].skills 和 employees[1].skills 是等同的
            String finalPath = path.replaceAll(JsonComparator.REGEX, JsonComparator.ARRAY_REPLACEMENT);
            return tolerantPath.stream()
                    .filter(e -> e.getPath().equals(finalPath))
                    .findFirst().orElse(null);
        });
    }

    /**
     * 判断当前字段是否配置了key映射
     *
     * @param expectedFieldName
     * @param currentPath
     * @return
     */
    public boolean containsMappingKey(String expectedFieldName, String currentPath) {
        if (fieldMappings.isEmpty()) {
            return false;
        }
        String expectedCurrentPath = (currentPath + JsonComparator.SPLIT_POINT + expectedFieldName)
                .replaceAll(JsonComparator.REGEX, JsonComparator.ARRAY_REPLACEMENT);
        for (FieldMapping fieldMapping : fieldMappings) {
            List<String> split = CharSequenceUtil.split(fieldMapping.getPath(), JsonComparator.SPLIT_POINT);
            if (split.isEmpty()) {
                continue;
            }
            List<String> subList = split.subList(0, split.size() - 1);
            String path = CharSequenceUtil.join(JsonComparator.SPLIT_POINT, subList) +
                    JsonComparator.SPLIT_POINT + expectedFieldName;
            if (fieldMapping.getMappingKey().equals(expectedFieldName) && expectedCurrentPath.equals(path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 根据路径获取到当前路径所配置的唯一键
     *
     * @param path
     * @return
     */
    public String getArrayWithDisorderUniqueKey(String path) {
        if (arrayWithDisorderPath.isEmpty()) {
            return null;
        }
        for (ArrayWithDisorderConfig config : arrayWithDisorderPath) {
            if (config.getPath().equals(path)) {
                return config.getUniqueKey();
            }
        }
        return null;
    }

    /**
     * 根据路径获取到是否配置了键映射, 如果配置了则返回映射对象
     *
     * @param path
     * @return
     */
    public FieldMapping getMappingConfig(String path) {
        if (fieldMappings.isEmpty()) {
            return null;
        }
        Optional<FieldMapping> fieldMapping = fieldMappings.stream()
                .filter(e -> e.getPath().equals(path))
                .findFirst();
        return fieldMapping.orElseGet(() -> {
            // employees[*].skills 和 employees[1].skills 是等同的
            String finalPath = path.replaceAll(JsonComparator.REGEX, JsonComparator.ARRAY_REPLACEMENT);
            return fieldMappings.stream()
                    .filter(e -> e.getPath().equals(finalPath))
                    .findFirst().orElse(null);
        });
    }

    @Data
    public static class ArrayWithDisorderConfig implements Serializable {
        /**
         * 当前忽略数组顺序的唯一键
         */
        @JsonProperty("unique_key")
        private String uniqueKey;

        /**
         * 当前实际中的路径
         */
        private String path;


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ArrayWithDisorderConfig)) {
                return false;
            }
            ArrayWithDisorderConfig other = (ArrayWithDisorderConfig) o;
            return Objects.equals(path, other.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }

    @Data
    public static class TolerantConfig implements Serializable {

        /**
         * 当前实际的路径
         */
        private String path;

        /**
         * 容差
         */
        private String tolerant;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EscapedJson)) {
                return false;
            }
            EscapedJson other = (EscapedJson) o;
            return Objects.equals(path, other.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }

    @Data
    public static class EscapedJson extends JsonCompareConfig {
        /**
         * 当前实际中的json
         */
        private String path;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof EscapedJson)) {
                return false;
            }
            EscapedJson other = (EscapedJson) o;
            return Objects.equals(path, other.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }

    @Data
    public static class FieldMapping implements Serializable {

        /**
         * 当前实际的JsonPath
         */
        private String path;

        /**
         * 需要进行映射的key, 即在预期中与实际中相对应的key
         */
        @JsonProperty("mapping_key")
        private String mappingKey;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof FieldMapping)) {
                return false;
            }
            FieldMapping other = (FieldMapping) o;
            return Objects.equals(path, other.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }
}

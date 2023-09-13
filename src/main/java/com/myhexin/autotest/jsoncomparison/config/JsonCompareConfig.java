package com.myhexin.autotest.jsoncomparison.config;


import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * json对比配置
 *
 * @author baoyh
 * @since 2023/6/26
 */
@Data
public class JsonCompareConfig implements Serializable {

    /**
     * 需要忽略对比的字段路径集合
     */
    private Set<String> ignorePath = new HashSet<>();

    /**
     * 需要忽略顺序的数组对比路径集合 --> 支持乱序
     */
    private Set<ArrayWithDisorderConfig> arrayWithDisorderPath = new HashSet<>();

    /**
     * 需要进行Json转义进行对比的路径集合
     */
    private Set<ExcapedJson> excapedJsonPath = new HashSet<>();

    /**
     * todo 支持字段映射
     */
    private Set<FieldMapping> feildMappings = new HashSet<>();

    /**
     * 需要忽略容差的路径集合
     */
    private Set<ToleantConfig> toleantPath = new HashSet<>();

    /**
     * 根据路径获取到当前路径所配置的唯一键
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

    @Data
    @Builder
    public static class ArrayWithDisorderConfig implements Serializable {
        /**
         * 当前忽略数组顺序的唯一键
         */
        private String uniqueKey;

        /**
         * 当前忽略数组顺序的路径
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
    @Builder
    public static class ToleantConfig implements Serializable {

        /**
         * 路径
         */
        private String path;

        /**
         * 容差
         */
        private String toleant;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ExcapedJson)) {
                return false;
            }
            ExcapedJson other = (ExcapedJson) o;
            return Objects.equals(path, other.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }

    @Data
    @Builder
    public static class ExcapedJson extends JsonCompareConfig {
        private String path;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ExcapedJson)) {
                return false;
            }
            ExcapedJson other = (ExcapedJson) o;
            return Objects.equals(path, other.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(path);
        }
    }

    @Data
    @Builder
    public static class FieldMapping implements Serializable {

        /**
         * 当前实际的JsonPath
         */
        private String path;

        /**
         * 需要进行映射的key
         */
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

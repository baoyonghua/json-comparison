package com.myhexin.autotest.jsoncomparison.config;


import lombok.Data;

import java.io.Serializable;
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
    private Set<String> ignorePath;

    /**
     * 需要忽略顺序的数组对比路径集合 --> 支持乱序
     */
    private Set<String> arrayWithDisorderPath;

    /**
     * 需要进行Json转义进行对比的路径集合
     */
    private Set<JsonCompareConfig> excapedJsonPath;

    /**
     * 需要忽略容差的路径集合
     */
    private Set<ToleantConfig> toleantPath;

    @Data
    public static class ToleantConfig implements Serializable {

        /**
         * 路径
         */
        private String path;

        /**
         * 容差
         */
        private String toleant;
    }
}

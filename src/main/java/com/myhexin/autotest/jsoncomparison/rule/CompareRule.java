package com.myhexin.autotest.jsoncomparison.rule;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 对比规则
 *
 * @author baoyonghua@myhexin.com
 * @date 2023/6/21
 */
@Data
public class CompareRule {

    /**
     * 匹配器
     */
    private List<String> matcher;

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class JmesPath extends CompareRule {
        /**
         * JmesPath路径
         */
        private List<String> path;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class JsonPath extends CompareRule {
        /**
         * JsonPath路径
         */
        private List<String> path;
    }
}

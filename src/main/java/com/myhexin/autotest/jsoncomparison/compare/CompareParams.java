package com.myhexin.autotest.jsoncomparison.compare;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.myhexin.autotest.jsoncomparison.config.JsonCompareConfig;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * 进行Json对比时所必须的参数
 *
 * @author baoyh
 * @since 2023/6/21
 */
@Data
@Builder
public class CompareParams<T extends JsonNode> implements Serializable {

    /**
     * 对比Json时需要的配置信息
     */
    private JsonCompareConfig config;

    /**
     * 当前路径, 针对于实际的json而言
     */
    @JsonProperty("current_path")
    private String currentPath;

    /**
     * 预期的JSON
     */
    private T expected;

    /**
     * 实际的JSON
     */
    private T actual;

    public void setCurrentPath(String fieldName) {
        if (CharSequenceUtil.isBlank(fieldName)) {
            return;
        }
        if (CharSequenceUtil.isBlank(currentPath)) {
            currentPath = fieldName;
            return;
        }
        currentPath = currentPath + "." + fieldName;
    }
}

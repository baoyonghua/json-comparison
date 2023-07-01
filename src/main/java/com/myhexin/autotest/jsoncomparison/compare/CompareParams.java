package com.myhexin.autotest.jsoncomparison.compare;

import cn.hutool.core.text.CharSequenceUtil;
import com.myhexin.autotest.jsoncomparison.config.JsonCompareConfig;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Objects;

/**
 * 进行Json对比时所必须的参数
 *
 * @author baoyh
 * @since 2023/6/21
 */
@Data
@Builder
public class CompareParams<T> implements Serializable {

    /**
     * 对比Json时需要的配置信息
     */
    private JsonCompareConfig config;

    /**
     * 当前路径
     */
    private String currentPath;

    /**
     * 上一次的路径
     */
    private String prevPath;

    /**
     * 原始的预期json字符串
     */
    private String originalExcepetd;

    /**
     * 预期的结果
     */
    private T expected;

    /**
     * 实际的结果
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

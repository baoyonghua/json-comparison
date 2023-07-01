package com.myhexin.autotest.jsoncomparison.rule;

import lombok.Getter;

/**
 * 对比规则枚举
 * @author baoyonghua@myhexin.com
 * @date 2023/6/21
 */
@Getter
public enum CompareRuleEnum {
    /**
     * 对比规则枚举
     */
    BASIC(0, "基础的对比规则"),
    BLACK_LIST(1, "黑名单, 在黑名单下的路径将会进行忽略"),
    ARRAY(2, "数组对比规则"),
    ARRAY_WITH_DISORDER(3, "乱序数组, 忽略顺序对比"),
    EXCAPED_JSON(4, "转义Json规则, 转义后进行对比"),
    TOLERANT(5, "容差规则, 允许值存在一定误差"),
    ;

    private final Integer code;

    private final String msg;

    CompareRuleEnum(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }
}

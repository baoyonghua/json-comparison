package com.myhexin.autotest.jsoncomparison.compare.enums;

import lombok.Getter;

/**
 * @author baoyh
 * @since 2023/8/26
 */
@Getter
public enum DiffEnum {
    /**
     * 差异类型枚举
     */
    ONLY_IN_ACTUAL(0, "字段只在实际JSON中存在值"),
    ONLY_IN_EXPECTED(1, "字段只在预期JSON中存在值"),
    TYPE_UNEQUALS(2, "实际值与预期值的类型不符合"),
    VALUE_UNEQUALS(3, "实际值和预期值不一致"),
    VALUE_UNEQUALS_WITH_TOLERANT(4, "实际值和预期值在允许容差为{}的情况下仍不一致"),
    ESCAPED_COMPARE_NOT_EQUALS(5, "对Json串转义后进行对比存在差异"),
    EXPECTED_MISS_KEY(6, "字段只在实际中存在, 在预期中不存在"),
    ACTUAL_MISS_KEY(7, "字段只在预期中存在, 在实际中不存在"),
    LIST_LENGTH_NOT_EQUALS(8, "数组长度不一致"),
    DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_EXCEPTED(9, "针对于乱序数组, 在预期中未找到与实际相匹配的元素"),
    DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_ACTUAL(10,  "针对于乱序数组, 在预期中未找到与实际相匹配的元素"),
    ;

    private final Integer type;

    private final String msg;

    DiffEnum(Integer type, String msg) {
        this.type = type;
        this.msg = msg;
    }

}

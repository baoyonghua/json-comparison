package com.myhexin.autotest.jsoncomparison.compare.constant;

/**
 * @author baoyh
 * @since 2023/6/25
 */
public final class CompareMessageConstant {

    public static final String ONLY_IN_ACTUAL = "字段只在实际中存在，在实际中该字段的值: [%s], 在预期中该字段的值: [null]";

    public static final String ONLY_IN_EXPECTED = "字段只在预期中存在, 在预期中该字段的值: [%s]，在实际中该字段的值: [null]";

    public static final String TYPE_UNEQUALS = "实际值与预期值的类型不符合, 实际值类型: [%s], 预期值类型: [%s]";

    public static final String VALUE_UNEQUALS = "实际值和预期值不一致, 实际值: [%s], 预期值: [%s]";

    public static final String VALUE_NOTEQUALS_WITH_TOLEANT =
            "实际值与预期值在容差为[%s]下仍然不一致, 实际值: [%s], 预期值: [%s]";

    public static final String EXCAPED_COMPARE_NOT_EQUALS = "对Json串转义后进行对比存在差异！";

    public static final String EXPECTED_MISS_KEY = "该键[%s]只在预期中存在, 在实际中不存在此键";

    public static final String ACTUAL_MISS_KEY = "该键[%s]只在实际中存在, 在预期中不存在此键";

    public static final String LIST_LENGTH_NOT_EQUALS =
            "实际与预期的列表长度不一致, 实际Json的列表长度: [%s], 预期Json的列表长度: [%s]";

    public static final String DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_EXCEPTED =
            "针对于乱序的数组, 在预期中未找到与实际相匹配的元素！";

}

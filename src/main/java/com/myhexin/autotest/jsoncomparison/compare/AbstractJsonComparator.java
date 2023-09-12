package com.myhexin.autotest.jsoncomparison.compare;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
import com.myhexin.autotest.jsoncomparison.compare.enums.DiffEnum;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import lombok.extern.slf4j.Slf4j;


/**
 * JSON对比器抽象实现
 *
 * @author baoyh
 * @since 2023/6/21
 */
@Slf4j
public abstract class AbstractJsonComparator<T extends JsonNode> implements JsonComparator<T> {

    @Override
    public void beforeCompare(CompareParams<T> params) {
        if (CharSequenceUtil.isBlank(params.getCurrentPath())) {
            params.setCurrentPath(JsonComparator.ROOT_PATH);
        }
    }

    /**
     * 校验两个JsonType的类型是否一致
     *
     * @param path     当前路径
     * @param actual   实际的Json
     * @param expected 预期的Json
     * @return
     */
    protected BriefDiffResult.BriefDiff checkJsonNodeType(
            String path, JsonNode actual, JsonNode expected) {
        // 如果俩个jsonNode对象的type不一致则需要校验
        if (actual.getNodeType() != expected.getNodeType()) {
            String reason;
            DiffEnum diffEnum;
            if (actual.getNodeType() == JsonNodeType.NULL) {
                reason = String.format(CompareMessageConstant.ONLY_IN_EXPECTED, expected.asText());
                diffEnum = DiffEnum.ONLY_IN_EXPECTED;
            } else if (expected.getNodeType() == JsonNodeType.NULL) {
                reason = String.format(CompareMessageConstant.ONLY_IN_ACTUAL, actual.asText());
                diffEnum = DiffEnum.ONLY_IN_ACTUAL;
            } else {
                reason = String.format(
                        CompareMessageConstant.TYPE_UNEQUALS, actual.getNodeType(), expected.getNodeType()
                );
                diffEnum = DiffEnum.TYPE_UNEQUALS;
            }
            return BriefDiffResult.BriefDiff.builder()
                    .actual(actual.asText())
                    .expected(expected.asText())
                    .type(diffEnum.getType())
                    .msg(diffEnum.getMsg())
                    .diffKey(path)
                    .reason(reason)
                    .build();
        }
        return null;
    }

    @Override
    public void afterCompare() {

    }
}

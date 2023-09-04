package com.myhexin.autotest.jsoncomparison.compare;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
import com.myhexin.autotest.jsoncomparison.compare.enums.DiffEnum;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import com.myhexin.autotest.jsoncomparison.utils.JsonUtils;
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
            Integer type;
            if (actual.getNodeType() == JsonNodeType.NULL) {
                reason = String.format(CompareMessageConstant.ONLY_IN_EXPECTED, expected.asText());
                type = DiffEnum.ONLY_IN_EXPECTED.getType();
            } else if (expected.getNodeType() == JsonNodeType.NULL) {
                reason = String.format(CompareMessageConstant.ONLY_IN_ACTUAL, actual.asText());
                type = DiffEnum.ONLY_IN_ACTUAL.getType();
            } else {
                reason = String.format(
                        CompareMessageConstant.TYPE_UNEQUALS, actual.getNodeType(), expected.getNodeType(),
                        actual, expected
                );
                type = DiffEnum.TYPE_UNEQUALS.getType();
            }
            return BriefDiffResult.BriefDiff.builder()
                    .actual(actual.asText())
                    .expected(expected.asText())
                    .type(type)
                    .diffKey(path)
                    .reason(reason)
                    .build();
        }
        return null;
    }

    @Override
    public void afterCompare() {

    }

    /**
     * 解析Json并根据path来获取具体JsonNode
     *
     * @param jsonStr json字符串
     * @param path    路径
     * @return
     */
    protected JsonNode getJsonNodeByPath(String jsonStr, String path) {
        if (CharSequenceUtil.isBlank(path)) {
            return JsonUtils.getJsonNode(jsonStr);
        }
        String jmesPath = JsonComparator.cropPath2JmesPath(path);
        if (CharSequenceUtil.isBlank(jmesPath)) {
            return JsonUtils.getJsonNode(jsonStr);
        }
        return JsonUtils.getJsonNodeByJmesPath(jsonStr, jmesPath);
    }
}

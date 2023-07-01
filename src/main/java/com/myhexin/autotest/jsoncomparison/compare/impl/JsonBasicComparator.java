package com.myhexin.autotest.jsoncomparison.compare.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.myhexin.autotest.jsoncomparison.compare.AbstractJsonComparator;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 基础的JSON对比器，提供默认的对比规则
 * <t>提供基础的对比能力, 对比普通类型{number, string, boolean, null}</t>
 *
 * @author baoyh
 * @since 2023/6/21
 */
@Slf4j
public class JsonBasicComparator extends AbstractJsonComparator<JsonNode> {

    @Override
    public List<BriefDiffResult.BriefDiff> compare(CompareParams<JsonNode> params) {
        if (isIgnorePath(params.getCurrentPath(), params.getConfig().getIgnorePath())) {
            return Collections.emptyList();
        }

        JsonNode actual = params.getActual();
        JsonNode expected = params.getExpected();
        List<BriefDiffResult.BriefDiff> diffs = checkType(params, actual, expected);
        if (!diffs.isEmpty()) {
            return diffs;
        }
        boolean pass = false;
        switch (actual.getNodeType()) {
            case NULL:
                pass = true;
                break;
            case NUMBER:
                pass = (actual.asLong() == expected.asLong());
                break;
            case STRING:
            case BINARY:
                pass = (actual.asText().equals(expected.asText()));
                break;
            case BOOLEAN:
                pass = (actual.asBoolean() == expected.asBoolean());
                break;
            default:
                break;
        }
        if (!pass) {
            String reason = CompareMessageConstant.UNEQUALS;
            BriefDiffResult.BriefDiff diff = BriefDiffResult.BriefDiff.builder()
                    .actual(actual.asText())
                    .expected(expected.asText())
                    .diffKey(params.getCurrentPath())
                    .reason(reason)
                    .build();
            return Collections.singletonList(diff);
        }
        return Collections.emptyList();
    }

    private List<BriefDiffResult.BriefDiff> checkType(CompareParams<JsonNode> params, JsonNode actual, JsonNode expected) {
        if (actual.getNodeType() != expected.getNodeType()) {
            String reason;
            if (actual.getNodeType() == JsonNodeType.NULL) {
                reason = CompareMessageConstant.ONLY_IN_EXPECTED;
            } else if (expected.getNodeType() == JsonNodeType.NULL) {
                reason = CompareMessageConstant.ONLY_IN_ACTUAL;
            } else {
                reason = CompareMessageConstant.UNEQUALS;
            }
            BriefDiffResult.BriefDiff diff = BriefDiffResult.BriefDiff.builder()
                    .actual(actual.asText())
                    .expected(expected.asText())
                    .diffKey(params.getCurrentPath())
                    .reason(reason)
                    .build();
            return Collections.singletonList(diff);
        }
        return Collections.emptyList();
    }

    /**
     * 如果节点不属于数组、对象则默认属于普通类型
     *
     * @param node json中的某一个节点
     * @return
     */
    @Override
    protected boolean check(JsonNode node) {
        List<JsonNodeType> nodeTypes =
                Arrays.asList(JsonNodeType.ARRAY, JsonNodeType.OBJECT, JsonNodeType.POJO);
        return !nodeTypes.contains(node.getNodeType());
    }

    @Override
    public String toString() {
        return "Json基础类型数据对比器";
    }
}

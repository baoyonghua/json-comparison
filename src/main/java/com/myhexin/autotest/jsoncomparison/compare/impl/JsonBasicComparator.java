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
import java.util.Objects;

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
        JsonNode actual = params.getActual();
        JsonNode expected = params.getExpected();
        BriefDiffResult.BriefDiff diff = checkJsonNodeType(params.getCurrentPath(), actual, expected);
        if (Objects.nonNull(diff)) {
            return Collections.singletonList(diff);
        }
        boolean pass = false;
        String actualText = actual.asText();
        String expectedText = expected.asText();
        switch (actual.getNodeType()) {
            // 如果实际为null则预期也一定为null
            case NULL:
                pass = true;
                break;
            case NUMBER:
                pass = (actual.asLong() == expected.asLong());
                break;
            case STRING:
            case BINARY:
                pass = (actualText.equals(expectedText));
                break;
            case BOOLEAN:
                pass = (actual.asBoolean() == expected.asBoolean());
                break;
            default:
                break;
        }
        if (!pass) {
            diff = BriefDiffResult.BriefDiff.builder()
                    .actual(actualText)
                    .expected(expectedText)
                    .diffKey(params.getCurrentPath())
                    .reason(String.format(
                            CompareMessageConstant.VALUE_UNEQUALS,
                            actual,
                            expected)
                    )
                    .build();
            return Collections.singletonList(diff);
        }
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "Json基础类型数据对比器";
    }
}

package com.myhexin.autotest.jsoncomparison.compare.factory.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
import com.myhexin.autotest.jsoncomparison.compare.enums.DiffEnum;
import com.myhexin.autotest.jsoncomparison.config.JsonCompareConfig;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;

/**
 * 基础的JSON对比器，提供默认的对比规则
 * 提供基础的对比能力, 对比普通类型{number, string, boolean, null, ...}
 *
 * @author baoyh
 * @since 2023/6/21
 */
@Slf4j
public class JsonBasicComparator extends AbstractJsonComparator<JsonNode> {

    @Override
    public BriefDiffResult compare(CompareParams<JsonNode> params) {
        BriefDiffResult result = new BriefDiffResult();
        JsonNode actual = params.getActual();
        JsonNode expected = params.getExpected();
        String actualText = actual.asText();
        String expectedText = expected.asText();
        String currentPath = params.getCurrentPath();
        boolean pass = false;
        switch (actual.getNodeType()) {
            // 如果实际为null则预期也一定为null
            case NULL:
                pass = true;
                break;
            case NUMBER:
                JsonCompareConfig.TolerantConfig tolerantConfig = params.getConfig().getTolerantConfig(currentPath);
                if (Objects.nonNull(tolerantConfig)) {
                    return tolerantCompare(params, tolerantConfig, actualText, expectedText).orElse(result);
                } else {
                    BigDecimal actualNumber = new BigDecimal(actualText);
                    BigDecimal expectedNumber = new BigDecimal(expectedText);
                    pass = actualNumber.compareTo(expectedNumber) == 0;
                    break;
                }
            case STRING:
            case BINARY:
                JsonCompareConfig.EscapedJson escapedJson = params.getConfig().getEscapedJson(currentPath);
                if (Objects.nonNull(escapedJson)) {
                    return escapedJsonCompare(params, escapedJson, actualText, expectedText).orElse(result);
                } else {
                    pass = actualText.equals(expectedText);
                    break;
                }
            case BOOLEAN:
                pass = actual.asBoolean() == expected.asBoolean();
                break;
            default:
                log.warn("未知的类型, 当前实际Json与预期Json的类型为: {}", actual.getNodeType());
                break;
        }
        if (!pass) {
            result.getBriefDiffs().add(buildValueUnEqualsDiff(currentPath, actualText, expectedText));
            result.setChildExpectedJson(expected);
            result.setChildActualJson(actual);
        }
        return result;
    }

    private BriefDiffResult.BriefDiff buildValueUnEqualsDiff(String currentPath, String actualText, String expectedText) {
        BriefDiffResult.BriefDiff diff;
        diff = BriefDiffResult.BriefDiff.builder()
                .actual(actualText)
                .expected(expectedText)
                .diffKey(currentPath)
                .type(DiffEnum.VALUE_UNEQUALS.getType())
                .msg(DiffEnum.VALUE_UNEQUALS.getMsg())
                .reason(String.format(
                        CompareMessageConstant.VALUE_UNEQUALS,
                        actualText,
                        expectedText)
                ).build();
        return diff;
    }

    @Override
    public String toString() {
        return "Json基础类型数据对比器";
    }
}

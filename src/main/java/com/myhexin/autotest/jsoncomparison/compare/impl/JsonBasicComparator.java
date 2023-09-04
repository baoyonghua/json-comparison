package com.myhexin.autotest.jsoncomparison.compare.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.myhexin.autotest.jsoncomparison.compare.AbstractJsonComparator;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
import com.myhexin.autotest.jsoncomparison.compare.enums.DiffEnum;
import com.myhexin.autotest.jsoncomparison.compare.factory.JsonComparatorFactory;
import com.myhexin.autotest.jsoncomparison.config.JsonCompareConfig;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import com.myhexin.autotest.jsoncomparison.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.*;

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
    public BriefDiffResult compare(CompareParams<JsonNode> params) {
        BriefDiffResult result = new BriefDiffResult();
        JsonNode actual = params.getActual();
        JsonNode expected = params.getExpected();
        String actualText = actual.asText();
        String expectedText = expected.asText();
        BriefDiffResult.BriefDiff diff =
                checkJsonNodeType(params.getCurrentPath(), actual, expected);
        if (Objects.nonNull(diff)) {
            result.getBriefDiffs().add(diff);
            return result;
        }
        boolean pass = false;
        switch (actual.getNodeType()) {
            // 如果实际为null则预期也一定为null
            case NULL:
                pass = true;
                break;
            case NUMBER:
                Optional<BriefDiffResult.BriefDiff> optional = toleantCompare(params, actual, expected);
                if (optional.isPresent()) {
                    result.getBriefDiffs().add(optional.get());
                    return result;
                } else {
                    BigDecimal actualNumber = new BigDecimal(actualText);
                    BigDecimal expectedNumber = new BigDecimal(expectedText);
                    pass = actualNumber.compareTo(expectedNumber) == 0;
                    break;
                }
            case STRING:
            case BINARY:
                Optional<BriefDiffResult.BriefDiff> diffOptional =
                        excapedJsonCompare(params, actualText, expectedText);
                if (diffOptional.isPresent()) {
                    result.getBriefDiffs().add(diffOptional.get());
                    return result;
                } else {
                    pass = (actualText.equals(expectedText));
                    break;
                }
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
                    .type(DiffEnum.VALUE_UNEQUALS.getType())
                    .reason(String.format(
                            CompareMessageConstant.VALUE_UNEQUALS,
                            actual,
                            expected)
                    ).build();
                    result.getBriefDiffs().add(diff);
                    return result;
        }
        return null;
    }

    /**
     * 支持容差的对比
     *
     * @param params
     * @param actual
     * @param expected
     * @return
     */
    private Optional<BriefDiffResult.BriefDiff> toleantCompare(
            CompareParams<JsonNode> params,
            JsonNode actual,
            JsonNode expected
    ) {
        String actualText = actual.asText();
        String expectedText = expected.asText();
        BigDecimal actualNumber = new BigDecimal(actualText);
        BigDecimal expectedNumber = new BigDecimal(expectedText);
        for (JsonCompareConfig.ToleantConfig toleantConfig : params.getConfig().getToleantPath()) {
            if (toleantConfig.getPath().equals(params.getCurrentPath())) {
                BigDecimal toleant = new BigDecimal(toleantConfig.getToleant());
                BigDecimal max = actualNumber.add(toleant);
                BigDecimal min = actualNumber.subtract(toleant);
                if (max.compareTo(expectedNumber) < 0 || min.compareTo(expectedNumber) > 0) {
                    BriefDiffResult.BriefDiff diff = BriefDiffResult.BriefDiff.builder()
                            .actual(actualText)
                            .expected(expectedText)
                            .type(DiffEnum.VALUE_UNEQUALS.getType())
                            .diffKey(params.getCurrentPath())
                            .reason(String.format(
                                    CompareMessageConstant.VALUE_NOTEQUALS_WITH_TOLEANT,
                                    toleant.toString(),
                                    actual,
                                    expected)
                            ).build();
                    return Optional.of(diff);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<BriefDiffResult.BriefDiff> excapedJsonCompare(
            CompareParams<JsonNode> params,
            String actualText,
            String expectedText
    ) {
        Set<JsonCompareConfig.ExcapedJson> excapedJsons = params.getConfig().getExcapedJsonPath();
        for (JsonCompareConfig.ExcapedJson excapedJson : excapedJsons) {
            if (excapedJson.getPath().equals(params.getCurrentPath())) {
                // 需要进行转义对比的字符串
                JsonNode jsonNode1 = JsonUtils.getJsonNode(actualText);
                JsonNode jsonNode2 = JsonUtils.getJsonNode(expectedText);
                CompareParams<JsonNode> compareParams = CompareParams.<JsonNode>builder()
                        .actual(jsonNode1)
                        .expected(jsonNode2)
                        .config(excapedJson)
                        .build();
                BriefDiffResult diffResult = JsonComparatorFactory.build()
                        .executeContrast(jsonNode1.getNodeType(), compareParams);
                if (diffResult != null) {
                    BriefDiffResult.BriefDiff diff = BriefDiffResult.BriefDiff.builder()
                            .actual(actualText)
                            .expected(expectedText)
                            .type(DiffEnum.EXCAPED_COMPARE_NOT_EQUALS.getType())
                            .diffKey(params.getCurrentPath())
                            .reason(CompareMessageConstant.EXCAPED_COMPARE_NOT_EQUALS)
                            .subDiffs(diffResult.getBriefDiffs())
                            .build();
                    return Optional.of(diff);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return "Json基础类型数据对比器";
    }
}

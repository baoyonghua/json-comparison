package com.myhexin.autotest.jsoncomparison.compare;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
import com.myhexin.autotest.jsoncomparison.compare.enums.DiffEnum;
import com.myhexin.autotest.jsoncomparison.compare.factory.JsonComparatorFactory;
import com.myhexin.autotest.jsoncomparison.config.JsonCompareConfig;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import com.myhexin.autotest.jsoncomparison.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * JSON对比器抽象实现
 *
 * @author baoyh
 * @since 2023/6/21
 */
@Slf4j
public abstract class AbstractJsonComparator<T extends JsonNode> implements JsonComparator<T> {

    /**
     * Json对比器工厂
     */
    protected static final JsonComparatorFactory COMPARATOR_FACTORY = JsonComparatorFactory.build();

    /**
     * 默认的序列化方式
     */
    protected static final DefaultSerializerProvider.Impl SERIALIZER_PROVIDER = new DefaultSerializerProvider.Impl();

    @Override
    public boolean beforeCompare(CompareParams<T> params, BriefDiffResult result) {
        if (CharSequenceUtil.isBlank(params.getCurrentPath())) {
            params.setCurrentPath(ROOT_PATH);
        } else if (!params.getCurrentPath().startsWith(JsonComparator.ROOT_PATH)) {
            params.setCurrentPath(ROOT_PATH + SPLIT_POINT + params.getCurrentPath());
        }
        if (JsonComparator.isIgnorePath(params.getCurrentPath(), params.getConfig().getIgnorePath())) {
            log.info("当前路径{}配置了无需对比", params.getCurrentPath());
            return false;
        }
        return isSameJsonNodeType(params, result);
    }

    @Override
    public void afterCompare(BriefDiffResult result) {
        result.setDiffNum(result.getBriefDiffs().size());
    }

    /**
     * 校验两个JsonType的类型是否一致
     *
     * @param path     当前路径
     * @param actual   实际的Json
     * @param expected 预期的Json
     * @return 如果类型不一致则返回差异信息, 否则为null
     */
    protected Optional<BriefDiffResult.BriefDiff> checkJsonNodeType(String path, JsonNode actual, JsonNode expected) {
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
            return Optional.of(
                    BriefDiffResult.BriefDiff.builder()
                            .actual(actual.asText())
                            .expected(expected.asText())
                            .type(diffEnum.getType())
                            .msg(diffEnum.getMsg())
                            .diffKey(path)
                            .reason(reason)
                            .build()
            );
        }
        return Optional.empty();
    }

    /**
     * 校验是否是两个json对象是否是相同的类型
     *
     * @param params 对比参数
     * @param result 结果对象
     * @param <N>
     * @return true: 是相同的 false: 不是相同的
     */
    protected <N extends JsonNode> boolean isSameJsonNodeType(CompareParams<N> params, BriefDiffResult result) {
        JsonNode expected = params.getExpected();
        JsonNode actual = params.getActual();
        Optional<BriefDiffResult.BriefDiff> optional = checkJsonNodeType(params.getCurrentPath(), actual, expected);
        optional.ifPresent(
                diff -> {
                    result.getBriefDiffs().add(diff);
                    result.setChildExpectedJson(expected);
                    result.setChildActualJson(actual);
                }
        );
        return !optional.isPresent();
    }

    /**
     * 支持容差的对比
     *
     * @param params
     * @param actualText
     * @param expectedText
     * @return
     */
    protected Optional<BriefDiffResult.BriefDiff> tolerantCompare(
            CompareParams<JsonNode> params,
            String actualText,
            String expectedText
    ) {
        BigDecimal actualNumber = new BigDecimal(actualText);
        BigDecimal expectedNumber = new BigDecimal(expectedText);
        String currentPath = params.getCurrentPath();
        for (JsonCompareConfig.TolerantConfig tolerantConfig : params.getConfig().getTolerantPath()) {
            String tolerantConfigPath = tolerantConfig.getPath();
            if (tolerantConfigPath.equals(currentPath)
                    || tolerantConfigPath.replaceAll(REGEX, ARRAY_REPLACEMENT).equals(currentPath)) {
                log.debug("当前路径[{}]配置了允许容差, 允许的范围为: {}", currentPath, tolerantConfig.getTolerant());
                BigDecimal tolerant = new BigDecimal(tolerantConfig.getTolerant());
                BigDecimal max = actualNumber.add(tolerant);
                BigDecimal min = actualNumber.subtract(tolerant);
                if (max.compareTo(expectedNumber) < 0 || min.compareTo(expectedNumber) > 0) {
                    BriefDiffResult.BriefDiff diff =
                            buildValueUnEqualsWithTolerant(currentPath, actualText, expectedText, tolerant);
                    return Optional.of(diff);
                }
            }
        }
        return Optional.empty();
    }

    /**
     * 支持对转移的Json进行反转义后进行对比
     *
     * @param params       对比参数
     * @param actualText   实际的转义json
     * @param expectedText 预期的转义json
     * @return
     */
    protected Optional<BriefDiffResult.BriefDiff> escapedJsonCompare(
            CompareParams<JsonNode> params,
            String actualText,
            String expectedText
    ) {
        Set<JsonCompareConfig.EscapedJson> escapedJsons = params.getConfig().getEscapedJsonPath();
        String currentPath = params.getCurrentPath();
        for (JsonCompareConfig.EscapedJson escapedJson : escapedJsons) {
            String escapedJsonPath = escapedJson.getPath();
            if (escapedJsonPath.equals(currentPath)
                    || escapedJsonPath.replaceAll(REGEX, ARRAY_REPLACEMENT).equals(currentPath)) {
                // 需要进行转义对比的字符串
                JsonNode jsonNode1 = JsonUtils.getJsonNode(actualText);
                JsonNode jsonNode2 = JsonUtils.getJsonNode(expectedText);
                CompareParams<JsonNode> compareParams = CompareParams.builder()
                        .actual(jsonNode1)
                        .expected(jsonNode2)
                        .config(escapedJson)
                        .build();
                BriefDiffResult diffResult = COMPARATOR_FACTORY.executeContrast(jsonNode1.getNodeType(), compareParams);
                if (diffResult != null && !diffResult.getBriefDiffs().isEmpty()) {
                    return Optional.of(buildEscapedCompareNotEqualsDiff(
                            currentPath, actualText, expectedText, diffResult.getBriefDiffs()
                    ));
                }
            }
        }
        return Optional.empty();
    }

    private BriefDiffResult.BriefDiff buildEscapedCompareNotEqualsDiff(
            String currentPath, String actualText, String expectedText, List<BriefDiffResult.BriefDiff> subDiffs) {
        return BriefDiffResult.BriefDiff.builder()
                .actual(actualText)
                .expected(expectedText)
                .type(DiffEnum.ESCAPED_COMPARE_NOT_EQUALS.getType())
                .msg(DiffEnum.ESCAPED_COMPARE_NOT_EQUALS.getMsg())
                .diffKey(currentPath)
                .reason(CompareMessageConstant.EXCAPED_COMPARE_NOT_EQUALS)
                .subDiffs(subDiffs)
                .build();
    }

    private BriefDiffResult.BriefDiff buildValueUnEqualsWithTolerant(
            String currentPath, String actualText, String expectedText, BigDecimal tolerant
    ) {
        return BriefDiffResult.BriefDiff.builder()
                .actual(actualText)
                .expected(expectedText)
                .type(DiffEnum.VALUE_UNEQUALS_WITH_TOLERANT.getType())
                .msg(CharSequenceUtil.format(DiffEnum.VALUE_UNEQUALS_WITH_TOLERANT.getMsg(), tolerant))
                .diffKey(currentPath)
                .reason(String.format(
                        CompareMessageConstant.VALUE_NOTEQUALS_WITH_TOLEANT,
                        tolerant,
                        actualText,
                        expectedText)
                ).build();
    }
}

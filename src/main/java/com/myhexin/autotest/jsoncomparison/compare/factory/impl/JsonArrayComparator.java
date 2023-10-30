package com.myhexin.autotest.jsoncomparison.compare.factory.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myhexin.autotest.jsoncomparison.compare.factory.AbstractJsonComparator;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
import com.myhexin.autotest.jsoncomparison.compare.enums.DiffEnum;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import lombok.extern.slf4j.Slf4j;

import java.util.*;


/**
 * 数组JSON对比器实现
 *
 * @author baoyh
 * @since 2023/6/21
 */
@Slf4j
public class JsonArrayComparator extends AbstractJsonComparator<ArrayNode> {

    private static final String UNIQUE_KEY_REASON_TEMP = "当前唯一键[{}][{}], {}";
    private static final String UNIQUE_KEY_INDEX_TEMP = "当前唯一键[{}][{}], 下标为[{}]";
    private static final String UNIQUE_KEY_NOT_EXIST_TEMP = "当前唯一键[{}][{}]不存在";
    private static final String INDEX_TEMP = "下标为{}";
    private static final String PATH_TEMP = "{}[{}]";

    @Override
    public BriefDiffResult compare(CompareParams<ArrayNode> params) {
        BriefDiffResult result = new BriefDiffResult();
        int actualSize = params.getActual().size();
        int expectedSize = params.getExpected().size();
        Boolean isActualArrayLonger = null;
        if (actualSize != expectedSize) {
            isActualArrayLonger = actualSize > expectedSize;
            result.getBriefDiffs().add(buildLengthNotEqualDiff(params.getCurrentPath(), actualSize, expectedSize));
        }
        int size = Math.min(actualSize, expectedSize);
        if (size == 0) {
            ArrayNode childActualJson = JsonNodeFactory.instance.arrayNode().addAll(params.getActual());
            ArrayNode childExpectedJson = JsonNodeFactory.instance.arrayNode().addAll(params.getExpected());
            result.setChildActualJson(childActualJson);
            result.setChildExpectedJson(childExpectedJson);
            return result;
        }
        if (params.getConfig().isWithDisorderPath(params.getCurrentPath())) {
            log.debug("当前路径[{}]配置了支持乱序的数组对比...", params.getCurrentPath());
            compareWithDisorderArray(params, result);
        } else {
            compareArray(params, result, size);
            if (Objects.isNull(isActualArrayLonger)) {
                return result;
            }
            // 如果存在数组长度不一致, 则将多的元素添加到子差异json中
            if (Boolean.TRUE.equals(isActualArrayLonger)) {
                for (int i = expectedSize; i < actualSize; i++) {
                    ((ObjectNode) result.getChildActualJson()).set(getIndexString(i), params.getActual().get(i));
                }
            } else {
                for (int i = actualSize; i < expectedSize; i++) {
                    ((ObjectNode) result.getChildExpectedJson()).set(getIndexString(i), params.getExpected().get(i));
                }
            }

        }
        return result;
    }

    @Override
    public String toString() {
        return "Json数组对比器";
    }

    /**
     * 对比有序的数组
     *
     * @param params
     * @param result
     * @param size
     */
    private void compareArray(CompareParams<ArrayNode> params, BriefDiffResult result, int size) {
        ObjectNode childActualJson = JsonNodeFactory.instance.objectNode();
        ObjectNode childExpectedJson = JsonNodeFactory.instance.objectNode();
        for (int index = 0; index < size; index++) {
            String path = CharSequenceUtil.format(PATH_TEMP, params.getCurrentPath(), index);
            JsonNode node1 = params.getActual().get(index);
            JsonNode node2 = params.getExpected().get(index);
            CompareParams<JsonNode> compareParams = bulidCompareParams(params, path, node1, node2);
            BriefDiffResult diffResult = COMPARATOR_FACTORY.executeContrast(node1.getNodeType(), compareParams);
            if (Objects.isNull(diffResult) || diffResult.getBriefDiffs().isEmpty()) {
                continue;
            }
            String indexString = getIndexString(index);
            result.getBriefDiffs().addAll(diffResult.getBriefDiffs());
            childActualJson.set(indexString, diffResult.getChildActualJson());
            childExpectedJson.set(indexString, diffResult.getChildExpectedJson());
        }
        result.setChildActualJson(childActualJson);
        result.setChildExpectedJson(childExpectedJson);
    }

    /**
     * 对比无序的数组
     *
     * @param params
     * @param result
     */
    @SuppressWarnings("all")
    private void compareWithDisorderArray(CompareParams<ArrayNode> params, BriefDiffResult result) {
        ObjectNode childActualJson = JsonNodeFactory.instance.objectNode();
        ObjectNode childExpectedJson = JsonNodeFactory.instance.objectNode();
        int expectedSize = params.getExpected().size();
        ArrayList<String> matchedPath = new ArrayList<>();
        JsonNode valueOfActualUniqueKey = null;
        String uniqueKey = params.getConfig().getArrayWithDisorderUniqueKey(params.getCurrentPath());

        for (int i = 0; i < params.getActual().size(); i++) {
            JsonNode actualJsonNode = params.getActual().get(i);
            String actualPath = CharSequenceUtil.format(PATH_TEMP, params.getCurrentPath(), i);
            String actualIndexString = getIndexString(i);
            if (CharSequenceUtil.isNotBlank(uniqueKey)) {
                valueOfActualUniqueKey = actualJsonNode.get(uniqueKey);
            }
            for (int j = 0; j < expectedSize; j++) {
                String expectedPath = CharSequenceUtil.format(PATH_TEMP, params.getCurrentPath(), j);
                JsonNode expectedJsonNode = params.getExpected().get(j);
                CompareParams<JsonNode> compareParams = bulidCompareParams(params, actualPath, actualJsonNode, expectedJsonNode);
                if (Objects.nonNull(valueOfActualUniqueKey)) {
                    actualIndexString = getUniqueKeyIndexString(uniqueKey, i, valueOfActualUniqueKey);
                    // 如果根据唯一键匹配到了, 则只该元素对比即可
                    if (valueOfActualUniqueKey.equals(expectedJsonNode.get(uniqueKey))) {
                        matchedPath.add(expectedPath);
                        String expectedIndexString = getUniqueKeyIndexString(uniqueKey, j, valueOfActualUniqueKey);
                        BriefDiffResult diffResult = COMPARATOR_FACTORY.executeContrast(actualJsonNode.getNodeType(), compareParams);
                        if (!diffResult.getBriefDiffs().isEmpty()) {
                            final JsonNode finalValueOfActualUniqueKey = valueOfActualUniqueKey;
                            diffResult.getBriefDiffs().forEach(d ->
                                    d.setReason(getUniqueReason(uniqueKey, finalValueOfActualUniqueKey, d.getReason()))
                            );
                            childActualJson.set(actualIndexString, diffResult.getChildActualJson());
                            childExpectedJson.set(expectedIndexString, diffResult.getChildExpectedJson());
                            result.getBriefDiffs().addAll(diffResult.getBriefDiffs());
                        }
                        break;
                    }
                    // 如果遍历了整个预期JSON数组都没有发现这个唯一键则标记在预期中不存在这个唯一键
                    if (j + 1 == expectedSize) {
                        result.getBriefDiffs().add(
                                buildElementNotFoundInExpectedDiff(actualPath, valueOfActualUniqueKey, actualJsonNode)
                        );
                        childActualJson.set(actualIndexString, actualJsonNode);
                        childExpectedJson.set(getUniqueKeyNotExist(valueOfActualUniqueKey, uniqueKey), null);
                    }
                } else {
                    // 未匹配到唯一键或者未设置唯一键则走普通的对比逻辑, 双重for遍历每个数组元素
                    BriefDiffResult diffResult = COMPARATOR_FACTORY.executeContrast(actualJsonNode.getNodeType(), compareParams);
                    // 如果俩个json串没有差异信息则代表在预期中匹配到了
                    if (diffResult.getBriefDiffs().isEmpty()) {
                        matchedPath.add(expectedPath);
                        break;
                    }
                    if (j + 1 == expectedSize) {
                        // 如果遍历整个预期数组都没有找到与实际数组下此元素相同的元素则标记此元素在预期中不存在
                        result.getBriefDiffs().add(buildElementNotFoundInExceptedDiff(actualPath, actualJsonNode));
                        childActualJson.set(actualIndexString, actualJsonNode);
                    }
                }
            }
        }

        // 再次遍历预期数组, 防止有元素在预期中存在而在实际中不存在
        ArrayList<BriefDiffResult.BriefDiff> diffs =
                findElementNotInActual(params, childActualJson, childExpectedJson, matchedPath, uniqueKey);
        result.getBriefDiffs().addAll(diffs);
        result.setChildExpectedJson(childExpectedJson);
        result.setChildActualJson(childActualJson);
    }

    private ArrayList<BriefDiffResult.BriefDiff> findElementNotInActual(
            CompareParams<ArrayNode> params,
            ObjectNode childActualJson,
            ObjectNode childExpectedJson,
            ArrayList<String> matchedPath,
            String uniqueKey
    ) {
        ArrayList<BriefDiffResult.BriefDiff> diffs = new ArrayList<>();
        for (int i = 0; i < params.getExpected().size(); i++) {
            String expectedPath = CharSequenceUtil.format(PATH_TEMP, params.getCurrentPath(), i);
            if (matchedPath.contains(expectedPath)) {
                continue;
            }
            JsonNode expectedJsonNode = params.getExpected().get(i);
            JsonNode valueOfExpectedUniqueKey = expectedJsonNode.get(uniqueKey);
            if (Objects.nonNull(valueOfExpectedUniqueKey)) {
                BriefDiffResult.BriefDiff diff = buildElementNotFoundInActualDiff(expectedPath, valueOfExpectedUniqueKey, expectedJsonNode);
                diffs.add(diff);
                childExpectedJson.set(getUniqueKeyIndexString(uniqueKey, i, valueOfExpectedUniqueKey), expectedJsonNode);
                childActualJson.set(getUniqueKeyNotExist(valueOfExpectedUniqueKey, uniqueKey), null);
            } else {
                diffs.add(buildElementNotFoundInActualDiff(expectedPath, expectedJsonNode));
                childExpectedJson.set(getIndexString(i), expectedJsonNode);
            }
        }
        return diffs;
    }

    private String getUniqueReason(String uniqueKey, JsonNode finalValueOfActualUniqueKey, String originReason) {
        return CharSequenceUtil.format(
                UNIQUE_KEY_REASON_TEMP, uniqueKey,
                finalValueOfActualUniqueKey, originReason
        );
    }

    private String getIndexString(int i) {
        return CharSequenceUtil.format(INDEX_TEMP, i);
    }

    private String getUniqueKeyNotExist(JsonNode valueOfActualUniqueKey, String uniqueKey) {
        return CharSequenceUtil.format(UNIQUE_KEY_NOT_EXIST_TEMP, uniqueKey, valueOfActualUniqueKey);
    }

    private String getUniqueKeyIndexString(String uniqueKey, int i, JsonNode valueOfExpectedUniqueKey) {
        return CharSequenceUtil.format(UNIQUE_KEY_INDEX_TEMP, uniqueKey, valueOfExpectedUniqueKey, i);
    }

    /**
     * 构建对比时所必须的参数
     *
     * @param params
     * @return
     */
    private CompareParams<JsonNode> bulidCompareParams(
            CompareParams<ArrayNode> params, String path, JsonNode actualJsonNode, JsonNode expectedJsonNode) {
        return CompareParams.<JsonNode>builder()
                .currentPath(path)
                .actual(actualJsonNode)
                .expected(expectedJsonNode)
                .config(params.getConfig())
                .build();
    }

    private BriefDiffResult.BriefDiff buildElementNotFoundInExceptedDiff(String actualPath, JsonNode actualJsonNode) {
        BriefDiffResult.BriefDiff diff;
        diff = BriefDiffResult.BriefDiff.builder()
                .diffKey(actualPath)
                .reason(CompareMessageConstant.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_EXCEPTED)
                .type(DiffEnum.DISORDER_ARRAY_NOT_FOUND_IN_EXCEPTED.getType())
                .msg(DiffEnum.DISORDER_ARRAY_NOT_FOUND_IN_EXCEPTED.getMsg())
                .actual(actualJsonNode.toString())
                .expected(null)
                .build();
        return diff;
    }

    private BriefDiffResult.BriefDiff buildElementNotFoundInActualDiff(String expectedPath, JsonNode expectedJsonNode) {
        BriefDiffResult.BriefDiff diff;
        diff = BriefDiffResult.BriefDiff.builder()
                .diffKey(expectedPath)
                .reason(CompareMessageConstant.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_ACTUAL)
                .type(DiffEnum.DISORDER_ARRAY_NOT_FOUND_IN_ACTUAL.getType())
                .msg(DiffEnum.DISORDER_ARRAY_NOT_FOUND_IN_ACTUAL.getMsg())
                .actual(null)
                .expected(expectedJsonNode.toString())
                .build();
        return diff;
    }

    private BriefDiffResult.BriefDiff buildElementNotFoundInExpectedDiff(
            String actualPath, JsonNode valueOfActualUniqueKey, JsonNode actualJsonNode
    ) {
        BriefDiffResult.BriefDiff diff;
        diff = BriefDiffResult.BriefDiff.builder()
                .type(DiffEnum.DISORDER_ARRAY_NOT_FOUND_IN_EXCEPTED.getType())
                .msg(DiffEnum.DISORDER_ARRAY_NOT_FOUND_IN_EXCEPTED.getMsg())
                .actual(actualJsonNode.toString())
                .expected(String.format("当前uniqueKey[%s]不存在", valueOfActualUniqueKey))
                .diffKey(actualPath)
                .reason(String.format("当前uniqueKey[%s]在预期中不存在", valueOfActualUniqueKey))
                .build();
        return diff;
    }

    private BriefDiffResult.BriefDiff buildElementNotFoundInActualDiff(
            String expectedPath, JsonNode valueOfExpectedUniqueKey, JsonNode expectedJsonNode
    ) {
        BriefDiffResult.BriefDiff diff;
        diff = BriefDiffResult.BriefDiff.builder()
                .type(DiffEnum.DISORDER_ARRAY_NOT_FOUND_IN_ACTUAL.getType())
                .msg(DiffEnum.DISORDER_ARRAY_NOT_FOUND_IN_ACTUAL.getMsg())
                .actual(String.format("当前uniqueKey[%s]不存在", valueOfExpectedUniqueKey))
                .expected(expectedJsonNode.toString())
                .diffKey(expectedPath)
                .reason(String.format("当前uniqueKey[%s]在实际中不存在", valueOfExpectedUniqueKey))
                .build();
        return diff;
    }

    private BriefDiffResult.BriefDiff buildLengthNotEqualDiff(String currentPath, int actualSize, int expectedSize) {
        BriefDiffResult.BriefDiff diff;
        diff = BriefDiffResult.BriefDiff.builder()
                .actual("实际的列表长度为: " + actualSize)
                .expected("预期的列表长度为: " + expectedSize)
                .diffKey(currentPath)
                .type(DiffEnum.LIST_LENGTH_NOT_EQUALS.getType())
                .msg(DiffEnum.LIST_LENGTH_NOT_EQUALS.getMsg())
                .reason(String.format(CompareMessageConstant.LIST_LENGTH_NOT_EQUALS, actualSize, expectedSize))
                .build();
        return diff;
    }
}

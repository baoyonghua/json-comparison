package com.myhexin.autotest.jsoncomparison.compare.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myhexin.autotest.jsoncomparison.compare.AbstractJsonComparator;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
import com.myhexin.autotest.jsoncomparison.compare.enums.DiffEnum;
import com.myhexin.autotest.jsoncomparison.compare.factory.JsonComparatorFactory;
import com.myhexin.autotest.jsoncomparison.config.JsonCompareConfig;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;


/**
 * 数组JSON对比器实现
 *
 * @author baoyh
 * @since 2023/6/21
 */
@Slf4j
public class JsonArrayComparator extends AbstractJsonComparator<ArrayNode> {

    @Override
    public BriefDiffResult compare(CompareParams<ArrayNode> params) {
        BriefDiffResult result = new BriefDiffResult();
        JsonNode expected = params.getExpected();
        JsonNode actual = params.getActual();
        // 如果两个类型不一致则不予对比, 直接返回
        String currentPath = params.getCurrentPath();
        checkType(result, expected, actual, currentPath);
        if (!result.getBriefDiffs().isEmpty()) {
            return result;
        }
        int actualSize = actual.size();
        int expectedSize = expected.size();
        if (actualSize != expectedSize) {
            BriefDiffResult.BriefDiff diff = buildLengthNotEqualDiff(currentPath, actualSize, expectedSize);
            result.getBriefDiffs().add(diff);
        }
        int size = Math.min(actualSize, expectedSize);
        if (size == 0) {
            ArrayNode childActualJson = JsonNodeFactory.instance.arrayNode();
            ArrayNode childExpectedJson = JsonNodeFactory.instance.arrayNode();
            childActualJson.addAll(params.getActual());
            childExpectedJson.addAll(params.getExpected());
            result.setChildActualJson(childActualJson);
            result.setChildExpectedJson(childExpectedJson);
            return result;
        }
        if (isWithDisorderPath(params, currentPath)) {
            log.info("当前路径[{}]配置了支持乱序的数组对比...", currentPath);
            compareWithDisorderArray(params, result);
        } else {
            ObjectNode childActualJson = JsonNodeFactory.instance.objectNode();
            ObjectNode childExpectedJson = JsonNodeFactory.instance.objectNode();
            for (int index = 0; index < size; index++) {
                String nowIndex = "下标为" + index;
                String path = currentPath + "[" + index + "]";
                JsonNode node1 = actual.get(index);
                JsonNode node2 = expected.get(index);
                CompareParams<JsonNode> compareParams =
                        bulidCompareParams(params, path, node1, node2);
                BriefDiffResult diffResult = JsonComparatorFactory.build()
                        .executeContrast(node1.getNodeType(), compareParams);
                if (Objects.isNull(diffResult) || Objects.isNull(diffResult.getBriefDiffs())
                        || diffResult.getBriefDiffs().isEmpty()) {
                    continue;
                }
                result.getBriefDiffs().addAll(diffResult.getBriefDiffs());
                childActualJson.set(nowIndex, diffResult.getChildActualJson());
                childExpectedJson.set(nowIndex, diffResult.getChildExpectedJson());
            }
            result.setChildActualJson(childActualJson);
            result.setChildExpectedJson(childExpectedJson);
        }
        return result;
    }

    private void checkType(BriefDiffResult result, JsonNode expected, JsonNode actual, String currentPath) {
        BriefDiffResult.BriefDiff diff = checkJsonNodeType(currentPath, actual, expected);
        if (Objects.nonNull(diff)) {
            result.getBriefDiffs().add(diff);
            result.setChildActualJson(actual);
            result.setChildExpectedJson(expected);
        }
    }

    @Override
    public String toString() {
        return "Json数组对比器";
    }

    private void compareWithDisorderArray(CompareParams<ArrayNode> params, BriefDiffResult result) {
        ObjectNode childActualJson = JsonNodeFactory.instance.objectNode();
        ObjectNode childExpectedJson = JsonNodeFactory.instance.objectNode();
        String currentPath = params.getCurrentPath();
        JsonNode actual = params.getActual();
        JsonNode expected = params.getExpected();
        ArrayList<String> matchedPath = new ArrayList<>();
        JsonNode valueOfActualUniqueKey = null;
        String temp = "当前唯一键[{}][{}], {}";
        String indexTemp = "当前唯一键[{}][{}], 下标为[{}]";

        String uniqueKey = params.getConfig().getArrayWithDisorderUniqueKey(currentPath);
        e:
        for (int i = 0; i < actual.size(); i++) {
            JsonNode actualJsonNode = actual.get(i);
            boolean isPass = false;
            String actualPath = currentPath + "[" + i + "]";
            if (CharSequenceUtil.isNotBlank(uniqueKey)
                    && actualJsonNode.getNodeType().equals(JsonNodeType.OBJECT)) {
                valueOfActualUniqueKey = actualJsonNode.get(uniqueKey);
            }
            String actualIndexString = CharSequenceUtil.format(indexTemp, uniqueKey, valueOfActualUniqueKey, i);
            for (int j = 0; j < expected.size(); j++) {
                String expectedPath = currentPath + "[" + j + "]";
                JsonNode expectedJsonNode = expected.get(j);
                CompareParams<JsonNode> compareParams =
                        bulidCompareParams(params, actualPath, actualJsonNode, expectedJsonNode);
                if (Objects.nonNull(valueOfActualUniqueKey)) {
                    // 如果配置了唯一键，则只有在两个数组的元素唯一键相同时进行对比, 否则不进行对比, 并且由于是唯一的, 找到了直接break即可
                    if (valueOfActualUniqueKey.equals(expectedJsonNode.get(uniqueKey))) {
                        String expectedIndexString =
                                CharSequenceUtil.format(indexTemp, uniqueKey, valueOfActualUniqueKey, j);
                        matchedPath.add(expectedPath);
                        BriefDiffResult diffResult = JsonComparatorFactory.build()
                                .executeContrast(actualJsonNode.getNodeType(), compareParams);
                        if (diffResult == null || diffResult.getBriefDiffs().isEmpty()) {
                            continue e;
                        }
                        for (BriefDiffResult.BriefDiff diff : diffResult.getBriefDiffs()) {
                            diff.setReason(
                                    CharSequenceUtil.format(temp, uniqueKey, valueOfActualUniqueKey, diff.getReason())
                            );
                        }
                        childActualJson.set(actualIndexString, diffResult.getChildActualJson());
                        childExpectedJson.set(expectedIndexString, diffResult.getChildExpectedJson());
                        result.getBriefDiffs().addAll(diffResult.getBriefDiffs());
                        continue e;
                    }
                    // 如果遍历了整个预期JSON数组都没有发现这个唯一键则标记在预期中不存在这个唯一键
                    if (j + 1 == expected.size()) {
                        result.getBriefDiffs().add(
                                buildElementNotFoundInExpectedDiff(actualPath, valueOfActualUniqueKey, actualJsonNode)
                        );
                        childActualJson.set(actualIndexString, actualJsonNode);
                        childExpectedJson.set(
                                CharSequenceUtil.format(indexTemp, uniqueKey, valueOfActualUniqueKey, -1),
                                null
                        );
                        continue e;
                    }
                } else {
                    BriefDiffResult diffResult = JsonComparatorFactory.build()
                            .executeContrast(actualJsonNode.getNodeType(), compareParams);
                    // 如果俩个json串没有差异信息则代表在预期中匹配到了
                    if (diffResult == null || diffResult.getBriefDiffs().isEmpty()) {
                        matchedPath.add(expectedPath);
                        isPass = true;
                        break;
                    }
                }
            }
            // 如果遍历整个预期数组都没有找到与实际数组下此元素相同的元素则标记此元素在预期中不存在
            if (!isPass) {
                result.getBriefDiffs().add(
                        buildElementNotFoundInExceptedDiff(expected, actualPath, actualJsonNode)
                );
                if (Objects.isNull(valueOfActualUniqueKey)) {
                    actualIndexString = "下标为" + i;
                }
                childActualJson.set(actualIndexString, actualJsonNode);
            }
        }
        // 再次遍历预期数组, 防止有元素在预期中存在而在实际中不存在
        for (int i = 0; i < expected.size(); i++) {
            String expectedPath = currentPath + "[" + i + "]";
            if (matchedPath.contains(expectedPath)) {
                continue;
            }
            JsonNode expectedjsonNode = expected.get(i);
            JsonNode valueOfExpectedUniqueKey = expectedjsonNode.get(uniqueKey);
            String expectedIndexString =
                    CharSequenceUtil.format(indexTemp, uniqueKey, valueOfExpectedUniqueKey, i);
            if (Objects.nonNull(valueOfExpectedUniqueKey)) {
                result.getBriefDiffs().add(
                        buildElementNotFoundInActualDiff(expectedPath, valueOfExpectedUniqueKey, expectedjsonNode)
                );
                childExpectedJson.set(expectedIndexString, expectedjsonNode);
                childActualJson.set(
                        CharSequenceUtil.format(indexTemp, uniqueKey, valueOfExpectedUniqueKey, -1),
                        null
                );
            } else {
                result.getBriefDiffs().add(
                        buildElementNotFoundInActualDiff(actual, expectedPath, expectedjsonNode)
                );
                childExpectedJson.set("下标为" + i, expectedjsonNode);
            }
        }
        result.setChildExpectedJson(childExpectedJson);
        result.setChildActualJson(childActualJson);
    }

    private boolean isWithDisorderPath(CompareParams<ArrayNode> params, String path) {
        Set<String> arrayWithDisorderPaths = params.getConfig().getArrayWithDisorderPath().stream()
                .map(JsonCompareConfig.ArrayWithDisorderConfig::getPath)
                .collect(Collectors.toSet());
        // 如果这里包含则直接返回
        if (arrayWithDisorderPaths.contains(path)) {
            return true;
        }
        // employees[*].skills 和 employees[1].skills 是等同的
        String finalPath = path.replaceAll("\\[\\d+\\]", "[*]");
        return arrayWithDisorderPaths.contains(finalPath);
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

    private BriefDiffResult.BriefDiff buildElementNotFoundInExceptedDiff(
            JsonNode expected, String actualPath, JsonNode actualJsonNode
    ) {
        BriefDiffResult.BriefDiff diff;
        diff = BriefDiffResult.BriefDiff.builder()
                .diffKey(actualPath)
                .reason(CompareMessageConstant.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_EXCEPTED)
                .type(DiffEnum.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_EXCEPTED.getType())
                .msg(DiffEnum.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_EXCEPTED.getMsg())
                .actual(actualJsonNode.toString())
                .expected(expected.toString())
                .build();
        return diff;
    }

    private BriefDiffResult.BriefDiff buildElementNotFoundInActualDiff(
            JsonNode actual, String expectedPath, JsonNode expectedJsonNode
    ) {
        BriefDiffResult.BriefDiff diff;
        diff = BriefDiffResult.BriefDiff.builder()
                .diffKey(expectedPath)
                .reason(CompareMessageConstant.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_ACTUAL)
                .type(DiffEnum.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_ACTUAL.getType())
                .msg(DiffEnum.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_ACTUAL.getMsg())
                .actual(actual.toString())
                .expected(expectedJsonNode.toString())
                .build();
        return diff;
    }

    private BriefDiffResult.BriefDiff buildElementNotFoundInExpectedDiff(
            String actualPath, JsonNode valueOfActualUniqueKey, JsonNode actualJsonNode
    ) {
        BriefDiffResult.BriefDiff diff;
        diff = BriefDiffResult.BriefDiff.builder()
                .type(DiffEnum.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_EXCEPTED.getType())
                .msg(DiffEnum.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_EXCEPTED.getMsg())
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
                .type(DiffEnum.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_ACTUAL.getType())
                .msg(DiffEnum.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_ACTUAL.getMsg())
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

package com.myhexin.autotest.jsoncomparison.compare.impl;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myhexin.autotest.jsoncomparison.compare.AbstractJsonComparator;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
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
    public List<BriefDiffResult.BriefDiff> compare(CompareParams<ArrayNode> params) {
        List<BriefDiffResult.BriefDiff> diffs = new ArrayList<>();
        BriefDiffResult.BriefDiff diff;
        JsonNode actual = params.getActual();
        JsonNode expected = params.getExpected();
        // 如果两个类型不一致则不予对比, 直接返回
        String currentPath = params.getCurrentPath();
        diff = checkJsonNodeType(currentPath, actual, expected);
        if (Objects.nonNull(diff)) {
            return Collections.singletonList(diff);
        }
        int actualSize = actual.size();
        int expectedSize = expected.size();
        if (actualSize != expectedSize) {
            diff = BriefDiffResult.BriefDiff.builder()
                    .actual("实际的列表长度为: " + actualSize)
                    .expected("预期的列表长度为: " + expectedSize)
                    .diffKey(currentPath)
                    .reason(String.format(CompareMessageConstant.LIST_LENGTH_NOT_EQUALS, actualSize, expectedSize))
                    .build();
            diffs.add(diff);
        }
        // 乱序时不支持长度不一致的对比
        if (isWithDisorderPath(params, currentPath)) {
            log.info("当前路径[{}]配置了支持乱序的数组对比...", currentPath);
            if (actualSize != expectedSize) {
                log.warn("不支持乱序时进行长度不一致的数组对比！");
                return diffs;
            }
            String uniqueKey = params.getConfig().getArrayWithDisorderUniqueKey(currentPath);
            String actualPath;
            int actualIndex = 0;
            int count = 0;
            JsonNode valueOfUniqueKey = null;
            for (JsonNode actualJsonNode : actual) {
                if (CharSequenceUtil.isNotBlank(uniqueKey)
                        && actualJsonNode.getNodeType().equals(JsonNodeType.OBJECT)) {
                    valueOfUniqueKey = actualJsonNode.get(uniqueKey);
                }
                boolean isPass = false;
                actualPath = currentPath + "[" + ++actualIndex + "]";
                String expectedPath;
                int expectedIndex = 0;
                for (JsonNode expectedJsonNode : expected) {
                    count++;
                    expectedPath = currentPath + "[" + ++expectedIndex + "]";
                    CompareParams<JsonNode> compareParams =
                            bulidCompareParams(params, expectedPath, actualJsonNode, expectedJsonNode);
                    if (Objects.nonNull(valueOfUniqueKey)
                            && !valueOfUniqueKey.equals(expectedJsonNode.get(uniqueKey))) {
                        // todo 如果实际JSON中存在预期JSON中不存在的uniqueKey, 那么预期JSON中也可能会存在实际JSON中不存在的uniqueKey, 需要把它们找出来
                        if (expectedIndex + 1 == expectedSize) {
                            diff = BriefDiffResult.BriefDiff.builder()
                                    .actual(String.format("当前uniqueKey[%s]在预期中不存在", valueOfUniqueKey))
                                    .expected(String.format("当前uniqueKey[%s]不存在", valueOfUniqueKey))
                                    .diffKey(actualPath)
                                    .reason(CompareMessageConstant.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_EXCEPTED)
                                    .build();
                            diffs.add(diff);
                        }
                        continue;
                    }
                    // 从工厂中获取对比器进行对比
                    List<BriefDiffResult.BriefDiff> diffList = JsonComparatorFactory.build()
                            .executeContrast(actualJsonNode.getNodeType(), compareParams);
                    // 如果俩个json串没有差异信息则代表在预期中匹配到了
                    if (diffList.isEmpty()) {
                        isPass = true;
                        break;
                    }
                }
                if (!isPass) {
                    diff = BriefDiffResult.BriefDiff.builder()
                            .diffKey(actualPath)
                            .reason(CompareMessageConstant.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_EXCEPTED)
                            .actual(actualJsonNode.toString())
                            .expected(expected.toString())
                            .build();
                    diffs.add(diff);
                }
            }
            log.info("遍历了{}遍", count);
        } else {
            // 不是乱序时需要支持不同长度的对比
            int size = Math.min(actualSize, expectedSize);
            for (int index = 0; index < size; index++) {
                String path = currentPath + "[" + index + "]";
                JsonNode node1 = actual.get(index);
                JsonNode node2 = expected.get(index);
                CompareParams<JsonNode> compareParams =
                        bulidCompareParams(params, path, node1, node2);
                List<BriefDiffResult.BriefDiff> diffList = JsonComparatorFactory.build()
                        .executeContrast(node1.getNodeType(), compareParams);
                diffs.addAll(diffList);
            }
        }
        return diffs;
    }

    @Override
    public String toString() {
        return "Json数组对比器";
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
        String finalPath = path.replaceAll("[\\d+]", "*");
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
}

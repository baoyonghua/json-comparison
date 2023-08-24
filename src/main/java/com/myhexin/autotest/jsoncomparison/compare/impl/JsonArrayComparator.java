package com.myhexin.autotest.jsoncomparison.compare.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.myhexin.autotest.jsoncomparison.compare.AbstractJsonComparator;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
import com.myhexin.autotest.jsoncomparison.compare.factory.JsonComparatorFactory;
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

    @Override
    public List<BriefDiffResult.BriefDiff> compare(CompareParams<ArrayNode> params) {
        List<BriefDiffResult.BriefDiff> diffs = new ArrayList<>();
        BriefDiffResult.BriefDiff diff;
        ArrayNode actual = params.getActual();
        ArrayNode expected = params.getExpected();
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
            Iterator<JsonNode> actualIterator = actual.iterator();
            Iterator<JsonNode> expectedIterator = expected.iterator();
            while (actualIterator.hasNext()) {
                List<BriefDiffResult.BriefDiff> subDiffs = null;
                boolean isPass = false;
                JsonNode actualJsonNode = actualIterator.next();
                int expectedIndex = 0;
                while (expectedIterator.hasNext()) {
                    String expectedPath = currentPath + "[" + expectedIndex + "]";
                    JsonNode expectedJsonNode = expectedIterator.next();
                    CompareParams<JsonNode> compareParams =
                            bulidCompareParams(params, expectedPath, actualJsonNode, expectedJsonNode);
                    // 从工厂中获取对比器进行对比
                    List<BriefDiffResult.BriefDiff> diffList = JsonComparatorFactory.build()
                            .executeContrast(actualJsonNode.getNodeType(), compareParams);
                    if (diffList.isEmpty()) {
                        isPass = true;
                        break;
                    }
                    subDiffs = diffList;
                    expectedIndex++;
                }
                if (!isPass) {
                    diff = BriefDiffResult.BriefDiff.builder()
                            .diffKey(currentPath)
                            .reason(CompareMessageConstant.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_EXCEPTED)
                            .actual(actualJsonNode.toString())
                            .expected(expected.toString())
                            .subDiffs(subDiffs)
                            .build();
                    diffs.add(diff);
                }
            }
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
        Set<String> arrayWithDisorderPaths = params.getConfig().getArrayWithDisorderPath();
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

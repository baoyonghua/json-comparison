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
        log.info("开始两个数组类型对象的Json对比操作...");
        // 乱序时不支持长度不一致的对比
        if (params.getConfig().getIgnorePath().contains(currentPath)) {
            log.info("当前路径[{}]配置了支持乱序的数组对比...", currentPath);
            if (actualSize != expectedSize) {
                diff = BriefDiffResult.BriefDiff.builder()
                        .actual(actual.asText())
                        .expected(expected.asText())
                        .diffKey(currentPath)
                        .reason(String.format(CompareMessageConstant.LIST_LENGTH_NOT_EQUALS, actualSize, expectedSize))
                        .build();
                return Collections.singletonList(diff);
            } else {
                String expectedJson = getJsonNodeByPath(params.getOriginalExcepetd(), currentPath).toString();
                Iterator<JsonNode> actualIterator = actual.iterator();
                Iterator<JsonNode> expectedIterator = expected.iterator();
                while (actualIterator.hasNext()) {
                    boolean isPass = false;
                    JsonNode actualJsonNode = actualIterator.next();
                    int expectedIndex = 0;
                    while (expectedIterator.hasNext()) {
                        String expectedPath = currentPath + "[" + expectedIndex + "]";
                        JsonNode node = getJsonNodeByPath(params.getOriginalExcepetd(), expectedPath);
                        CompareParams<JsonNode> compareParams =
                                bulidCompareParams(params, expectedPath, actualJsonNode, node);
                        // 从工厂中获取对比器进行对比
                        List<BriefDiffResult.BriefDiff> diffList = JsonComparatorFactory.build()
                                .executeContrast(actualJsonNode.getNodeType(), compareParams);
                        if (diffList.isEmpty()) {
                            isPass = true;
                            break;
                        }
                        expectedIndex++;
                    }
                    if (!isPass) {
                        diff = BriefDiffResult.BriefDiff.builder()
                                .diffKey(currentPath)
                                .reason(CompareMessageConstant.DISORDER_ARRAY_ACTUAL_NOT_FOUND_IN_EXCEPTED)
                                .actual(params.getActual().toString())
                                .expected(expectedJson)
                                .build();
                        diffs.add(diff);
                    }
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

    protected String buildPath(String oldPath, int index) {
        return oldPath + "[" + index + "]";
    }

    @Override
    public String toString() {
        return "Json数组对比器";
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
                .prevPath(params.getCurrentPath())
                .actual(actualJsonNode)
                .expected(expectedJsonNode)
                .originalExcepetd(params.getOriginalExcepetd())
                .config(params.getConfig())
                .build();
    }

    /**
     * 构建对比时所必须的参数
     *
     * @param params
     * @return
     */
    private CompareParams<JsonNode> bulidCompareParams(
            CompareParams<ArrayNode> params, int index, JsonNode actualJsonNode) {
        String path = buildPath(params.getCurrentPath(), index);
        return CompareParams.<JsonNode>builder()
                .currentPath(path)
                .prevPath(params.getCurrentPath())
                .actual(actualJsonNode)
                .expected(getJsonNodeByPath(params.getOriginalExcepetd(), path))
                .originalExcepetd(params.getOriginalExcepetd())
                .config(params.getConfig())
                .build();
    }
}

package com.myhexin.autotest.jsoncomparison.compare.impl;

import cn.hutool.core.collection.ListUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myhexin.autotest.jsoncomparison.compare.AbstractJsonComparator;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
import com.myhexin.autotest.jsoncomparison.compare.enums.DiffEnum;
import com.myhexin.autotest.jsoncomparison.compare.factory.JsonComparatorFactory;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;

import java.util.*;

/**
 * @author baoyh
 * @since 2023/7/1
 */
public class JsonObjectComparator extends AbstractJsonComparator<ObjectNode> {

    @Override
    public List<BriefDiffResult.BriefDiff> compare(CompareParams<ObjectNode> params) {
        JsonNode actual = params.getActual();
        JsonNode expected = params.getExpected();
        // 如果两个类型不一致则不予对比, 直接返回
        String currentPath = params.getCurrentPath();
        BriefDiffResult.BriefDiff diff = checkJsonNodeType(currentPath, actual, expected);
        if (Objects.nonNull(diff)) {
            return Collections.singletonList(diff);
        }
        ArrayList<BriefDiffResult.BriefDiff> diffs = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> actualFields = actual.fields();
        List<String> expectedFieldNames = ListUtil.list(false, expected.fieldNames());
        List<String> actualFieldNames = ListUtil.list(false, actual.fieldNames());

        //以实际的字段为基准进行对比
        while (actualFields.hasNext()) {
            Map.Entry<String, JsonNode> actualField = actualFields.next();
            String actualFieldName = actualField.getKey();
            JsonNode expectedJsonNode = expected.get(actualFieldName);
            // 如果预期中此字段为null则需要去校验下是否是预期中没有此字段
            if (Objects.isNull(expectedJsonNode)) {
                diff = BriefDiffResult.BriefDiff.builder()
                        .diffKey(buildPath(currentPath, actualFieldName))
                        .type(DiffEnum.EXPECTED_MISS_KEY.getType())
                        .reason(String.format(CompareMessageConstant.EXPECTED_MISS_KEY, actualFieldName))
                        .actual(actualField.getValue().asText())
                        .expected(String.format("预期中不存在该key: [%s]", actualFieldName))
                        .build();
                diffs.add(diff);
                continue;
            }
            CompareParams<JsonNode> compareParams = bulidCompareParams(params, actualField, expectedJsonNode);
            // 从工厂中获取对比器进行对比
            List<BriefDiffResult.BriefDiff> diffList = JsonComparatorFactory.build()
                    .executeContrast(actualField.getValue().getNodeType(), compareParams);
            diffs.addAll(diffList);
        }
        //找出预期结果中可能多出来的字段<即在实际结果中不存在的字段>
        diffs.addAll(findFieldsInExpected(expectedFieldNames, actualFieldNames, params));
        return diffs;
    }

    /**
     * 找出只存在在预期中的字段
     *
     * @param params
     * @param <T>
     * @return
     */
    private <T extends JsonNode> ArrayList<BriefDiffResult.BriefDiff> findFieldsInExpected(
            List<String> expectedFieldNames, List<String> actualFieldNames, CompareParams<ObjectNode> params
    ) {
        //找出预期结果中可能多出来的字段<即在实际结果中不存在的字段>, 因为之前只是以实际结果为基准进行了对比
        ArrayList<BriefDiffResult.BriefDiff> briefDiffs = new ArrayList<>();
        for (String fieldName : expectedFieldNames) {
            if (!actualFieldNames.contains(fieldName)) {
                BriefDiffResult.BriefDiff diff = BriefDiffResult.BriefDiff.builder()
                        .diffKey(params.getCurrentPath() + "." + fieldName)
                        .type(DiffEnum.ACTUAL_MISS_KEY.getType())
                        .reason(String.format(CompareMessageConstant.ACTUAL_MISS_KEY, fieldName))
                        .actual(String.format("实际中不存在该key: [%s]", fieldName))
                        .expected(params.getExpected().get(fieldName).toString())
                        .build();
                briefDiffs.add(diff);
            }
        }
        return briefDiffs;
    }


    @Override
    public String toString() {
        return "Json对象对比器";
    }

    /**
     * 根据老的path构建出新的path
     *
     * @param oldPath
     * @param fieldName
     * @return
     */
    private String buildPath(String oldPath, String fieldName) {
        return oldPath + "." + fieldName;
    }

    /**
     * 构建对比时所必须的参数
     *
     * @param params
     * @param actualField
     * @param expectedJsonNode
     * @return
     */
    private CompareParams<JsonNode> bulidCompareParams(
            CompareParams<ObjectNode> params, Map.Entry<String, JsonNode> actualField, JsonNode expectedJsonNode) {
        String path = buildPath(params.getCurrentPath(), actualField.getKey());
        return CompareParams.<JsonNode>builder()
                .currentPath(path)
                .actual(actualField.getValue())
                .expected(expectedJsonNode)
                .config(params.getConfig())
                .build();
    }
}

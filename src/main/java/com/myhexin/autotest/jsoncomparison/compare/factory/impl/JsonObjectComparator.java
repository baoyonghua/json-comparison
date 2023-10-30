package com.myhexin.autotest.jsoncomparison.compare.factory.impl;

import cn.hutool.core.collection.ListUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myhexin.autotest.jsoncomparison.compare.factory.AbstractJsonComparator;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
import com.myhexin.autotest.jsoncomparison.compare.enums.DiffEnum;
import com.myhexin.autotest.jsoncomparison.config.JsonCompareConfig;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * @author baoyh
 * @since 2023/7/1
 */
@Slf4j
public class JsonObjectComparator extends AbstractJsonComparator<ObjectNode> {

    @Override
    public BriefDiffResult compare(CompareParams<ObjectNode> params) {
        BriefDiffResult result = new BriefDiffResult();
        ObjectNode childActualJson = JsonNodeFactory.instance.objectNode();
        ObjectNode childExpectedJson = JsonNodeFactory.instance.objectNode();
        JsonNode actual = params.getActual();
        JsonNode expected = params.getExpected();
        String currentPath = params.getCurrentPath();
        Iterator<Map.Entry<String, JsonNode>> actualFields = actual.fields();
        List<String> expectedFieldNames = ListUtil.list(false, expected.fieldNames());
        List<String> actualFieldNames = ListUtil.list(false, actual.fieldNames());

        //以实际的字段为基准进行对比
        while (actualFields.hasNext()) {
            Map.Entry<String, JsonNode> actualField = actualFields.next();
            String actualFieldName = actualField.getKey();
            JsonNode actualJsonNode = actualField.getValue();
            String actualPath = buildPath(currentPath, actualFieldName);
            JsonCompareConfig.FieldMapping mappingConfig = params.getConfig().getMappingConfig(actualPath);
            // 获取需要进行映射的字段
            String expectedFieldName;
            if (Objects.nonNull(mappingConfig)) {
                String mappingKey = mappingConfig.getMappingKey();
                log.debug("当前实际路径[{}]字段[{}]配置了键映射, 需要映射的键为: [{}]", actualPath, actualFieldName, mappingKey);
                expectedFieldName = mappingKey;
            } else {
                expectedFieldName = actualFieldName;
            }
            JsonNode expectedJsonNode = expected.get(expectedFieldName);
            if (Objects.isNull(expectedJsonNode)) {
                childActualJson.set(actualFieldName, actualJsonNode);
                result.getBriefDiffs().add(buildExpectedMissKeyDiff(actualPath, actualField));
                continue;
            }
            CompareParams<JsonNode> compareParams =
                    buildCompareParams(params, actualPath, actualJsonNode, expectedJsonNode);
            // 从工厂中获取对比器进行对比
            BriefDiffResult diffResult = COMPARATOR_FACTORY.executeContrast(actualJsonNode.getNodeType(), compareParams);
            if (diffResult != null && !diffResult.getBriefDiffs().isEmpty()) {
                childActualJson.set(actualFieldName, diffResult.getChildActualJson());
                childExpectedJson.set(expectedFieldName, diffResult.getChildExpectedJson());
                result.getBriefDiffs().addAll(diffResult.getBriefDiffs());
            }
        }
        //找出预期结果中可能多出来的字段<即在实际结果中不存在的字段>
        for (String expectedFieldName : expectedFieldNames) {
            if (!actualFieldNames.contains(expectedFieldName) &&
                    !params.getConfig().containsMappingKey(expectedFieldName, currentPath)) {
                childExpectedJson.set(expectedFieldName, expected.get(expectedFieldName));
                result.getBriefDiffs().add(buildActualMissKeyDiff(params, expectedFieldName));
            }
        }
        if (!childActualJson.isEmpty(SERIALIZER_PROVIDER)) {
            result.setChildActualJson(childActualJson);
        }
        if (!childExpectedJson.isEmpty(SERIALIZER_PROVIDER)) {
            result.setChildExpectedJson(childExpectedJson);
        }
        return result;
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
        return oldPath + SPLIT_POINT + fieldName;
    }

    /**
     * 构建对比时所必须的参数
     *
     * @param params
     * @param actualJsonNode
     * @param expectedJsonNode
     * @return
     */
    private CompareParams<JsonNode> buildCompareParams(
            CompareParams<ObjectNode> params, String currentPath, JsonNode actualJsonNode, JsonNode expectedJsonNode) {
        return CompareParams.builder()
                .currentPath(currentPath)
                .actual(actualJsonNode)
                .expected(expectedJsonNode)
                .config(params.getConfig())
                .build();
    }

    private BriefDiffResult.BriefDiff buildActualMissKeyDiff(CompareParams<ObjectNode> params, String expectedFieldName) {
        return BriefDiffResult.BriefDiff.builder()
                .diffKey(params.getCurrentPath() + SPLIT_POINT + expectedFieldName)
                .type(DiffEnum.ACTUAL_MISS_KEY.getType())
                .msg(DiffEnum.ACTUAL_MISS_KEY.getMsg())
                .reason(String.format(CompareMessageConstant.ACTUAL_MISS_KEY, expectedFieldName))
                .actual(String.format("实际中不存在该key: [%s]", expectedFieldName))
                .expected(params.getExpected().get(expectedFieldName).toString())
                .build();
    }

    private BriefDiffResult.BriefDiff buildExpectedMissKeyDiff(String currentPath, Map.Entry<String, JsonNode> actualField) {
        return BriefDiffResult.BriefDiff.builder()
                .diffKey(currentPath)
                .type(DiffEnum.EXPECTED_MISS_KEY.getType())
                .msg(DiffEnum.EXPECTED_MISS_KEY.getMsg())
                .reason(String.format(CompareMessageConstant.EXPECTED_MISS_KEY, actualField.getKey()))
                .actual(actualField.getValue().asText())
                .expected(String.format("预期中不存在该key: [%s]", actualField.getKey()))
                .build();
    }
}

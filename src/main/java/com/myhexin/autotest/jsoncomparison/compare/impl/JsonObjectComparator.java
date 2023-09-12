package com.myhexin.autotest.jsoncomparison.compare.impl;

import cn.hutool.core.collection.ListUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.myhexin.autotest.jsoncomparison.compare.AbstractJsonComparator;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
import com.myhexin.autotest.jsoncomparison.compare.enums.DiffEnum;
import com.myhexin.autotest.jsoncomparison.compare.factory.JsonComparatorFactory;
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
        ObjectNode childActualJson = JsonNodeFactory.instance.objectNode();
        ObjectNode childExpectedJson = JsonNodeFactory.instance.objectNode();
        BriefDiffResult result = new BriefDiffResult();
        JsonNode actual = params.getActual();
        JsonNode expected = params.getExpected();
        // 如果两个类型不一致则不予对比, 直接返回
        String currentPath = params.getCurrentPath();
        BriefDiffResult.BriefDiff diff = checkJsonNodeType(currentPath, actual, expected);
        if (Objects.nonNull(diff)) {
            result.getBriefDiffs().add(diff);
            result.setChildActualJson(actual);
            result.setChildExpectedJson(expected);
            return result;
        }
        Iterator<Map.Entry<String, JsonNode>> actualFields = actual.fields();
        List<String> expectedFieldNames = ListUtil.list(false, expected.fieldNames());
        List<String> actualFieldNames = ListUtil.list(false, actual.fieldNames());

        //以实际的字段为基准进行对比
        while (actualFields.hasNext()) {
            Map.Entry<String, JsonNode> actualField = actualFields.next();
            String actualFieldName = actualField.getKey();
            currentPath = buildPath(params.getCurrentPath(), actualFieldName);
            String fieldName = getMappingKey(currentPath, params).orElse(actualFieldName);
            JsonNode expectedJsonNode = expected.get(fieldName);
            if (Objects.isNull(expectedJsonNode)) {
                diff = BriefDiffResult.BriefDiff.builder()
                        .diffKey(currentPath)
                        .type(DiffEnum.EXPECTED_MISS_KEY.getType())
                        .msg(DiffEnum.EXPECTED_MISS_KEY.getMsg())
                        .reason(String.format(CompareMessageConstant.EXPECTED_MISS_KEY, actualFieldName))
                        .actual(actualField.getValue().asText())
                        .expected(String.format("预期中不存在该key: [%s]", actualFieldName))
                        .build();
                childActualJson.set(actualFieldName, actualField.getValue());
                result.getBriefDiffs().add(diff);
                continue;
            }
            CompareParams<JsonNode> compareParams = bulidCompareParams(params, currentPath, actualField, expectedJsonNode);
            // 从工厂中获取对比器进行对比
            BriefDiffResult diffResult = JsonComparatorFactory.build()
                    .executeContrast(actualField.getValue().getNodeType(), compareParams);
            if (diffResult == null || diffResult.getBriefDiffs().isEmpty()) {
                continue;
            }
            childActualJson.set(actualFieldName, diffResult.getChildActualJson());
            childExpectedJson.set(actualFieldName, diffResult.getChildExpectedJson());
            result.getBriefDiffs().addAll(diffResult.getBriefDiffs());
        }
        //找出预期结果中可能多出来的字段<即在实际结果中不存在的字段>
        for (String expectedFieldName : expectedFieldNames) {
            if (!actualFieldNames.contains(expectedFieldName) && containsMappingKey(expectedFieldName, params)) {
                diff = BriefDiffResult.BriefDiff.builder()
                        .diffKey(params.getCurrentPath() + "." + expectedFieldName)
                        .type(DiffEnum.ACTUAL_MISS_KEY.getType())
                        .msg(DiffEnum.ACTUAL_MISS_KEY.getMsg())
                        .reason(String.format(CompareMessageConstant.ACTUAL_MISS_KEY, expectedFieldName))
                        .actual(String.format("实际中不存在该key: [%s]", expectedFieldName))
                        .expected(params.getExpected().get(expectedFieldName).toString())
                        .build();
                childExpectedJson.set(expectedFieldName, expected.get(expectedFieldName));
                result.getBriefDiffs().add(diff);
            }
        }
        if (!childActualJson.isEmpty(new DefaultSerializerProvider.Impl())) {
            result.setChildActualJson(childActualJson);
        }
        if (!childExpectedJson.isEmpty(new DefaultSerializerProvider.Impl())) {
            result.setChildExpectedJson(childExpectedJson);
        }
        return result;
    }

    private boolean containsMappingKey(String mappingKey, CompareParams<ObjectNode> params) {
        Set<JsonCompareConfig.FieldMapping> feildMappings = params.getConfig().getFeildMappings();
        if (feildMappings.isEmpty()) {
            return false;
        }
        for (JsonCompareConfig.FieldMapping feildMapping : feildMappings) {
            String expectedCurrentPath = (params.getCurrentPath() + mappingKey).replace("\\[\\d+\\]", "[*]");
            if (expectedCurrentPath.equals(feildMapping.getMappingKey())) {
                return true;
            }
        }
        return false;
    }

    private Optional<String> getMappingKey(String currentPath, CompareParams<ObjectNode> params) {
        Set<JsonCompareConfig.FieldMapping> feildMappings = params.getConfig().getFeildMappings();
        if (feildMappings.isEmpty()) {
            return Optional.empty();
        }
        for (JsonCompareConfig.FieldMapping feildMapping : feildMappings) {
            // employees[*].skills 和 employees[1].skills 是等同的
            String finalPath = currentPath.replaceAll("\\[\\d+\\]", "[*]");
            if (feildMapping.getPath().equals(finalPath)) {
                String key = feildMapping.getMappingKey();
                log.info("当前路径{}配置的key映射, 映射的key为: {}", currentPath, key);
                return Optional.of(key);
            }
        }
        return Optional.empty();
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
                        .msg(DiffEnum.ACTUAL_MISS_KEY.getMsg())
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
            CompareParams<ObjectNode> params, String currentPath, Map.Entry<String, JsonNode> actualField, JsonNode expectedJsonNode) {
        return CompareParams.<JsonNode>builder()
                .currentPath(currentPath)
                .actual(actualField.getValue())
                .expected(expectedJsonNode)
                .config(params.getConfig())
                .build();
    }
}

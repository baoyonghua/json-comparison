package com.myhexin.autotest.jsoncomparison.compare.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.myhexin.autotest.jsoncomparison.compare.AbstractJsonComparator;
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
        ObjectNode childActualJson = JsonNodeFactory.instance.objectNode();
        ObjectNode childExpectedJson = JsonNodeFactory.instance.objectNode();
        BriefDiffResult result = new BriefDiffResult();
        JsonNode actual = params.getActual();
        JsonNode expected = params.getExpected();
        String currentPath = params.getCurrentPath();
        // 如果两个类型不一致则不予对比, 直接返回
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
            JsonNode actualJsonNode = actualField.getValue();
            String actualPath = buildPath(currentPath, actualFieldName);
            String fieldName = getMappingKey(actualPath, params).orElse(actualFieldName);
            JsonNode expectedJsonNode = expected.get(fieldName);
            if (Objects.isNull(expectedJsonNode)) {
                childActualJson.set(actualFieldName, actualJsonNode);
                result.getBriefDiffs().add(buildExpectedMissKeyDiff(actualPath, actualField));
                continue;
            }
            CompareParams<JsonNode> compareParams =
                    bulidCompareParams(params, actualPath, actualJsonNode, expectedJsonNode);
            // 从工厂中获取对比器进行对比
            BriefDiffResult diffResult =
                    COMPARATOR_FACTORY.executeContrast(actualJsonNode.getNodeType(), compareParams);
            if (diffResult == null || diffResult.getBriefDiffs().isEmpty()) {
                continue;
            }
            childActualJson.set(actualFieldName, diffResult.getChildActualJson());
            childExpectedJson.set(actualFieldName, diffResult.getChildExpectedJson());
            result.getBriefDiffs().addAll(diffResult.getBriefDiffs());
        }
        //找出预期结果中可能多出来的字段<即在实际结果中不存在的字段>
        for (String expectedFieldName : expectedFieldNames) {
            if (!actualFieldNames.contains(expectedFieldName)
                    && !containsMappingKey(expectedFieldName, params)) {
                childExpectedJson.set(expectedFieldName, expected.get(expectedFieldName));
                result.getBriefDiffs().add(buildActualMissKeyDiff(params, expectedFieldName));
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

    private boolean containsMappingKey(String expectedFieldName, CompareParams<ObjectNode> params) {
        Set<JsonCompareConfig.FieldMapping> feildMappings = params.getConfig().getFeildMappings();
        if (feildMappings.isEmpty()) {
            return false;
        }
        // todo 有时候只需要某一个元素的path即可, 不需要进行统一处理
        String expectedCurrentPath = (params.getCurrentPath() + SPLIT_POINT + expectedFieldName)
                .replaceAll("\\[\\d+\\]", "[*]");
        for (JsonCompareConfig.FieldMapping feildMapping : feildMappings) {
            List<String> split = CharSequenceUtil.split(feildMapping.getPath(), SPLIT_POINT);
            if (split.size() < 1) {
                continue;
            }
            List<String> subList = split.subList(0, split.size() - 1);
            String path = CharSequenceUtil.join(SPLIT_POINT, subList) + SPLIT_POINT + expectedFieldName;
            if (feildMapping.getMappingKey().equals(expectedFieldName) && expectedCurrentPath.equals(path)) {
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
    private CompareParams<JsonNode> bulidCompareParams(
            CompareParams<ObjectNode> params, String currentPath, JsonNode actualJsonNode, JsonNode expectedJsonNode) {
        return CompareParams.<JsonNode>builder()
                .currentPath(currentPath)
                .actual(actualJsonNode)
                .expected(expectedJsonNode)
                .config(params.getConfig())
                .build();
    }
}

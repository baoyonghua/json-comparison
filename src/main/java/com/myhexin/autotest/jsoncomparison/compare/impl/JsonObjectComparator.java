package com.myhexin.autotest.jsoncomparison.compare.impl;

import cn.hutool.core.collection.ListUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myhexin.autotest.jsoncomparison.compare.AbstractJsonComparator;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
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
        ArrayList<BriefDiffResult.BriefDiff> diffs = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> actualFields = params.getActual().fields();
        //以实际的字段为基准进行对比
        while (actualFields.hasNext()) {
            Map.Entry<String, JsonNode> field = actualFields.next();
            CompareParams<JsonNode> compareParams = bulidCompareParams(params, field);
            // 如果预期中此字段为null则需要去校验下是否预期中是没有此字段
            if (compareParams.getExpected().isNull()) {
                // 根据上一次的path获取源预期的object json
                ObjectNode node = getObjectNodeByPath(compareParams.getOriginalExcepetd(), compareParams.getPrevPath());
                if (!ListUtil.list(false, node.fieldNames()).contains(field.getKey())) {
                    BriefDiffResult.BriefDiff diff = BriefDiffResult.BriefDiff.builder()
                            .diffKey(compareParams.getCurrentPath())
                            .reason(String.format(CompareMessageConstant.EXPECTED_MISS_KEY, field.getKey()))
                            .actual(compareParams.getActual().toString())
                            .expected(String.format("预期中不存在该key: [%s]", field.getKey()))
                            .build();
                    diffs.add(diff);
                    continue;
                }
            }
            // 从工厂中获取对比器进行对比
            List<BriefDiffResult.BriefDiff> diffList = JsonComparatorFactory.build()
                    .executeContrast(field.getValue().getNodeType(), compareParams);
            diffs.addAll(diffList);
        }
        //找出预期结果中可能多出来的字段<即在实际结果中不存在的字段>
        diffs.addAll(findFieldsInExpected(params));
        return diffs;
    }

    /**
     * 找出只存在在预期中的字段
     *
     * @param params
     * @param <T>
     * @return
     */
    private <T extends JsonNode> ArrayList<BriefDiffResult.BriefDiff> findFieldsInExpected(CompareParams<T> params) {
        //找出预期结果中可能多出来的字段<即在实际结果中不存在的字段>, 因为之前只是以实际结果为基准进行了对比
        Iterator<String> expectedFieldNames = params.getExpected().fieldNames();
        List<String> expectedFieldNameList = ListUtil.list(false, expectedFieldNames);
        Iterator<String> actualFieldNames = params.getActual().fieldNames();
        List<String> actualFieldNameList = ListUtil.list(false, actualFieldNames);
        ArrayList<BriefDiffResult.BriefDiff> briefDiffs = new ArrayList<>();
        for (String fieldName : expectedFieldNameList) {
            if (!actualFieldNameList.contains(fieldName)) {
                BriefDiffResult.BriefDiff diff = BriefDiffResult.BriefDiff.builder()
                        .diffKey(params.getCurrentPath() + "." + fieldName)
                        .reason(CompareMessageConstant.ACTUAL_MISS_KEY)
                        .actual("key miss!!")
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
     * @param field
     * @return
     */
    private CompareParams<JsonNode> bulidCompareParams(
            CompareParams<ObjectNode> params, Map.Entry<String, JsonNode> field) {
        String path = buildPath(params.getCurrentPath(), field.getKey());
        return CompareParams.<JsonNode>builder()
                .currentPath(path)
                .prevPath(params.getCurrentPath())
                .actual(field.getValue())
                .expected(getJsonNodeByPath(params.getOriginalExcepetd(), path))
                .originalExcepetd(params.getOriginalExcepetd())
                .config(params.getConfig())
                .build();
    }
}

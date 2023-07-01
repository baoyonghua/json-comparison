package com.myhexin.autotest.jsoncomparison.compare.impl;

import cn.hutool.core.collection.ListUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
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
        if (isIgnorePath(params.getCurrentPath(), params.getConfig().getIgnorePath())) {
            return Collections.emptyList();
        }
        ArrayList<BriefDiffResult.BriefDiff> diffs = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> actualFields = params.getActual().fields();
        //以实际结果的字段为基准进行对比
        while (actualFields.hasNext()) {
            Map.Entry<String, JsonNode> field = actualFields.next();
            CompareParams<JsonNode> compareParams = buildCompareParams(params, field);
            // 如果预期结果中此字段为null则需要去校验下是否是没有此字段
            if (compareParams.getExpected().isNull()) {
                ObjectNode node = getObjectNodeByPath(compareParams.getOriginalExcepetd(), compareParams.getPrevPath());
                if (!ListUtil.list(false, node.fieldNames()).contains(field.getKey())) {
                    BriefDiffResult.BriefDiff diff = BriefDiffResult.BriefDiff.builder()
                            .diffKey(compareParams.getCurrentPath())
                            .reason(CompareMessageConstant.EXPECTED_MISS_KEY)
                            .actual(compareParams.getActual().toString())
                            .expected("key miss!!")
                            .build();
                    diffs.add(diff);
                    continue;
                }
            }
            List<BriefDiffResult.BriefDiff> diffList = JsonComparatorFactory.build()
                    .executeContrast(field.getValue().getNodeType(), compareParams);
            diffs.addAll(diffList);
        }
        //找出预期结果中可能多出来的字段<即在实际结果中不存在的字段>
        diffs.addAll(findFieldsInExpected(params));
        return diffs;
    }

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

    /**
     * 如果是json对象则返回true否则返回false
     *
     * @param node json中的某一个节点
     * @return
     */
    @Override
    protected boolean check(ObjectNode node) {
        return Arrays.asList(JsonNodeType.OBJECT, JsonNodeType.POJO).contains(node.getNodeType());
    }

    @Override
    public String toString() {
        return "Json对象对比器";
    }

    private CompareParams<JsonNode> buildCompareParams(CompareParams<ObjectNode> params, Map.Entry<String, JsonNode> field) {
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

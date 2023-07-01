package com.myhexin.autotest.jsoncomparison.compare.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.myhexin.autotest.jsoncomparison.compare.AbstractJsonComparator;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


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
        return null;
    }

    /**
     * 如果节点的类型为数组则返回true
     * @param node json中的某一个节点
     * @return
     */
    @Override
    protected boolean check(ArrayNode node) {
        return JsonNodeType.ARRAY.equals(node.getNodeType());
    }

    @Override
    public String toString() {
        return "Json数组对比器";
    }
}

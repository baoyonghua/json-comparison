package com.myhexin;

import cn.hutool.core.lang.Assert;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.impl.JsonBasicComparator;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import org.json.JSONException;
import org.junit.Test;

import java.util.List;

/**
 * @author baoyh
 * @since 2023/6/25
 */
public class TestJsonBasicCompartor {

    @Test
    public void testNull() {
        JsonBasicComparator comparator = new JsonBasicComparator();
        CompareParams<JsonNode> params = CompareParams.<JsonNode>builder()
                .actual(NullNode.instance)
                .expected(NullNode.instance)
                .currentPath("").build();
        List<BriefDiffResult.BriefDiff> diffs = comparator.compare(params);
        Assert.isNull(diffs);
    }

    @Test
    public void testInt() {
        JsonBasicComparator comparator = new JsonBasicComparator();
        CompareParams<JsonNode> params = CompareParams.<JsonNode>builder()
                .actual(IntNode.valueOf(1))
                .expected(IntNode.valueOf(1))
                .currentPath("").build();
        List<BriefDiffResult.BriefDiff> diffs = comparator.compare(params);
        Assert.isNull(diffs);
    }

    @Test
    public void testBoolean() {
        JsonBasicComparator comparator = new JsonBasicComparator();
        CompareParams<JsonNode> params = CompareParams.<JsonNode>builder()
                .actual(BooleanNode.valueOf(true))
                .expected(BooleanNode.valueOf(true))
                .currentPath("").build();
        List<BriefDiffResult.BriefDiff> diffs = comparator.compare(params);
        Assert.isNull(diffs);
    }

    @Test
    public void testString() {
        JsonBasicComparator comparator = new JsonBasicComparator();
        CompareParams<JsonNode> params = CompareParams.<JsonNode>builder()
                .actual(TextNode.valueOf("baoyonghua"))
                .expected(TextNode.valueOf("baoyonghua"))
                .currentPath("").build();
        List<BriefDiffResult.BriefDiff> diffs = comparator.compare(params);
        Assert.isNull(diffs);
    }
}

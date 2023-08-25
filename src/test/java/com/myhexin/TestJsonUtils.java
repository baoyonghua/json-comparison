package com.myhexin;

import cn.hutool.core.io.resource.ResourceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.factory.JsonComparatorFactory;
import com.myhexin.autotest.jsoncomparison.config.JsonCompareConfig;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import com.myhexin.autotest.jsoncomparison.utils.JsonUtils;
import org.junit.Test;
import java.util.*;

/**
 * @author baoyh
 * @since 2023/6/22
 */
public class TestJsonUtils {

    private static final String ACTUAL = ResourceUtil.readUtf8Str("classpath:actual.json");
    private static final String EXCEPTED = ResourceUtil.readUtf8Str("classpath:expected.json");

    @Test
    public void test01() {
        System.out.println(ACTUAL);
    }

    @Test
    public void test02() {
        JsonNode actual = JsonUtils.getJsonNode(ACTUAL);
        JsonNode excepted = JsonUtils.getJsonNode(EXCEPTED);
        JsonCompareConfig config = new JsonCompareConfig();
        // 忽略数组顺序
        JsonCompareConfig.ArrayWithDisorderConfig config2 = JsonCompareConfig.ArrayWithDisorderConfig.builder()
                .path("root.data.share_price")
                .uniqueKey("date")
                .build();
        JsonCompareConfig.ArrayWithDisorderConfig config3 = JsonCompareConfig.ArrayWithDisorderConfig.builder()
                .path("root.data.product_price")
                .uniqueKey("date")
                .build();
        HashSet<JsonCompareConfig.ArrayWithDisorderConfig> set = new HashSet<>(Arrays.asList(config2, config3));
        config.setArrayWithDisorderPath(set);
        CompareParams<JsonNode> params = CompareParams.<JsonNode>builder()
                .actual(actual)
                .expected(excepted)
                .config(config)
                .build();
        List<BriefDiffResult.BriefDiff> diffs = JsonComparatorFactory.build()
                .execute(actual.getNodeType(), params);
        String jsonStr = JsonUtils.toJsonString(diffs);
        System.out.println(jsonStr);
    }
}

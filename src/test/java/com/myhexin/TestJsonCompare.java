package com.myhexin;

import cn.hutool.core.io.resource.ResourceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.factory.impl.JsonComparatorFactory;
import com.myhexin.autotest.jsoncomparison.config.JsonCompareConfig;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import com.myhexin.autotest.jsoncomparison.utils.JsonUtils;
import org.junit.Test;

import java.util.*;

/**
 * @author baoyh
 * @since 2023/6/22
 */
public class TestJsonCompare {

    @Test
    public void testBizJson() {
        final String ACTUAL = ResourceUtil.readUtf8Str("classpath:actual.json");
        final String EXCEPTED = ResourceUtil.readUtf8Str("classpath:expected.json");
        JsonNode actual = JsonUtils.getJsonNode(ACTUAL);
        JsonNode excepted = JsonUtils.getJsonNode(EXCEPTED);
        JsonCompareConfig config = new JsonCompareConfig();
        HashSet<JsonCompareConfig.ArrayWithDisorderConfig> disorderConfigs = new HashSet<>();
        JsonCompareConfig.ArrayWithDisorderConfig disorderConfig = new JsonCompareConfig.ArrayWithDisorderConfig();
        disorderConfig.setPath("root.data.product_list");
        disorderConfig.setUniqueKey("product_id");

        JsonCompareConfig.ArrayWithDisorderConfig disorderConfig1 = new JsonCompareConfig.ArrayWithDisorderConfig();
        disorderConfig1.setPath("root.data.product_price");
        disorderConfig1.setUniqueKey("date");

        JsonCompareConfig.ArrayWithDisorderConfig disorderConfig2 = new JsonCompareConfig.ArrayWithDisorderConfig();
        disorderConfig2.setPath("root.data.share_price");
        disorderConfig2.setUniqueKey("date");

        disorderConfigs.add(disorderConfig);
        disorderConfigs.add(disorderConfig1);
        disorderConfigs.add(disorderConfig2);
        config.setArrayWithDisorderPath(disorderConfigs);
        CompareParams<JsonNode> params = CompareParams.<JsonNode>builder()
                .actual(actual)
                .expected(excepted)
                .config(config)
                .build();
        BriefDiffResult diffResult = JsonComparatorFactory.build()
                .execute(actual.getNodeType(), params);
        String jsonStr = JsonUtils.toJsonString(diffResult);
        System.out.println(jsonStr);
    }

    @Test
    public void testMultiNestingArray() {
        final String ACTUAL = ResourceUtil.readUtf8Str("classpath:multiNestingArray/actual.json");
        final String EXCEPTED = ResourceUtil.readUtf8Str("classpath:multiNestingArray/expected.json");
        JsonNode actual = JsonUtils.getJsonNode(ACTUAL);
        JsonNode excepted = JsonUtils.getJsonNode(EXCEPTED);
        JsonCompareConfig config = new JsonCompareConfig();
        CompareParams<JsonNode> params = CompareParams.<JsonNode>builder()
                .actual(actual)
                .expected(excepted)
                .config(config)
                .build();
        BriefDiffResult diffResult = JsonComparatorFactory.build()
                .execute(actual.getNodeType(), params);
        String jsonStr = JsonUtils.toJsonString(diffResult);
        System.out.println(jsonStr);
    }

    @Test
    public void testNormalJson() {
        final String ACTUAL = ResourceUtil.readUtf8Str("classpath:normal/actual.json");
        final String EXCEPTED = ResourceUtil.readUtf8Str("classpath:normal/expected.json");
        JsonNode actual = JsonUtils.getJsonNode(ACTUAL);
        JsonNode excepted = JsonUtils.getJsonNode(EXCEPTED);
        JsonCompareConfig config = new JsonCompareConfig();
        CompareParams<JsonNode> params = CompareParams.<JsonNode>builder()
                .actual(actual)
                .expected(excepted)
                .config(config)
                .build();
        BriefDiffResult diffResult = JsonComparatorFactory.build()
                .execute(actual.getNodeType(), params);
        String jsonStr = JsonUtils.toJsonString(diffResult);
        System.out.println(jsonStr);
    }
}

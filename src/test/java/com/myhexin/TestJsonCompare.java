package com.myhexin;

import cn.hutool.core.io.resource.ResourceUtil;
import com.fasterxml.jackson.databind.JsonNode;
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
public class TestJsonCompare {

    @Test
    public void testBizJson() {
        final String ACTUAL = ResourceUtil.readUtf8Str("classpath:actual.json");
        final String EXCEPTED = ResourceUtil.readUtf8Str("classpath:expected.json");
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
        JsonCompareConfig.ArrayWithDisorderConfig arrayWithDisorderConfig = JsonCompareConfig.ArrayWithDisorderConfig.builder()
                .path("root.data.list1").uniqueKey("date1").build();
        JsonCompareConfig.FieldMapping fieldMapping = JsonCompareConfig.FieldMapping.builder()
                .path("root.data.list[*].cate").mappingKey("cate1").build();
        JsonCompareConfig.FieldMapping fieldMapping1 = JsonCompareConfig.FieldMapping.builder()
                .path("root.code").mappingKey("status_code").build();
        HashSet<JsonCompareConfig.ArrayWithDisorderConfig> arrayWithDisorderConfigs = new HashSet<>();
        HashSet<JsonCompareConfig.FieldMapping> fieldMappings = new HashSet<>();
        arrayWithDisorderConfigs.add(arrayWithDisorderConfig);
        fieldMappings.add(fieldMapping);
        fieldMappings.add(fieldMapping1);
        config.setArrayWithDisorderPath(arrayWithDisorderConfigs);
        config.setFeildMappings(fieldMappings);
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

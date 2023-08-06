package com.myhexin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.factory.JsonComparatorFactory;
import com.myhexin.autotest.jsoncomparison.config.JsonCompareConfig;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import com.myhexin.autotest.jsoncomparison.utils.JsonUtils;
import org.junit.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author baoyh
 * @since 2023/6/22
 */
public class TestJsonUtils {

    private static final String JSON = "{\n" +
            "    \"id\": \"42100b46601e4352a4c589b1fc69c082\",\n" +
            "    \"record_id\": 21,\n" +
            "    \"biz_name\": \"b2cweb-java-api\",\n" +
            "    \"interface_id\": 1,\n" +
            "    \"contrast_interface_id\": 1,\n" +
            "    \"case_id\": 12,\n" +
            "  \"info\": [\n" +
            "    {\"id\": 1, \"name\": \"baoyonghua\"},\n" +
            "    {\"id\": 2, \"name\": \"huanglin\"}\n" +
            "  ]\n" +
            "}\n";

    private static final String ACTUAL = "{\n" +
            "  \"name\": \"John Doe\",\n" +
            "  \"age\": 30,\n" +
            "  \"email\": \"johndoe@example.com\",\n" +
            "  \"address\": {\n" +
            "    \"street\": \"123 Main St\",\n" +
            "    \"city\": \"New York\",\n" +
            "    \"state\": \"NY\"\n" +
            "  },\n" +
            "  \"phone_numbers\": [\n" +
            "    \"123-456-7891\",\n" +
            "    \"987-654-3210\"\n" +
            "  ],\n" +
            "  \"interests\": [\n" +
            "    \"Sports\",\n" +
            "    \"Music\",\n" +
            "    \"Travel\"\n" +
            "  ],\n" +
            "  \"education\": {\n" +
            "    \"degree\": \"Bachelor's\",\n" +
            "    \"university\": \"ABC University\"\n" +
            "  },\n" +
            "  \"friends\": [\n" +
            "    {\n" +
            "      \"name\": \"Jane Smith\",\n" +
            "      \"age\": 28,\n" +
            "      \"email\": \"janesmith@example.com\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Bob Johnson\",\n" +
            "      \"age\": 32,\n" +
            "      \"email\": \"bobjohnson@example.com\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private static final String EXCEPTED = "{\n" +
            "  \"name\": \"John Doe\",\n" +
            "  \"age\": 30,\n" +
            "  \"email\": \"johndoe@example.com\",\n" +
            "  \"address\": {\n" +
            "    \"street\": \"123 Main St\",\n" +
            "    \"city\": \"New York\",\n" +
            "    \"state\": \"NY\"\n" +
            "  },\n" +
            "  \"phone_numbers\": [\n" +
            "    \"123-456-7890\",\n" +
            "    \"987-654-3210\"\n" +
            "  ],\n" +
            "  \"interests\": [\n" +
            "    \"Sports\",\n" +
            "    \"Music\",\n" +
            "    \"Travel\"\n" +
            "  ],\n" +
            "  \"education\": {\n" +
            "    \"degree\": \"Bachelor's\",\n" +
            "    \"university\": \"ABC University\"\n" +
            "  },\n" +
            "  \"friends\": [\n" +
            "    {\n" +
            "      \"name\": \"Jane Smith\",\n" +
            "      \"age\": 28,\n" +
            "      \"email\": \"janesmith@example.com\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Bob Johnson\",\n" +
            "      \"age\": 32,\n" +
            "      \"email\": \"bobjohnson@example.com\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @Test
    public void test01() {
        JsonNode node = JsonUtils.getJsonNode(JSON);
        System.out.println(node);
        System.out.println(node.getNodeType());
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            System.out.println(field.getKey());
            System.out.println(field.getValue());
        }
    }

    @Test
    public void test02() {
        JsonNode actual = JsonUtils.getJsonNode(ACTUAL);
        JsonNode excepted = JsonUtils.getJsonNode(EXCEPTED);

        JsonCompareConfig config = new JsonCompareConfig();
        config.setIgnorePath(new HashSet<>());
        config.setExcapedJsonPath(new HashSet<>());
        CompareParams<JsonNode> params = CompareParams.<JsonNode>builder()
                .actual(actual)
                .expected(excepted)
                .originalExcepetd(EXCEPTED)
                .config(config)
                .build();
        List<BriefDiffResult.BriefDiff> diffs = JsonComparatorFactory.build()
                .executeContrast(JsonNodeType.OBJECT, params);
        diffs.forEach(System.out::println);
    }


}

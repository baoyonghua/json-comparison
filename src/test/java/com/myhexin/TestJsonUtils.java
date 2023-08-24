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
            "  \"employees\": [\n" +
            "    {\n" +
            "      \"name\": \"John Doe\",\n" +
            "      \"age\": 30,\n" +
            "      \"skills\": [\n" +
            "        {\n" +
            "          \"language\": \"Python\",\n" +
            "          \"level\": \"Intermediate\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"language\": \"JavaScript\",\n" +
            "          \"level\": \"Advanced\"\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Jane Smith\",\n" +
            "      \"age\": 28,\n" +
            "      \"skills\": [\n" +
            "        {\n" +
            "          \"language\": \"Java\",\n" +
            "          \"level\": \"Advanced\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"language\": \"C++\",\n" +
            "          \"level\": \"Intermediate\"\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Bob Johnson\",\n" +
            "      \"age\": 32,\n" +
            "      \"skills\": [\n" +
            "        {\n" +
            "          \"language\": \"Ruby\",\n" +
            "          \"level\": \"Intermediate\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"language\": \"PHP\",\n" +
            "          \"level\": \"Advanced\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private static final String EXCEPTED = "{\n" +
            "  \"employees\": [\n" +
            "    {\n" +
            "      \"name\": \"John Doe\",\n" +
            "      \"age\": 31,\n" +
            "      \"skills\": [\n" +
            "        {\n" +
            "          \"language\": \"Python\",\n" +
            "          \"level\": \"Intermediate\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"language\": \"JavaScript\",\n" +
            "          \"level\": \"Advanced\"\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Jane Smith\",\n" +
            "      \"age\": 25,\n" +
            "      \"skills\": [\n" +
            "        {\n" +
            "          \"language\": \"Java\",\n" +
            "          \"level\": \"Advanced\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"language\": \"C++\",\n" +
            "          \"level\": \"Intermediate\"\n" +
            "        }\n" +
            "      ]\n" +
            "    },\n" +
            "    {\n" +
            "      \"name\": \"Bob Johnson\",\n" +
            "      \"age\": 32,\n" +
            "      \"skills\": [\n" +
            "        {\n" +
            "          \"language\": \"Ruby1\",\n" +
            "          \"level\": \"Intermediate\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"language\": \"PHP1\",\n" +
            "          \"level\": \"Advanced\"\n" +
            "        }\n" +
            "      ]\n" +
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
        HashSet<String> set = new HashSet<>();
        set.add("root.employees[2].skills");
        config.setArrayWithDisorderPath(set);
        CompareParams<JsonNode> params = CompareParams.<JsonNode>builder()
                .actual(actual)
                .expected(excepted)
                .config(config)
                .build();
        List<BriefDiffResult.BriefDiff> diffs = JsonComparatorFactory.build()
                .execute(JsonNodeType.OBJECT, params);
        String jsonStr = JsonUtils.toJsonString(diffs);
        System.out.println(jsonStr);
    }
}

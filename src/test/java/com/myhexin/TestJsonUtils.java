package com.myhexin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.factory.JsonComparatorFactory;
import com.myhexin.autotest.jsoncomparison.config.JsonCompareConfig;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import com.myhexin.autotest.jsoncomparison.utils.JsonUtils;
import org.junit.Test;

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
            "  \"field1\": null,\n" +
            "  \"field101\": 101,\n" +
            "  \"field2\": true,\n" +
            "  \"field3\": {\n" +
            "    \"nested1\": 123,\n" +
            "    \"nested2\": {\n" +
            "      \"nested3\": \"abc\",\n" +
            "      \"nested4\": {\n" +
            "        \"nested5\": \"xyz\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"nested6\": {\n" +
            "      \"nested7\": \"def\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"field4\": {\n" +
            "    \"nested8\": {\n" +
            "      \"nested9\": \"ghi\",\n" +
            "      \"nested10\": \"jkl\",\n" +
            "      \"nested100\": \"jkl\"\n" +
            "    },\n" +
            "    \"nested11\": {\n" +
            "      \"nested12\": \"mno\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"field5\": \"value2\",\n" +
            "  \"field6\": {\n" +
            "    \"nested13\": {\n" +
            "      \"nested14\": {\n" +
            "        \"nested15\": {\n" +
            "          \"nested16\": {\n" +
            "            \"nested17\": {\n" +
            "              \"nested18\": {\n" +
            "                \"nested19\": {\n" +
            "                  \"nested20\": {\n" +
            "                    \"nested21\": {\n" +
            "                      \"nested22\": {\n" +
            "                        \"nested23\": {\n" +
            "                          \"nested24\": {\n" +
            "                            \"nested25\": \"pqr\"\n" +
            "                          }\n" +
            "                        }\n" +
            "                      }\n" +
            "                    }\n" +
            "                  }\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
            "}";

    private static final String EXCEPTED = "{\n" +
            "  \"field1\": \"value1\",\n" +
            "  \"field2\": true,\n" +
            "  \"field3\": {\n" +
            "    \"nested1\": 123,\n" +
            "    \"nested2\": {\n" +
            "      \"nested3\": \"abc\",\n" +
            "      \"nested4\": {\n" +
            "        \"nested5\": \"xyz\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"nested6\": {\n" +
            "      \"nested7\": \"def\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"field4\": {\n" +
            "    \"nested8\": {\n" +
            "      \"nested9\": \"ghi\",\n" +
            "      \"nested10\": \"jkl\",\n" +
            "      \"nested200\": \"jkl\"\n" +
            "    },\n" +
            "    \"nested11\": {\n" +
            "      \"nested12\": \"mno\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"field5\": \"value2\",\n" +
            "  \"field102\": \"value102\",\n" +
            "  \"field6\": {\n" +
            "    \"nested13\": {\n" +
            "      \"nested14\": {\n" +
            "        \"nested15\": {\n" +
            "          \"nested16\": {\n" +
            "            \"nested17\": {\n" +
            "              \"nested18\": {\n" +
            "                \"nested19\": {\n" +
            "                  \"nested20\": {\n" +
            "                    \"nested21\": {\n" +
            "                      \"nested22\": {\n" +
            "                        \"nested23\": {\n" +
            "                          \"nested24\": {\n" +
            "                            \"nested25\": \"pqr\"\n" +
            "                          }\n" +
            "                        }\n" +
            "                      }\n" +
            "                    }\n" +
            "                  }\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          }\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  }\n" +
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
        CompareParams<JsonNode> params = CompareParams.<JsonNode>builder()
                .actual(actual)
                .expected(excepted)
                .originalExcepetd(EXCEPTED)
                .config(config)
                .build();
        List<BriefDiffResult.BriefDiff> diffs = JsonComparatorFactory.build().executeContrast(JsonNodeType.OBJECT, params);
        diffs.forEach(System.out::println);
    }
}

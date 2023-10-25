package com.myhexin.autotest.jsoncomparison.compare.factory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.JsonComparator;
import com.myhexin.autotest.jsoncomparison.compare.impl.JsonArrayComparator;
import com.myhexin.autotest.jsoncomparison.compare.impl.JsonBasicComparator;
import com.myhexin.autotest.jsoncomparison.compare.impl.JsonObjectComparator;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

/**
 * JSON对比器的工厂
 *
 * @author baoyh
 * @since 2023/6/21
 */
@Slf4j
public final class JsonComparatorFactory {

    private static final Map<JsonNodeType, JsonComparator<? extends JsonNode>> COMPARATOR_MAP =
            new EnumMap<>(JsonNodeType.class);

    private static final JsonComparatorFactory FACTORY = new JsonComparatorFactory();

    public static JsonComparatorFactory build() {
        return FACTORY;
    }

    @SuppressWarnings("unchecked")
    public <T extends JsonNode> JsonComparator<T> getJsonComparator(JsonNodeType nodeType) {
        if (COMPARATOR_MAP.get(nodeType) != null) {
            return (JsonComparator<T>) COMPARATOR_MAP.get(nodeType);
        }
        JsonComparator<? extends JsonNode> comparator;
        switch (nodeType) {
            case ARRAY:
                comparator = new JsonArrayComparator();
                break;
            case OBJECT:
            case POJO:
                comparator = new JsonObjectComparator();
                break;
            default:
                comparator = new JsonBasicComparator();
                break;
        }
        COMPARATOR_MAP.put(nodeType, comparator);
        return (JsonComparator<T>) comparator;
    }

    public BriefDiffResult execute(JsonNodeType nodeType, CompareParams<JsonNode> params) {
        log.info("开始进行两个Json之间的对比");
        long begin = System.currentTimeMillis();
        BriefDiffResult result = new BriefDiffResult();
        if (!params.getExpected().toString().equals(params.getActual().toString())) {
            result = executeContrast(nodeType, params);
        }
        log.info("当前对比操作完成, 当前两个Json之间的的差异数为: [{}], 当前Json对比耗时: [{}]",
                result.getDiffNum(), System.currentTimeMillis() - begin);
        return result;
    }

    /**
     * 启动对比
     *
     * @param nodeType 当前的json类型Node
     * @param params   当前对比时所必须的参数
     * @return
     */
    public BriefDiffResult executeContrast(JsonNodeType nodeType, CompareParams<JsonNode> params) {
        JsonComparator<JsonNode> comparator = getJsonComparator(nodeType);
        BriefDiffResult result = new BriefDiffResult();
        boolean needToCompare = comparator.beforeCompare(params, result);
        if (needToCompare) {
            result = comparator.compare(params);
        }
        comparator.afterCompare(result);
        return result;
    }
}

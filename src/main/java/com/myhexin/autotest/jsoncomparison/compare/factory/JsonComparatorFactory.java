package com.myhexin.autotest.jsoncomparison.compare.factory;

import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.compare.JsonComparator;
import com.myhexin.autotest.jsoncomparison.compare.impl.JsonArrayComparator;
import com.myhexin.autotest.jsoncomparison.compare.impl.JsonBasicComparator;
import com.myhexin.autotest.jsoncomparison.compare.impl.JsonObjectComparator;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * JSON对比器的工厂
 *
 * @author baoyh
 * @since 2023/6/21
 */
public final class JsonComparatorFactory {

    private final static Map<JsonNodeType, JsonComparator<?>> COMPARATOR_MAP =
            new HashMap<>(8);

    private JsonComparatorFactory() {

    }

    public static JsonComparatorFactory build() {
        return new JsonComparatorFactory();
    }

    public JsonComparator<?> getJsonComparator(JsonNodeType nodeType) {
        if (COMPARATOR_MAP.get(nodeType) != null) {
            return COMPARATOR_MAP.get(nodeType);
        }
        JsonComparator<?> comparator;
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
        return comparator;
    }


    @SuppressWarnings("unchecked")
    public List<BriefDiffResult.BriefDiff> executeContrast(JsonNodeType nodeType, CompareParams params) {
        JsonComparator<?> comparator = getJsonComparator(nodeType);
        comparator.beforeCompare(params);
        List<BriefDiffResult.BriefDiff> diffs = comparator.compare(params);
        comparator.afterCompare();
        return diffs.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }
}

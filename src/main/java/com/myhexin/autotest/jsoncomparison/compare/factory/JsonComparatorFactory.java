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
import java.util.stream.Collectors;

/**
 * JSON对比器的工厂
 *
 * @author baoyh
 * @since 2023/6/21
 */
@Slf4j
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

    public List<BriefDiffResult.BriefDiff> execute(JsonNodeType nodeType, CompareParams<JsonNode> params) {
        log.info("开始进行两个Json之间的对比");
        long begin = System.currentTimeMillis();
        List<BriefDiffResult.BriefDiff> diffs = executeContrast(nodeType, params);
        log.info("当前对比操作完成, 当前两个Json之间的的差异数为: [{}], 当前Json对比耗时: [{}]",
                diffs.size(), System.currentTimeMillis() - begin);
        return diffs;
    }

    /**
     * 启动对比
     *
     * @param nodeType 当前的json类型Node
     * @param params   当前对比时所必须的参数
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<BriefDiffResult.BriefDiff> executeContrast(JsonNodeType nodeType, CompareParams params) {
        if (isIgnorePath(params.getCurrentPath(), params.getConfig().getIgnorePath())) {
            return Collections.emptyList();
        }
        JsonComparator<?> comparator = getJsonComparator(nodeType);
        comparator.beforeCompare(params);
        List<BriefDiffResult.BriefDiff> diffs = comparator.compare(params);
        comparator.afterCompare();
        return diffs.stream().filter(Objects::nonNull).collect(Collectors.toList());
    }

    /**
     * 是否是需要忽略的path
     *
     * @param path        当前path
     * @param ignorePaths
     * @return
     */
    private boolean isIgnorePath(String path, Set<String> ignorePaths) {
        path = JsonComparator.cropPath2JmesPath(path);
        return Objects.nonNull(ignorePaths) && !ignorePaths.isEmpty()
                && !ignorePaths.contains(path);
    }
}

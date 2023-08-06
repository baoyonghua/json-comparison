package com.myhexin.autotest.jsoncomparison.compare;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myhexin.autotest.jsoncomparison.compare.constant.CompareMessageConstant;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import com.myhexin.autotest.jsoncomparison.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.Set;


/**
 * JSON对比器抽象实现
 *
 * @author baoyh
 * @since 2023/6/21
 */
@Slf4j
public abstract class AbstractJsonComparator<T extends JsonNode> implements JsonComparator<T> {

    protected static final String ROOT_PATH = "root";

    @Override
    public void beforeCompare(CompareParams<T> params) {
        if (isIgnorePath(params.getCurrentPath(), params.getConfig().getIgnorePath())) {
            return;
        }
        if (CharSequenceUtil.isBlank(params.getCurrentPath())) {
            params.setCurrentPath(ROOT_PATH);
        }
    }

    /**
     * 校验两个JsonType的类型是否一致
     *
     * @param path     当前路径
     * @param actual   实际的Json
     * @param expected 预期的Json
     * @return
     */
    protected BriefDiffResult.BriefDiff checkJsonNodeType(
            String path, JsonNode actual, JsonNode expected) {
        // 如果俩个jsonNode对象的type不一致则需要校验
        if (actual.getNodeType() != expected.getNodeType()) {
            log.warn("当前两个Json对象的类型不匹配, 无法参与对比！");
            String reason;
            if (actual.getNodeType() == JsonNodeType.NULL) {
                reason = CompareMessageConstant.ONLY_IN_EXPECTED;
            } else if (expected.getNodeType() == JsonNodeType.NULL) {
                reason = CompareMessageConstant.ONLY_IN_ACTUAL;
            } else {
                reason = String.format(
                        CompareMessageConstant.TYPE_UNEQUALS, actual.getNodeType(), expected.getNodeType()
                );
            }
            return BriefDiffResult.BriefDiff.builder()
                    .actual(actual.asText())
                    .expected(expected.asText())
                    .diffKey(path)
                    .reason(reason)
                    .build();
        }
        return null;
    }

    /**
     * 是否是需要忽略的path
     *
     * @param path        当前path
     * @param ignorePaths
     * @return
     */
    protected boolean isIgnorePath(String path, Set<String> ignorePaths) {
        path = cropPath2JmesPath(path);
        return Objects.nonNull(ignorePaths) && !ignorePaths.isEmpty()
                && !ignorePaths.contains(path);
    }

    @Override
    public void afterCompare() {

    }

    /**
     * 解析Json并根据path来获取具体JsonNode
     *
     * @param jsonStr json字符串
     * @param path    路径
     * @return
     */
    protected JsonNode getJsonNodeByPath(String jsonStr, String path) {
        if (CharSequenceUtil.isBlank(path)) {
            return JsonUtils.getJsonNode(jsonStr);
        }
        String jmesPath = cropPath2JmesPath(path);
        if (CharSequenceUtil.isBlank(jmesPath)) {
            return JsonUtils.getJsonNode(jsonStr);
        }
        return JsonUtils.getJsonNodeByJmesPath(jsonStr, jmesPath);
    }

    protected ObjectNode getObjectNodeByPath(String jsonStr, String path) {
        JsonNode node = getJsonNodeByPath(jsonStr, path);
        if (node.isObject()) {
            return ((ObjectNode) node);
        }
        throw new IllegalArgumentException("该Json不是对象！当前类型: " + node.getNodeType());
    }

    /**
     * 将Path转换为实际的jmesPath
     *
     * @param path 内部path
     * @return
     */
    private String cropPath2JmesPath(String path) {
        if (CharSequenceUtil.isBlank(path)) {
            return path;
        }
        if (path.length() == ROOT_PATH.length()) {
            return "";
        }
        if (path.startsWith(ROOT_PATH)) {
            path = path.substring(ROOT_PATH.length() + 1);
        }
        return path;
    }

}

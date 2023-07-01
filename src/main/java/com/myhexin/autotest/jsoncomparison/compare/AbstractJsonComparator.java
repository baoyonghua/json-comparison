package com.myhexin.autotest.jsoncomparison.compare;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.myhexin.autotest.jsoncomparison.utils.JsonUtils;

import java.util.Objects;
import java.util.Set;


/**
 * JSON对比器抽象实现
 *
 * @author baoyh
 * @since 2023/6/21
 */
public abstract class AbstractJsonComparator<T extends JsonNode> implements JsonComparator<T> {

    protected static final String ROOT_PATH = "root";

    @Override
    public void beforeCompare(CompareParams<T> params) {
        if (!check(params.getActual()) || !check(params.getExpected())) {
            throw new IllegalArgumentException(
                    CharSequenceUtil.format(
                            "预期的参数或者实际的参数并不是预期的数据类型！预期的参数类型: {}, 实际的参数类型: {}, 当前Json比较器: {}",
                            params.getActual().getNodeType(), params.getExpected().getNodeType(), this.toString()
                    )
            );
        }
        if (CharSequenceUtil.isBlank(params.getCurrentPath())) {
            params.setCurrentPath(ROOT_PATH);
        }
    }

    protected boolean isIgnorePath(String path, Set<String> ignorePaths) {
        path = cropPath2JmesPath(path);
        return Objects.nonNull(ignorePaths) && !ignorePaths.isEmpty()
                && !ignorePaths.contains(path);
    }

    @Override
    public void afterCompare() {

    }

    /**
     * 校验json节点是否是预期的类型, 由子类提供实现
     *
     * @param node json中的某一个节点
     * @return
     */
    protected abstract boolean check(T node);

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

    protected String buildPath(String oldPath, String fieldName) {
        return oldPath + "." + fieldName;
    }

}

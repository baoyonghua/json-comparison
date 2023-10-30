package com.myhexin.autotest.jsoncomparison.compare.factory;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.myhexin.autotest.jsoncomparison.compare.CompareParams;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;

import java.util.Objects;
import java.util.Set;

/**
 * JSON对比器
 *
 * @param <T> 需要对比的JSON类型, 例如 JsonObject or JsonArray
 * @author baoyonghua@myhexin.com
 * @date 2023/6/21
 */
public interface JsonComparator<T extends JsonNode> {

    String ROOT_PATH = "$";

    String SPLIT_POINT = ".";

    String REGEX = "\\[\\d+\\]";

    String ARRAY_REPLACEMENT = "[*]";

    /**
     * 在对比之前进行一些操作
     *
     * @param params json对比所需要的参数
     * @param result
     * @return 是否需要进行对比
     */
    boolean beforeCompare(CompareParams<T> params, BriefDiffResult result);

    /**
     * Json对比
     *
     * @param params json对比所需要的参数
     * @return 如果存在差异则返回差异信息, 否则返回null
     */
    BriefDiffResult compare(CompareParams<T> params);

    /**
     * 在对比之后进行一些操作
     *
     * @param result
     */
    void afterCompare(BriefDiffResult result);

}

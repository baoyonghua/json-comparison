package com.myhexin.autotest.jsoncomparison.compare;

import cn.hutool.core.text.CharSequenceUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.myhexin.autotest.jsoncomparison.result.BriefDiffResult;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;

/**
 * JSON对比器
 *
 * @param <T> 需要对比的JSON类型, 例如JsonObject or JsonArray
 * @author baoyonghua@myhexin.com
 * @date 2023/6/21
 */
public interface JsonComparator<T extends JsonNode> {

    String ROOT_PATH = "root";

    /**
     * 在对比之前进行一些操作
     *
     * @param params json对比所需要的参数
     */
    void beforeCompare(CompareParams<T> params);

    /**
     * 执行对比
     *
     * @param params json对比所需要的参数
     * @return 如果存在差异则返回差异信息, 否则返回null
     */
    List<BriefDiffResult.BriefDiff> compare(CompareParams<T> params);

    /**
     * 在对比之后进行一些操作
     */
    void afterCompare();

    /**
     * 根据JsonAssert库进行对比
     *
     * @param expected
     * @param actual
     * @param strict
     * @return 对比的结果信息
     * @throws JSONException
     */
    default String compareByJsonAssert(String expected, String actual, boolean strict) throws JSONException {
        String reason = "";
        try {
            JSONAssert.assertEquals(expected, actual, strict);
        } catch (AssertionError e) {
            reason = e.getMessage();
        }
        return reason;
    }

    /**
     * 将Path转换为实际的jmesPath
     *
     * @param path 内部path
     * @return
     */
    static String cropPath2JmesPath(String path) {
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

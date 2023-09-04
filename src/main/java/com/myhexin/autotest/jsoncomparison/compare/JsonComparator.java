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
 * @param <T> 需要对比的JSON类型, 例如 JsonObject or JsonArray
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
    BriefDiffResult compare(CompareParams<T> params);

    /**
     * 在对比之后进行一些操作
     */
    void afterCompare();

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

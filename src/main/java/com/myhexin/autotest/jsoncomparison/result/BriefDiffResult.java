package com.myhexin.autotest.jsoncomparison.result;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author baoyh
 * @since 2023/6/25
 */
@Data
@SuppressWarnings("all")
public class BriefDiffResult implements Serializable {

    /**
     * 当前的对比所产生的差异信息
     */
    @JsonProperty("brief_diffs")
    private List<BriefDiff> briefDiffs = new ArrayList<>();

    /**
     * 差异数
     */
    @JsonProperty("diff_num")
    private Integer diffNum = 0;

    @JsonProperty("child_actual_json")
    private JsonNode childActualJson = JsonNodeFactory.instance.objectNode();

    @JsonProperty("child_expected_json")
    private JsonNode childExpectedJson = JsonNodeFactory.instance.objectNode();

    public BriefDiffResult() {
        briefDiffs = new ArrayList<>();
        diffNum = 0;
    }


    @Data
    @Builder
    public static class BriefDiff implements Serializable {

        /**
         * 差异类型
         */
        private Integer type;

        /**
         * 当前差异类型的描述
         */
        private String msg;

        /**
         * 预期值
         */
        private String expected;

        /**
         * 实际值
         */
        private String actual;

        /**
         * 产生差异的key, 针对于实际的json而言
         */
        @JsonProperty("diff_key")
        private String diffKey;

        /**
         * 当前差异原因
         */
        private String reason;

        /**
         * 子差异信息(针对需要进行转义的字符串)
         */
        @JsonProperty("sub_diffs")
        private List<BriefDiff> subDiffs;
    }
}

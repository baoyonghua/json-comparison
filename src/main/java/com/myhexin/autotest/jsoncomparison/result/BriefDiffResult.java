package com.myhexin.autotest.jsoncomparison.result;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author baoyh
 * @since 2023/6/25
 */
@Data
public class BriefDiffResult implements Serializable {

    /**
     * 当前的对比所产生的差异信息
     */
    private List<BriefDiff> briefDiffs;

    @Data
    @Builder
    public static class BriefDiff implements Serializable {

        /**
         * 差异类型
         */
        private Integer type;

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
        private String diffKey;

        /**
         * 当前差异原因
         */
        private String reason;

        /**
         * 子差异信息(针对需要进行转移的字符串)
         */
        private List<BriefDiff> subDiffs;
    }
}

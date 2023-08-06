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

    private List<BriefDiff> briefDiffs;

    @Data
    @Builder
    public static class BriefDiff implements Serializable {
        private String expected;

        private String actual;

        private String diffKey;

        private String reason;

        private List<BriefDiff> subDiffs;
    }
}

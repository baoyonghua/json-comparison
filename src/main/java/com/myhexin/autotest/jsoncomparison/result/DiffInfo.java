package com.myhexin.autotest.jsoncomparison.result;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.io.Serializable;

/**
 * 差异的具体信息
 * 产生当前差异的具体差异信息
 * @author baoyh
 * @since 2023/9/4
 */
@Data
public class DiffInfo implements Serializable {

    /**
     * 实际Json的子Json
     */
    private JsonNode childActualJson;

    /**
     * 预期Json的子Json
     */
    private JsonNode childExpectedJson;
}

package com.myhexin.autotest.jsoncomparison.exception;

import lombok.Getter;

/**
 * @author baoyh
 * @since 2023/6/22
 */
@Getter
public class JsonParseException extends RuntimeException {

    private String message;

    public JsonParseException(String message, Throwable e) {
        super(message, e);
    }

}

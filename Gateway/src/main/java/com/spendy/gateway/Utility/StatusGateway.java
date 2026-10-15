package com.spendy.gateway.Utility;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Gateway operation result status enumeration", enumAsRef = true)
public enum StatusGateway {
    TOKEN_GENERATION_SUCCESS,
    TOKEN_GENERATION_FAILED,
    USER_NOT_FOUND,
    USER_ALREADY_EXISTS,
    INVALID_CREDENTIALS,
    TOKEN_VERIFICATION_SUCCESS,
    TOKEN_VERIFICATION_FAILED,
    INTERNAL_ERROR
}

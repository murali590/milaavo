
package com.milaavo.common.dto;

import java.util.Map;

public record ApiErrorResponse(
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
}

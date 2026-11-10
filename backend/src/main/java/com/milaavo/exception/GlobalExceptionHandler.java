
package com.milaavo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import com.milaavo.common.dto.ApiErrorResponse;
import com.milaavo.otp.exception.InvalidOtpException;
import com.milaavo.otp.exception.OtpResendRateLimitException;

import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import com.milaavo.auth.exception.UserAlreayActiveException;
import com.milaavo.auth.exception.AccountDisabledException;
import com.milaavo.auth.exception.InvalidRefreshTokenException;
import com.milaavo.auth.exception.InvalidVerificationRequestException;
import com.milaavo.auth.exception.MobileNotVerifiedException;

import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ApiErrorResponse> handleValidationException(
                        MethodArgumentNotValidException exception) {

                Map<String, String> fieldErrors = new TreeMap<>();

                exception.getBindingResult()
                                .getFieldErrors()
                                .forEach(error -> fieldErrors.putIfAbsent(
                                                error.getField(),
                                                error.getDefaultMessage()));

                ApiErrorResponse response = new ApiErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "VALIDATION_ERROR",
                                "Request validation failed",
                                fieldErrors);

                return ResponseEntity.badRequest().body(response);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadableException(
                        HttpMessageNotReadableException exception) {
                ApiErrorResponse response = new ApiErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "INVALID_REQUEST_BODY",
                                "Request body is missing or contains invalid JSON",
                                null);

                return ResponseEntity.badRequest().body(response);
        }

        @ExceptionHandler(HandlerMethodValidationException.class)
        public ResponseEntity<ApiErrorResponse> handleHandlerMethodValidationException(
                        HandlerMethodValidationException exception) {

                Map<String, String> fieldErrors = new TreeMap<>();

                exception.getParameterValidationResults().forEach(result -> {
                        String name = result.getMethodParameter().getParameterName();
                        String key = name != null ? name : "parameter";

                        result.getResolvableErrors().stream()
                                        .findFirst()
                                        .ifPresent(error -> fieldErrors.putIfAbsent(key, error.getDefaultMessage()));
                });

                ApiErrorResponse response = new ApiErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "VALIDATION_ERROR",
                                "Request validation failed",
                                fieldErrors);

                return ResponseEntity.badRequest().body(response);
        }

        @ExceptionHandler(ReportNotFoundException.class)
        public ResponseEntity<ApiErrorResponse> handleReportNotFoundException(
                        ReportNotFoundException exception) {
                ApiErrorResponse response = new ApiErrorResponse(
                                HttpStatus.NOT_FOUND.value(),
                                "REPORT_NOT_FOUND",
                                exception.getMessage(),
                                null);

                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ApiErrorResponse> handleMethodArgumentTypeMismatchException(
                        MethodArgumentTypeMismatchException exception) {
                ApiErrorResponse response = new ApiErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "INVALID_PARAMETER",
                                "Invalid parameter value for " + exception.getName(),
                                null);

                return ResponseEntity.badRequest().body(response);
        }

        @ExceptionHandler(DuplicateMobileNumberException.class)
        public ResponseEntity<ApiErrorResponse> handleDuplicateMobileNumberException(
                        DuplicateMobileNumberException exception) {
                ApiErrorResponse response = new ApiErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                "MOBILE_NUMBER_ALREADY_REGISTERED",
                                exception.getMessage(),
                                null);

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<ApiErrorResponse> handleAccessDenied(
                        AccessDeniedException ex) {

                ApiErrorResponse response = new ApiErrorResponse(
                                HttpStatus.FORBIDDEN.value(),
                                "FORBIDDEN",
                                "You do not have permission to perform this action",
                                null);

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<ApiErrorResponse> handleBadCredentials(
                        BadCredentialsException ex) {

                ApiErrorResponse response = new ApiErrorResponse(
                                401,
                                "INVALID_CREDENTIALS",
                                "Invalid mobile number or password",
                                null);

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        @ExceptionHandler(InvalidOtpException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidOtp(
                        InvalidOtpException ex) {
                return ResponseEntity.badRequest().body(
                                new ApiErrorResponse(
                                                400,
                                                ex.getErrorCode(),
                                                ex.getMessage(),
                                                Map.of()));
        }

        @ExceptionHandler(UserAlreayActiveException.class)
        public ResponseEntity<ApiErrorResponse> handleUserAlreayActiveException(
                        UserAlreayActiveException ex) {
                ApiErrorResponse response = new ApiErrorResponse(
                                HttpStatus.CONFLICT.value(),
                                "USER_ALREADY_ACTIVE",
                                ex.getMessage(),
                                null);

                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        }

        @ExceptionHandler(OtpResendRateLimitException.class)
        public ResponseEntity<ApiErrorResponse> handleOtpResendRateLimit(
                        OtpResendRateLimitException ex) {

                ApiErrorResponse response = new ApiErrorResponse(
                                HttpStatus.TOO_MANY_REQUESTS.value(),
                                "TOO_MANY_REQUESTS",
                                ex.getMessage(),
                                null);

                return ResponseEntity
                                .status(HttpStatus.TOO_MANY_REQUESTS)
                                .body(response);
        }

        @ExceptionHandler(InvalidVerificationRequestException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidVerificationRequestException(
                        InvalidVerificationRequestException ex) {
                ApiErrorResponse response = new ApiErrorResponse(
                                HttpStatus.BAD_REQUEST.value(),
                                "INVALID_VERIFICATION_REQUEST",
                                ex.getMessage(),
                                null);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
                        Exception ex) {

                if (ex instanceof ErrorResponse errorResponse) {
                        return handleSpringMvcException(errorResponse);
                }

                log.error("Unexpected application exception", ex);

                ApiErrorResponse response = new ApiErrorResponse(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "INTERNAL_SERVER_ERROR",
                                "An unexpected error occurred",
                                null);

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(response);
        }

        @ExceptionHandler(MobileNotVerifiedException.class)
        public ResponseEntity<ApiErrorResponse> handleMobileNotVerified(
                        MobileNotVerifiedException ex) {
                ApiErrorResponse response = new ApiErrorResponse(
                                HttpStatus.FORBIDDEN.value(),
                                "MOBILE_NOT_VERIFIED",
                                ex.getMessage(),
                                null);

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        @ExceptionHandler(AccountDisabledException.class)
        public ResponseEntity<ApiErrorResponse> handleAccountDisabled(
                        AccountDisabledException ex) {
                ApiErrorResponse response = new ApiErrorResponse(
                                HttpStatus.FORBIDDEN.value(),
                                "ACCOUNT_DISABLED",
                                ex.getMessage(),
                                null);

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        // Spring's own client errors (404, 405, 415, ...) already carry the right
        // status.
        private ResponseEntity<ApiErrorResponse> handleSpringMvcException(
                        ErrorResponse errorResponse) {

                HttpStatusCode statusCode = errorResponse.getStatusCode();
                HttpStatus status = HttpStatus.resolve(statusCode.value());

                String error = status != null ? status.name() : "REQUEST_ERROR";
                String message = status != null ? status.getReasonPhrase() : "Request could not be processed";

                ApiErrorResponse response = new ApiErrorResponse(
                                statusCode.value(),
                                error,
                                message,
                                null);

                return ResponseEntity
                                .status(statusCode)
                                .headers(errorResponse.getHeaders())
                                .body(response);
        }

        @ExceptionHandler(InvalidRefreshTokenException.class)
        public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken(
                        InvalidRefreshTokenException ex) {

                ApiErrorResponse response = new ApiErrorResponse(
                                401,
                                "INVALID_REFRESH_TOKEN",
                                ex.getMessage(),
                                null);

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
}

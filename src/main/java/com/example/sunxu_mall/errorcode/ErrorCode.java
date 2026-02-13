package com.example.sunxu_mall.errorcode;

/**
 * Error code enum class
 * Support custom error codes
 *
 * @author sunxu
 * @version 1.0
 * @date 2025/12/24 20:30
 */
public enum ErrorCode {
    // ====================================== System Errors ======================================
    SUCCESS(20000, "Success", 200),
    INTERNAL_SERVER_ERROR(50000, "Internal Server Error", 500),
    SERVICE_UNAVAILABLE(50300, "Service Unavailable", 503),
    GATEWAY_ERROR(50400, "Gateway Error", 504),
    
    // ====================================== Parameter Errors ======================================
    BAD_REQUEST(40000, "Bad Request", 400),
    PARAMETER_MISSING(40001, "Missing Required Parameter", 400),
    PARAMETER_FORMAT_ERROR(40002, "Parameter Format Error", 400),
    PARAMETER_VALIDATION_ERROR(40003, "Parameter Validation Failed", 400),
    CAPTCHA_EXPIRED(40004, "Captcha Expired", 400),
    CAPTCHA_ERROR(40005, "Captcha Error", 400),
    DECRYPTION_FAILED(40006, "Decryption Failed", 400),
    
    // ====================================== Authentication & Authorization Errors ======================================
    UNAUTHORIZED(40100, "Unauthorized", 401),
    INVALID_TOKEN(40101, "Invalid Token", 401),
    EXPIRED_TOKEN(40102, "Expired Token", 401),
    MISSING_TOKEN(40103, "Missing Token", 401),
    FORBIDDEN(40300, "Forbidden", 403),
    PERMISSION_DENIED(40301, "Permission Denied", 403),
    
    // ====================================== Business Logic Errors ======================================
    NOT_FOUND(40400, "Resource Not Found", 404),
    USER_NOT_EXIST(40401, "User Not Found", 404),
    PRODUCT_NOT_EXIST(40402, "Product Not Found", 404),
    RESOURCE_CONFLICT(40900, "Resource Conflict", 409),
    DUPLICATE_SUBMIT(40901, "Duplicate Submit, please do not resubmit", 409),
    OPERATION_FAILED(50001, "Operation Failed", 500),
    DATA_CONVERSION_ERROR(50002, "Data Conversion Error", 500),
    
    // ====================================== Database Errors ======================================
    DATABASE_CONNECTION_ERROR(50003, "Database Connection Error", 500),
    DATABASE_EXECUTION_ERROR(50004, "SQL Execution Error", 500),
    DATA_INTEGRITY_VIOLATION(50005, "Data Integrity Violation", 500),
    DUPLICATE_KEY_ERROR(50006, "Duplicate Key Error", 500),
    
    // ====================================== Configuration Errors ======================================
    CONFIG_ERROR(50007, "Configuration Error", 500),
    CONFIG_MISSING(50008, "Missing Required Configuration", 500),
    
    // ====================================== Service Operation Errors ======================================
    THIRD_PARTY_API_ERROR(50009, "Third-party API Call Failed", 500),
    EXPORT_CONFIG_ERROR(50010, "Export Configuration Error", 500),
    CLASS_NOT_FOUND_ERROR(50011, "Class Not Found", 500);
    
    // Error code
    private final int code;
    // Error message
    private final String message;
    // HTTP status code
    private final int httpStatus;
    
    /**
     * Constructor
     *
     * @param code       Error code
     * @param message    Error message
     * @param httpStatus HTTP status code
     */
    ErrorCode(int code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
    
    /**
     * Get error code
     *
     * @return Error code
     */
    public int getCode() {
        return code;
    }
    
    /**
     * Get error message
     *
     * @return Error message
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * Get HTTP status code
     *
     * @return HTTP status code
     */
    public int getHttpStatus() {
        return httpStatus;
    }
    
    /**
     * Get enum value by error code
     *
     * @param code Error code
     * @return Corresponding enum value, null if not exists
     */
    public static ErrorCode getByCode(int code) {
        for (ErrorCode errorCode : ErrorCode.values()) {
            if (errorCode.code == code) {
                return errorCode;
            }
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "ErrorCode{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", httpStatus=" + httpStatus +
                '}';
    }
}

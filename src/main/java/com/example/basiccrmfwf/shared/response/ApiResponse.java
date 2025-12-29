package com.example.basiccrmfwf.shared.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard API response wrapper for consistent response structure.
 * This wrapper maintains backward compatibility with existing JSON structure.
 *
 * @param <T> The type of data being returned
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    
    /**
     * Response status code (HTTP status)
     */
    private Integer statusCode;
    
    /**
     * Response message
     */
    private String message;
    
    /**
     * Response data (generic type)
     */
    private T data;
    
    /**
     * Timestamp of the response
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Success flag
     */
    @Builder.Default
    private Boolean success = true;
    
    /**
     * Creates a successful response with data
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .statusCode(200)
                .message("Success")
                .data(data)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates a successful response with custom message
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .statusCode(200)
                .message(message)
                .data(data)
                .success(true)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates an error response
     */
    public static <T> ApiResponse<T> error(Integer statusCode, String message) {
        return ApiResponse.<T>builder()
                .statusCode(statusCode)
                .message(message)
                .data(null)
                .success(false)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * Creates an error response with data
     */
    public static <T> ApiResponse<T> error(Integer statusCode, String message, T errorData) {
        return ApiResponse.<T>builder()
                .statusCode(statusCode)
                .message(message)
                .data(errorData)
                .success(false)
                .timestamp(LocalDateTime.now())
                .build();
    }
}

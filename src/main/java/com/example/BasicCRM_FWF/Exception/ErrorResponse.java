package com.example.BasicCRM_FWF.Exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResponse {

    private Integer statusCode;
    private String message;
    private String details;

    public ErrorResponse(String error, String message) {
    }
}

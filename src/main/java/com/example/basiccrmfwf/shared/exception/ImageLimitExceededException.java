package com.example.basiccrmfwf.shared.exception;

public class ImageLimitExceededException extends RuntimeException {

    public ImageLimitExceededException(String message) {
        super(message);
        System.out.println("Message: " + message);
    }
}

package com.example.BasicCRM_FWF.Exception;

public class ImageLimitExceededException extends RuntimeException {


    public ImageLimitExceededException(String message) {
        super(message);
        System.out.println("Message: " + message);
    }
}

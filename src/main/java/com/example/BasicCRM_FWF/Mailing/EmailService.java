package com.example.BasicCRM_FWF.Mailing;

import jakarta.mail.MessagingException;

public interface EmailService {

    void sendMail(final AbstractEmailContext email) throws MessagingException;

    void sendOtp(String email, String body, Integer randomNumber);
}

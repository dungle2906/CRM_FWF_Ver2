package com.example.BasicCRM_FWF.Mailing;

import com.example.BasicCRM_FWF.Model.User;
import org.springframework.web.util.UriComponentsBuilder;

public class AccountVerificationEmailContext extends AbstractEmailContext {

    private String token;

    @Override
    public <T> void init(T context) {
        User user = (User) context;

        put("firstName", user.getUsername());
        setTemplateLocation("mailing/email-verification");
        setSubject("Please Complete Your Confirmation To Access Face Wash Fox System");
        setFrom("vandung290603@gmail.com");
        setTo(user.getEmail());
    }

    public void setToken(String token) {
        this.token = token;
        put("token", token);
    }

    public void buildVerificationUrl(final String baseURL, final String token) {
        final String url = UriComponentsBuilder.fromHttpUrl(baseURL)
                .path("/api/auth/register/verify")
                .queryParam("token", token) // ➜ thêm token vào URL
                .build()
                .toUriString();
        put("verificationURL", url);
    }

    public void buildVerificationUrlForgotPass(final String baseURL, final String token) {
        final String url = UriComponentsBuilder.fromHttpUrl(baseURL)
                .path("/change-forgot-password") // Không thêm queryParam("token", token)
                .build()
                .toUriString();
        put("verificationURL", url);
    }
}

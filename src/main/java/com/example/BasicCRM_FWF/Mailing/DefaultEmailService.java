package com.example.BasicCRM_FWF.Mailing;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DefaultEmailService implements EmailService {

    private final JavaMailSender emailSender;

    @Value("${application.logo-url}")
    String logoUrl;

    @Override
    public void sendMail(AbstractEmailContext email) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(email.getTo());
        helper.setFrom(email.getFrom());
        helper.setSubject(email.getSubject());

        String content = "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>Account Verification</title>" +
                "</head>" +
                "<body style='font-family: Arial, sans-serif; text-align: center; padding: 20px; background-color: #f9f9f9;'>" +

                // ✅ Thêm logo vào đây
                "<div style='margin-bottom: 30px;'>" +
                "<img src='" + logoUrl + "' alt='Face Wash Fox Logo' " +
                "style='max-width: 180px; height: auto; display: block; margin: 0 auto;'/>" +
                "</div>" +

                "<h2 style='color: #333;'>Welcome</h2>" +
                "<h1 style='font-size: 22px; color: #333;'>Finish accessing with your Google account.</h1>" +
                "<p style='font-size: 16px;'>Hi " + email.getContext().get("firstName") + ",</p>" +
                "<p style='font-size: 16px; color: #555;'>We're excited to have you get started in Face Wash Fox System.<br>" +
                "First, you need to confirm your account. Just press the button below.</p>" +

                "<a href='" + email.getContext().get("verificationURL") + "' " +
                "style='display: inline-block; margin: 20px auto; padding: 12px 24px; background-color: #F17C54; " +
                "color: white; font-weight: bold; text-decoration: none; border-radius: 5px;'>Validate Account</a>" +

                "<p style='font-size: 15px; color: #aaa; margin-top: 40px;'>This is an automated message, please do not reply.</p>" +
                "</body></html>";

        helper.setText(content, true); // true = HTML

        emailSender.send(message);
    }


    @Override
    public void sendOtp(String email, String subject, Integer randomNumber) {
        String[] parts = Integer.toString(randomNumber).split("(?<=.)");
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH);
        String formattedDate = currentDate.format(formatter);

        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject(subject);

            StringBuilder htmlContent = new StringBuilder();

            // Base
            // Logo
            // Text
            // OTP
            // Footer
            // Dark mode
            // Mobile
            // Logo
            htmlContent.append("<!DOCTYPE html>")
                    .append("<html lang='en'>")
                    .append("<head>")
                    .append("<meta charset='UTF-8'>")
                    .append("<meta name='viewport' content='width=device-width, initial-scale=1.0'>")
                    .append("<title>OTP Verification</title>")
                    .append("<style>")
                    // Base
                    .append("body { font-family: Arial, sans-serif; margin: 0; padding: 0; background-color: #f9f9f9; }")
                    .append(".container { max-width: 520px; margin: 20px auto; background: #fff; border-radius: 8px; ")
                    .append("box-shadow: 0 2px 8px rgba(0,0,0,0.05); padding: 20px 15px; text-align: center; }")

                    // Logo
                    .append(".logo-container { text-align: center; margin-bottom: 15px; }")
                    .append(".logo { max-width: 140px; height: auto; display: inline-block; background: #fff; padding: 5px 10px; border-radius: 6px; }")

                    // Text
                    .append("h2 { color: #333; font-size: 20px; margin: 10px 0; }")
                    .append("p { font-size: 15px; color: #555; line-height: 1.5; margin: 8px 0; }")

                    // OTP
                    .append(".otp-box { margin: 20px 0; }")
                    .append(".otp-digit { display: inline-block; margin: 4px; padding: 12px 16px; font-size: 18px; font-weight: bold; ")
                    .append("color: #F17C54; border: 2px solid #F17C54; border-radius: 6px; background-color: #fff; }")

                    // Footer
                    .append(".footer { font-size: 12px; color: #999; margin-top: 15px; text-align: center; }")

                    // Dark mode
                    .append("@media (prefers-color-scheme: dark) { ")
                    .append("body { background-color: #121212 !important; color: #e0e0e0 !important; }")
                    .append(".container { background-color: #1e1e1e !important; }")
                    .append(".otp-digit { background-color: #2b2b2b !important; border-color: #F17C54 !important; color: #F17C54 !important; }")
                    .append("}")

                    // Mobile
                    .append("@media only screen and (max-width: 480px) { ")
                    .append(".container { width: 92% !important; padding: 15px 10px !important; }")
                    .append(".otp-digit { margin: 3px; padding: 10px 14px; font-size: 16px; }")
                    .append("}")
                    .append("</style>")
                    .append("</head>")
                    .append("<body>")
                    .append("<div class='container'>")

                    // Logo
                    .append("<div class='logo-container'>").append("<img src='").append(logoUrl).append("' ")
                    .append("alt='Face Wash Fox Logo' class='logo'/>")
                    .append("</div>")

                    // Main content (gọn hơn)
                    .append("<h2>Face Wash Fox System – Password Reset</h2>")
                    .append("<p>You requested to reset your password on <b>").append(formattedDate).append("</b>.</p>")
                    .append("<p>Use the following 6-digit OTP to continue:</p>")

                    // OTP digits
                    .append("<div class='otp-box'>");
            for (String digit : parts) {
                htmlContent.append("<span class='otp-digit'>").append(digit).append("</span>");
            }
            htmlContent.append("</div>")

                    // Footer
                    .append("<p style='font-size: 13px; color: #888;'>This OTP is valid for 10 minutes. Do not share it with anyone.</p>")
                    .append("<p class='footer'>This is an automated message. Please do not reply.</p>")

                    .append("</div>")
                    .append("</body></html>");

            helper.setText(htmlContent.toString(), true);
            emailSender.send(message);

        } catch (MessagingException | MailException e) {
            throw new IllegalArgumentException("Error sending OTP email!", e);
        }
    }

}

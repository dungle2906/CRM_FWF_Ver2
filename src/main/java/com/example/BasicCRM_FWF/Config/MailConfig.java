package com.example.BasicCRM_FWF.Config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
@EnableConfigurationProperties(MailProperties.class)
public class MailConfig {

    @Bean
    @ConditionalOnMissingBean(JavaMailSender.class)
    public JavaMailSender javaMailSender(MailProperties props) {
        JavaMailSenderImpl s = new JavaMailSenderImpl();
        s.setHost(props.getHost());
        s.setPort(props.getPort());
        s.setUsername(props.getUsername());
        s.setPassword(props.getPassword());
        if (props.getProperties() != null) {
            s.getJavaMailProperties().putAll(props.getProperties());
        }
        return s;
    }
}
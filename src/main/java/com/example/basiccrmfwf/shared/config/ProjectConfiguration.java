package com.example.basiccrmfwf.shared.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProjectConfiguration {
    @Bean
    public ModelMapper mapper() {
        return new ModelMapper();
    }
}

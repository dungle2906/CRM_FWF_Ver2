package com.example.BasicCRM_FWF;

import com.example.BasicCRM_FWF.DTO.RegisterRequest;
import com.example.BasicCRM_FWF.Model.User;
import com.example.BasicCRM_FWF.Repository.UserRepository;
import com.example.BasicCRM_FWF.RoleAndPermission.Role;
import com.example.BasicCRM_FWF.Service.AuthenticationService.IAuthenticationService;
import com.github.javafaker.Faker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableScheduling
public class BasicCrmFwfApplication {

	public static void main(String[] args) {
		SpringApplication.run(BasicCrmFwfApplication.class, args);
	}

//	@Bean
	public CommandLineRunner commandLineRunner(
			IAuthenticationService service,
			PasswordEncoder passwordEncoder,
			HttpServletRequest servletRequest,
			HttpServletResponse servletResponse
	) {
		return args -> {
			var admin = RegisterRequest.builder()
					.username("admin")
					.password(passwordEncoder.encode("123"))
					.build();

			var manager = RegisterRequest.builder()
					.username("manager")
					.password(passwordEncoder.encode("12345678a"))
					.build();
		};
	}

	@Bean
	public CommandLineRunner commandLineRunner(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder) {
		return args -> {

			if (!(userRepository.findByUsername("admin").isPresent())) {
				// Create user
				var user = User.builder()
						.firstname("Face Wash Fox")
						.lastname("IT")
						.username("admin")
						.password(passwordEncoder.encode("f123"))
						.avatar(null)
						.role(Role.ADMIN)
						.createdAt(LocalDateTime.now())
						.updatedAt(LocalDateTime.now())
						.createdBy("SYSTEM")
						.updatedBy("SYSTEM")
						.isVerified(true)
						.isActive(true)
						.build();
				userRepository.save(user);
			}
		};
	}
}

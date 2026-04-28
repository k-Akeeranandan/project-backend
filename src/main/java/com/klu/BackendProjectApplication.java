package com.klu;

import com.klu.entity.AccountStatus;
import com.klu.entity.User;
import com.klu.repo.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;

@SpringBootApplication
public class BackendProjectApplication implements CommandLineRunner {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Value("${app.bootstrap.promote-admin-email:}")
	private String promoteAdminEmail;

	@Value("${app.bootstrap.set-password:}")
	private String bootstrapSetPassword;

	public static void main(String[] args) {
		SpringApplication.run(BackendProjectApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// Check if admin user exists, create if not
		if (!userRepository.existsByEmail("admin@gmail.com")) {
			User admin = new User();
			admin.setName("admin");
			admin.setEmail("admin@gmail.com");
			admin.setPassword(passwordEncoder.encode("admin123"));
			admin.setRole("ADMIN");
			admin.setAccountStatus(AccountStatus.APPROVED);
			userRepository.save(admin);
			System.out.println("Admin user created: admin@gmail.com / admin123");
		} else {
			System.out.println("Admin user already exists");
		}

		// Optional bootstrap: promote an existing user to ADMIN by email
		if (promoteAdminEmail != null && !promoteAdminEmail.isBlank()) {
			Optional<User> u = userRepository.findByEmail(promoteAdminEmail.trim());
			if (u.isPresent()) {
				User user = u.get();
				user.setRole("ADMIN");
				if (user.getAccountStatus() != AccountStatus.APPROVED) {
					user.setAccountStatus(AccountStatus.APPROVED);
				}
				if (bootstrapSetPassword != null && !bootstrapSetPassword.isBlank()) {
					user.setPassword(passwordEncoder.encode(bootstrapSetPassword));
				}
				userRepository.save(user);
				System.out.println("Promoted to ADMIN: " + promoteAdminEmail
						+ (bootstrapSetPassword != null && !bootstrapSetPassword.isBlank() ? " (password reset)" : ""));
			} else {
				System.out.println("Bootstrap promote-admin-email not found: " + promoteAdminEmail);
			}
		}
	}
}

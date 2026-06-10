package com.librarylane.config;

import com.librarylane.entities.Library;
import com.librarylane.entities.User;
import com.librarylane.enums.UserRole;
import com.librarylane.repositories.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedData(UserRepository userRepository) {
        return args -> {
            if (!userRepository.existsByEmail("demo@librarylane.com")) {

                User user = User.builder()
                        .email("demo@librarylane.com")
                        .password("password")
                        .displayName("Demo Reader")
                        .role(UserRole.USER)
                        .build();

                Library library = Library.builder()
                        .name("Demo Reader's Library")
                        .description("A cozy starter library for testing Library Lane.")
                        .selectedTheme("cozy-classic")
                        .user(user)
                        .build();

                user.setLibrary(library);

                userRepository.save(user);
            }
        };
    }
}
package com.back.config;

import com.back.user.model.entity.Role;
import com.back.user.model.entity.RoleName;
import com.back.user.repo.IRoleRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final IRoleRepo roleRepo;

    @Bean
    CommandLineRunner initRoles() {
        return args -> {
            if (roleRepo.count() == 0) {
                roleRepo.save(Role.builder()
                        .name(RoleName.ROLE_USER)
                        .description("Default user role")
                        .build());

                roleRepo.save(Role.builder()
                        .name(RoleName.ROLE_ADMIN)
                        .description("Administrator role")
                        .build());

                roleRepo.save(Role.builder()
                        .name(RoleName.ROLE_MODERATOR)
                        .description("Moderator role")
                        .build());
            }
        };
    }
}
package com.cda.carpooling.config;

import com.cda.carpooling.entity.Role;
import com.cda.carpooling.entity.User;
import com.cda.carpooling.entity.UserStatus;
import com.cda.carpooling.repository.RoleRepository;
import com.cda.carpooling.repository.UserRepository;
import com.cda.carpooling.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataLoader {

    private final UserStatusRepository userStatusRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        log.info("🔄 Initializing reference data...");

        initUserStatuses();
        initRoles();

        if (isDevelopmentProfile()) {
            log.info("🧪 Development profile detected - loading test data...");
            initTestUsers();
        }

        log.info("✅ Database initialization complete!");
    }

    private void initUserStatuses() {
        if (userStatusRepository.count() == 0) {
            log.info("📊 Creating user statuses...");

            userStatusRepository.save(UserStatus.builder()
                    .label(UserStatus.ACTIVE)
                    .description("Utilisateur actif et en règle")
                    .build());

            userStatusRepository.save(UserStatus.builder()
                    .label(UserStatus.PENDING)
                    .description("Utilisateur en attente de validation")
                    .build());

            userStatusRepository.save(UserStatus.builder()
                    .label(UserStatus.SUSPENDED)
                    .description("Utilisateur suspendu temporairement")
                    .build());

            userStatusRepository.save(UserStatus.builder()
                    .label(UserStatus.DELETED)
                    .description("Utilisateur supprimé (soft delete)")
                    .build());

            log.info("✅ User statuses created");
        }
    }

    private void initRoles() {
        if (roleRepository.count() == 0) {
            log.info("📊 Creating roles...");

            roleRepository.save(Role.builder()
                    .label(Role.ROLE_STUDENT)
                    .description("Élève du GRETA pouvant réserver des trajets")
                    .build());

            roleRepository.save(Role.builder()
                    .label(Role.ROLE_DRIVER)
                    .description("Conducteur pouvant proposer des trajets")
                    .build());

            roleRepository.save(Role.builder()
                    .label(Role.ROLE_ADMIN)
                    .description("Administrateur avec tous les droits")
                    .build());

            log.info("✅ Roles created");
        }
    }

    private void initTestUsers() {
        if (userRepository.count() > 0) {
            log.info("⏭️  Test users already exist, skipping...");
            return;
        }

        log.info("👤 Creating test users...");

        UserStatus activeStatus = userStatusRepository.findByLabel(UserStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("ACTIVE status not found"));

        createAdminUser(activeStatus);
        createStudentUser(activeStatus);
        createDriverUser(activeStatus);

        log.info("✅ Test users created");
    }

    private void createAdminUser(UserStatus activeStatus) {
        Role adminRole = roleRepository.findByLabel(Role.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

        User admin = User.builder()
                .email("admin@greta.fr")
                .password(passwordEncoder.encode("Admin@123"))
                .status(activeStatus)
                .createdAt(LocalDateTime.now())
                .build();

        admin.getRoles().add(adminRole);
        userRepository.save(admin);

        log.info("✅ Admin user: admin@greta.fr / Admin@123");
    }

    private void createStudentUser(UserStatus activeStatus) {
        Role studentRole = roleRepository.findByLabel(Role.ROLE_STUDENT)
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));

        User student = User.builder()
                .email("student@test.fr")
                .password(passwordEncoder.encode("Student@123"))
                .status(activeStatus)
                .createdAt(LocalDateTime.now())
                .build();

        student.getRoles().add(studentRole);
        userRepository.save(student);

        log.info("✅ Student user: student@test.fr / Student@123");
    }

    private void createDriverUser(UserStatus activeStatus) {
        Role studentRole = roleRepository.findByLabel(Role.ROLE_STUDENT)
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));
        Role driverRole = roleRepository.findByLabel(Role.ROLE_DRIVER)
                .orElseThrow(() -> new IllegalStateException("DRIVER role not found"));

        User driver = User.builder()
                .email("driver@test.fr")
                .password(passwordEncoder.encode("Driver@123"))
                .status(activeStatus)
                .createdAt(LocalDateTime.now())
                .build();

        driver.getRoles().add(studentRole);
        driver.getRoles().add(driverRole);
        userRepository.save(driver);

        log.info("✅ Driver user: driver@test.fr / Driver@123");
    }

    private boolean isDevelopmentProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return true;
        }
        for (String profile : activeProfiles) {
            if (profile.equals("dev") || profile.equals("default")) {
                return true;
            }
        }
        return false;
    }
}
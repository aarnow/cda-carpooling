package com.cda.carpooling.config;

import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.Role;
import com.cda.carpooling.entity.PersonStatus;
import com.cda.carpooling.repository.RoleRepository;
import com.cda.carpooling.repository.PersonRepository;
import com.cda.carpooling.repository.PersonStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Service responsable de l'initialisation des données de référence et des fixtures en base de données.
 * Les fixtures (données de test) ne sont chargées qu'en environnement de dev.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataLoader {

    private final PersonStatusRepository personStatusRepository;
    private final RoleRepository roleRepository;
    private final PersonRepository personRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        log.info("🔄 Initializing reference data...");

        initPersonStatuses();
        initRoles();

        if (isDevelopmentProfile()) {
            log.info("🧪 Development profile detected - loading test data...");
            initTestPersons();
        }

        log.info("✅ Database initialization complete!");
    }

    /**
     * Initialise les statuts utilisateur s'ils n'existent pas déjà en base.
     */
    private void initPersonStatuses() {
        if (personStatusRepository.count() == 0) {
            log.info("📊 Creating person statuses...");

            personStatusRepository.save(PersonStatus.builder()
                    .label(PersonStatus.ACTIVE)
                    .description("Personne active et en règle")
                    .build());

            personStatusRepository.save(PersonStatus.builder()
                    .label(PersonStatus.PENDING)
                    .description("Personne en attente de validation")
                    .build());

            personStatusRepository.save(PersonStatus.builder()
                    .label(PersonStatus.SUSPENDED)
                    .description("Personne suspendu temporairement")
                    .build());

            personStatusRepository.save(PersonStatus.builder()
                    .label(PersonStatus.DELETED)
                    .description("Personne supprimée (soft delete)")
                    .build());

            log.info("✅ Person statuses created");
        }
    }

    /**
     * Initialise les rôles s'ils n'existent pas déjà en base.
     */
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

    /**
     * Initialise les personnes de test (admin, student, driver) UNIQUEMENT en environnement de dev.
     */
    private void initTestPersons() {
        if (personRepository.count() > 0) {
            log.info("⏭️  Test persons already exist, skipping...");
            return;
        }

        log.info("👤 Creating test persons...");

        PersonStatus activeStatus = personStatusRepository.findByLabel(PersonStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("ACTIVE status not found"));

        createAdminPerson(activeStatus);
        createStudentPerson(activeStatus);
        createDriverPerson(activeStatus);

        log.info("✅ Test persons created");
    }

    /**
     * Crée un utilisateur admin avec le rôle ADMIN.
     */
    private void createAdminPerson(PersonStatus activeStatus) {
        Role adminRole = roleRepository.findByLabel(Role.ROLE_ADMIN)
                .orElseThrow(() -> new IllegalStateException("ADMIN role not found"));

        Person admin = Person.builder()
                .email("admin@greta.fr")
                .password(passwordEncoder.encode("Admin@123"))
                .status(activeStatus)
                .createdAt(LocalDateTime.now())
                .build();

        admin.getRoles().add(adminRole);
        personRepository.save(admin);

        log.info("✅ Admin person: admin@greta.fr / Admin@123");
    }

    /**
     * Crée un utilisateur étudiant avec le rôle STUDENT.
     */
    private void createStudentPerson(PersonStatus activeStatus) {
        Role studentRole = roleRepository.findByLabel(Role.ROLE_STUDENT)
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));

        Person student = Person.builder()
                .email("student@test.fr")
                .password(passwordEncoder.encode("Student@123"))
                .status(activeStatus)
                .createdAt(LocalDateTime.now())
                .build();

        student.getRoles().add(studentRole);
        personRepository.save(student);

        log.info("✅ Student person: student@test.fr / Student@123");
    }

    /**
     * Crée un utilisateur conducteur avec les rôles STUDENT et DRIVER.
     */
    private void createDriverPerson(PersonStatus activeStatus) {
        Role studentRole = roleRepository.findByLabel(Role.ROLE_STUDENT)
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));
        Role driverRole = roleRepository.findByLabel(Role.ROLE_DRIVER)
                .orElseThrow(() -> new IllegalStateException("DRIVER role not found"));

        Person driver = Person.builder()
                .email("driver@test.fr")
                .password(passwordEncoder.encode("Driver@123"))
                .status(activeStatus)
                .createdAt(LocalDateTime.now())
                .build();

        driver.getRoles().add(studentRole);
        driver.getRoles().add(driverRole);
        personRepository.save(driver);

        log.info("✅ Driver person: driver@test.fr / Driver@123");
    }

    /**
     * Détermine si l'application est en environnement de développement.
     * Spring Boot utilise la propriété "spring.profiles.active" pour définir l'environnement.
     */
    private boolean isDevelopmentProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            return true;
        }
        for (String profile : activeProfiles) {
            if (profile.equals("dev") || profile.equals("default") || profile.equals("test")) {
                return true;
            }
        }
        return false;
    }
}
package com.cda.carpooling.config;

import com.cda.carpooling.entity.*;
import com.cda.carpooling.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    private final BrandRepository brandRepository;
    private final TripStatusRepository tripStatusRepository;
    private final ReservationStatusRepository reservationStatusRepository;
    private final VehicleRepository vehicleRepository;
    private final CityRepository cityRepository;
    private final AddressRepository addressRepository;
    private final TripRepository tripRepository;
    private final ReservationRepository reservationRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void onApplicationReady() {
        log.info("🔄 Initializing reference data...");

        initPersonStatuses();
        initRoles();
        initTripStatus();
        initReservationStatus();
        initBrands();

        if (isDevelopmentProfile()) {
            log.info("🧪 Development profile detected - loading fixtures...");
            initTestPersons();
            initVehicles();
            initCities();
            initAddresses();
            initTrips();
            initReservations();
        }

        log.info("✅ Database initialization complete!");
    }

    //region REFERENCES
    /**
     * Initialise les statuts utilisateur s'ils n'existent pas déjà en base.
     */
    private void initPersonStatuses() {
        if (personStatusRepository.count() == 0) {
            log.info("🔄 Creating person statuses...");

            personStatusRepository.saveAll(List.of(
                    PersonStatus.builder()
                            .label(PersonStatus.ACTIVE)
                            .description("Personne active et en règle").build(),
                    PersonStatus.builder()
                            .label(PersonStatus.PENDING)
                            .description("Personne en attente de validation").build(),
                    PersonStatus.builder()
                            .label(PersonStatus.SUSPENDED)
                            .description("Personne suspendu temporairement").build(),
                    PersonStatus.builder()
                            .label(PersonStatus.DELETED)
                            .description("Personne supprimée (soft delete)").build()
            ));

            log.info("✅ Person statuses created");
        }
    }

    /**
     * Initialise les rôles s'ils n'existent pas déjà en base.
     */
    private void initRoles() {
        if (roleRepository.count() == 0) {
            log.info("🔄 Creating roles...");

            roleRepository.saveAll(List.of(
                    Role.builder()
                            .label(Role.ROLE_STUDENT)
                            .description("Élève du GRETA pouvant réserver des trajets").build(),
                    Role.builder()
                            .label(Role.ROLE_DRIVER)
                            .description("Conducteur pouvant proposer des trajets").build(),
                    Role.builder()
                            .label(Role.ROLE_ADMIN)
                            .description("Administrateur avec tous les droits").build()
            ));

            log.info("✅ Roles created");
        }
    }

    /**
     * Initialise les états des covoiturages
     */
    private void initTripStatus() {
        if (tripStatusRepository.count() == 0) {
            log.info("🔄 Creating trip status...");

            tripStatusRepository.saveAll(List.of(
                    TripStatus.builder().label(TripStatus.CANCELLED).build(),
                    TripStatus.builder().label(TripStatus.PLANNED).build(),
                    TripStatus.builder().label(TripStatus.COMPLETED).build()
            ));

            log.info("✅ Trip status created");
        }
    }

    /**
     * Initialise les états des réservations
     */
    private void initReservationStatus() {
        if (reservationStatusRepository.count() == 0) {
            log.info("🔄 Creating reservations status...");

            reservationStatusRepository.saveAll(List.of(
                    ReservationStatus.builder().label(ReservationStatus.CONFIRMED).build(),
                    ReservationStatus.builder().label(ReservationStatus.CANCELLED).build()
            ));

            log.info("✅ Reservations status created");
        }
    }

    private void initBrands() {
        if (brandRepository.count() > 0) return;
        log.info("🔄 Creating brands...");
        List<String> brandNames = List.of(
                "Mercedes-Benz", "Porsche", "Honda", "Jeep", "Toyota",
                "Nissan", "BMW", "Chevrolet", "Chrysler", "Tesla",
                "Buick", "Hyundai", "GMC", "Volvo", "Dodge",
                "Scion", "Mitsubishi", "Volkswagen", "Saab", "Isuzu",
                "Kia", "Pontiac", "Mazda", "Lamborghini", "Audi",
                "Mercury", "Lexus", "Cadillac", "BYD", "Ford",
                "Bentley", "Jaguar", "Saturn", "Subaru", "Acura",
                "Eagle", "Suzuki", "Lotus", "Oldsmobile", "Plymouth",
                "Ferrari", "MINI", "Ram", "Infiniti", "Maserati",
                "Fiat", "Aston Martin", "Polestar", "Lincoln", "Land Rover",
                "Karma", "Rivian", "Roush Performance", "Peugeot", "Rolls-Royce",
                "Tecstar, LP", "Environmental Rsch and Devp Corp", "J.K. Motors",
                "Genesis", "Federal Coach", "American Motors Corporation", "Geo",
                "Alfa Romeo", "McLaren Automotive", "Bugatti",
                "Volga Associated Automobile", "Daihatsu", "smart", "Bertone",
                "Mcevoy Motors", "PAS Inc - GMC", "Daewoo",
                "Grumman Allied Industries", "Maybach", "Wallace Environmental",
                "Pininfarina", "Consulier Industries Inc", "BMW Alpina",
                "Kenyon Corporation Of America", "Autokraft Limited", "Renault",
                "Grumman Olson", "CX Automotive", "Lucid", "Import Trade Services",
                "Morgan", "Hummer", "Merkur", "Pagani",
                "Dabryan Coach Builders Inc", "Panther Car Company Limited",
                "Bitter Gmbh and Co. Kg", "Excalibur Autos", "Saleen",
                "Aurora Cars Ltd", "Spyker", "PAS, Inc", "Vinfast",
                "Saleen Performance", "Sterling", "AM General", "Evans Automobiles",
                "Koenigsegg", "RUF Automobile", "CODA Automotive", "Yugo",
                "Vector", "Kandi", "Bill Dovell Motor Car Company", "Fisker",
                "TVR Engineering Ltd", "INEOS Automotive", "SRT", "Red Shift Ltd.",
                "Texas Coach Company", "General Motors", "E. P. Dutton, Inc.",
                "Mobility Ventures LLC", "Avanti Motor Corporation", "STI",
                "Ruf Automobile Gmbh", "Dacia", "Superior Coaches Div E.p. Dutton",
                "CCC Engineering", "Quantum Technologies", "VPG",
                "Import Foreign Auto Sales Inc", "Vixen Motor Company",
                "London Taxi", "Laforza Automobile Inc",
                "S and S Coach Company  E.p. Dutton", "Qvale", "Panos",
                "London Coach Co Inc", "Mahindra", "Lordstown", "Goldacre",
                "Panoz Auto-Development", "Shelby", "ASC Incorporated",
                "Azure Dynamics", "JBA Motorcars, Inc.", "Lambda Control Systems",
                "Isis Imports Ltd"
        );

        brandNames.stream()
                .map(name -> Brand.builder().name(name).build())
                .forEach(brandRepository::save);

        log.info("✅ {} brands created", brandNames.size());
    }
    //endregion

    //region FIXTURES
    /**
     * Initialise les personnes de test (admin, student, driver) UNIQUEMENT en environnement de dev.
     */
    private void initTestPersons() {
        if (personRepository.count() > 0) {
            log.info("⏭️  Test persons already exist, skipping...");
            return;
        }

        log.info("🧪 Creating test persons...");

        PersonStatus activeStatus = personStatusRepository.findByLabel(PersonStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException("ACTIVE status not found"));

        createAdminPerson(activeStatus);
        createStudentPerson(activeStatus, "student1@test.fr", "Student1@123");
        createStudentPerson(activeStatus, "student2@test.fr", "Student2@123");
        createStudentPerson(activeStatus, "student3@test.fr", "Student3@123");
        createDriverPerson(activeStatus);

        log.info("✅ Test persons created");
    }
    //region create persons
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

        log.info("Admin person: admin@greta.fr / Admin@123");
    }

    /**
     * Crée un utilisateur étudiant avec le rôle STUDENT.
     */
    private void createStudentPerson(PersonStatus activeStatus, String email, String password) {
        Role studentRole = roleRepository.findByLabel(Role.ROLE_STUDENT)
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));

        Person student = Person.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .status(activeStatus)
                .build();

        student.getRoles().add(studentRole);
        personRepository.save(student);
        log.info("Student person created : {} / {}", email, password);
    }

    /**
     * Crée un utilisateur conducteur avec les rôles STUDENT et DRIVER + profil complet.
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

        PersonProfile driverProfile = PersonProfile.builder()
                .person(driver)
                .lastname("Martin")
                .firstname("Jean")
                .birthday(LocalDate.of(1990, 5, 15))
                .phone("0698765432")
                .build();

        driver.setProfile(driverProfile);
        driver.getRoles().add(studentRole);
        driver.getRoles().add(driverRole);

        personRepository.save(driver);

        log.info("✅ Driver person: driver@test.fr / Driver@123 (with profile)");
    }
    //endregion

    /**
     * Crée un véhicule Renault Clio pour le conducteur.
     */
    private void initVehicles() {
        if (vehicleRepository.count() > 0) {
            log.info("⏭️  Vehicles already exist, skipping...");
            return;
        }

        log.info("🧪 Creating vehicles...");

        Person driver = personRepository.findByEmail("driver@test.fr")
                .orElseThrow(() -> new IllegalStateException("Driver not found"));

        Brand renault = brandRepository.findByName("Renault")
                .orElseThrow(() -> new IllegalStateException("Renault brand not found"));

        Vehicle vehicle = Vehicle.builder()
                .person(driver)
                .brand(renault)
                .model("Clio")
                .seats(4)
                .plate("AB-123-CD")
                .description("Voiture confortable et économique")
                .build();

        vehicleRepository.save(vehicle);
        log.info("✅ Vehicle created");
    }

    /**
     * Crée les villes de Séné et Vannes.
     */
    private void initCities() {
        if (cityRepository.count() > 0) {
            log.info("⏭️ Cities already exist, skipping...");
            return;
        }

        log.info("🧪 Creating cities...");

        cityRepository.save(City.builder()
                .name("Séné")
                .postalCode("56860")
                .build());

        cityRepository.save(City.builder()
                .name("Vannes")
                .postalCode("56000")
                .build());

        log.info("✅ Cities created");
    }

    /**
     * Crée une adresse de départ à Séné et une adresse d'arrivée à Vannes.
     */
    private void initAddresses() {
        if (addressRepository.count() > 0) {
            log.info("⏭️  Addresses already exist, skipping...");
            return;
        }

        log.info("🧪 Creating addresses...");

        City sene = cityRepository.findByName("Séné")
                .orElseThrow(() -> new IllegalStateException("Séné city not found"));

        City vannes = cityRepository.findByName("Vannes")
                .orElseThrow(() -> new IllegalStateException("Vannes city not found"));

        addressRepository.save(Address.builder()
                .streetNumber("5")
                .streetName("Rue de la Forêt")
                .city(sene)
                .latitude(47.6008)
                .longitude(-2.6892)
                .build());

        addressRepository.save(Address.builder()
                .streetNumber("1")
                .streetName("Place de la République")
                .city(vannes)
                .latitude(47.6559)
                .longitude(-2.7603)
                .build());

        log.info("✅ Addresses created");
    }

    /**
     * Crée un trajet Séné → Vannes proposé par le conducteur.
     * Départ dans 7 jours à 8h30, 3 places disponibles.
     */
    private void initTrips() {
        if (tripRepository.count() > 0) {
            log.info("⏭️ Trips already exist, skipping...");
            return;
        }

        log.info("🧪 Creating trips...");

        Person driver = personRepository.findByEmail("driver@test.fr")
                .orElseThrow(() -> new IllegalStateException("Driver not found"));

        TripStatus plannedStatus = tripStatusRepository.findByLabel(TripStatus.PLANNED)
                .orElseThrow(() -> new IllegalStateException("PLANNED status not found"));

        Address departure = addressRepository.findAll().stream()
                .filter(a -> a.getCity().getName().equals("Séné"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Séné address not found"));

        Address arriving = addressRepository.findAll().stream()
                .filter(a -> a.getCity().getName().equals("Vannes"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Vannes address not found"));

        Trip trip = Trip.builder()
                .driver(driver)
                .tripDatetime(LocalDateTime.now().plusDays(7).withHour(8).withMinute(30).withSecond(0))
                .availableSeats(3)
                .smokingAllowed(false)
                .tripStatus(plannedStatus)
                .departureAddress(departure)
                .arrivingAddress(arriving)
                .build();

        tripRepository.save(trip);
        log.info("✅ Trip created");
    }

    /**
     * Crée 2 réservations sur le trajet Séné → Vannes.
     * student1 et student2 réservent, student3 ne réserve pas.
     */
    private void initReservations() {
        if (reservationRepository.count() > 0) {
            log.info("⏭️ Reservations already exist, skipping...");
            return;
        }

        log.info("🧪 Creating reservations...");

        ReservationStatus pendingStatus = reservationStatusRepository.findByLabel(ReservationStatus.CONFIRMED)
                .orElseThrow(() -> new IllegalStateException("PENDING status not found"));

        Trip trip = tripRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No trip found"));

        Person student1 = personRepository.findByEmail("student1@test.fr")
                .orElseThrow(() -> new IllegalStateException("Student1 not found"));

        Person student2 = personRepository.findByEmail("student2@test.fr")
                .orElseThrow(() -> new IllegalStateException("Student2 not found"));

        reservationRepository.save(Reservation.builder()
                .trip(trip)
                .person(student1)
                .reservationStatus(pendingStatus)
                .build());

        reservationRepository.save(Reservation.builder()
                .trip(trip)
                .person(student2)
                .reservationStatus(pendingStatus)
                .build());

        // Mettre à jour les places disponibles sur le trajet
        trip.setAvailableSeats(trip.getAvailableSeats() - 2);
        tripRepository.save(trip);

        log.info("✅ Reservations created");
    }
    //endregion

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
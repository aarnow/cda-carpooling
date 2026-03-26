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
        createDriverPerson(activeStatus, "driver1@test.fr", "Driver1@123", "Martin",  "Jean",   LocalDate.of(1990, 5, 15), "0698765432");
        createDriverPerson(activeStatus, "driver2@test.fr", "Driver2@123", "Dubois",  "Sophie", LocalDate.of(1987, 3, 22), "0612345678");

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
    private void createDriverPerson(PersonStatus activeStatus,
                                    String email, String password,
                                    String lastname, String firstname,
                                    LocalDate birthday, String phone) {
        Role studentRole = roleRepository.findByLabel(Role.ROLE_STUDENT)
                .orElseThrow(() -> new IllegalStateException("STUDENT role not found"));
        Role driverRole = roleRepository.findByLabel(Role.ROLE_DRIVER)
                .orElseThrow(() -> new IllegalStateException("DRIVER role not found"));

        Person driver = Person.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .status(activeStatus)
                .createdAt(LocalDateTime.now())
                .build();

        PersonProfile driverProfile = PersonProfile.builder()
                .person(driver)
                .lastname(lastname)
                .firstname(firstname)
                .birthday(birthday)
                .phone(phone)
                .build();

        driver.setProfile(driverProfile);
        driver.getRoles().add(studentRole);
        driver.getRoles().add(driverRole);

        personRepository.save(driver);
        log.info("✅ Driver person: {} / {} (with profile)", email, password);
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

        Person driver1 = personRepository.findByEmail("driver1@test.fr")
                .orElseThrow(() -> new IllegalStateException("Driver1 not found"));
        Person driver2 = personRepository.findByEmail("driver2@test.fr")
                .orElseThrow(() -> new IllegalStateException("Driver2 not found"));

        Brand renault = brandRepository.findByName("Renault")
                .orElseThrow(() -> new IllegalStateException("Renault brand not found"));
        Brand peugeot = brandRepository.findByName("Peugeot")
                .orElseThrow(() -> new IllegalStateException("Peugeot brand not found"));

        vehicleRepository.save(Vehicle.builder()
                .person(driver1)
                .brand(renault)
                .model("Clio")
                .seats(4)
                .plate("AB-123-CD")
                .description("Voiture confortable et économique")
                .build());

        vehicleRepository.save(Vehicle.builder()
                .person(driver2)
                .brand(peugeot)
                .model("208")
                .seats(4)
                .plate("EF-456-GH")
                .description("Citadine récente, climatisée")
                .build());

        log.info("✅ Vehicles created");
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
     * Crée 10 trajets de test avec différents statuts.
     */
    private void initTrips() {
        if (tripRepository.count() > 0) {
            log.info("⏭️ Trips already exist, skipping...");
            return;
        }

        log.info("🧪 Creating trips...");

        Person driver1 = personRepository.findByEmail("driver1@test.fr")
                .orElseThrow(() -> new IllegalStateException("Driver1 not found"));
        Person driver2 = personRepository.findByEmail("driver2@test.fr")
                .orElseThrow(() -> new IllegalStateException("Driver2 not found"));

        Address sene = addressRepository.findAll().stream()
                .filter(a -> a.getCity().getName().equals("Séné")).findFirst()
                .orElseThrow(() -> new IllegalStateException("Séné address not found"));

        Address vannes = addressRepository.findAll().stream()
                .filter(a -> a.getCity().getName().equals("Vannes")).findFirst()
                .orElseThrow(() -> new IllegalStateException("Vannes address not found"));

        TripStatus planned   = tripStatusRepository.findByLabel(TripStatus.PLANNED)
                .orElseThrow(() -> new IllegalStateException("PLANNED status not found"));
        TripStatus completed = tripStatusRepository.findByLabel(TripStatus.COMPLETED)
                .orElseThrow(() -> new IllegalStateException("COMPLETED status not found"));
        TripStatus cancelled = tripStatusRepository.findByLabel(TripStatus.CANCELLED)
                .orElseThrow(() -> new IllegalStateException("CANCELLED status not found"));

        // ─── driver1 — 4 PLANNED ──────────────────────────────────────────────────
        tripRepository.save(Trip.builder()
                .driver(driver1).tripStatus(planned)
                .tripDatetime(LocalDateTime.now().plusDays(7).withHour(8).withMinute(30).withSecond(0).withNano(0))
                .departureAddress(sene).arrivingAddress(vannes)
                .availableSeats(3).smokingAllowed(false)
                .distanceKm(8.4).durationMinutes(13).build());

        tripRepository.save(Trip.builder()
                .driver(driver1).tripStatus(planned)
                .tripDatetime(LocalDateTime.now().plusDays(3).withHour(7).withMinute(45).withSecond(0).withNano(0))
                .departureAddress(sene).arrivingAddress(vannes)
                .availableSeats(2).smokingAllowed(false)
                .distanceKm(8.4).durationMinutes(13).build());

        tripRepository.save(Trip.builder()
                .driver(driver1).tripStatus(planned)
                .tripDatetime(LocalDateTime.now().plusDays(14).withHour(8).withMinute(0).withSecond(0).withNano(0))
                .departureAddress(sene).arrivingAddress(vannes)
                .availableSeats(4).smokingAllowed(true)
                .distanceKm(8.4).durationMinutes(13).build());

        tripRepository.save(Trip.builder()
                .driver(driver1).tripStatus(planned)
                .tripDatetime(LocalDateTime.now().plusDays(5).withHour(17).withMinute(30).withSecond(0).withNano(0))
                .departureAddress(vannes).arrivingAddress(sene)
                .availableSeats(3).smokingAllowed(false)
                .distanceKm(8.2).durationMinutes(15).build());

        // ─── driver1 — 1 COMPLETED + 1 CANCELLED ─────────────────────────────────
        tripRepository.save(Trip.builder()
                .driver(driver1).tripStatus(completed)
                .tripDatetime(LocalDateTime.now().minusDays(3).withHour(14).withMinute(0).withSecond(0).withNano(0))
                .departureAddress(sene).arrivingAddress(vannes)
                .availableSeats(0).smokingAllowed(false)
                .distanceKm(8.4).durationMinutes(13).build());

        tripRepository.save(Trip.builder()
                .driver(driver1).tripStatus(cancelled)
                .tripDatetime(LocalDateTime.now().plusDays(1).withHour(18).withMinute(0).withSecond(0).withNano(0))
                .departureAddress(sene).arrivingAddress(vannes)
                .availableSeats(4).smokingAllowed(true)
                .distanceKm(8.4).durationMinutes(13).build());

        // ─── driver2 — 4 PLANNED + 1 COMPLETED ───────────────────────────────────
        tripRepository.save(Trip.builder()
                .driver(driver2).tripStatus(planned)
                .tripDatetime(LocalDateTime.now().plusDays(2).withHour(8).withMinute(15).withSecond(0).withNano(0))
                .departureAddress(vannes).arrivingAddress(sene)
                .availableSeats(3).smokingAllowed(false)
                .distanceKm(8.2).durationMinutes(15).build());

        tripRepository.save(Trip.builder()
                .driver(driver2).tripStatus(planned)
                .tripDatetime(LocalDateTime.now().plusDays(10).withHour(7).withMinute(30).withSecond(0).withNano(0))
                .departureAddress(sene).arrivingAddress(vannes)
                .availableSeats(1).smokingAllowed(false)
                .distanceKm(8.4).durationMinutes(13).build());

        tripRepository.save(Trip.builder()
                .driver(driver2).tripStatus(planned)
                .tripDatetime(LocalDateTime.now().plusDays(6).withHour(18).withMinute(0).withSecond(0).withNano(0))
                .departureAddress(vannes).arrivingAddress(sene)
                .availableSeats(2).smokingAllowed(false)
                .distanceKm(8.2).durationMinutes(15).build());

        tripRepository.save(Trip.builder()
                .driver(driver2).tripStatus(planned)
                .tripDatetime(LocalDateTime.now().plusDays(21).withHour(8).withMinute(0).withSecond(0).withNano(0))
                .departureAddress(sene).arrivingAddress(vannes)
                .availableSeats(4).smokingAllowed(false)
                .distanceKm(8.4).durationMinutes(13).build());

        tripRepository.save(Trip.builder()
                .driver(driver2).tripStatus(completed)
                .tripDatetime(LocalDateTime.now().minusDays(5).withHour(17).withMinute(0).withSecond(0).withNano(0))
                .departureAddress(vannes).arrivingAddress(sene)
                .availableSeats(0).smokingAllowed(false)
                .distanceKm(8.2).durationMinutes(15).build());

        log.info("✅ 11 trips created: 8 PLANNED, 2 COMPLETED, 1 CANCELLED");
    }

    /**
     * Crée des réservations sur les trajets :
     * - Trajet futur : student1 et student2 (CONFIRMED)
     * - Trajet passé : student1 et student3 (CONFIRMED)
     * - Trajet annulé : student2 (CANCELLED)
     */
    private void initReservations() {
        if (reservationRepository.count() > 0) {
            log.info("⏭️ Reservations already exist, skipping...");
            return;
        }

        log.info("🧪 Creating reservations...");

        ReservationStatus confirmedStatus = reservationStatusRepository.findByLabel(ReservationStatus.CONFIRMED)
                .orElseThrow(() -> new IllegalStateException("CONFIRMED status not found"));

        ReservationStatus cancelledStatus = reservationStatusRepository.findByLabel(ReservationStatus.CANCELLED)
                .orElseThrow(() -> new IllegalStateException("CANCELLED status not found"));

        List<Trip> trips = tripRepository.findAll();

        Trip plannedTrip = trips.stream()
                .filter(t -> t.getTripStatus().getLabel().equals(TripStatus.PLANNED)
                        && t.getDriver().getEmail().equals("driver1@test.fr"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No planned trip for driver1 found"));

        Trip completedTrip = trips.stream()
                .filter(t -> t.getTripStatus().getLabel().equals(TripStatus.COMPLETED)
                        && t.getDriver().getEmail().equals("driver1@test.fr"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No completed trip for driver1 found"));

        Trip cancelledTrip = trips.stream()
                .filter(t -> t.getTripStatus().getLabel().equals(TripStatus.CANCELLED))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No cancelled trip found"));

        Person student1 = personRepository.findByEmail("student1@test.fr")
                .orElseThrow(() -> new IllegalStateException("Student1 not found"));

        Person student2 = personRepository.findByEmail("student2@test.fr")
                .orElseThrow(() -> new IllegalStateException("Student2 not found"));

        Person student3 = personRepository.findByEmail("student3@test.fr")
                .orElseThrow(() -> new IllegalStateException("Student3 not found"));

        reservationRepository.save(Reservation.builder()
                .trip(plannedTrip)
                .person(student1)
                .reservationStatus(confirmedStatus)
                .build());

        reservationRepository.save(Reservation.builder()
                .trip(plannedTrip)
                .person(student2)
                .reservationStatus(confirmedStatus)
                .build());

        plannedTrip.setAvailableSeats(1);
        tripRepository.save(plannedTrip);

        reservationRepository.save(Reservation.builder()
                .trip(completedTrip)
                .person(student1)
                .reservationStatus(confirmedStatus)
                .build());

        reservationRepository.save(Reservation.builder()
                .trip(completedTrip)
                .person(student3)
                .reservationStatus(confirmedStatus)
                .build());

        reservationRepository.save(Reservation.builder()
                .trip(cancelledTrip)
                .person(student2)
                .reservationStatus(cancelledStatus)
                .build());

        log.info("✅ Reservations created (2 on planned, 2 on completed, 1 cancelled on cancelled)");
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
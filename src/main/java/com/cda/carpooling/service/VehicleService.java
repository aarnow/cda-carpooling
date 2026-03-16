package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.CreateVehicleRequest;
import com.cda.carpooling.dto.request.UpdateVehicleRequest;
import com.cda.carpooling.dto.response.VehicleMinimalResponse;
import com.cda.carpooling.dto.response.VehicleResponse;
import com.cda.carpooling.entity.Brand;
import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.Role;
import com.cda.carpooling.entity.Vehicle;
import com.cda.carpooling.exception.DuplicateResourceException;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.mapper.VehicleMapper;
import com.cda.carpooling.repository.BrandRepository;
import com.cda.carpooling.repository.PersonRepository;
import com.cda.carpooling.repository.RoleRepository;
import com.cda.carpooling.repository.VehicleRepository;
import com.cda.carpooling.validation.VehiclePlateValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service de gestion des véhicules.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final PersonRepository personRepository;
    private final BrandRepository brandRepository;
    private final RoleRepository roleRepository;
    private final VehicleMapper vehicleMapper;
    private final TripService tripService;

    /**
     * Récupère tous les véhicules.
     */
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehicles() {
        return vehicleRepository.findAll()
                .stream()
                .map(vehicleMapper::toResponse)
                .toList();
    }

    /**
     * Récupère un véhicule par son ID.
     */
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        return vehicleMapper.toResponse(findVehicleOrThrow(id));
    }

    /**
     * Crée un véhicule pour une personne.
     * Attribution automatique du rôle DRIVER.
     *
     * @param targetPersonId ID de la personne cible
     * @param request Données du véhicule
     * @return VehicleResponse
     * @throws DuplicateResourceException Si la personne a déjà un véhicule
     */
    @Transactional
    public VehicleResponse createVehicle(Long targetPersonId, CreateVehicleRequest request) {
        if (vehicleRepository.existsByPersonId(targetPersonId)) {
            log.warn("Tentative de création d'un 2e véhicule pour personne {}", targetPersonId);
            throw new DuplicateResourceException(
                    "Cette personne possède déjà un véhicule"
            );
        }

        Person person = findPersonOrThrow(targetPersonId);
        Brand brand = findBrandOrThrow(request.getBrandId());

        Vehicle vehicle = Vehicle.builder()
                .person(person)
                .brand(brand)
                .model(request.getModel())
                .seats(request.getSeats())
                .plate(VehiclePlateValidator.normalize(request.getPlate()))
                .description(request.getDescription())
                .build();

        Role driverRole = roleRepository.findByLabel(Role.ROLE_DRIVER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "label", Role.ROLE_DRIVER));

        if (!person.getRoles().contains(driverRole)) {
            person.addRole(driverRole);
            log.info("Rôle DRIVER attribué à personne {}", targetPersonId);
        }

        personRepository.save(person);
        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Véhicule créé : {} {} ({}) pour personne {} (id={})",
                brand.getName(), saved.getModel(), saved.getPlate(),
                targetPersonId, saved.getId());

        return vehicleMapper.toResponse(saved);
    }

    /**
     * Récupère le véhicule de l'utilisateur authentifié.
     * Retourne un Optional vide si l'utilisateur n'a pas de véhicule.
     *
     * @param personId ID de la personne authentifiée
     * @return Optional<VehicleResponse>
     */
    @Transactional(readOnly = true)
    public Optional<VehicleMinimalResponse> getMyVehicle(Long personId) {
        return vehicleRepository.findByPersonId(personId)
                .map(vehicleMapper::toMinimalResponse);
    }

    /**
     * Met à jour un véhicule existant.
     * Seuls les champs non null sont mis à jour.
     *
     * @param id      ID du véhicule
     * @param request Données à mettre à jour
     */
    @Transactional
    public VehicleResponse updateVehicle(Long id, UpdateVehicleRequest request) {
        Vehicle vehicle = findVehicleOrThrow(id);

        if (request.getBrandId() != null) {
            vehicle.setBrand(findBrandOrThrow(request.getBrandId()));
        }
        if (request.getModel() != null) {
            vehicle.setModel(request.getModel());
        }
        if (request.getSeats() != null) {
            vehicle.setSeats(request.getSeats());
        }
        if (request.getPlate() != null) {
            vehicle.setPlate(VehiclePlateValidator.normalize(request.getPlate()));
        }
        if (request.getDescription() != null) {
            vehicle.setDescription(request.getDescription());
        }

        Vehicle updated = vehicleRepository.save(vehicle);
        log.info("Véhicule mis à jour : id={}", id);

        return vehicleMapper.toResponse(updated);
    }

    /**
     * Supprime un véhicule.
     */
    @Transactional
    public void deleteVehicle(Long id) {
        Vehicle vehicle = findVehicleOrThrow(id);
        Person person = vehicle.getPerson();

        log.info("Suppression véhicule {} (propriétaire {})", id, person.getId());

        tripService.cancelDriverTrips(person.getId());

        Role driverRole = roleRepository.findByLabel(Role.ROLE_DRIVER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "label", Role.ROLE_DRIVER));

        if (person.getRoles().contains(driverRole)) {
            person.removeRole(driverRole);
            personRepository.save(person);
            log.info("Rôle DRIVER retiré à personne {}", person.getId());
        }

        person.setVehicle(null);
        vehicle.setPerson(null);
        vehicleRepository.delete(vehicle);

        log.warn("Véhicule {} supprimé (propriétaire {})", id, person.getId());
    }

    /**
     * Retourne l'ID du propriétaire d'un véhicule.
     */
    @Transactional(readOnly = true)
    public Long getVehicleOwnerId(Long vehicleId) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);
        return vehicle.getPerson().getId();
    }

    //region Utils
    private Vehicle findVehicleOrThrow(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Véhicule", "id", id));
    }

    private Person findPersonOrThrow(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personne", "id", id));
    }

    private Brand findBrandOrThrow(Long id) {
        return brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marque", "id", id));
    }
    //endregion
}
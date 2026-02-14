package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.CreateVehicleRequest;
import com.cda.carpooling.dto.request.UpdateVehicleRequest;
import com.cda.carpooling.dto.response.VehicleResponse;
import com.cda.carpooling.entity.Brand;
import com.cda.carpooling.entity.Person;
import com.cda.carpooling.entity.Vehicle;
import com.cda.carpooling.exception.DuplicateResourceException;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.mapper.VehicleMapper;
import com.cda.carpooling.repository.BrandRepository;
import com.cda.carpooling.repository.PersonRepository;
import com.cda.carpooling.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service de gestion des véhicules.
 */
@Service
@RequiredArgsConstructor
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final PersonRepository personRepository;
    private final BrandRepository brandRepository;
    private final VehicleMapper vehicleMapper;

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
     * Une personne ne peut posséder qu'un seul véhicule.
     * TODO : La création d'un véhicule accorde le role DRIVER
     * @param targetPersonId ID de la personne cible
     * @param request        Données du véhicule
     */
    @Transactional
    public VehicleResponse createVehicle(Long targetPersonId, CreateVehicleRequest request) {
        if (vehicleRepository.existsByPersonId(targetPersonId)) {
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
                .plate(request.getPlate())
                .description(request.getDescription())
                .build();

        Vehicle saved = vehicleRepository.save(vehicle);

        return vehicleMapper.toResponse(saved);
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
            vehicle.setPlate(request.getPlate());
        }
        if (request.getDescription() != null) {
            vehicle.setDescription(request.getDescription());
        }

        Vehicle updated = vehicleRepository.save(vehicle);

        return vehicleMapper.toResponse(updated);
    }

    /**
     * Supprime un véhicule.
     * TODO : La suppression du véhicule retire le role DRIVER de son propriétaire
     * TODO : la suppression doit entrainer l'annulation des trips à venir avec ce conducteur
     */
    @Transactional
    public void deleteVehicle(Long vehicleId) {
        Vehicle vehicle = findVehicleOrThrow(vehicleId);

        Person person = vehicle.getPerson();
        if (person != null) {
            person.setVehicle(null);
            vehicle.setPerson(null);
        }

        vehicleRepository.delete(vehicle);
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
package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.CreateTripRequest;
import com.cda.carpooling.dto.request.UpdateTripRequest;
import com.cda.carpooling.dto.response.PersonMinimalResponse;
import com.cda.carpooling.dto.response.TripMinimalResponse;
import com.cda.carpooling.dto.response.TripResponse;
import com.cda.carpooling.entity.*;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.mapper.TripMapper;
import com.cda.carpooling.repository.*;
import com.cda.carpooling.specification.TripSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Service de gestion des trajets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TripService {

    private final TripRepository tripRepository;
    private final PersonRepository personRepository;
    private final TripStatusRepository tripStatusRepository;
    private final ReservationService reservationService;
    private final AddressService addressService;
    private final TripMapper tripMapper;

    /**
     * Recherche des trajets avec filtres optionnels.
     *
     * @param tripDate Date du trajet (filtre par jour)
     * @param startingCity Nom de la ville de départ
     * @param arrivalCity Nom de la ville d'arrivée
     * @return Liste de TripMinimalResponse
     */
    @Transactional(readOnly = true)
    public List<TripMinimalResponse> getAllTrips(
            LocalDate tripDate,
            String startingCity,
            String arrivalCity) {

        log.debug("Recherche trajets : date={}, départ={}, arrivée={}",
                tripDate, startingCity, arrivalCity);

        Specification<Trip> spec = Specification
                .where(TripSpecification.hasDate(tripDate))
                .and(TripSpecification.hasDepartureCity(startingCity))
                .and(TripSpecification.hasArrivingCity(arrivalCity));

        List<TripMinimalResponse> results = tripRepository.findAll(spec)
                .stream()
                .map(tripMapper::toMinimalResponse)
                .toList();

        log.debug("{} trajets trouvés", results.size());
        return results;
    }

    @Transactional(readOnly = true)
    public TripResponse getTripById(Long id) {
        return tripMapper.toResponse(findTripOrThrow(id));
    }

    /**
     * Retourne la liste des passagers d'un trajet (réservations non annulées).
     */
    @Transactional(readOnly = true)
    public List<PersonMinimalResponse> getTripPassengers(Long tripId) {
        Trip trip = findTripOrThrow(tripId);

        return trip.getReservations().stream()
                .filter(r -> !r.getReservationStatus().getLabel().equals(ReservationStatus.CANCELLED))
                .map(r -> PersonMinimalResponse.builder()
                        .id(r.getPerson().getId())
                        .email(r.getPerson().getEmail())
                        .status(r.getPerson().getStatus().getLabel())
                        .build())
                .toList();
    }

    /**
     * Retourne l'ID du conducteur d'un trajet.
     */
    @Transactional(readOnly = true)
    public Long getTripDriverId(Long tripId) {
        return findTripOrThrow(tripId).getDriver().getId();
    }

    /**
     * Vérifie si une personne est en relation avec un trajet.
     */
    @Transactional(readOnly = true)
    public boolean isPersonRelatedToTrip(Long personId, Long tripId) {
        boolean isPassenger = reservationService.isPersonRelatedToTrip(personId, tripId);
        boolean isDriver = getTripDriverId(tripId).equals(personId);
        return isPassenger || isDriver;
    }

    /**
     * Crée un trajet. Réservé aux conducteurs (ROLE_DRIVER).
     * Les adresses doivent exister en BDD (issues de la recherche BAN).
     */
    @Transactional
    public TripResponse createTrip(Long driverId, CreateTripRequest request) {
        Person driver = findPersonOrThrow(driverId);

        TripStatus plannedStatus = tripStatusRepository.findByLabel(TripStatus.PLANNED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Statut", "label", TripStatus.PLANNED));

        Address departureAddress = addressService.findAddressOrThrow(request.getDepartureAddressId());
        Address arrivingAddress = addressService.findAddressOrThrow(request.getArrivingAddressId());

        Trip trip = Trip.builder()
                .driver(driver)
                .tripDatetime(request.getTripDatetime())
                .availableSeats(request.getAvailableSeats())
                .smokingAllowed(request.isSmokingAllowed())
                .tripStatus(plannedStatus)
                .departureAddress(departureAddress)
                .arrivingAddress(arrivingAddress)
                .build();

        Trip saved = tripRepository.save(trip);
        log.info("✅ Trajet créé : {} → {} par conducteur {} (tripId={}, {} places)",
                departureAddress.getCity().getName(),
                arrivingAddress.getCity().getName(),
                driverId,
                saved.getId(),
                saved.getAvailableSeats());

        return tripMapper.toResponse(saved);
    }

    /**
     * Met à jour un trajet. Réservé au conducteur propriétaire ou à un admin.
     * TODO : la moindre modification doit alerter les passagers par email
     * TODO : TripStatus CANCELLED doit annuler les réservations + envoyer des emails (endpoint dédié ?)
     */
    @Transactional
    public TripResponse updateTrip(Long id, UpdateTripRequest request) {
        Trip trip = findTripOrThrow(id);

        if (request.getTripDatetime() != null) {
            trip.setTripDatetime(request.getTripDatetime());
        }
        if (request.getAvailableSeats() != null) {
            trip.setAvailableSeats(request.getAvailableSeats());
        }
        if (request.getSmokingAllowed() != null) {
            trip.setSmokingAllowed(request.getSmokingAllowed());
        }
        if (request.getDepartureAddressId() != null) {
            trip.setDepartureAddress(
                    addressService.findAddressOrThrow(request.getDepartureAddressId()));
        }
        if (request.getArrivingAddressId() != null) {
            trip.setArrivingAddress(
                    addressService.findAddressOrThrow(request.getArrivingAddressId()));
        }

        Trip updated = tripRepository.save(trip);
        log.info("Trajet mis à jour : id={}", id);

        return tripMapper.toResponse(updated);
    }

    /**
     * Supprime un trajet.
     * Annule les réservations associées avant suppression.
     * TODO : envoyer un email aux passagers impactés
     */
    @Transactional
    public void deleteTrip(Long id) {
        Trip trip = findTripOrThrow(id);

        reservationService.cancelTripReservations(trip);
        tripRepository.delete(trip);

        log.warn("🗑Trajet {} supprimé", id);
    }

    /**
     * Annule tous les trajets à venir d'un conducteur.
     * @param driverId ID du conducteur
     */
    @Transactional
    public void cancelDriverTrips(Long driverId) {
        List<Trip> upcomingTrips = tripRepository
                .findAllByDriverIdAndTripStatusLabel(driverId, TripStatus.PLANNED);

        if (upcomingTrips.isEmpty()) {
            log.debug("Aucun trajet à venir pour conducteur {}", driverId);
            return;
        }

        TripStatus cancelledStatus = tripStatusRepository.findByLabel(TripStatus.CANCELLED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Statut", "label", TripStatus.CANCELLED));

        upcomingTrips.forEach(trip -> {
            reservationService.cancelTripReservations(trip);
            trip.setTripStatus(cancelledStatus);
            tripRepository.save(trip);
            log.info("Trajet {} annulé", trip.getId());
        });

        log.info("{} trajets annulés (conducteur {} sans véhicule)",
                upcomingTrips.size(), driverId);
    }

    /**
     * Retourne tous les trajets d'une personne en tant que conducteur.
     */
    @Transactional(readOnly = true)
    public List<TripMinimalResponse> getTripsByDriver(Long driverId) {
        return tripRepository.findAllByDriverId(driverId)
                .stream()
                .map(tripMapper::toMinimalResponse)
                .toList();
    }

    /**
     * Retourne tous les trajets d'une personne en tant que passager
     */
    @Transactional(readOnly = true)
    public List<TripMinimalResponse> getTripsByPassenger(Long personId) {
        return tripRepository.findAllByPassengerId(personId)
                .stream()
                .map(tripMapper::toMinimalResponse)
                .toList();
    }

    //region Utils
    public Trip findTripOrThrow(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet", "id", id));
    }

    private Person findPersonOrThrow(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personne", "id", id));
    }
    //endregion
}
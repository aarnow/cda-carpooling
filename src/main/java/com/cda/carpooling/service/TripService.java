package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.CreateTripRequest;
import com.cda.carpooling.dto.request.UpdateTripRequest;
import com.cda.carpooling.dto.response.PersonMinimalResponse;
import com.cda.carpooling.dto.response.ReservationResponse;
import com.cda.carpooling.dto.response.TripMinimalResponse;
import com.cda.carpooling.dto.response.TripResponse;
import com.cda.carpooling.entity.*;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.mapper.TripMapper;
import com.cda.carpooling.repository.*;
import com.cda.carpooling.specification.TripSpecification;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final PersonRepository personRepository;
    private final TripStatusRepository tripStatusRepository;
    private final ReservationService reservationService;
    private final AddressService addressService;
    private final TripMapper tripMapper;

    @Transactional(readOnly = true)
    public List<TripMinimalResponse> getAllTrips(
            LocalDate tripDate,
            String startingCity,
            String arrivalCity) {

        Specification<Trip> spec = Specification
                .where(TripSpecification.hasDate(tripDate))
                .and(TripSpecification.hasDepartureCity(startingCity))
                .and(TripSpecification.hasArrivingCity(arrivalCity));

        return tripRepository.findAll(spec)
                .stream()
                .map(tripMapper::toMinimalResponse)
                .toList();
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
     * Utilisé par TripController pour vérifier les permissions.
     */
    @Transactional(readOnly = true)
    public Long getTripDriverId(Long tripId) {
        return findTripOrThrow(tripId).getDriver().getId();
    }

    /**
     * Vérifie si une personne est en relation avec un trajet.
     * Utilisé par TripController pour GET /trips/{id}/persons.
     */
    @Transactional(readOnly = true)
    public boolean isPersonRelatedToTrip(Long personId, Long tripId) {
        boolean isPassenger = reservationService.isPersonRelatedToTrip(personId, tripId);
        boolean isDriver = getTripDriverId(tripId).equals(personId);
        return isPassenger || isDriver;
    }

    /**
     * Crée un trajet. Réservé aux conducteurs (ROLE_DRIVER).
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
        return tripMapper.toResponse(saved);
    }

    /**
     * Met à jour un trajet. Réservé au conducteur propriétaire ou à un admin.
     * TODO : la moindre modification doit alerter les passagers par email
     * TODO : TripStatus CANCELLED doit annuler les réservations + envoyer des emails (endpoint dédié ?)
     * TODO : TripStatus COMPLETED ne doit pas être sélectionnable manuellement
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
        if (request.getTripStatusLabel() != null) {
            TripStatus status = tripStatusRepository.findByLabel(request.getTripStatusLabel())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Statut", "label", request.getTripStatusLabel()));
            trip.setTripStatus(status);
        }

        Trip updated = tripRepository.save(trip);
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

        // 1. Annuler toutes les réservations actives
        reservationService.cancelTripReservations(trip);

        // 2. Supprimer le trajet — plus de FK qui bloque
        tripRepository.delete(trip);
    }

    /**
     * Annule tous les trajets à venir d'un conducteur.
     * Appelé par VehicleService lors de la suppression du véhicule du conducteur.
     */
    @Transactional
    public void cancelDriverTrips(Long driverId) {
        List<Trip> upcomingTrips = tripRepository
                .findAllByDriverIdAndTripStatusLabel(driverId, TripStatus.PLANNED);

        TripStatus cancelledStatus = tripStatusRepository.findByLabel(TripStatus.CANCELLED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Statut", "label", TripStatus.CANCELLED));

        upcomingTrips.forEach(trip -> {
            // 1. Annuler les réservations associées
            reservationService.cancelTripReservations(trip);

            // 2. Annuler le trajet
            trip.setTripStatus(cancelledStatus);
            tripRepository.save(trip);
        });
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
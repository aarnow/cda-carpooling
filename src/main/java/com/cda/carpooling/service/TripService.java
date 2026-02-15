package com.cda.carpooling.service;

import com.cda.carpooling.dto.request.CreateAddressRequest;
import com.cda.carpooling.dto.request.CreateTripRequest;
import com.cda.carpooling.dto.request.UpdateTripRequest;
import com.cda.carpooling.dto.response.PersonMinimalResponse;
import com.cda.carpooling.dto.response.ReservationResponse;
import com.cda.carpooling.dto.response.TripResponse;
import com.cda.carpooling.dto.response.TripMinimalResponse;
import com.cda.carpooling.entity.*;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.mapper.AddressMapper;
import com.cda.carpooling.mapper.ReservationMapper;
import com.cda.carpooling.mapper.TripMapper;
import com.cda.carpooling.repository.*;
import com.cda.carpooling.specification.TripSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TripService {

    private final TripRepository tripRepository;
    private final PersonRepository personRepository;
    private final TripStatusRepository tripStatusRepository;
    private final ReservationRepository reservationRepository;
    private final ReservationStatusRepository reservationStatusRepository;
    private final AddressMapper addressMapper;
    private final TripMapper tripMapper;
    private final ReservationMapper reservationMapper;
    private final AddressService addressService;

    @Transactional(readOnly = true)
    public List<TripMinimalResponse> getAllTrips(
            LocalDate tripDate,
            String startingCityId,
            String arrivalCityId) {

        Specification<Trip> spec = Specification
                .where(TripSpecification.hasDate(tripDate))
                .and(TripSpecification.hasDepartureCity(startingCityId))
                .and(TripSpecification.hasArrivingCity(arrivalCityId));

        return tripRepository.findAll(spec)
                .stream()
                .map(tripMapper::toMinimalResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TripMinimalResponse getTripById(Long id) {
        return tripMapper.toMinimalResponse(findTripOrThrow(id));
    }

    /**
     * Retourne la liste des passagers d'un trajet.
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
     * Crée un trajet. Réservé aux conducteurs (ROLE_DRIVER).
     * Les adresses sont créées à la volée si elles n'existent pas encore.
     */
    @Transactional
    public TripResponse createTrip(Long driverId, CreateTripRequest request) {
        Person driver = findPersonOrThrow(driverId);

        TripStatus plannedStatus = tripStatusRepository.findByLabel(TripStatus.PLANNED)
                .orElseThrow(() -> new ResourceNotFoundException("Statut", "label", TripStatus.PLANNED));

        Address departureAddress = addressService.findOrCreate(
                buildAddressRequest(request.getDepartureAddressId())
        );

        if (!departureAddress.isValidated()) {
            throw new IllegalStateException("L'adresse de départ invalide");
        }

        Address arrivingAddress = addressService.findOrCreate(
                buildAddressRequest(request.getArrivingAddressId())
        );

        if (!arrivingAddress.isValidated()) {
            throw new IllegalStateException("L'adresse de d'arrivée invalide");
        }

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
     * TODO : TripStatus CANCELLED doit entrainer l'annulation des réservations associées + déclencher l'envoi d'un email pour prevenir les personnes impactées
     * TODO : L'utilisateur ne devrait pas pouvoir sélectionner TripStatus IN_PROGRESS ou COMPLETED (automatique)
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
            trip.setDepartureAddress(addressService.findAddressOrThrow(request.getDepartureAddressId()));
        }
        if (request.getArrivingAddressId() != null) {
            trip.setArrivingAddress(addressService.findAddressOrThrow(request.getArrivingAddressId()));
        }
        if (request.getTripStatusLabel() != null) {
            TripStatus status = tripStatusRepository.findByLabel(request.getTripStatusLabel())
                    .orElseThrow(() -> new ResourceNotFoundException("Statut", "label", request.getTripStatusLabel()));
            trip.setTripStatus(status);
        }

        Trip updated = tripRepository.save(trip);
        return tripMapper.toResponse(updated);
    }

    /**
     * Supprime un trajet. Réservé au conducteur propriétaire ou à un admin.
     * TODO : doit supprimer les réservations associés + déclencher l'envoi d'un email pour prevenir les personnes impactées
     */
    @Transactional
    public void deleteTrip(Long id) {
        Trip trip = findTripOrThrow(id);
        tripRepository.delete(trip);
    }

    /**
     * Réserve une place sur un trajet OU annule la réservation existante.
     * - Si pas de réservation → crée une réservation CONFIRMED et décrémente availableSeats
     * - Si réservation existante → annule et restitue la place
     */
    @Transactional
    public ReservationResponse toggleReservation(Long tripId, Long personId) {
        Trip trip = findTripOrThrow(tripId);
        Person person = findPersonOrThrow(personId);

        return reservationRepository
                .findByPersonIdAndTripId(personId, tripId)
                .map(existing -> cancelReservation(existing, trip))
                .orElseGet(() -> createReservation(trip, person));
    }

    /**
     * Retourne l'ID du conducteur d'un trajet.
     */
    @Transactional(readOnly = true)
    public Long getTripDriverId(Long tripId) {
        return findTripOrThrow(tripId).getDriver().getId();
    }


    // TODO : Peut être séparer via un ReservationService
    //region Utils
    @Transactional(readOnly = true)
    public boolean isPersonRelatedToTrip(Long personId, Long tripId) {
        boolean isPassenger = reservationRepository
                .findByPersonIdAndTripId(personId, tripId)
                .isPresent();
        boolean isDriver = getTripDriverId(tripId).equals(personId);
        return isPassenger || isDriver;
    }

    private ReservationResponse createReservation(Trip trip, Person person) {
        if (trip.getAvailableSeats() <= 0) {
            throw new IllegalStateException("Ce trajet n'a plus de places disponibles");
        }

        if (trip.getDriver().getId().equals(person.getId())) {
            throw new AccessDeniedException("Un conducteur ne peut pas réserver son propre trajet");
        }

        ReservationStatus pendingStatus = reservationStatusRepository
                .findByLabel(ReservationStatus.CONFIRMED)
                .orElseThrow(() -> new ResourceNotFoundException("Statut", "label", ReservationStatus.CONFIRMED));

        // Décrémente une place
        trip.setAvailableSeats(trip.getAvailableSeats() - 1);
        tripRepository.save(trip);

        Reservation reservation = Reservation.builder()
                .trip(trip)
                .person(person)
                .reservationStatus(pendingStatus)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        return reservationMapper.toResponse(saved);
    }

    private ReservationResponse cancelReservation(Reservation reservation, Trip trip) {
        ReservationStatus cancelledStatus = reservationStatusRepository
                .findByLabel(ReservationStatus.CANCELLED)
                .orElseThrow(() -> new ResourceNotFoundException("Statut", "label", ReservationStatus.CANCELLED));

        if (!reservation.getReservationStatus().getLabel().equals(ReservationStatus.CANCELLED)) {
            trip.setAvailableSeats(trip.getAvailableSeats() + 1);
            tripRepository.save(trip);
        }

        reservation.setReservationStatus(cancelledStatus);
        Reservation updated = reservationRepository.save(reservation);
        return reservationMapper.toResponse(updated);
    }

    private Trip findTripOrThrow(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet", "id", id));
    }

    private Person findPersonOrThrow(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personne", "id", id));
    }

    /**
     * Construit un AddressRequest minimal depuis un ID existant.
     * Utilisé pour le findOrCreate quand l'adresse est fournie par ID.
     */
    private CreateAddressRequest buildAddressRequest(Long addressId) {
        Address existing = addressService.findAddressOrThrow(addressId);
        return addressMapper.toRequest(existing);
    }
    //endregion
}
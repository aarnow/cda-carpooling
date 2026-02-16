package com.cda.carpooling.service;

import com.cda.carpooling.dto.response.ReservationResponse;
import com.cda.carpooling.entity.*;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.mapper.ReservationMapper;
import com.cda.carpooling.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationStatusRepository reservationStatusRepository;
    private final TripRepository tripRepository;
    private final PersonRepository personRepository;
    private final ReservationMapper reservationMapper;

    /**
     * Réserve une place sur un trajet OU annule la réservation existante (toggle).
     * Appelé par TripController sur POST /trips/{id}/persons
     */
    @Transactional
    public ReservationResponse toggleReservation(Long tripId, Long personId) {
        Trip trip = findTripOrThrow(tripId);

        if (trip.getTripStatus().getLabel().equals(TripStatus.CANCELLED)) {
            throw new IllegalStateException("Impossible de réserver sur un trajet annulé");
        }
        if (trip.getTripStatus().getLabel().equals(TripStatus.COMPLETED)) {
            throw new IllegalStateException("Impossible de réserver sur un trajet terminé");
        }

        Person person = findPersonOrThrow(personId);

        return reservationRepository
                .findByPersonIdAndTripId(personId, tripId)
                .map(existing -> {
                    if (existing.getReservationStatus().getLabel().equals(ReservationStatus.CANCELLED)) {
                        return createReservation(trip, person);
                    } else {
                        return cancelReservation(existing, trip);
                    }
                })
                .orElseGet(() -> createReservation(trip, person));
    }

    /**
     * Annule toutes les réservations actives d'un trajet.
     * Appelé par TripService lors de l'annulation ou suppression d'un trajet.
     *
     * @param trip Le trajet dont les réservations doivent être annulées
     */
    @Transactional
    public void cancelTripReservations(Trip trip) {
        ReservationStatus cancelledStatus = findCancelledStatusOrThrow();

        List<Reservation> activeReservations = trip.getReservations().stream()
                .filter(r -> !r.getReservationStatus().getLabel().equals(ReservationStatus.CANCELLED))
                .toList();

        activeReservations.forEach(reservation -> {
            reservation.setReservationStatus(cancelledStatus);
            reservationRepository.save(reservation);
        });
    }

    /**
     * Vérifie si une personne est en relation avec un trajet
     * (conducteur ou passager avec réservation active).
     * Appelé par TripController pour vérifier les permissions.
     */
    @Transactional(readOnly = true)
    public boolean isPersonRelatedToTrip(Long personId, Long tripId) {
        return reservationRepository
                .findByPersonIdAndTripId(personId, tripId)
                .isPresent();
    }

    //region Utils
    private ReservationResponse createReservation(Trip trip, Person person) {
        if (trip.getAvailableSeats() <= 0) {
            throw new IllegalStateException("Ce trajet n'a plus de places disponibles");
        }

        if (trip.getDriver().getId().equals(person.getId())) {
            throw new AccessDeniedException("Un conducteur ne peut pas réserver son propre trajet");
        }

        ReservationStatus confirmedStatus = reservationStatusRepository
                .findByLabel(ReservationStatus.CONFIRMED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Statut", "label", ReservationStatus.CONFIRMED));

        trip.setAvailableSeats(trip.getAvailableSeats() - 1);
        tripRepository.save(trip);

        Reservation reservation = Reservation.builder()
                .trip(trip)
                .person(person)
                .reservationStatus(confirmedStatus)
                .build();

        Reservation saved = reservationRepository.save(reservation);
        return reservationMapper.toResponse(saved);
    }

    private ReservationResponse cancelReservation(Reservation reservation, Trip trip) {
        ReservationStatus cancelledStatus = findCancelledStatusOrThrow();

        if (!reservation.getReservationStatus().getLabel().equals(ReservationStatus.CANCELLED)) {
            trip.setAvailableSeats(trip.getAvailableSeats() + 1);
            tripRepository.save(trip);
        }

        reservation.setReservationStatus(cancelledStatus);
        Reservation updated = reservationRepository.save(reservation);
        return reservationMapper.toResponse(updated);
    }

    private ReservationStatus findCancelledStatusOrThrow() {
        return reservationStatusRepository
                .findByLabel(ReservationStatus.CANCELLED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Statut", "label", ReservationStatus.CANCELLED));
    }

    private Trip findTripOrThrow(Long id) {
        return tripRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Trajet", "id", id));
    }

    private Person findPersonOrThrow(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Personne", "id", id));
    }
    //endregion
}
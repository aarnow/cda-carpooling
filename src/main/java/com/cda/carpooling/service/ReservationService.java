package com.cda.carpooling.service;

import com.cda.carpooling.dto.response.ReservationResponse;
import com.cda.carpooling.entity.*;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.mapper.ReservationMapper;
import com.cda.carpooling.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service de gestion des réservations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final ReservationStatusRepository reservationStatusRepository;
    private final TripRepository tripRepository;
    private final PersonRepository personRepository;
    private final ReservationMapper reservationMapper;

    /**
     * Réserve une place sur un trajet OU gère l'annulation/réactivation.
     *
     * @param tripId ID du trajet
     * @param personId ID de la personne
     * @return ReservationResponse avec le nouvel état
     */
    @Transactional
    public ReservationResponse toggleReservation(Long tripId, Long personId) {
        log.debug("Toggle réservation : tripId={}, personId={}", tripId, personId);
        Trip trip = findTripOrThrow(tripId);

        if (trip.getTripStatus().getLabel().equals(TripStatus.CANCELLED)) {
            log.warn("Tentative de réservation sur trajet annulé : tripId={}", tripId);
            throw new IllegalStateException("Impossible de réserver sur un trajet annulé");
        }
        if (trip.getTripStatus().getLabel().equals(TripStatus.COMPLETED)) {
            log.warn("Tentative de réservation sur trajet terminé : tripId={}", tripId);
            throw new IllegalStateException("Impossible de réserver sur un trajet terminé");
        }

        Person person = findPersonOrThrow(personId);

        if (person.getProfile() == null) {
            log.warn("❌ Tentative de réservation par personne {} sans profil", personId);
            throw new IllegalStateException("Vous devez compléter votre profil avant de réserver un trajet");
        }

        return reservationRepository
                .findByPersonIdAndTripId(personId, tripId)
                .map(existing -> {
                    if (existing.getReservationStatus().getLabel().equals(ReservationStatus.CANCELLED)) {
                        log.info("Réactivation réservation {} (personId={}, tripId={})",
                                existing.getId(), personId, tripId);
                        return createReservation(trip, person);
                    } else {
                        log.info("Annulation réservation {} (personId={}, tripId={})",
                                existing.getId(), personId, tripId);
                        return cancelReservation(existing, trip);
                    }
                })
                .orElseGet(() -> {
                    log.info("➕ Nouvelle réservation (personId={}, tripId={})", personId, tripId);
                    return createReservation(trip, person);
                });
    }

    /**
     * Annule toutes les réservations actives d'un trajet.
     *
     * @param trip Le trajet dont les réservations doivent être annulées
     */
    @Transactional
    public void cancelTripReservations(Trip trip) {
        ReservationStatus cancelledStatus = findCancelledStatusOrThrow();

        List<Reservation> activeReservations = trip.getReservations().stream()
                .filter(r -> !r.getReservationStatus().getLabel().equals(ReservationStatus.CANCELLED))
                .toList();

        if (activeReservations.isEmpty()) {
            log.debug("Aucune réservation active à annuler pour tripId={}", trip.getId());
            return;
        }

        activeReservations.forEach(reservation -> {
            reservation.setReservationStatus(cancelledStatus);
            reservationRepository.save(reservation);
            log.info("Réservation {} annulée (trajet {} annulé)",
                    reservation.getId(), trip.getId());
        });

        log.info("{} réservations annulées (trajet {})", activeReservations.size(), trip.getId());
    }

    /**
     * Vérifie si une personne a une réservation (active ou annulée) sur un trajet.
     */
    @Transactional(readOnly = true)
    public boolean isPersonRelatedToTrip(Long personId, Long tripId) {
        return reservationRepository
                .findByPersonIdAndTripId(personId, tripId)
                .isPresent();
    }

    //region Utils
    /**
     * Crée une nouvelle réservation CONFIRMED et décrémente les places disponibles.
     */
    private ReservationResponse createReservation(Trip trip, Person person) {
        if (trip.getAvailableSeats() <= 0) {
            log.warn("Plus de places disponibles : tripId={}", trip.getId());
            throw new IllegalStateException("Ce trajet n'a plus de places disponibles");
        }

        if (trip.getDriver().getId().equals(person.getId())) {
            log.warn("Conducteur tente de réserver son propre trajet : personId={}, tripId={}",
                    person.getId(), trip.getId());
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
        log.info("Réservation créée : id={}, personId={}, tripId={} ({} places restantes)",
                saved.getId(), person.getId(), trip.getId(), trip.getAvailableSeats());
        return reservationMapper.toResponse(saved);
    }

    /**
     * Annule une réservation et restitue la place si elle était confirmée.
     */
    private ReservationResponse cancelReservation(Reservation reservation, Trip trip) {
        ReservationStatus cancelledStatus = findCancelledStatusOrThrow();

        if (!reservation.getReservationStatus().getLabel().equals(ReservationStatus.CANCELLED)) {
            trip.setAvailableSeats(trip.getAvailableSeats() + 1);
            tripRepository.save(trip);
            log.debug("Place restituée : tripId={} ({} places disponibles)",
                    trip.getId(), trip.getAvailableSeats());
        }

        reservation.setReservationStatus(cancelledStatus);
        Reservation updated = reservationRepository.save(reservation);
        log.info("Réservation annulée : id={}", updated.getId());
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
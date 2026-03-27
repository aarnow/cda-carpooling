package com.cda.carpooling.service;

import com.cda.carpooling.dto.response.ReservationResponse;
import com.cda.carpooling.entity.*;
import com.cda.carpooling.exception.ResourceNotFoundException;
import com.cda.carpooling.exception.business.NoSeatsAvailableException;
import com.cda.carpooling.exception.business.ProfileIncompleteException;
import com.cda.carpooling.exception.business.TripNotAvailableException;
import com.cda.carpooling.integration.EmailService;
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
    private final EmailService emailService;

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
            throw new TripNotAvailableException("Impossible de réserver sur un trajet annulé");
        }
        if (trip.getTripStatus().getLabel().equals(TripStatus.COMPLETED)) {
            log.warn("Tentative de réservation sur trajet terminé : tripId={}", tripId);
            throw new TripNotAvailableException("Impossible de réserver sur un trajet terminé");
        }

        Person person = findPersonOrThrow(personId);

        if (person.getProfile() == null) {
            log.warn("Tentative de réservation par personne {} sans profil", personId);
            throw new ProfileIncompleteException("Vous devez compléter votre profil avant de réserver un trajet");
        }

        return reservationRepository
                .findByPersonIdAndTripId(personId, tripId)
                .map(existing -> {
                    if (existing.getReservationStatus().getLabel().equals(ReservationStatus.CANCELLED)) {
                        log.info("Réactivation réservation {} (personId={}, tripId={})", existing.getId(), personId, tripId);
                        return createOrReactivateReservation(trip, person, existing);
                    } else {
                        log.info("Annulation réservation {} (personId={}, tripId={})", existing.getId(), personId, tripId);
                        return cancelReservation(existing, trip);
                    }
                })
                .orElseGet(() -> {
                    log.info("Nouvelle réservation (personId={}, tripId={})", personId, tripId);
                    return createOrReactivateReservation(trip, person, null);
                });
    }

    /**
     * Retourne toutes les réservations d'une personne en tant que passager.
     *
     * @param personId ID de la personne
     * @return Liste des réservations
     */
    @Transactional(readOnly = true)
    public List<ReservationResponse> getTripsByPassenger(Long personId) {
        return reservationRepository.findByPersonId(personId)
                .stream()
                .map(reservationMapper::toResponse)
                .toList();
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
     * Annule la réservation d'une personne sur un trajet spécifique.
     * Lève une exception si aucune réservation active n'est trouvée.
     *
     * @param tripId   ID du trajet
     * @param personId ID de la personne
     * @return La réservation annulée
     */
    @Transactional
    public Reservation cancelSingleTripReservation(Long tripId, Long personId) {
        Reservation reservation = reservationRepository
                .findByTripIdAndPersonIdAndStatusNotCancelled(tripId, personId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Réservation active", "tripId/personId", tripId + "/" + personId));

        ReservationStatus cancelledStatus = reservationStatusRepository.findByLabel(ReservationStatus.CANCELLED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Statut", "label", ReservationStatus.CANCELLED));

        reservation.setReservationStatus(cancelledStatus);

        // Restitue la place au trajet
        Trip trip = reservation.getTrip();
        trip.setAvailableSeats(trip.getAvailableSeats() + 1);
        tripRepository.save(trip);

        Reservation saved = reservationRepository.save(reservation);

        // Notifier le passager
        emailService.sendReservationCancelledByDriverNotification(saved.getPerson(), trip);

        return saved;
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
     * Crée ou réactive une réservation et décrémente les places disponibles.
     *
     * @param trip Trajet concerné
     * @param person Personne qui réserve
     * @param existingReservation Réservation existante à réactiver (null si nouvelle réservation)
     * @return ReservationResponse
     */
    private ReservationResponse createOrReactivateReservation(
            Trip trip,
            Person person,
            Reservation existingReservation) {

        if (trip.getAvailableSeats() <= 0) {
            throw new NoSeatsAvailableException("Ce trajet n'a plus de places disponibles");
        }

        if (trip.getDriver().getId().equals(person.getId())) {
            throw new IllegalStateException("Un conducteur ne peut pas réserver son propre trajet");
        }

        ReservationStatus confirmedStatus = reservationStatusRepository
                .findByLabel(ReservationStatus.CONFIRMED)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Statut", "label", ReservationStatus.CONFIRMED));

        Reservation reservation;

        if (existingReservation != null) {
            existingReservation.setReservationStatus(confirmedStatus);
            reservation = reservationRepository.save(existingReservation);
            log.info("Réservation réactivée : id={}, personId={}, tripId={}",
                    reservation.getId(), person.getId(), trip.getId());

            // Notifier le conducteur : réservation réactivée
            emailService.sendReservationToggledNotification(trip.getDriver(), person, trip, true);
        } else {
            reservation = Reservation.builder()
                    .trip(trip)
                    .person(person)
                    .reservationStatus(confirmedStatus)
                    .build();
            reservation = reservationRepository.save(reservation);
            log.info("Réservation créée : id={}, personId={}, tripId={}",
                    reservation.getId(), person.getId(), trip.getId());

            // Notifier le conducteur : nouvelle réservation
            emailService.sendReservationToggledNotification(trip.getDriver(), person, trip, true);
        }

        trip.setAvailableSeats(trip.getAvailableSeats() - 1);
        tripRepository.save(trip);

        return reservationMapper.toResponse(reservation);
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

        emailService.sendReservationToggledNotification(trip.getDriver(), reservation.getPerson(), trip, false);

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
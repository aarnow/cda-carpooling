package com.cda.carpooling.scheduler;

import com.cda.carpooling.entity.Trip;
import com.cda.carpooling.entity.TripStatus;
import com.cda.carpooling.repository.TripRepository;
import com.cda.carpooling.repository.TripStatusRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Tâche planifiée pour mettre à jour automatiquement les statuts des trajets.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TripStatusScheduler {

    private final TripRepository tripRepository;
    private final TripStatusRepository tripStatusRepository;

    /**
     * Exécuté toutes les 5 minutes pour marquer les trajets terminés.
     */
    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void updateCompletedTrips() {
        log.debug("Vérification des trajets terminés...");

        TripStatus completedStatus = tripStatusRepository.findByLabel(TripStatus.COMPLETED)
                .orElseThrow(() -> new IllegalStateException("Statut COMPLETED introuvable"));

        List<Trip> tripsToComplete = tripRepository.findAllByTripStatusLabel(TripStatus.PLANNED)
                .stream()
                .filter(trip -> isTripCompleted(trip))
                .toList();

        if (tripsToComplete.isEmpty()) {
            log.debug("Aucun trajet à marquer comme terminé");
            return;
        }

        // Mettre à jour les statuts
        tripsToComplete.forEach(trip -> {
            trip.setTripStatus(completedStatus);
            tripRepository.save(trip);
            log.info("Trajet {} marqué COMPLETED (départ: {})",
                    trip.getId(), trip.getTripDatetime());
        });

        log.info("{} trajet(s) marqué(s) comme COMPLETED", tripsToComplete.size());
    }

    /**
     * Détermine si un trajet est terminé.
     */
    private boolean isTripCompleted(Trip trip) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime tripEnd;

        if (trip.getDurationMinutes() != null) {
            tripEnd = trip.getTripDatetime().plusMinutes(trip.getDurationMinutes());
        } else {
            tripEnd = trip.getTripDatetime().plusHours(2);
        }

        return now.isAfter(tripEnd);
    }
}
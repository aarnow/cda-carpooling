package com.cda.carpooling.specification;

import com.cda.carpooling.entity.Trip;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TripSpecification {

    public static Specification<Trip> hasDate(LocalDate date) {
        return (root, query, cb) -> date == null ? null :
                cb.equal(cb.function("DATE", LocalDate.class, root.get("tripDatetime")), date);
    }

    public static Specification<Trip> hasDepartureCity(String cityName) {
        return (root, query, cb) -> cityName == null ? null :
                cb.equal(root.get("departureAddress").get("city").get("name"), cityName);
    }

    public static Specification<Trip> hasArrivingCity(String cityName) {
        return (root, query, cb) -> cityName == null ? null :
                cb.equal(root.get("arrivingAddress").get("city").get("name"), cityName);
    }

    /**
     * Filtre par trajets futurs (non passés).
     * Si isUpcoming == true, retourne uniquement les trajets dont la date est >= maintenant.
     * Si isUpcoming == false ou null, pas de filtre.
     */
    public static Specification<Trip> isUpcoming(Boolean isUpcoming) {
        return (root, query, cb) -> {
            if (isUpcoming == null || !isUpcoming) {
                return null;  // Pas de filtre
            }
            // Retourne uniquement les trajets futurs
            return cb.greaterThanOrEqualTo(root.get("tripDatetime"), LocalDateTime.now());
        };
    }
}
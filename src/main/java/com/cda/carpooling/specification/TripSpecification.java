package com.cda.carpooling.specification;

import com.cda.carpooling.entity.Trip;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

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
}